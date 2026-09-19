package com.jjw.blackscreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjw.blackscreen.R
import com.jjw.blackscreen.media.NowPlaying
import kotlinx.coroutines.delay

/**
 * 진행 막대를 다시 그리는 주기. 시계를 분 단위로만 갱신하는 것과 같은 이유 — 초마다 그리면
 * 8시간 내내 1Hz 로 깨어 있는 셈이다. 곡 전환·일시정지는 이 주기와 무관하게 콜백으로 즉시 온다.
 */
private const val ProgressRedrawMillis = 10_000L

/** 하단 여백. 예전 밀어서 잠금 해제 트랙이 있던 자리다. 2차 제어 버튼은 이 아래에 들어간다. */
val MediaBarBottomMargin = 72.dp

/**
 * 재생 바 — 화면 하단 고정. 제목 · 아티스트/앱 · 진행 막대 · `1:23 / 4:56`. 표시 전용.
 *
 * 시계 블록과 분리된 레이어라 번인 궤도를 자기 것으로 하나 더 탄다.
 * [nowPlaying] 이 없고 [permissionMissing] 이면 같은 자리에 권한 안내 문구를 흐리게 띄운다 —
 * 사용자가 알림 접근을 꺼 버린 것을 검은 화면에서 바로 알려 주는 자리다.
 */
@Composable
fun BoxScope.MediaBarLayer(
    nowPlaying: NowPlaying?,
    permissionMissing: Boolean,
    tint: Color,
    fontFamily: FontFamily,
    /**
     * 번인 궤도. **제어 버튼과 같은 값을 받아야 한다** — 각자 `rememberBurnInShift()` 를 부르면
     * 타이머가 따로 돌고, 컨트롤은 재생이 시작될 때 생기므로 위상이 어긋나 한 덩어리로 보여야 할
     * 두 블록이 따로 흔들린다.
     */
    shift: IntOffset,
    bottomMargin: Dp = MediaBarBottomMargin,
) {
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .offset { shift }
            .padding(bottom = bottomMargin)
            .fillMaxWidth(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (nowPlaying == null) {
            if (permissionMissing) {
                Text(
                    text = stringResource(R.string.media_permission_needed),
                    color = tint.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            return
        }

        // 재생 중일 때만 10초마다 위치를 다시 계산한다. 일시정지면 루프 자체가 없다.
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(nowPlaying.playing, nowPlaying.updatedAtMs) {
            if (!nowPlaying.playing) return@LaunchedEffect
            while (true) {
                delay(ProgressRedrawMillis)
                tick++
            }
        }
        val position = remember(nowPlaying, tick) { nowPlaying.currentPositionMs() }

        Text(
            text = nowPlaying.title,
            color = tint,
            fontSize = 16.sp,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = nowPlaying.subtitle,
            color = tint.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nowPlaying.durationMs > 0) {
                // 손잡이 없는 얇은 막대. 제어가 없으니 잡을 것도 없다.
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(tint.copy(alpha = 0.25f)),
                ) {
                    val fraction = (position.toFloat() / nowPlaying.durationMs).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(2.dp)
                            .background(tint),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${formatClock(position)} / ${formatClock(nowPlaying.durationMs)}",
                    color = tint.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = fontFamily,
                )
            } else {
                // 라이브·길이 미상. 막대는 거짓말이 되니 경과 시간만.
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatClock(position),
                    color = tint.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = fontFamily,
                )
            }
        }
    }
}

/** `m:ss`, 한 시간이 넘으면 `h:mm:ss`. */
private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * 재생 제어 버튼 — 이전 · 재생/일시정지 · 다음. **차폐의 모든 해제 레이어 위**에 놓여야 한다.
 * 그래야 버튼 탭이 해제 제스처(3회 탭 등)로 새지 않는다 — 버튼이 탭을 소비하면 아래 제스처
 * 레이어는 그 이벤트를 못 본다. 그래서 [BlackScreenRoot] 의 **마지막 자식**으로 그린다.
 *
 * 재생 바(제목·막대)와 세로로 이어 보이도록 같은 하단 정렬을 쓰되, 막대보다 조금 위에 앉힌다.
 */
