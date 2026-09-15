package com.jjw.blackscreen.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import androidx.compose.runtime.mutableStateOf
import com.jjw.blackscreen.data.CONTENT_BRIGHTNESS
import com.jjw.blackscreen.media.MediaPermission
import com.jjw.blackscreen.media.MediaWatcher
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.ui.BlackScreenRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 화면 전체를 덮는 창을 올리기 위한 접근성 서비스.
 *
 * **이 서비스가 존재하는 유일한 이유는 `TYPE_ACCESSIBILITY_OVERLAY` 를 쓰기 위해서다.**
 * 일반 오버레이(`TYPE_APPLICATION_OVERLAY`)는 Android 8(O)부터 시스템 UI 위에 그리는 것이
 * 금지되어 상태바와 내비게이션 바를 덮지 못한다. 접근성 오버레이는 그 위까지 덮는다.
 *
 * 화면 내용은 읽지 않는다(`canRetrieveWindowContent=false`). 이벤트도 쓰지 않는다.
 *
 * `ComposeView` 를 올리려면 Lifecycle / SavedStateRegistry / ViewModelStore owner 가
 * 필요한데 `AccessibilityService` 에는 없으므로 직접 구현한다.
 * `LifecycleService` 와 달리 여기서는 **RESUMED 까지 직접 올린다** — 애니메이션을
 * 구동하는 프레임 클럭이 그 상태를 본다.
 */
