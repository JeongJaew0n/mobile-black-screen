package com.jjw.blackscreen

import android.content.Context
import android.content.Intent
import android.widget.Toast
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

    fun start(context: Context, mode: Mode): Result = when (Mode.sanitize(mode)) {
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

    /**
     * 차폐를 푼다. 지문 프롬프트처럼 **차폐 창 바깥**에서 해제할 때 쓴다 — 창 안의 제스처는
     * 자기 창의 `onUnlock` 을 직접 부른다. 제공 모드가 FULL 뿐이라 접근성 창만 내린다.
     */
    fun stop(context: Context) {
        ScreenOffAccessibilityService.instance?.hideCover()
        if (Mode.OVERLAY.available) ScreenCoverService.stopCover(context)
    }

    /**
     * 접근성 설정을 연다. **왜 열리는지 먼저 말한다** — 타일·버블에서 켰는데 설정 화면이 불쑥 뜨면
     * 고장으로 보인다. 재설치·강제 종료 뒤 접근성이 꺼져 흔히 겪는다.
     */
    fun openAccessibilitySettings(context: Context) {
        Toast.makeText(context, R.string.toast_accessibility_needed, Toast.LENGTH_LONG).show()
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
