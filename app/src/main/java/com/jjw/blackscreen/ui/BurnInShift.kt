package com.jjw.blackscreen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 궤도는 정원이 아니라 **세로로 긴 타원**이다.
 *
 * 가로로 크게 흔들리면 화면 정중앙에 있어야 할 시계가 한쪽으로 치우쳐 보인다.
 * 번인 방지에 필요한 것은 "픽셀이 조금씩 옮겨 다니는 것"이지 이동 방향이 아니므로,
 * 가로는 눈에 띄지 않을 만큼만 움직이고 세로로 더 크게 움직인다.
 */
private val ShiftRadiusX = 8.dp
private val ShiftRadiusY = 28.dp
private const val ShiftIntervalMillis = 60_000L
private const val ShiftSteps = 12

/**
 * AMOLED 번인 방지용 픽셀 시프트.
 *
 * 같은 위치에 시계를 몇 시간 띄우면 잔상이 남으므로 [ShiftIntervalMillis] 마다
 * 반경 [ShiftRadius] 원 궤도 위의 다음 지점으로 옮긴다.
 *
 * 이동은 의도적으로 애니메이션 없이 즉시 처리한다. 어차피 화면 밝기가 0 으로 눌려 있어
 * 사용자는 이동을 인지하지 못하고, 프레임을 계속 돌리는 쪽이 전력만 먹는다.
 */
@Composable
fun rememberBurnInShift(enabled: Boolean): IntOffset {
    val density = LocalDensity.current
    val radiusXPx = with(density) { ShiftRadiusX.toPx() }
    val radiusYPx = with(density) { ShiftRadiusY.toPx() }
    var step by remember { mutableIntStateOf(0) }

    // enabled 여부와 무관하게 항상 같은 수의 훅을 호출해 컴포지션 구조를 고정한다.
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            delay(ShiftIntervalMillis)
            step++
        }
    }

    return remember(enabled, step, radiusXPx, radiusYPx) {
        if (!enabled) {
            IntOffset.Zero
        } else {
            val angle = step * (2 * PI / ShiftSteps)
            IntOffset(
                x = (cos(angle) * radiusXPx).roundToInt(),
                y = (sin(angle) * radiusYPx).roundToInt(),
            )
        }
    }
}
