package com.jjw.blackscreen.bubble

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.jjw.blackscreen.data.Edge

/** 가장자리에서 살짝 띄운다. 0 이면 곡면 디스플레이에서 잘린다. */
private const val EDGE_MARGIN_DP = 6

internal fun bubbleSizePx(density: Float, sizeDp: Int) = (sizeDp * density).toInt()

/** 해당 가장자리에 붙였을 때의 x 좌표. 스냅과 최초 배치가 같은 식을 쓰게 한다. */
internal fun bubbleEdgeX(edge: Edge, screenWidth: Int, density: Float, sizeDp: Int): Int {
    val margin = (EDGE_MARGIN_DP * density).toInt()
    return if (edge == Edge.LEFT) margin else screenWidth - bubbleSizePx(density, sizeDp) - margin
}

/**
 * 상주 버블 창 파라미터.
 *
 * 창 크기가 버블 크기와 같으므로 **그 바깥 터치는 자동으로 아래 앱에 전달된다.**
 * 전체 차폐 창에서 `FLAG_NOT_TOUCHABLE` 을 쓰지 않는 이유는 "터치를 삼켜야 해서"였지만,
 * 여기서는 "자기 영역만 받으면 되어서"다 — 같은 결론이지만 이유가 다르다.
 */
internal fun bubbleLayoutParams(): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

/** 가장자리 + 세로 비율을 실제 픽셀 좌표로 푼다. */
internal fun WindowManager.LayoutParams.placeBubble(
    edge: Edge,
    yRatio: Float,
    screenWidth: Int,
    screenHeight: Int,
    density: Float,
    sizeDp: Int,
) {
    x = bubbleEdgeX(edge, screenWidth, density, sizeDp)
    y = ((screenHeight - bubbleSizePx(density, sizeDp)) * yRatio.coerceIn(0f, 1f)).toInt()
}
