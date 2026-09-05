package com.jjw.blackscreen.ui

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import com.jjw.blackscreen.data.Gesture

private const val HoldMillis = 1_500f

/** 밀어 올려 해제하는 데 필요한 거리. 화면 높이 대비 비율이라 기기를 안 탄다. */
private const val SlideDismissFraction = 0.28f
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

                    // 밀어 올리기는 화면이 손가락을 따라 움직여야 해서 표면 전체를
                    // 다뤄야 한다. BlackScreenRoot 의 SlideToDismissLayer 가 맡는다.
                    Gesture.SWIPE -> Unit
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


/**
 * 밀어 올려 해제.
 *
 * 다른 제스처와 달리 **화면 표면 자체가 손가락을 따라 움직인다.** 비워진 자리로 아래 앱이
 * 드러나므로 창이 반투명이어야 한다(`PixelFormat.TRANSLUCENT`).
 *
 * 손을 뗐을 때 [SlideDismissFraction] 을 넘겼으면 마저 밀어내고 해제하고, 모자라면
 * 제자리로 되돌린다. 되돌아오는 동작이 있어야 "얼마나 더 올려야 하는지" 를 알 수 있다.
 *
 * 여기서 쓰는 `animate()` 는 컴포지션 스코프라 프레임 클럭이 있다 —
 * 서비스의 `lifecycleScope` 에서 부르면 크래시한다.
 */
@Composable
fun BoxScope.SlideToDismissLayer(
    onOffsetChange: (Float) -> Unit,
    containerHeight: Int,
    onUnlock: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(containerHeight) {
                // 드래그 중 누적치는 지역 변수로 둔다. 컴포지션 값을 캡처하면
                // 콜백이 옛 값을 보게 된다.
                var offset = 0f

                detectVerticalDragGestures(
                    onDragStart = {
                        offset = 0f
                        onOffsetChange(0f)
                    },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        // 위로만 민다. 아래로 끌어도 제자리를 넘지 않는다.
                        offset = (offset + dy).coerceAtMost(0f)
                        onOffsetChange(offset)
                    },
                    onDragEnd = {
                        val from = offset
                        val enough = containerHeight > 0 &&
                            -from >= containerHeight * SlideDismissFraction
                        scope.launch {
                            if (enough) {
                                animate(
                                    initialValue = from,
                                    targetValue = -containerHeight.toFloat(),
                                    animationSpec = tween(durationMillis = 160),
                                ) { v, _ -> onOffsetChange(v) }
                                onUnlock()
                            } else {
                                animate(
                                    initialValue = from,
                                    targetValue = 0f,
                                    animationSpec = spring(),
                                ) { v, _ -> onOffsetChange(v) }
                            }
                        }
                    },
                    onDragCancel = {
                        val from = offset
                        scope.launch {
                            animate(from, 0f, animationSpec = spring()) { v, _ ->
                                onOffsetChange(v)
                            }
                        }
                    },
                )
            },
    )
}
