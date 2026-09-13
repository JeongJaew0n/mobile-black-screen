package com.jjw.blackscreen.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BatteryStatus(val percent: Int, val charging: Boolean)

/**
 * 배터리 상태. **폴링하지 않는다.** `ACTION_BATTERY_CHANGED` 는 sticky 라 등록 즉시 현재 값이
 * 오고, 그 뒤로는 시스템이 잔량이 바뀔 때만 알려 준다 — 밤새 몇십 번이다.
 */
@Composable
fun rememberBatteryStatus(): BatteryStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(BatteryStatus(percent = 0, charging = false)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                if (level >= 0 && scale > 0) {
                    status = BatteryStatus(percent = level * 100 / scale, charging = plugged != 0)
                }
            }
        }
        // sticky 브로드캐스트라 registerReceiver 가 마지막 값을 즉시 돌려준다.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (sticky != null) receiver.onReceive(context, sticky)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return status
}

/**
 * 배터리 한 줄 — 작은 배터리 그림 + `78%`. 충전 중이면 그림 안에 번개.
 *
 * 그림은 이모지가 아니라 도형으로 그린다(버블 기호와 같은 이유). 글자는 시계·날짜와 같은
 * 흰색·글꼴 계열이라 한 덩어리로 보인다.
 */
@Composable
fun BatteryLine(
    status: BatteryStatus,
    tint: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BatteryGlyph(status, tint, Modifier.size(width = 30.dp, height = 14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${status.percent}%",
            color = tint,
            fontSize = 22.sp,
            fontFamily = fontFamily,
        )
    }
}

@Composable
private fun BatteryGlyph(status: BatteryStatus, tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val capW = size.width * 0.08f
        val bodyW = size.width - capW
        val inset = stroke / 2

        // 몸통(테두리)과 단자
        drawRoundRect(
            color = tint,
            topLeft = Offset(inset, inset),
            size = Size(bodyW - stroke, size.height - stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 1.5f),
            style = Stroke(stroke),
        )
        drawRect(
            color = tint,
            topLeft = Offset(bodyW, size.height * 0.3f),
            size = Size(capW, size.height * 0.4f),
        )
        // 잔량 채움
        val pad = stroke * 2
        val fillW = (bodyW - pad * 2) * (status.percent.coerceIn(0, 100) / 100f)
        if (fillW > 0f) {
            drawRect(color = tint, topLeft = Offset(pad, pad), size = Size(fillW, size.height - pad * 2))
        }
        // 충전 중 번개 — 채움과 대비되게 검정으로 판다
        if (status.charging) {
            val cx = bodyW / 2
            val h = size.height
            val bolt = Path().apply {
                moveTo(cx + h * 0.10f, h * 0.10f)
                lineTo(cx - h * 0.20f, h * 0.55f)
                lineTo(cx + h * 0.02f, h * 0.55f)
                lineTo(cx - h * 0.10f, h * 0.90f)
                lineTo(cx + h * 0.20f, h * 0.45f)
                lineTo(cx - h * 0.02f, h * 0.45f)
                close()
            }
            drawPath(bolt, Color.Black)
        }
    }
}
