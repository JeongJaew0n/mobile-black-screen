package com.jjw.blackscreen.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjw.blackscreen.R
import com.jjw.blackscreen.data.Gesture
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


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
    holdMillis: Int,
    onUnlock: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var pressing by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableIntStateOf(0) }

    // 롱프레스 진행률. 링이 실제로 보이는 동안에만 프레임 루프를 돌린다.
    LaunchedEffect(pressing, gesture, holdMillis) {
        if (!pressing || gesture != Gesture.LONG_PRESS) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (true) {
            val elapsed = withFrameMillis { it } - start
            progress = (elapsed / holdMillis.toFloat()).coerceIn(0f, 1f)
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
                    // 화면을 움직이거나 트랙을 그리는 방식은 표면 전체를 다뤄야 해서
                    // BlackScreenRoot 가 별도 레이어로 처리한다.
                    Gesture.SWIPE, Gesture.SLIDE_TO_UNLOCK -> Unit
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

/** 밀어서 잠금 해제 트랙의 치수. */
private val TrackHeight = 56.dp
private val TrackKnob = 46.dp
private val TrackInset = 5.dp
private val TrackBottomMargin = 72.dp

/** 트랙 가로 비율. 넓으면 화면을 가로지르는 막대처럼 보여 답답하다. */
private const val TrackWidthFraction = 0.66f

/** 손잡이가 이 비율까지 가면 해제한다. 끝까지 딱 붙이지 않아도 되게 약간 여유를 둔다. */
private const val SlideUnlockAt = 0.92f

/**
 * 옛 아이폰식 밀어서 잠금 해제.
 *
 * 다른 제스처들은 **알려주지 않으면 알 수 없다.** 롱프레스도 3회 탭도 화면에 아무 단서가
 * 없다. 이 방식은 트랙과 손잡이가 보이므로 처음 보는 사람도 무엇을 해야 하는지 안다.
 *
 * 트랙 어디를 잡아도 끌린다. 손잡이만 잡게 하면 46dp 과녁을 맞혀야 해서 답답하다.
 * 끝까지 못 가고 놓으면 튕기듯 처음으로 돌아온다.
 *
 * 하단에 고정된 UI 라 번인이 걱정되므로 시계와 같은 궤도로 함께 움직인다.
 */
@Composable
fun BoxScope.SlideToUnlockLayer(
    tint: Color,
    burnInEnabled: Boolean,
    onUnlock: () -> Unit,
) {
    val burnInOffset = rememberBurnInShift(burnInEnabled)
    val density = LocalDensity.current
    val knobPx = with(density) { TrackKnob.toPx() }
    val insetPx = with(density) { TrackInset.toPx() }

    var trackWidth by remember { mutableIntStateOf(0) }
    var knobX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val maxTravel = (trackWidth - knobPx - insetPx * 2).coerceAtLeast(0f)
    val progress = if (maxTravel > 0f) (knobX / maxTravel).coerceIn(0f, 1f) else 0f

    val shape = RoundedCornerShape(percent = 50)

    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .offset { burnInOffset }
            .padding(bottom = TrackBottomMargin)
            .fillMaxWidth(TrackWidthFraction)
            .height(TrackHeight)
            .clip(shape)
            .border(1.dp, tint.copy(alpha = 0.22f), shape)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(maxTravel) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        knobX = (knobX + dx).coerceIn(0f, maxTravel)
                    },
                    onDragEnd = {
                        if (maxTravel > 0f && knobX >= maxTravel * SlideUnlockAt) {
                            onUnlock()
                        } else {
                            val from = knobX
                            scope.launch {
                                animate(from, 0f, animationSpec = spring()) { v, _ -> knobX = v }
                            }
                        }
                    },
                    onDragCancel = {
                        val from = knobX
                        scope.launch {
                            animate(from, 0f, animationSpec = spring()) { v, _ -> knobX = v }
                        }
                    },
                )
            },
    ) {
        // 왼→오른쪽으로 미는 물리 제스처라 RTL 에서도 방향을 고정한다. 안 그러면
        // CenterStart 와 offset 은 뒤집히는데 드래그 delta 는 물리 방향이라, 손잡이가
        // 오른쪽에서 시작해 손가락과 반대로 간다.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                // 손잡이가 나아갈수록 안내 문구는 물러난다.
                Text(
                    text = stringResource(R.string.slide_to_unlock_hint),
                    color = tint.copy(alpha = (1f - progress * 1.6f).coerceAtLeast(0f) * 0.55f),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center),
                )

                Box(
                    Modifier
                        .offset { IntOffset((insetPx + knobX).roundToInt(), 0) }
                        .size(TrackKnob)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // 앱의 가로 막대 모티프. 화살표를 쓰면 다른 아이콘 언어가 하나 더 생긴다.
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}
