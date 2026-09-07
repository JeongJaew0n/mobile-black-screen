package com.jjw.blackscreen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jjw.blackscreen.R
import com.jjw.blackscreen.MainActivity
import com.jjw.blackscreen.ScreenOff
import com.jjw.blackscreen.blackout.BlackoutActivity
import androidx.compose.ui.geometry.Offset
import com.jjw.blackscreen.bubble.AimTargets
import com.jjw.blackscreen.bubble.BubbleAim
import com.jjw.blackscreen.bubble.BubbleContent
import com.jjw.blackscreen.bubble.aimTargetsLayoutParams
import com.jjw.blackscreen.bubble.bubbleEdgeX
import com.jjw.blackscreen.bubble.bubbleSizePx
import com.jjw.blackscreen.bubble.bubbleLayoutParams
import com.jjw.blackscreen.bubble.placeBubble
import com.jjw.blackscreen.data.Edge
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.overlay.overlayLayoutParams
import com.jjw.blackscreen.ui.BlackScreenRoot
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 오버레이 창들을 소유하는 단일 포그라운드 서비스.
 *
 * 두 종류의 창을 관리한다.
 * - **차폐 창** : Overlay 모드에서 화면 전체를 덮는다
 * - **버블 창** : 앱을 닫아도 떠 있는 작은 원형 버튼
 *
 * 서비스를 하나로 두는 이유는 상시 알림을 하나로 유지하기 위해서다 —
 * "아무것도 없는 화면"을 지향하는 앱에서 알림 두 개는 컨셉과 어긋난다.
 *
 * 용도가 기존 FGS 타입 어디에도 맞지 않아 `specialUse` 를 쓴다.
 */