@Composable
fun BoxScope.MediaControlsLayer(
    nowPlaying: NowPlaying,
    tint: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    /** 재생 바와 **같은** 궤도값. [MediaBarLayer] 참조. */
    shift: IntOffset,
    bottomMargin: Dp = MediaBarBottomMargin,
) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .offset { shift }
            // 재생 바(제목·부제·막대) 위에 얹는다. 막대 높이만큼 더 띄운다.
            .padding(bottom = bottomMargin + 84.dp)
            // 컨트롤 영역의 탭은 여기서 끝낸다. 비활성 버튼과 버튼 사이 간격은 자식이 소비하지
            // 않아 그대로 아래 해제 제스처 레이어로 내려간다 — 그러면 흐린 이전/다음 버튼을
            // 세 번 누르는 것만으로 3회 탭 해제가 발동한다. 자식이 소비한 탭은
            // detectTapGestures 의 requireUnconsumed 때문에 여기까지 오지 않는다.
            .pointerInput(Unit) { detectTapGestures { } },
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CtrlButton(enabled = nowPlaying.canPrev, tint = tint, onClick = onPrevious) { c, t ->
            drawSkip(c, t, forward = false)
        }
        CtrlButton(enabled = true, tint = tint, onClick = onPlayPause) { c, t ->
            if (nowPlaying.playing) drawPause(c, t) else drawPlay(c, t)
        }
        CtrlButton(enabled = nowPlaying.canNext, tint = tint, onClick = onNext) { c, t ->
            drawSkip(c, t, forward = true)
        }
    }
}

@Composable
private fun CtrlButton(
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    draw: DrawScope.(size: Float, color: Color) -> Unit,
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.3f)
    Canvas(
        Modifier
            .size(44.dp)               // 접근성 권장 최소 터치 영역
            // enabled=false 여도 clickable 을 붙여 둔다. 스크린리더에 '사용 불가 버튼' 으로
            // 읽히고, 탭이 새는 것은 부모 Row 가 막는다.
            .clickable(enabled = enabled, onClick = onClick),
    ) { draw(size.minDimension, color) }
}

private fun DrawScope.drawPlay(s: Float, c: Color) {
    val p = Path().apply {
        moveTo(s * 0.34f, s * 0.28f); lineTo(s * 0.34f, s * 0.72f); lineTo(s * 0.72f, s * 0.5f); close()
    }
    drawPath(p, c)
}
private fun DrawScope.drawPause(s: Float, c: Color) {
    val w = s * 0.11f; val top = s * 0.30f; val h = s * 0.40f
    drawRect(c, Offset(s * 0.34f, top), Size(w, h))
    drawRect(c, Offset(s * 0.55f, top), Size(w, h))
}
/** 다음/이전. 삼각형 두 개 + 끝의 세로 막대. forward=false 면 좌우를 뒤집는다. */
private fun DrawScope.drawSkip(s: Float, c: Color, forward: Boolean) {
    val cap = StrokeCap.Butt
    fun tri(cx: Float, dir: Int) {
        val p = Path().apply {
            moveTo(cx - dir * s * 0.12f, s * 0.30f)
            lineTo(cx - dir * s * 0.12f, s * 0.70f)
            lineTo(cx + dir * s * 0.12f, s * 0.5f); close()
        }
        drawPath(p, c)
    }
    val dir = if (forward) 1 else -1
    tri(s * 0.34f, dir); tri(s * 0.56f, dir)
    val barX = if (forward) s * 0.70f else s * 0.30f
    drawLine(c, Offset(barX, s * 0.30f), Offset(barX, s * 0.70f), strokeWidth = s * 0.07f, cap = cap)
}
