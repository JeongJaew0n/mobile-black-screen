package com.jjw.blackscreen.bubble

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ✕ 타겟 지름. 버블보다 커야 겨냥하기 쉽다. */
const val REMOVE_TARGET_SIZE_DP = 68

/** 화면 하단에서 띄우는 거리. 내비게이션 바를 피한다. */
private const val REMOVE_TARGET_BOTTOM_DP = 96

/** 버블 중심이 타겟 중심에서 이 거리 안에 들어오면 제거 대상으로 본다. */
const val REMOVE_TARGET_RADIUS_DP = 72

/** 드래그하는 동안에만 띄우는 창이라 터치를 받을 필요가 없다. */
internal fun removeTargetLayoutParams(density: Float): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        // LayoutParams.y 는 픽셀이다. dp 값을 그대로 넣으면 안 된다.
        y = (REMOVE_TARGET_BOTTOM_DP * density).toInt()
    }

/**
 * 타겟 중심 좌표. **버블 창과 같은 좌표계(부모 프레임 기준)** 여야 거리 판정이 맞는다.
 * 두 창 모두 시스템 바를 제외한 영역 안에 놓이므로 [usableWidth]/[usableHeight] 를 쓴다.
 */
internal fun removeTargetCenter(usableWidth: Int, usableHeight: Int, density: Float): Pair<Int, Int> {
    val bottom = (REMOVE_TARGET_BOTTOM_DP * density).toInt()
    val size = (REMOVE_TARGET_SIZE_DP * density).toInt()
    return usableWidth / 2 to (usableHeight - bottom - size / 2)
}

@Composable
fun RemoveTarget(active: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.25f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "removeTargetScale",
    )
    Box(
        Modifier
            .size(REMOVE_TARGET_SIZE_DP.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) Color(0xF0C0392B) else Color(0xC0202020)),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Color.White, fontSize = 24.sp)
    }
}
