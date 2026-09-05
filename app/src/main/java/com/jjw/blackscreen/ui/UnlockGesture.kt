package com.jjw.blackscreen.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jjw.blackscreen.data.Gesture

private const val HoldMillis = 1_500f

/** 쓸어서 해제에 필요한 순 변위. 화면 폭의 절반에 가까워 우연히 나오기 어렵다. */
private val SwipeDistance = 180.dp
private const val TapWindowMillis = 400L
private val RingSize = 64.dp
private val RingStroke = 3.dp

/**
 * 차폐 해제 제스처를 받는 레이어.
 *
 * 단일 탭 해제는 주머니 안에서 그대로 풀려 버리므로 선택지에 두지 않는다.
 * 기본값인 롱프레스는 누르는 동안 원형 진행 링을 띄워 "지금 해제 중"임을 알린다.
 * 피드백이 없으면 1.5초가 고장으로 느껴진다.
 *
 * 이 레이어는 터치를 삼킨다. Overlay 모드에서 터치를 아래 앱으로 통과시키면
 * 주머니 속 오작동이 그대로 아래 앱에 전달되고, 동시에 해제 제스처를 받을
 * 방법이 사라진다.
 */
@Composable
fun BoxScope.UnlockGestureLayer(
    gesture: Gesture,
    ringColor: Color,
    onUnlock: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var pressing by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableIntStateOf(0) }

    // 롱프레스 진행률. 링이 실제로 보이는 동안에만 프레임 루프를 돌린다.
    LaunchedEffect(pressing, gesture) {
        if (!pressing || gesture != Gesture.LONG_PRESS) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (true) {
            val elapsed = withFrameMillis { it } - start
            progress = (elapsed / HoldMillis).coerceIn(0f, 1f)
            if (progress >= 1f) {
                onUnlock()
                break
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height }
            .pointerInput(gesture) {
                when (gesture) {
                    Gesture.LONG_PRESS -> detectTapGestures(
                        onPress = {
                            pressing = true
                            tryAwaitRelease()
                            pressing = false
                        },
                    )

                    Gesture.DOUBLE_TAP -> detectTapCount(required = 2, onUnlock = onUnlock)
                    Gesture.TRIPLE_TAP -> detectTapCount(required = 3, onUnlock = onUnlock)

                    Gesture.SWIPE -> detectSwipe(
                        thresholdPx = SwipeDistance.toPx(),
                        onProgress = { progress = it },
                        onUnlock = onUnlock,
                    )
                }
            },
    )

    if (progress > 0f) {
        // 화면 정중앙은 시계·문장이 쓰므로 링은 그보다 아래에 둔다.
        Canvas(
            Modifier
                .align(Alignment.Center)
                .offset { IntOffset(0, (containerHeight * 0.28f).toInt()) }
                .size(RingSize),
        ) {
            val stroke = RingStroke.toPx()
            drawArc(
                // 진행 초반에도 보이도록 알파를 살짝 앞당겨 올린다.
                color = ringColor.copy(alpha = (progress * 1.4f).coerceAtMost(1f)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
            )
        }
    }
}

/**
 * 한 방향으로 [thresholdPx] 만큼 쓸면 해제한다.
 *
 * **누적 경로가 아니라 시작점 대비 순 변위로 잰다.** 누적으로 재면 주머니 속 잔진동이
 * 쌓여서 풀린다. 진행률은 롱프레스와 같은 원형 링으로 보여준다.
 */
private suspend fun PointerInputScope.detectSwipe(
    thresholdPx: Float,
    onProgress: (Float) -> Unit,
    onUnlock: () -> Unit,
) {
    var travelled = Offset.Zero
    var fired = false
    detectDragGestures(
        onDragStart = {
            travelled = Offset.Zero
            fired = false
            onProgress(0f)
        },
        onDragEnd = { onProgress(0f) },
        onDragCancel = { onProgress(0f) },
        onDrag = { change, delta ->
            change.consume()
            travelled += delta
            val progress = (travelled.getDistance() / thresholdPx).coerceIn(0f, 1f)
            onProgress(progress)
            if (progress >= 1f && !fired) {
                fired = true
                onUnlock()
            }
        },
    )
}

/** [TapWindowMillis] 안에 [required] 번 연속으로 탭하면 해제한다. */
private suspend fun PointerInputScope.detectTapCount(
    required: Int,
    onUnlock: () -> Unit,
) {
    var count = 0
    var lastTapAt = 0L
    detectTapGestures(
        onTap = {
            val now = System.currentTimeMillis()
            count = if (now - lastTapAt <= TapWindowMillis) count + 1 else 1
            lastTapAt = now
            if (count >= required) {
                count = 0
                onUnlock()
            }
        },
    )
}
