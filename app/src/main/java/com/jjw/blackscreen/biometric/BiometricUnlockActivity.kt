package com.jjw.blackscreen.biometric

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.jjw.blackscreen.R
import com.jjw.blackscreen.ScreenOff
import com.jjw.blackscreen.accessibility.ScreenOffAccessibilityService

/**
 * 시스템 지문 프롬프트를 띄우는 투명 Activity. 인식되면 차폐를 풀고 끝난다.
 *
 * 프롬프트는 Activity 토큰이 있어야 안전하게 뜬다 — 접근성 서비스 컨텍스트에서 직접 부르는
 * 것은 버전마다 동작이 갈려 쓰지 않는다. 화면에는 아무것도 그리지 않는다(투명 테마).
 *
 * 프롬프트가 떠 있는 동안 [ScreenOffAccessibilityService.setBiometricPromptActive] 로 차폐를
 * 반투명으로 낮춘다. 프롬프트는 접근성 오버레이 아래에 그려져서, 그냥 두면 사용자가
 * 아이콘을 볼 수 없다. 센서 자체는 창과 무관하게 손가락을 읽는다.
 *
 * `androidx.biometric` 을 쓰지 않는 이유: appcompat·fragment 를 함께 끌고 온다. 이 앱은 순수
 * Compose 이고 대상 기기는 API 28 을 훨씬 넘는다. 프레임워크 API 로 충분하다.
 */
class BiometricUnlockActivity : Activity() {

    private val cancel = CancellationSignal()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            finish()
            return
        }
        authenticate()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticate() {
        val builder = BiometricPrompt.Builder(this)
            .setTitle(getString(R.string.biometric_title))
            .setNegativeButton(getString(R.string.biometric_cancel), mainExecutor) { _, _ -> finish() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 지문·얼굴 등 약한 생체까지. 기기 PIN 으로 대체하는 것은 허용하지 않는다 —
            // "지문으로" 푸는 기능이고, PIN 화면은 차폐 아래에 가려져 조작할 수 없다.
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        }
        ScreenOffAccessibilityService.instance?.setBiometricPromptActive(true)
        builder.build().authenticate(
            cancel,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    ScreenOff.stop(this@BiometricUnlockActivity)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 취소·시간 초과·잠금(시도 초과) 전부 여기로. 차폐는 그대로 두고 물러난다.
                    finish()
                }

                // onAuthenticationFailed(한 번 안 맞음)는 프롬프트가 알아서 다시 기다린다.
            },
        )
    }

    override fun onDestroy() {
        ScreenOffAccessibilityService.instance?.setBiometricPromptActive(false)
        if (!cancel.isCanceled) cancel.cancel()
        super.onDestroy()
    }
}
