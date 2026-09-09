package com.jjw.blackscreen.bubble

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jjw.blackscreen.data.BubbleIcon

/** 기호는 버블 지름의 절반 안에 그린다. 가장자리까지 차면 테두리와 붙어 답답하다. */
private const val GLYPH_FRACTION = 0.5f

/** 선 굵기. 버블 크기에 비례시키지 않는다 — 16dp 에서도 2dp 는 보이고, 96dp 에서 8dp 는 둔하다. */
private val GlyphStroke = 2.dp

/**
 * 버블 안의 기호. **실제 버블과 설정 미리보기가 이 함수 하나를 쓴다.**
 *
 * 크기는 [sizeDp] 에 비례한다 — 고정하면 큰 버블에서 초라하고 작은 버블에서 넘친다.
 * 달은 흰 원에서 작은 원을 지워 만든다. `BlendMode.Clear` 는 오프스크린 레이어에서만
 * 동작하므로 `CompositingStrategy.Offscreen` 을 건다.
 */
@Composable
fun BubbleGlyph(
    icon: BubbleIcon,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xB3FFFFFF),
) {
    Canvas(
        modifier
            .size((sizeDp * GLYPH_FRACTION).dp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val s = size.minDimension
        val c = center
        val sw = GlyphStroke.toPx()

        when (icon) {
            // 퀵 설정 타일과 같은 모티프. 길이는 예전 구현(버블 지름의 35%)과 같다.
            BubbleIcon.BAR -> drawRect(
                color = tint,
                topLeft = Offset(c.x - s * 0.35f, c.y - sw / 2),
                size = Size(s * 0.7f, sw),
            )

            BubbleIcon.DOT -> drawCircle(tint, radius = s * 0.14f, center = c)

            BubbleIcon.RING -> drawCircle(tint, radius = s * 0.32f, center = c, style = Stroke(sw))

            BubbleIcon.MOON -> {
                drawCircle(tint, radius = s * 0.34f, center = c)
                drawCircle(
                    color = Color.Transparent,
                    radius = s * 0.30f,
                    center = Offset(c.x + s * 0.16f, c.y - s * 0.10f),
                    blendMode = BlendMode.Clear,
                )
            }

            // 위쪽이 트인 원호 + 세로 선. 0° 가 3시 방향이라 -60° 에서 300° 돌면 틈이 12시에 온다.
            BubbleIcon.POWER -> {
                val r = s * 0.32f
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
                drawLine(
                    color = tint,
                    start = Offset(c.x, c.y - s * 0.38f),
                    end = Offset(c.x, c.y - s * 0.02f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
