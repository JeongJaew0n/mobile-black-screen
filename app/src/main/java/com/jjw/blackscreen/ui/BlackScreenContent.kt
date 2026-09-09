package com.jjw.blackscreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjw.blackscreen.data.Gesture
import com.jjw.blackscreen.data.Settings
import kotlin.math.roundToInt

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
    var slideOffset by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height },
    ) {
        // 검은 표면 전체가 함께 움직여야 비워진 자리로 아래 앱이 드러난다.
        // 배경을 바깥 Box 에 두면 밀어도 검정이 그대로 남는다.
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, slideOffset.roundToInt()) }
                .background(Color.Black),
        ) {
            BlackScreenContent(settings)
        }

        if (settings.unlockGesture == Gesture.SWIPE) {
            SlideToDismissLayer(
                onOffsetChange = { slideOffset = it },
                containerHeight = containerHeight,
                onUnlock = onUnlock,
            )
        } else if (settings.unlockGesture == Gesture.SLIDE_TO_UNLOCK) {
            SlideToUnlockLayer(
                tint = settings.textColor,
                onUnlock = onUnlock,
            )
        } else {
            UnlockGestureLayer(
                gesture = settings.unlockGesture,
                ringColor = settings.textColor,
                holdMillis = settings.holdMillis,
                onUnlock = onUnlock,
            )
        }
    }
}

/** 세로 -0.3 = 정중앙에서 위쪽으로 화면 높이의 15% 지점. */
private val ContentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/** 미리보기 상자 높이. 시계(64sp)와 문장 한 줄이 들어가는 최소치. */
private val PreviewHeight = 180.dp

/**
 * 설정 화면용 미리보기. **차폐 화면과 같은 [BlackScreenContent] 를 그대로 그린다.**
 *
 * 폰트 크기·정렬·번인 궤도까지 실제와 같은 코드다 — 미리보기 전용 레이아웃을 따로 두면
 * 실제와 어긋나는 날이 온다. 다른 것은 상자 높이와 둥근 모서리뿐이다.
 *
 * ⚠️ 패널 밝기(0.30)는 보여줄 수 없다. 이 앱의 창 밝기는 `screenBrightness` 로 낮추는데
 * 설정 화면 창을 낮추면 설정 자체가 안 보인다. 실제 화면은 이보다 어둡다 — 설정 화면이
 * 그 사실을 캡션으로 알린다.
 */
@Composable
fun BlackScreenPreview(settings: Settings, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
    ) {
        BlackScreenContent(settings)
    }
}

@Composable
private fun BlackScreenContent(settings: Settings) {
    // 기본값은 요구사항대로 완전한 무표시다. 이때는 텍스트 레이아웃 자체를 만들지 않는다.
    if (!settings.hasContent) return

    val offset = rememberBurnInShift()

    // 정중앙보다 조금 위. 손에 들었을 때 시선이 자연스럽게 닿는 높이다.
    Box(Modifier.fillMaxSize(), contentAlignment = ContentAlignment) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset { offset },
        ) {
            if (settings.showClock) {
                Text(
                    text = rememberClockText(settings.clockStyle),
                    color = settings.textColor,
                    fontSize = 64.sp,
                    fontFamily = settings.clockFont.family,
                    fontWeight = settings.clockFont.weight,
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
