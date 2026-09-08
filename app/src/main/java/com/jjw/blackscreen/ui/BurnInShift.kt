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
 * AMOLED 번인 방지용 픽셀 시프트. **끌 수 없다.**
 *
 * 같은 위치에 시계를 몇 시간 띄우면 잔상이 남으므로 [ShiftIntervalMillis] 마다
 * 반경 [ShiftRadiusX]×[ShiftRadiusY] 타원 궤도 위의 다음 지점으로 옮긴다.
 *
 * 예전에는 설정 토글이 있었다. 끄면 얻는 것이 없고 잃는 것은 되돌릴 수 없는 패널 손상이라
 * 없앴다 — 밝기 슬라이더를 없앤 것과 같은 이유다. 비용도 1분에 `step++` 한 번뿐이다.
 *
 * 이동은 의도적으로 애니메이션 없이 즉시 처리한다. 1분에 한 번 옮기는 데 프레임을 계속
 * 돌리는 것은 전력 낭비다.
 *
 * > 예전 주석은 "밝기가 0 이라 사용자가 이동을 인지하지 못한다" 를 근거로 들었는데
 * > 거꾸로였다. 밝기가 0 인 것은 표시할 내용이 없을 때이고, 그때는 시프트 자체가 무의미하다.
 * > 시프트가 필요한 상황에서는 밝기가 [CONTENT_BRIGHTNESS] 라 오히려 보인다.
 * > 결론은 그대로지만 근거가 틀렸다.
 */
@Composable
fun rememberBurnInShift(): IntOffset {
    val density = LocalDensity.current
    val radiusXPx = with(density) { ShiftRadiusX.toPx() }
    val radiusYPx = with(density) { ShiftRadiusY.toPx() }
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ShiftIntervalMillis)
            step++
        }
    }

    return remember(step, radiusXPx, radiusYPx) {
        val angle = step * (2 * PI / ShiftSteps)
        IntOffset(
            x = (cos(angle) * radiusXPx).roundToInt(),
            y = (sin(angle) * radiusYPx).roundToInt(),
        )
    }
}
