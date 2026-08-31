package com.jjw.blackscreen.overlay

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jjw.blackscreen.R
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.data.SettingsRepository

/**
 * Overlay 모드 — 다른 앱 위에 전체 화면 검은 창을 띄운다.
 *
 * Blackout 모드와 달리 아래 앱이 계속 렌더링된다. 대신 상태바는 덮이지 않는다
 * ([overlayLayoutParams] 주석 참조).
 *
 * 창이 살아 있으려면 프로세스가 죽지 않아야 하므로 포그라운드 서비스로 돌린다.
 * 용도가 기존 FGS 타입 어디에도 맞지 않아 `specialUse` 를 쓴다.
 */
class OverlayService :
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

    private var overlayView: View? = null

    override fun onCreate() {
        // ★ super.onCreate() 보다 먼저 호출해야 한다.
        //   super 가 라이프사이클을 CREATED 로 옮긴 뒤에 복원하면 예외가 난다.
        savedStateController.performRestore(null)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        // 권한이 도중에 회수되면 addView 가 던진다. 조용히 죽는 편이 크래시보다 낫다.
        if (!AndroidSettings.canDrawOverlays(this) || !showOverlay()) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        viewModelStore.clear()
        super.onDestroy()
    }

    private fun showOverlay(): Boolean {
        if (overlayView != null) return true

        val repository = SettingsRepository(this)
        val view = ComposeView(this).apply {
            // ComposeView 는 이 세 owner 를 ViewTree 에서 찾는다. Activity 가 아닌
            // Service 의 창에는 없으므로 직접 붙여야 한다.
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)

            setContent {
                val settings by repository.settings.collectAsStateWithLifecycle(
                    initialValue = Settings(),
                )
                com.jjw.blackscreen.ui.BlackScreenRoot(
                    settings = settings,
                    onUnlock = { stopSelf() },
                )
            }
        }

        return runCatching {
            windowManager.addView(view, overlayLayoutParams())
            overlayView = view
        }.isSuccess
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        overlayView = null
        // 이미 제거되었거나 창이 사라진 상태일 수 있다. 여기서 던지면 서비스가 크래시한다.
        runCatching { windowManager.removeView(view) }
    }

    private fun startAsForeground() {
        createChannel()

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.overlay_running))
            .setContentText(getString(R.string.overlay_running_desc))
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.overlay_stop),
                    stopIntent,
                ).build(),
            )
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel),
            // 차폐 중인 것은 알려야 하지만 소리나 헤드업으로 방해해서는 안 된다.
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.jjw.blackscreen.STOP_OVERLAY"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }
    }
}
