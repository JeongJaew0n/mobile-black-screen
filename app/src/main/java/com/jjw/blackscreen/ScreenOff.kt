package com.jjw.blackscreen

import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.core.net.toUri
import com.jjw.blackscreen.accessibility.ScreenOffAccessibilityService
import com.jjw.blackscreen.blackout.BlackoutActivity
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.service.ScreenCoverService

/**
 * Screen Off 를 시작하는 유일한 통로.
 *
 * 진입점이 셋(설정 화면 버튼 / 버블 / 퀵 설정 타일)이라 분기를 흩어 두면 금방 어긋난다.
 * 모드별 처리는 전부 여기 모은다.
 */
object ScreenOff {

    enum class Result {
        STARTED,

        /** [Mode.FULL] 인데 접근성 서비스가 꺼져 있다. 호출부가 설정으로 보내야 한다. */
        NEEDS_ACCESSIBILITY,

        /** [Mode.OVERLAY] 인데 '다른 앱 위에 표시' 권한이 없다. */
        NEEDS_OVERLAY_PERMISSION,
    }

    fun start(context: Context, mode: Mode): Result = when (mode) {
        Mode.FULL -> {
            val service = ScreenOffAccessibilityService.instance
            if (service == null) {
                Result.NEEDS_ACCESSIBILITY
            } else {
                service.showCover()
                Result.STARTED
            }
        }

        Mode.OVERLAY ->
            if (AndroidSettings.canDrawOverlays(context)) {
                ScreenCoverService.startCover(context)
                Result.STARTED
            } else {
                Result.NEEDS_OVERLAY_PERMISSION
            }

        // 유일하게 쓰던 앱을 뒤로 밀어내는 경로다. 권한이 하나도 없을 때의 최후 수단.
        Mode.BLACKOUT -> {
            context.startActivity(
                Intent(context, BlackoutActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            Result.STARTED
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
