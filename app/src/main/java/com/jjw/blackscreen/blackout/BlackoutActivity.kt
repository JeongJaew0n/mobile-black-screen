package com.jjw.blackscreen.blackout

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.ui.BlackScreenRoot

/**
 * Blackout 모드 — 풀스크린 Activity 로 화면 전체를 덮는다.
 *
 * 오버레이와 달리 상태바·내비게이션바까지 완전히 숨길 수 있어 "화면이 꺼진" 착시가
 * 깨지지 않는다. 대신 직전 최상단 앱의 UI 렌더링은 멈춘다.
 * (음악 재생·다운로드·내비게이션 등 백그라운드 동작은 그대로 유지된다.)
 */
class BlackoutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = SettingsRepository(this)

        applyBlackoutWindowFlags()

        // 뒤로 가기로 풀리면 차폐의 의미가 없다. 해제는 제스처로만 한다.
        onBackPressedDispatcher.addCallback(this) { /* 의도적으로 무시 */ }

        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(
                initialValue = Settings(),
            )
            BlackScreenRoot(settings = settings, onUnlock = ::finish)
        }
    }

    private fun applyBlackoutWindowFlags() {
        // 시스템 바를 완전히 숨긴다. BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 를 쓰면
        // 가장자리 스와이프 시 잠깐 나타났다 사라진다 — 바가 계속 남는 기본 동작보다 낫다.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // 이 창이 앞에 있는 동안만 밝기를 0 으로 덮어쓴다. 시스템 밝기 설정은 건드리지 않으므로
        // 화면을 벗어나면 자동으로 원래 밝기로 돌아간다.
        window.attributes = window.attributes.apply { screenBrightness = 0f }

        // 화면이 꺼지면 표시 중인 시계·문장도 같이 사라지므로 자동 꺼짐을 막는다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
