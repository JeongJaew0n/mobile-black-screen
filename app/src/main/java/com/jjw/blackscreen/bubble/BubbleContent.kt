package com.jjw.blackscreen.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 이 시간 동안 손대지 않으면 흐려진다. */
private const val IDLE_DELAY_MS = 3_000L
private const val IDLE_ALPHA = 0.4f

/**
 * 상주 버블.
 *
 * 다른 앱 위에 계속 떠 있으므로 눈에 덜 띄어야 한다. 어두운 반투명 원에 얇은 테두리만 두고,
 * 안에는 차폐를 뜻하는 가로 막대 하나만 넣는다(퀵 설정 타일 아이콘과 같은 모티프).
 *
 * 탭과 드래그는 별도의 `pointerInput` 으로 나눠 붙인다. 드래그 감지기는 터치 슬롭을
 * 넘겨야 발동하므로, 짧게 누른 것은 탭 감지기로 간다.
 */
@Composable
fun BubbleContent(
    sizeDp: Int,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    // 상호작용이 있을 때마다 증가시켜 유휴 타이머를 되감는다.
    var touchTick by remember { mutableIntStateOf(0) }
    var idle by remember { mutableStateOf(false) }

    LaunchedEffect(touchTick) {
        idle = false
        delay(IDLE_DELAY_MS)
        idle = true
    }

    // 유휴 상태 전환은 알파 애니메이션 한 번으로 끝낸다. 상시 프레임 루프를 돌리면
    // 다른 앱 위에서 계속 전력을 먹는다 (차폐 화면의 번인 시프트와 같은 이유).
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (idle) IDLE_ALPHA else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "bubbleAlpha",
    )

    Box(
        Modifier
            .size(sizeDp.dp)
            .alpha(bubbleAlpha)
            .clip(CircleShape)
            .background(Color(0xE0141414))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { touchTick++; onTap() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { touchTick++; onDragStart() },
                    onDrag = { change, delta -> change.consume(); onDrag(delta) },
                    onDragEnd = { touchTick++; onDragEnd() },
                    onDragCancel = { touchTick++; onDragEnd() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // 막대 길이는 버블 크기에 비례시킨다. 고정하면 큰 버블에서 초라해 보인다.
        Box(
            Modifier
                .width((sizeDp * 0.35f).dp)
                .height(2.dp)
                .background(Color(0xB3FFFFFF)),
        )
    }
}
