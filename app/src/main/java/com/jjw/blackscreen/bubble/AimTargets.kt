package com.jjw.blackscreen.bubble

import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 꾹 누른 채로 고른 방향. */
enum class BubbleAim { NONE, UP, DOWN }

/**
 * 방향이 정해지는 순 변위(dp).
 *
 * 누적 이동 거리가 아니라 **누른 지점 대비 순 변위**로 잰다. 누적으로 재면 손이 떨린 것이
 * 쌓여서 겨냥하지 않은 쪽이 잡힌다 — 쓸어서 해제에서 같은 실수를 이미 했다.
 * 올렸다가 다시 내리면 순 변위는 상쇄되므로 마음을 바꿀 수 있다.
 */
const val AIM_THRESHOLD_DP = 44

/** 화면 위아래 끝에서 띄우는 거리. */
private const val AIM_EDGE_DP = 112

/** 겨냥하는 동안만 띄우는 표시용 창이라 터치를 받지 않는다 — 터치는 버블 창이 계속 쥔다. */
internal fun aimTargetsLayoutParams(): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    )

/**
 * 위/아래 두 목표.
 *
 * 목표를 **맞히는** 것이 아니라 방향만 고르는 것이라, 위치는 화면 위아래에 고정해 두고
 * 어느 쪽이 잡혔는지만 밝혀 준다. 버블 옆에 붙이면 버블이 화면 끝에 있을 때 하나가
 * 화면 밖으로 나간다.
 */
@Composable
fun AimTargets(aim: BubbleAim) {
    Box(Modifier.fillMaxSize()) {
        AimChip(
            icon = "▲",
            label = "앱 열기",
            active = aim == BubbleAim.UP,
            activeColor = Color(0xF02F6FA8),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = AIM_EDGE_DP.dp),
        )
        AimChip(
            icon = "✕",
            label = "버블 삭제",
            active = aim == BubbleAim.DOWN,
            activeColor = Color(0xF0C0392B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AIM_EDGE_DP.dp),
        )
    }
}

@Composable
private fun AimChip(
    icon: String,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "aimChipScale",
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (active) activeColor else Color(0xC0202020))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = Color.White, fontSize = 16.sp)
        Text(
            label,
            color = if (active) Color.White else Color(0xB3FFFFFF),
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
