package com.jjw.blackscreen.data

import androidx.compose.ui.graphics.Color

/**
 * Screen Off 방식.
 *
 * 이 앱의 목적은 **쓰던 앱은 그대로 두고 화면만 어두워지는 것**이다.
 * 따라서 앱을 전환하지 않는 [FULL] / [OVERLAY] 가 정상 경로이고,
 * [BLACKOUT] 은 어떤 권한도 주기 싫은 경우의 최후 수단이다.
 */
enum class Mode {
    /**
     * 접근성 오버레이. `TYPE_ACCESSIBILITY_OVERLAY` 는 상태바·내비게이션 바·시스템
     * 다이얼로그 위까지 전부 덮는다. 앱 전환이 없고 화면 전체가 검어진다 — 원하는 동작.
     */
    FULL,

    /**
     * 일반 오버레이(`SYSTEM_ALERT_WINDOW`). 앱 전환은 없지만 상태바·내비바는 남는다.
     * 접근성 권한을 주기 싫을 때의 절충안.
     */
    OVERLAY,

    /**
     * 풀스크린 Activity. 화면 전체가 검어지지만 **쓰던 앱이 뒤로 밀린다.**
     * 권한이 하나도 필요 없다는 것만이 장점이다.
     */
    BLACKOUT,
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
    val mode: Mode = Mode.FULL,
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

    /**
     * 한국어는 오전/오후가 **앞**에 온다 — "오전 12:11" 이지 "12:11 오전" 이 아니다.
     * `h:mm a` 는 영어권 순서다.
     */
    const val H12 = "a h:mm"

    /** 예전에 저장된 영어권 순서. 읽을 때 [H12] 로 옮긴다. */
    const val LEGACY_H12 = "h:mm a"

    val ALL = listOf(H24, H12)

    /** 저장된 값이 사라진 형식이면 현재 형식으로 옮긴다. */
    fun migrate(stored: String?): String = when (stored) {
        null -> H24
        LEGACY_H12 -> H12
        else -> stored
    }
}
