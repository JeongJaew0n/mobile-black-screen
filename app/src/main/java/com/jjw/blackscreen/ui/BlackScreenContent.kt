package com.jjw.blackscreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjw.blackscreen.data.Settings

/**
 * Blackout 모드와 Overlay 모드가 공유하는 차폐 화면.
 *
 * 이 Composable 은 Activity 창과 Service 가 WindowManager 에 붙인 창 양쪽에서 그대로
 * 재사용된다. 두 모드의 차이는 "어떤 창에 올리는가"뿐이고 표시 내용은 완전히 동일하다.
 */
@Composable
fun BlackScreenRoot(
    settings: Settings,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        BlackScreenContent(settings)
        UnlockGestureLayer(
            gesture = settings.unlockGesture,
            ringColor = settings.textColor,
            onUnlock = onUnlock,
        )
    }
}

/** 세로 -0.3 = 정중앙에서 위쪽으로 화면 높이의 15% 지점. */
private val ContentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

@Composable
private fun BlackScreenContent(settings: Settings) {
    // 기본값은 요구사항대로 완전한 무표시다. 이때는 텍스트 레이아웃 자체를 만들지 않는다.
    if (!settings.hasContent) return

    val offset = rememberBurnInShift(settings.burnInShiftEnabled)

    // 정중앙보다 조금 위. 손에 들었을 때 시선이 자연스럽게 닿는 높이다.
    Box(Modifier.fillMaxSize(), contentAlignment = ContentAlignment) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset { offset },
        ) {
            if (settings.showClock) {
                Text(
                    text = rememberClockText(settings.clockFormat),
                    color = settings.textColor,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                )
            }
            if (settings.sentence.isNotBlank()) {
                Text(
                    text = settings.sentence,
                    color = settings.textColor,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .padding(top = if (settings.showClock) 24.dp else 0.dp),
                )
            }
        }
    }
}
