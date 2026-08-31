package com.jjw.blackscreen.overlay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.jjw.blackscreen.MainActivity
import com.jjw.blackscreen.service.ScreenCoverService

/**
 * 오버레이 서비스를 띄우고 곧바로 사라지는 중계 Activity.
 *
 * 포그라운드 서비스를 백그라운드에서 시작하면 Android 15+ 에서
 * `ForegroundServiceStartNotAllowedException` 이 날 수 있다. 오버레이 예외를 쓰려면
 * 권한과 "이미 보이는 오버레이 창"이 둘 다 필요한데, 시작 시점에는 당연히 창이 없다.
 * Activity 가 앞에 있는 상태에서 시작하면 이 문제를 통째로 피할 수 있다.
 */
class OverlayLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            ScreenCoverService.startCover(this)
        } else {
            // 권한이 없으면 조용히 실패하는 대신 설정 화면으로 보낸다.
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        finish()
    }
}
