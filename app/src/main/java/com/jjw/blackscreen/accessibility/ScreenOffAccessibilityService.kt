package com.jjw.blackscreen.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import androidx.compose.runtime.mutableStateOf
import com.jjw.blackscreen.data.CONTENT_BRIGHTNESS
import com.jjw.blackscreen.media.MediaPermission
import com.jjw.blackscreen.audio.MediaVolumeController
import kotlinx.coroutines.NonCancellable
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
import com.jjw.blackscreen.ui.theme.BlackScreenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

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

    private val volumeController by lazy { MediaVolumeController(this) }

    /**
     * 음소거하기 직전의 미디어 음량. null 이면 "우리가 음소거한 상태가 아님".
     *
     * DataStore 에도 같은 값을 쓴다([Settings.savedMediaVolume]) — 이건 즉시 복원용 메모리
     * 사본이고, 저장 쪽은 서비스가 죽어 복원을 못 했을 때의 보험이다.
     */
    private var mutedVolume: Int? = null

    /** 해제 안내가 떠 있는가. 떠 있는 동안만 밝기를 올린다([effectiveBrightness]). */
    private var unlockHintVisible = false

    /** 자동 끄기. 차폐를 켤 때 시작하고 풀 때 취소한다. */
    private var autoOffJob: Job? = null

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
        // hideCover() 는 창이 없으면 조기 반환하므로 여기에 맡기지 않는다 — MediaWatcher 에서
        // 이미 같은 함정을 밟았다(c161d3e).
        restoreMediaVolume()
        hideCover()
        // hideCover() 는 창이 없으면 바로 돌아가므로 정리가 건너뛰어진다. 시스템이 창을
        // 걷어간 뒤 서비스가 죽는 경로에서 리스너가 남지 않도록 여기서 한 번 더 부른다.
        mediaWatcher.stop()
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
                BlackScreenTheme {
                    BlackScreenRoot(
                        settings = settings,
                        onUnlock = ::hideCover,
                        dimmed = biometricPromptActive.value,
                        nowPlaying = nowPlaying,
                        mediaPermissionMissing = mediaPermissionMissing.value,
                        onMediaPlayPause = mediaWatcher::playPause,
                        onMediaNext = mediaWatcher::skipNext,
                        onMediaPrevious = mediaWatcher::skipPrevious,
                        onHintVisibleChange = { visible ->
                            unlockHintVisible = visible
                            applyBrightness()
                        },
                    )
                }
            }
        }

        // 시스템이 창을 걷어갈 수 있다 — PIN/패턴 확인 같은 민감 화면이 뜨면 Android 가
        // 오버레이를 강제로 떼어낸다(회피 불가, architecture.md F3). 그때 coverView 참조가
        // 남아 있으면 showCover() 가 `coverView != null` 에서 되돌아가 **다음부터 차폐가 아예
        // 뜨지 않는다.** 창이 떨어지는 순간 상태를 함께 비운다.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (coverView !== v) return   // hideCover() 가 이미 정리한 경로다
                coverView = null
                coverParams = null
                restoreMediaVolume()
                cancelAutoOff()
                unlockHintVisible = false
                // 창이 사라졌으면 세션 관찰도 멈춰야 한다. hideCover() 와 같은 정리를 해 주지
                // 않으면 "차폐가 떠 있을 때만 관찰한다" 는 원칙이 깨지고, 차폐를 다시 띄우지
                // 않는 한 세션 콜백이 등록된 채 남는다.
                syncMediaWatcher()
            }
        })

        val params = coverLayoutParams()
        runCatching {
            windowManager.addView(view, params)
            coverView = view
            coverParams = params
        }
        syncMediaWatcher()
        applyBrightness()
        applyMediaVolume()
        startAutoOff()
    }

    /**
     * 설정한 시간이 지나면 차폐를 풀고 **화면을 실제로 끈다**(잠금). 차폐만 풀면 기기 꺼짐 시간이
     * 이어받는데, 그게 길면 밝은 화면이 켜진 채 남아 타이머가 없느니만 못하다.
     *
     * 새 탈출구가 아니라 기존 탈출구에 하나를 더하는 것이다 — 서비스가 죽어 타이머가 안 돌아도
     * 해제 제스처는 그대로다. 실패 방향이 안전하다.
     */
    private fun startAutoOff() {
        cancelAutoOff()
        val minutes = latestSettings.autoOffMinutes
        if (minutes <= 0) return
        autoOffJob = scope?.launch {
            delay(minutes * 60_000L)
            autoOffJob = null
            hideCover()   // 볼륨 복원도 여기서 된다
            // 화면 끄기는 잠금뿐이다. API 27 이하는 이 동작이 없어 차폐만 푼다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        }
    }

    private fun cancelAutoOff() {
        autoOffJob?.cancel()
        autoOffJob = null
    }

    /**
     * 차폐를 켤 때 미디어 소리를 끈다. 이미 우리가 끈 상태면 아무것도 하지 않는다 —
     * 안 그러면 0 을 "원래 음량" 으로 덮어써 복원이 무의미해진다.
     */
    private fun applyMediaVolume() {
        if (mutedVolume != null) return
        val saved = volumeController.applyOnCover(latestSettings.mediaVolumeAction) ?: return
        mutedVolume = saved
        scope?.launch { repository.update { it.copy(savedMediaVolume = saved) } }
    }

    /** 차폐가 사라지는 **모든** 경로에서 불러야 한다. 하나라도 빠지면 소리가 안 돌아온다. */
    private fun restoreMediaVolume() {
        val saved = mutedVolume ?: return
        mutedVolume = null
        volumeController.restore(saved)
        // 서비스가 곧 죽을 수 있는 경로에서도 불리므로 스코프 취소에 휘둘리지 않게 한다.
        scope?.launch(NonCancellable) { repository.update { it.copy(savedMediaVolume = -1) } }
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
        // 지문 프롬프트와 해제 안내는 설정과 무관하게 읽혀야 한다. 0.30 과 0 은 실측 전력이 같다.
        return if (biometricPromptActive.value || unlockHintVisible) maxOf(base, CONTENT_BRIGHTNESS) else base
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
        val params = coverParams ?: return
        // 프롬프트가 떠 있는 동안만 차폐를 터치 통과로 바꾼다. 지문 프롬프트는 접근성 오버레이
        // **아래**에 그려지는데, 차폐가 터치를 삼키면 '취소' 버튼에 손가락이 닿지 못한다.
        // (지문 아이콘은 센서가 하드웨어로 읽어 무관하다.) NOT_TOUCHABLE 을 상시 쓰지 말라는
        // 함정은 "주머니 오작동 통과·해제 불가" 때문인데, 여기서는 프롬프트가 떠 있는 잠깐뿐이고
        // 그동안 유일한 조작 대상이 그 프롬프트라 안전하다. 끝나면 도로 삼킨다.
        params.flags = if (active) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        params.screenBrightness = effectiveBrightness()
        runCatching { windowManager.updateViewLayout(coverView, params) }
    }

    fun hideCover() {
        val view = coverView ?: return
        restoreMediaVolume()
        cancelAutoOff()
        unlockHintVisible = false
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
            private set(value) {
                field = value
                connected.value = value != null
            }

        val isEnabled: Boolean get() = instance != null

        /**
         * 연결 상태를 **관찰**하는 쪽. 재설치 직후처럼 서비스가 앱보다 몇 초 늦게 붙으면, 앱이 켤 때 한 번
         * 읽은 [isEnabled] 는 false 로 남아 켜져 있는데도 '권한이 필요합니다' 카드가 떠 있었다(실기기).
         */
        val connected = MutableStateFlow(false)
    }
}
