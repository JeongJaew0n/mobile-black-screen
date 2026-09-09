package com.jjw.blackscreen.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jjw.blackscreen.data.BubbleIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** 이 시간 동안 손대지 않으면 흐려진다. */
private const val IDLE_DELAY_MS = 3_000L
private const val IDLE_ALPHA = 0.4f

/** 손을 대고 나서 무엇을 하려는 것인지 갈리는 지점. */
private enum class Intent { TAP, DRAG, AIM, CANCEL }

/**
 * 상주 버블.
 *
 * 다른 앱 위에 계속 떠 있으므로 눈에 덜 띄어야 한다. 어두운 반투명 원에 얇은 테두리만 두고,
 * 안에는 사용자가 고른 기호 하나만 넣는다([BubbleGlyph]). 기본은 퀵 설정 타일과 같은 가로 막대.
 *
 * ## 제스처가 갈리는 방식
 *
 * 손을 댄 뒤 **길게 누르기 시간 안에** 무엇을 하려는 것인지 판정한다.
 *
 * | 그 사이에 | 판정 | 동작 |
 * |---|---|---|
 * | 손을 뗐다 | [Intent.TAP] | Screen Off |
 * | 터치 슬롭을 넘겼다 | [Intent.DRAG] | 버블을 옮긴다 |
 * | 아무것도 안 했다 | [Intent.AIM] | 위/아래 겨냥 — 위는 앱 열기, 아래는 버블 삭제 |
 *
 * 감지기를 `pointerInput` 두 개로 나눠 붙이면 이 판정을 할 수 없다. 겨냥 모드는 꾹 누른
 * **뒤에 이어지는 이동**을 봐야 하는데, `detectTapGestures` 의 `onLongPress` 는 그 시점에
 * 제스처를 끝내 버리기 때문이다.
 */
@Composable
fun BubbleContent(
    sizeDp: Int,
    icon: BubbleIcon,
    onTap: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onAimStart: () -> Unit,
    onAimUpdate: (BubbleAim) -> Unit,
    onAimPick: (BubbleAim) -> Unit,
) {
    // 상호작용이 있을 때마다 증가시켜 유휴 타이머를 되감는다.
    var touchTick by remember { mutableIntStateOf(0) }
    var idle by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

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
                val slop = viewConfiguration.touchSlop
                val aimThreshold = AIM_THRESHOLD_DP.dp.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchTick++

                    val intent = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var decided: Intent? = null
                        while (decided == null) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id }
                            decided = when {
                                change == null -> Intent.CANCEL
                                !change.pressed -> Intent.TAP
                                (change.position - down.position).getDistance() > slop ->
                                    Intent.DRAG
                                else -> null
                            }
                        }
                        decided
                    } ?: Intent.AIM

                    when (intent) {
                        Intent.CANCEL -> Unit

                        Intent.TAP -> onTap()

                        Intent.DRAG -> {
                            drag(down.id) { change ->
                                // 반드시 소비 **전에** 읽을 것. 소비된 변화의 positionChange()
                                // 는 Zero 라, 순서를 바꾸면 버블이 한 픽셀도 안 움직인다.
                                // detectDragGestures 는 delta 를 미리 계산해 줘서 이 함정이
                                // 없었다 — 직접 루프로 바꾸면서 한 번 걸렸다.
                                val delta = change.positionChange()
                                change.consume()
                                onDrag(delta)
                            }
                            touchTick++
                            onDragEnd()
                        }

                        Intent.AIM -> {
                            // 화면이 그대로라 눌린 줄 모른다. 진동으로 알린다.
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAimStart()

                            var aim = BubbleAim.NONE
                            var pressed = true
                            while (pressed) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                pressed = change.pressed

                                val dy = change.position.y - down.position.y
                                val next = when {
                                    dy <= -aimThreshold -> BubbleAim.UP
                                    dy >= aimThreshold -> BubbleAim.DOWN
                                    else -> BubbleAim.NONE
                                }
                                if (next != aim) {
                                    aim = next
                                    // 잡혔다는 것을 눈으로만 알리면 화면을 봐야 한다.
                                    if (next != BubbleAim.NONE) {
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove,
                                        )
                                    }
                                    onAimUpdate(next)
                                }
                            }
                            touchTick++
                            onAimPick(aim)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BubbleGlyph(icon = icon, sizeDp = sizeDp)
    }
}