class ScreenOffAccessibilityService :
    AccessibilityService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val windowManager: WindowManager by lazy {
        getSystemService(WindowManager::class.java)
    }
    private val repository: SettingsRepository by lazy { SettingsRepository(this) }

    private var scope: CoroutineScope? = null

    /**
     * 마지막으로 읽은 설정.
     *
     * 창을 붙이는 시점에 밝기를 알아야 하는데 [showCover] 는 suspend 가 아니다.
     * 수집기가 갱신해 두는 이 값을 초기 파라미터에 쓴다 — 없으면 첫 표시에서
     * 밝기 오버라이드가 걸리지 않아 시스템 밝기 그대로 뜬다.
     */
    @Volatile
    private var latestSettings: Settings = Settings()
    private var coverView: View? = null
    private var coverParams: WindowManager.LayoutParams? = null

    /**
     * 지문 프롬프트가 떠 있는 동안 true. 차폐를 반투명으로 낮추고 밝기를 올려 SystemUI 의
     * 지문 아이콘이 비쳐 보이게 한다 — 프롬프트는 접근성 오버레이 **아래**에 그려진다.
     */
    private val biometricPromptActive = mutableStateOf(false)

    /**
     * 재생 바용 세션 관찰. **차폐가 떠 있고 설정이 켜져 있을 때만** 산다 — [syncMediaWatcher].
     * 밝기는 설정값만으로 못 정한다: 재생 중이거나 권한 안내를 띄울 때도 올라가야 한다.
     * 그래서 [effectiveBrightness] 가 설정·재생 상태·프롬프트를 합친다.
     */
    private val mediaWatcher by lazy { MediaWatcher(this) }
    private val mediaPermissionMissing = mutableStateOf(false)

    val isCovering: Boolean get() = coverView != null

    override fun onCreate() {
        // ★ 라이프사이클이 CREATED 로 옮겨가기 전에 복원해야 한다.
        savedStateController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        // 밝기 설정이 바뀌면 떠 있는 창에 즉시 반영한다.
        newScope.launch {
            repository.settings.collect { settings ->
                latestSettings = settings
                syncMediaWatcher()
                applyBrightness()
            }
        }
        // 재생 상태가 바뀌면 밝기도 따라간다 — 곡이 끝나 세션이 사라지면 다시 0 으로.
        newScope.launch { mediaWatcher.nowPlaying.collect { applyBrightness() } }
        newScope.launch {
            mediaWatcher.permissionLost.collect { lost ->
                if (lost) mediaPermissionMissing.value = true
                applyBrightness()
            }
        }
    }

    override fun onDestroy() {
        hideCover()
        scope?.cancel()
        scope = null
        instance = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        super.onDestroy()
    }

    /** 이벤트는 쓰지 않는다. 오버레이 자격만 필요하다. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    // ------------------------------------------------------------------ 창

    fun showCover() {
        if (coverView != null) return

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScreenOffAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@ScreenOffAccessibilityService)
            setViewTreeViewModelStoreOwner(this@ScreenOffAccessibilityService)
            setContent {
                val settings by repository.settings.collectAsStateWithLifecycle(
                    initialValue = Settings(),
                )
                val nowPlaying by mediaWatcher.nowPlaying.collectAsStateWithLifecycle()
                BlackScreenRoot(
                    settings = settings,
                    onUnlock = ::hideCover,
                    dimmed = biometricPromptActive.value,
                    nowPlaying = nowPlaying,
                    mediaPermissionMissing = mediaPermissionMissing.value,
                    onMediaPlayPause = mediaWatcher::playPause,
                    onMediaNext = mediaWatcher::skipNext,
                    onMediaPrevious = mediaWatcher::skipPrevious,
                )
            }
        }

        val params = coverLayoutParams()
        runCatching {
            windowManager.addView(view, params)
            coverView = view
            coverParams = params
        }
        syncMediaWatcher()
        applyBrightness()
    }

    /** 차폐가 떠 있고 설정이 켜져 있으면 관찰, 아니면 중단. 권한이 없으면 안내 상태로. */
    private fun syncMediaWatcher() {
        val want = coverView != null && latestSettings.showMedia
        if (!want) {
            mediaWatcher.stop()
            mediaPermissionMissing.value = false
            return
        }
        val granted = MediaPermission.isGranted(this)
        mediaPermissionMissing.value = !granted
        if (granted) mediaWatcher.start() else mediaWatcher.stop()
    }

    /**
     * 실제로 걸 밝기. 설정([Settings.screenBrightness])만으로는 부족하다 — 재생 중이거나 권한
     * 안내를 띄울 때, 지문 프롬프트가 떠 있을 때도 올려야 한다. 밝기를 정하는 곳은 여기 하나다.
     */
    private fun effectiveBrightness(): Float {
        val mediaVisible = latestSettings.showMedia &&
            (mediaWatcher.nowPlaying.value != null || mediaPermissionMissing.value)
        val base = if (mediaVisible) CONTENT_BRIGHTNESS else latestSettings.screenBrightness
        return if (biometricPromptActive.value) maxOf(base, CONTENT_BRIGHTNESS) else base
    }

    private fun applyBrightness() {
        val params = coverParams ?: return
        val target = effectiveBrightness()
        if (params.screenBrightness == target) return
        params.screenBrightness = target
        runCatching { windowManager.updateViewLayout(coverView, params) }
    }

    fun setBiometricPromptActive(active: Boolean) {
        biometricPromptActive.value = active
        // 프롬프트가 보여야 하니 표시 내용이 없어도 밝기를 올린다. 끝나면 원래 규칙으로.
        applyBrightness()
    }

    fun hideCover() {
        val view = coverView ?: return
        coverView = null
        coverParams = null
        mediaWatcher.stop()
        mediaPermissionMissing.value = false
        runCatching { windowManager.removeView(view) }
    }

    /**
     * 화면 전체 크기를 **명시적으로** 지정한다.
     *
     * `MATCH_PARENT` 는 시스템 바를 제외한 부모 프레임에 맞춰지므로 위아래가 남는다.
     * 실제 디스플레이 크기를 직접 넣고 `FLAG_LAYOUT_NO_LIMITS` 로 경계를 푼다.
     */
    private fun coverLayoutParams(): WindowManager.LayoutParams {
        val (width, height) = displaySize()
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            // 밀어 올리기 해제에서 비워진 자리로 아래 앱이 드러나야 하므로 반투명이다.
            // 불투명이면 컴포지터가 아래 레이어를 건너뛸 수 있어 전력에 유리하지만,
            // 검정을 밀어도 그 자리에 또 검정만 보인다.
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            screenBrightness = effectiveBrightness()
            // 노치 영역까지 덮는다. ALWAYS 상수는 API 30 부터다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun displaySize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = windowManager.currentWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = android.util.DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }

    companion object {
        /**
         * 서비스는 사용자가 설정에서 켤 때만 살아 있다. 꺼져 있으면 null.
         *
         * 접근성 서비스는 시스템이 생성·소멸을 관장해서 외부에서 바인딩할 수단이 없다.
         * 정적 참조가 표준 접근이며, [onDestroy] 에서 반드시 null 로 되돌린다.
         */
        @Suppress("StaticFieldLeak")
        @Volatile
        var instance: ScreenOffAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
