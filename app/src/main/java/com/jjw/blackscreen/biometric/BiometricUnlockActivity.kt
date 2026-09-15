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

    private companion object {
        /**
         * 다시 시도해도 소용없는 오류들 — 하드웨어 없음/사용 불가, 등록된 생체 없음,
         * 영구 잠금. `BiometricPrompt.BIOMETRIC_ERROR_*` 와 같은 값이며 API 28 에서도 동일하다
         * (상수가 public 이 된 것은 API 29 부터라 값으로 적는다).
         */
        val UNRECOVERABLE_ERRORS = setOf(
            1,  // HW_UNAVAILABLE
            9,  // LOCKOUT_PERMANENT
            11, // NO_BIOMETRICS
            12, // HW_NOT_PRESENT
        )
    }

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
        // **기기 PIN/패턴 대체를 허용한다.** 지문만 허용했더니 인식이 계속 실패할 때 빠져나올
        // 길이 없어 차폐에 갇혔다(실제로 겪음). 지문이 주 수단이고 PIN 은 표준 탈출구다.
        // PIN 화면이 차폐 아래 가려지는 문제는 [ScreenOffAccessibilityService.setBiometricPromptActive]
        // 가 인증 중 차폐를 거의 투명하게 + 터치 통과로 바꿔서 푼다.
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                // DEVICE_CREDENTIAL 을 허용하면 setNegativeButton 을 쓸 수 없다(프레임워크가 예외).
                // 시스템이 그 자리에 'PIN 사용' 버튼을 직접 넣는다.
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)

            // API 28 은 PIN 대체가 없다. 취소 버튼만 두고, 아래 onAuthenticationError 의
            // 안전망(복구 불가 오류면 차폐 해제)이 갇힘을 막는다.
            else -> builder.setNegativeButton(
                getString(R.string.biometric_cancel),
                mainExecutor,
            ) { _, _ -> finish() }
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
                    // 사용자가 취소한 것이면 차폐를 그대로 둔다(정상 경로).
                    // 그러나 **인증 수단 자체가 불가능한 오류**면 차폐를 풀어 준다 — 안 그러면
                    // 화면을 되찾을 방법이 없다. 갇히는 것보다 열리는 쪽이 낫다.
                    if (errorCode in UNRECOVERABLE_ERRORS) {
                        ScreenOff.stop(this@BiometricUnlockActivity)
                    }
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
