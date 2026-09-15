package com.jjw.blackscreen.ui

import androidx.compose.foundation.background
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
    bottomMargin: Dp = MediaBarBottomMargin,
) {
    val shift = rememberBurnInShift()
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
