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
/** 버블이 붙어 있는 화면 가장자리. 픽셀 좌표로 저장하면 회전·해상도 변경 때 화면 밖으로 나간다. */
enum class Edge { LEFT, RIGHT }

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
    /** 1~5. 값이 클수록 밝다 — [screenBrightness] 참조. */
    val textLevel: Int = 3,
    val burnInShiftEnabled: Boolean = true,
    val unlockGesture: Gesture = Gesture.TRIPLE_TAP,

    /** 상주 버블. 켜면 Blackout 모드도 오버레이 권한을 요구하게 된다. */
    val bubbleEnabled: Boolean = false,
    val bubbleEdge: Edge = Edge.RIGHT,
    /** 화면 높이 대비 0..1. 회전·해상도가 바뀌어도 화면 안에 남는다. */
    val bubbleYRatio: Float = 0.5f,
) {
    /**
     * 글자는 **항상 흰색**이다.
     *
     * 보이는 밝기는 프레임버퍼 값 × 패널 밝기다. 어둡게 만드는 손잡이가 둘이면
     * 둘 다 깎아 놓고 왜 안 보이는지 헤매게 된다 — 실제로 두 번 그랬다.
     * (`#333333` + 패널 0 → 판독 불가, `#E6E6E6` + 패널 0.08 → 여전히 판독 불가.)
     *
     * 그래서 밝기 손잡이는 [screenBrightness] 하나로 통일했다. 여기는 건드리지 말 것.
     */
    val textColor: Color get() = Color.White

    /**
     * 창 밝기 오버라이드(0..1). 이 앱에서 밝기를 조절하는 **유일한** 값이다.
     *
     * **표시할 내용이 없으면 무조건 0** 이다. 아무것도 안 보여줄 거면 화면은 완전히
     * 어두워야 하고, 그게 이 앱의 기본 상태다.
     *
     * ⚠️ 이 값은 스크린샷으로 검증할 수 없다. `screencap` 은 프레임버퍼를 뜨는 것이라
     *    패널 밝기가 반영되지 않는다. 반드시 실제 화면을 눈으로 봐야 한다.
     */
    val screenBrightness: Float
        get() = if (!hasContent) {
            0f
        } else {
            when (textLevel.coerceIn(1, 5)) {
                1 -> 0.05f
                2 -> 0.15f
                3 -> 0.30f
                4 -> 0.50f
                else -> 0.80f
            }
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
