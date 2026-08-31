package com.jjw.blackscreen.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.jjw.blackscreen.blackout.BlackoutActivity
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.overlay.OverlayLauncherActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 퀵 설정 타일 — 실사용에서 가장 중요한 진입점.
 *
 * 앱을 열어 버튼을 누르는 것보다 상태바를 내려 타일 한 번 탭하는 쪽이 훨씬 자연스럽다.
 */
class BlackScreenTileService : TileService() {

    private var scope: CoroutineScope? = null

    // onClick 은 suspend 가 아니므로 DataStore 를 그 자리에서 읽을 수 없다.
    // 타일이 보이기 시작할 때 미리 읽어 둔다.
    private var mode: Mode = Mode.BLACKOUT

    override fun onStartListening() {
        super.onStartListening()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        newScope.launch {
            mode = SettingsRepository(this@BlackScreenTileService).settings.first().mode
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val target = when (mode) {
            Mode.BLACKOUT -> BlackoutActivity::class.java
            // 오버레이는 중계 Activity 를 거친다 — OverlayLauncherActivity 주석 참조.
            Mode.OVERLAY -> OverlayLauncherActivity::class.java
        }

        val intent = Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34 부터 Intent 오버로드는 UnsupportedOperationException 을 던진다.
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            // API 33 이하에는 PendingIntent 오버로드가 없다. 위 분기로 34+ 는 걸러지므로
            // 이 호출이 실제로 예외를 던지는 경로는 존재하지 않는다.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
