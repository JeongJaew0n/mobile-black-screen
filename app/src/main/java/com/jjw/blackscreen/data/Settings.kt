package com.jjw.blackscreen.data

import androidx.compose.ui.graphics.Color

/** 차폐 방식. 두 모드의 트레이드오프는 docs/plans/blackout-overlay-modes/spec.md 참조. */
enum class Mode {
    /** 풀스크린 Activity + 몰입 모드. 상태바까지 완전히 가려지지만 최상단 앱의 렌더링이 멈춘다. */
    BLACKOUT,

    /** 시스템 오버레이. 아래 앱이 계속 렌더링되지만 상태바는 가려지지 않는다. */
    OVERLAY,
}

/** 차폐 해제 제스처. 단일 탭은 주머니에서 바로 풀리므로 선택지에 없다. */
enum class Gesture {
    LONG_PRESS,
    DOUBLE_TAP,
    TRIPLE_TAP,
}

data class Settings(
    val mode: Mode = Mode.BLACKOUT,
    /** 기본값은 요구사항대로 "아무것도 표시하지 않는 완전한 검정"이다. */
    val showClock: Boolean = false,
    val clockFormat: String = ClockFormats.H24,
    val sentence: String = "",
    /** 1~5. 순백은 쓰지 않는다 — [textColor] 참조. */
    val textLevel: Int = 3,
    val burnInShiftEnabled: Boolean = true,
    val unlockGesture: Gesture = Gesture.LONG_PRESS,
) {
    /**
     * 창 밝기를 0으로 눌러 둔 상태이므로 순백을 쓰면 어두운 방에서 지나치게 튄다.
     * 저휘도 그레이만 사용하고, 이는 AMOLED 번인 부담을 줄이는 효과도 있다.
     */
    val textColor: Color
        get() = when (textLevel.coerceIn(1, 5)) {
            1 -> Color(0xFF181818)
            2 -> Color(0xFF242424)
            3 -> Color(0xFF333333)
            4 -> Color(0xFF4A4A4A)
            else -> Color(0xFF6B6B6B)
        }

    /** 표시할 것이 하나도 없으면 텍스트 레이아웃 자체를 건너뛴다. */
    val hasContent: Boolean
        get() = showClock || sentence.isNotBlank()
}

object ClockFormats {
    const val H24 = "HH:mm"
    const val H12 = "h:mm a"

    val ALL = listOf(H24, H12)
}
