package com.jjw.blackscreen.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
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
                val params = coverParams ?: return@collect
                if (params.screenBrightness != settings.screenBrightness) {
                    params.screenBrightness = settings.screenBrightness
                    runCatching { windowManager.updateViewLayout(coverView, params) }
                }
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
                BlackScreenRoot(settings = settings, onUnlock = ::hideCover)
            }
        }

        val params = coverLayoutParams()
        runCatching {
            windowManager.addView(view, params)
            coverView = view
            coverParams = params
        }
    }

    fun hideCover() {
        val view = coverView ?: return
        coverView = null
        coverParams = null
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
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            screenBrightness = latestSettings.screenBrightness
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