class ScreenCoverService :
    LifecycleService(),
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val windowManager: WindowManager by lazy {
        getSystemService(WindowManager::class.java)
    }

    private val repository: SettingsRepository by lazy { SettingsRepository(this) }

    private var coverView: View? = null
    private var coverParams: WindowManager.LayoutParams? = null
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var aimTargetsView: View? = null

    /** 버블이 ✕ 타겟 위에 올라와 있는가. 타겟 Composable 이 이 값을 구독한다. */
    private val aim = mutableStateOf(BubbleAim.NONE)

    /**
     * 버블 생성이 진행 중인가.
     *
     * [addBubble] 은 저장된 위치를 읽으려고 코루틴 안에서 창을 붙인다. 그 사이에 다시
     * 호출되면 `bubbleView` 가 아직 null 이라 경합으로 창이 두 개 생긴다.
     * 동기적으로 세워지는 이 플래그가 그것을 막는다.
     */
    private var bubbleAdding = false

    /**
     * 마지막으로 읽은 설정.
     *
     * 드래그 콜백과 창 배치는 suspend 가 아니라 그 자리에서 크기·가장자리를 알아야 한다.
     * 수집기가 갱신해 둔 값을 쓴다.
     */
    @Volatile
    private var latestSettings: Settings = Settings()

    /** 사용자가 버블을 켜 두었는가. 차폐 중 임시로 숨겨도 이 값은 유지된다. */
    private var bubbleWanted = false

    /** 차폐 중이라 버블을 잠시 감춰야 하는가. Blackout 모드는 Activity 라 외부에서 알려준다. */
    private var bubbleSuppressed = false

    /** 관리할 창이 하나도 없으면 서비스가 살아 있을 이유가 없다. */
    private val hasAnythingToDo: Boolean
        get() = coverView != null || bubbleWanted

    override fun onCreate() {
        // ★ super.onCreate() 보다 먼저 호출해야 한다.
        //   super 가 라이프사이클을 CREATED 로 옮긴 뒤에 복원하면 예외가 난다.
        savedStateController.performRestore(null)
        super.onCreate()

        // 시스템이 서비스를 죽였다가 되살렸을 수 있다. 저장된 설정에서 상태를 복원해
        // 서비스가 스스로 정합성을 맞추게 한다.
        lifecycleScope.launch {
            if (repository.settings.first().bubbleEnabled) {
                bubbleWanted = true
                syncBubble()
            }
        }

        // 글자 밝기 설정은 패널 밝기까지 함께 움직인다. 차폐 창이 떠 있는 동안
        // 설정이 바뀌면 즉시 반영해야 한다.
        lifecycleScope.launch {
            repository.settings.collect { settings ->
                val sizeChanged = settings.bubbleSizeDp != latestSettings.bubbleSizeDp
                latestSettings = settings

                coverParams?.let { params ->
                    if (params.screenBrightness != settings.screenBrightness) {
                        params.screenBrightness = settings.screenBrightness
                        runCatching { windowManager.updateViewLayout(coverView, params) }
                    }
                }

                // 크기가 바뀌면 창 자체는 WRAP_CONTENT 라 알아서 커지지만,
                // 오른쪽 가장자리에 붙어 있으면 x 를 다시 잡아야 화면 밖으로 밀리지 않는다.
                if (sizeChanged) bubbleParams?.let { params ->
                    val (w, h) = usableSize()
                    params.placeBubble(
                        edge = settings.bubbleEdge,
                        yRatio = settings.bubbleYRatio,
                        screenWidth = w,
                        screenHeight = h,
                        density = resources.displayMetrics.density,
                        sizeDp = settings.bubbleSizeDp,
                    )
                    runCatching { windowManager.updateViewLayout(bubbleView, params) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // startForegroundService 이후 포그라운드로 올릴 시간 제한이 있으므로
        // 어떤 액션이든 먼저 올려 두고 그 다음에 처리한다.
        goForeground()

        when (intent?.action) {
            ACTION_SHOW_BUBBLE -> {
                bubbleWanted = true
                bubbleSuppressed = false
            }
            ACTION_STOP_BUBBLE -> {
                bubbleWanted = false
                // 설정도 함께 꺼야 한다. 안 그러면 앱을 다시 열 때 되살아난다.
                // 아래에서 곧 stopSelf() 하므로 lifecycleScope 의 자식으로 두면 취소된다.
                // NonCancellable 로 서비스 수명과 떼어 놓는다.
                lifecycleScope.launch(NonCancellable) {
                    repository.update { it.copy(bubbleEnabled = false) }
                }
            }
            ACTION_SUPPRESS_BUBBLE -> bubbleSuppressed = true
            ACTION_RESTORE_BUBBLE -> bubbleSuppressed = false
            ACTION_STOP_COVER -> removeCover()
            ACTION_STOP_ALL -> {
                bubbleWanted = false
                removeCover()
            }
            else -> addCover()
        }

        syncBubble()

        if (!hasAnythingToDo) {
            stopSelf()
            return START_NOT_STICKY
        }

        updateNotification()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeCover()
        removeBubble()
        hideAimTargets()
        viewModelStore.clear()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- 차폐 창

    private fun addCover() {
        if (coverView != null) return
        if (!AndroidSettings.canDrawOverlays(this)) return

        val view = composeView { settings ->
            BlackScreenRoot(settings = settings, onUnlock = ::onCoverUnlocked)
        }
        val params = overlayLayoutParams()
        runCatching {
            windowManager.addView(view, params)
            coverView = view
            coverParams = params
        }
    }

    private fun removeCover() {
        coverView = detach(coverView)
        coverParams = null
    }

    /** 차폐를 풀어도 버블은 남아야 한다. 예전처럼 stopSelf() 로 끝내면 버블까지 사라진다. */
    private fun onCoverUnlocked() {
        removeCover()
        syncBubble()
        if (!hasAnythingToDo) stopSelf() else updateNotification()
    }

    // ---------------------------------------------------------------- 버블 창

    /** 원하는 상태와 실제 창 상태를 맞춘다. 모든 상태 전이는 이 함수 하나를 거친다. */
    private fun syncBubble() {
        val shouldShow = bubbleWanted && !bubbleSuppressed && coverView == null
        if (shouldShow) addBubble() else removeBubble()
    }

    private fun addBubble() {
        if (bubbleView != null || bubbleAdding) return
        if (!AndroidSettings.canDrawOverlays(this)) return

        bubbleAdding = true
        // 위치는 저장된 값에서 계산해야 하므로 첫 방출을 기다렸다가 붙인다.
        lifecycleScope.launch {
            try {
            val settings = repository.settings.first()
            if (!bubbleWanted || bubbleSuppressed || coverView != null) return@launch

            val view = composeView { current ->
                BubbleContent(
                    sizeDp = current.bubbleSizeDp,
                    onTap = ::onBubbleTapped,
                    onDrag = ::onBubbleDrag,
                    onDragEnd = ::onBubbleDragEnd,
                    onAimStart = ::onBubbleAimStart,
                    onAimUpdate = ::onBubbleAimUpdate,
                    onAimPick = ::onBubbleAimPick,
                )
            }
            val params = bubbleLayoutParams().apply {
                val (w, h) = usableSize()
                placeBubble(
                    edge = settings.bubbleEdge,
                    yRatio = settings.bubbleYRatio,
                    screenWidth = w,
                    screenHeight = h,
                    density = resources.displayMetrics.density,
                    sizeDp = settings.bubbleSizeDp,
                )
            }
            runCatching {
                windowManager.addView(view, params)
                bubbleView = view
                bubbleParams = params
            }
            } finally {
                bubbleAdding = false
            }
        }
    }

    private fun removeBubble() {
        bubbleView = detach(bubbleView)
        bubbleParams = null
        hideAimTargets()
    }

    // ------------------------------------------------------- 버블 드래그 / 겨냥

    private fun onBubbleDrag(delta: Offset) {
        val params = bubbleParams ?: return
        params.x += delta.x.toInt()
        params.y += delta.y.toInt()
        runCatching { windowManager.updateViewLayout(bubbleView, params) }
    }

    private fun onBubbleDragEnd() {
        snapToEdge(bubbleParams ?: return)
    }

    /**
     * 꾹 누른 채로 겨냥하는 동안 위/아래 목표를 띄운다.
     *
     * 표시 전용 창이라 터치를 받지 않는다 — 손가락은 계속 버블 창이 쥐고 있어야
     * 방향 판정이 이어진다.
     */
    private fun onBubbleAimStart() {
        aim.value = BubbleAim.NONE
        if (aimTargetsView != null) return
        val view = composeView { AimTargets(aim = aim.value) }
        runCatching {
            windowManager.addView(view, aimTargetsLayoutParams())
            aimTargetsView = view
        }
    }

    private fun onBubbleAimUpdate(next: BubbleAim) {
        aim.value = next
    }

    private fun onBubbleAimPick(picked: BubbleAim) {
        hideAimTargets()
        when (picked) {
            BubbleAim.UP -> openApp()
            BubbleAim.DOWN -> stopBubbleByUser()
            // 문턱을 못 넘겼다. 마음을 바꾼 것으로 보고 아무것도 하지 않는다.
            BubbleAim.NONE -> Unit
        }
    }

    /** 놓은 위치에서 가까운 좌/우 가장자리로 붙이고, 그 위치를 저장한다. */
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val density = resources.displayMetrics.density
        val (sw, sh) = usableSize()
        val size = bubbleSizePx(density, latestSettings.bubbleSizeDp)
        val edge = if (params.x + size / 2 < sw / 2) Edge.LEFT else Edge.RIGHT
        val targetX = bubbleEdgeX(edge, sw, density, latestSettings.bubbleSizeDp)
        val clampedY = params.y.coerceIn(0, (sh - size).coerceAtLeast(0))
        val ratio = if (sh > size) clampedY.toFloat() / (sh - size) else 0.5f

        lifecycleScope.launch {
            slideBubbleX(params, from = params.x, to = targetX, y = clampedY)
            repository.update { it.copy(bubbleEdge = edge, bubbleYRatio = ratio) }
        }
    }

    /**
     * 가장자리로 붙는 짧은 애니메이션.
     *
     * Compose 의 `animate()` 는 코루틴 컨텍스트에 `MonotonicFrameClock` 을 요구해서
     * 컴포지션 밖(서비스의 `lifecycleScope`)에서 호출하면 `IllegalStateException` 이 난다.
     * 창 좌표를 직접 갱신하는 이 경로에서는 단순 루프가 맞다.
     */
    private suspend fun slideBubbleX(
        params: WindowManager.LayoutParams,
        from: Int,
        to: Int,
        y: Int,
    ) {
        val steps = 12
        repeat(steps) { i ->
            val t = (i + 1) / steps.toFloat()
            val eased = 1f - (1f - t) * (1f - t)
            params.x = (from + (to - from) * eased).toInt()
            params.y = y
            runCatching { windowManager.updateViewLayout(bubbleView, params) }
            delay(12)
        }
    }

    private fun hideAimTargets() {
        aimTargetsView = detach(aimTargetsView)
        aim.value = BubbleAim.NONE
    }

    /** 아래로 겨냥해 지웠을 때. 설정도 함께 꺼야 앱을 다시 열 때 되살아나지 않는다. */
    private fun stopBubbleByUser() {
        bubbleWanted = false
        syncBubble()
        // 저장이 끝난 뒤에 서비스를 멈춰야 한다. 저장을 launch 로 던지고 바로 stopSelf()
        // 하면 onDestroy 가 lifecycleScope 를 취소해 저장이 실행되지 못한다 — 창은 사라졌는데
        // 설정은 true 로 남아, 앱을 다시 열면 버블이 되살아난다. 실기기에서 실제로 그랬다.
        lifecycleScope.launch {
            repository.update { it.copy(bubbleEnabled = false) }
            if (!hasAnythingToDo) stopSelf() else updateNotification()
        }
    }

    /**
     * 버블을 눌렀을 때. 설정된 모드대로 차폐를 시작한다.
     *
     * Blackout 모드는 Activity 라 서비스에서 띄워야 하는데, 이 시점에 **보이는 오버레이 창
     * (버블)이 이미 있으므로** 백그라운드 Activity 실행 제한의 예외에 해당한다.
     */
    /**
     * 위로 겨냥했을 때. 설정 화면을 연다.
     *
     * 버블만 남기고 앱을 닫아 둔 상태에서 설정을 바꾸려면 런처까지 가야 했다.
     * 백그라운드 Activity 시작 제한에 걸리지 않는 것은 이 서비스가 '다른 앱 위에 표시'
     * 권한을 가질 때만 버블을 띄우기 때문이다 — 그 권한이 면제 사유다.
     */
    private fun openApp() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun onBubbleTapped() {
        lifecycleScope.launch {
            val mode = repository.settings.first().mode

            // BLACKOUT 만 Activity 를 띄운다 — 그 동안은 버블을 감춰야 한다.
            // FULL 은 접근성 오버레이가 버블보다 위 레이어라 알아서 가려진다.
            if (mode == Mode.BLACKOUT) {
                bubbleSuppressed = true
                syncBubble()
            }

            when (ScreenOff.start(this@ScreenCoverService, mode)) {
                ScreenOff.Result.STARTED -> if (mode == Mode.OVERLAY) syncBubble()
                ScreenOff.Result.NEEDS_ACCESSIBILITY ->
                    ScreenOff.openAccessibilitySettings(this@ScreenCoverService)
                ScreenOff.Result.NEEDS_OVERLAY_PERMISSION ->
                    ScreenOff.openOverlaySettings(this@ScreenCoverService)
            }
            updateNotification()
        }
    }

    /**
     * 창을 놓을 수 있는 영역의 크기.
     *
     * 오버레이 창의 좌표는 **화면 전체가 아니라 시스템 바를 제외한 부모 프레임 기준**이다.
     * 실측상 부모 프레임은 `[0,94][1080,2214]` 였다. 전체 높이로 계산하면 버블이
     * 상태바 높이만큼 아래로 밀린다.
     */
    private fun usableSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets
                .getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
            (metrics.bounds.width() - insets.left - insets.right) to
                (metrics.bounds.height() - insets.top - insets.bottom)
        } else {
            val dm = resources.displayMetrics
            dm.widthPixels to dm.heightPixels
        }

    // ------------------------------------------------------------------ 공통

    /**
     * `ComposeView` 는 Lifecycle / SavedStateRegistry / ViewModelStore owner 를
     * ViewTree 에서 찾는다. Activity 가 아닌 Service 의 창에는 없으므로 직접 붙인다.
     */
    private fun composeView(content: @Composable (Settings) -> Unit): ComposeView =
        ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScreenCoverService)
            setViewTreeSavedStateRegistryOwner(this@ScreenCoverService)
            setViewTreeViewModelStoreOwner(this@ScreenCoverService)
            setContent {
                val settings by repository.settings.collectAsStateWithLifecycle(
                    initialValue = Settings(),
                )
                content(settings)
            }
        }

    /** 이미 제거되었거나 창이 사라진 상태일 수 있다. 여기서 던지면 서비스가 크래시한다. */
    private fun detach(view: View?): View? {
        if (view != null) runCatching { windowManager.removeView(view) }
        return null
    }

    // ------------------------------------------------------------------ 알림

    private fun goForeground() {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    /** 차폐 중이면 "해제", 버블만 떠 있으면 "버블 끄기" — 상태에 따라 다른 알림을 보여준다. */
    private fun buildNotification(): Notification {
        val covering = coverView != null || bubbleSuppressed

        val title = if (covering) R.string.overlay_running else R.string.bubble_running
        val text = if (covering) R.string.overlay_running_desc else R.string.bubble_running_desc
        val actionLabel = if (covering) R.string.overlay_stop else R.string.bubble_stop
        val action = if (covering) ACTION_STOP_ALL else ACTION_STOP_BUBBLE

        val pending = PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, ScreenCoverService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(title))
            .setContentText(getString(text))
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(null, getString(actionLabel), pending).build(),
            )
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel),
            // 상태는 알려야 하지만 소리나 헤드업으로 방해해서는 안 된다.
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_COVER = "com.jjw.blackscreen.START_COVER"
        const val ACTION_STOP_COVER = "com.jjw.blackscreen.STOP_COVER"
        const val ACTION_STOP_ALL = "com.jjw.blackscreen.STOP_ALL"
        const val ACTION_SHOW_BUBBLE = "com.jjw.blackscreen.SHOW_BUBBLE"
        const val ACTION_STOP_BUBBLE = "com.jjw.blackscreen.STOP_BUBBLE"

        /** 차폐가 화면에 뜨는 동안만 버블을 감춘다. 사용자 설정은 건드리지 않는다. */
        const val ACTION_SUPPRESS_BUBBLE = "com.jjw.blackscreen.SUPPRESS_BUBBLE"
        const val ACTION_RESTORE_BUBBLE = "com.jjw.blackscreen.RESTORE_BUBBLE"

        fun startCover(context: Context) = send(context, ACTION_START_COVER)
        fun showBubble(context: Context) = send(context, ACTION_SHOW_BUBBLE)
        fun stopBubble(context: Context) = send(context, ACTION_STOP_BUBBLE)

        /**
         * 차폐 Activity 가 뜰 때 버블을 감춘다.
         *
         * ⚠️ **버블이 꺼져 있으면 호출하면 안 된다.** `startForegroundService` 는 죽어 있는
         * 서비스를 새로 띄우므로 알림만 잠깐 떴다 사라진다. 호출부에서 확인할 것.
         */
        fun suppressBubble(context: Context) = send(context, ACTION_SUPPRESS_BUBBLE)
        fun restoreBubble(context: Context) = send(context, ACTION_RESTORE_BUBBLE)

        private fun send(context: Context, action: String) {
            context.startForegroundService(
                Intent(context, ScreenCoverService::class.java).setAction(action),
            )
        }
    }
}
