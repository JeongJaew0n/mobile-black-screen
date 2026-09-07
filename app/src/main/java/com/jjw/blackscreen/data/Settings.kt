package com.jjw.blackscreen.data

import androidx.compose.ui.graphics.Color

/**
 * Screen Off 방식.
 *
 * 이 앱의 목적은 **쓰던 앱은 그대로 두고 화면만 어두워지는 것**이다.
 * 따라서 앱을 전환하지 않는 [FULL] / [OVERLAY] 가 정상 경로이고,
 * [BLACKOUT] 은 어떤 권한도 주기 싫은 경우의 최후 수단이다.
 */
enum class Mode(
    /**
     * 이 모드를 쓸 수 있는가.
     *
     * `false` 로 두면 설정 화면에 나타나지 않고, 저장된 값도 읽는 시점에
     * [Mode.default] 로 교정된다. **코드는 그대로 남는다** — 되살릴 때 이 값만 바꾼다.
     * 매니페스트의 해당 컴포넌트도 `android:enabled="false"` 로 함께 꺼져 있다.
     */
    val available: Boolean,
) {
    /**
     * 접근성 오버레이. `TYPE_ACCESSIBILITY_OVERLAY` 는 상태바·내비게이션 바·시스템
     * 다이얼로그 위까지 전부 덮는다. 앱 전환이 없고 화면 전체가 검어진다 — 원하는 동작.
     */
    FULL(available = true),

    /**
     * 일반 오버레이(`SYSTEM_ALERT_WINDOW`). 앱 전환은 없지만 상태바·내비바는 남는다.
     * 접근성 권한을 주기 싫을 때의 절충안.
     */
    OVERLAY(available = false),

    /**
     * 풀스크린 Activity. 화면 전체가 검어지지만 **쓰던 앱이 뒤로 밀린다.**
     * 권한이 하나도 필요 없다는 것만이 장점이다.
     */
    BLACKOUT(available = false),

    ;

    companion object {
        /** 화면에 내보낼 모드. 하나뿐이면 설정 화면이 라디오를 감춘다. */
        val available: List<Mode> get() = entries.filter { it.available }

        val default: Mode get() = available.firstOrNull() ?: FULL

        /** 저장된 값이 비활성 모드를 가리키면 여기서 걸러낸다. */
        fun sanitize(mode: Mode): Mode = if (mode.available) mode else default
    }
}

/** 차폐 해제 제스처. 단일 탭은 주머니에서 바로 풀리므로 선택지에 없다. */
/** 버블이 붙어 있는 화면 가장자리. 픽셀 좌표로 저장하면 회전·해상도 변경 때 화면 밖으로 나간다. */
enum class Edge { LEFT, RIGHT }

/** 차폐 해제 제스처. 단일 탭은 주머니에서 바로 풀리므로 선택지에 없다. */
enum class Gesture {
    LONG_PRESS,
    DOUBLE_TAP,
    TRIPLE_TAP,

    /**
     * 한 방향으로 길게 쓸기.
     *
     * 판정 기준은 이동한 경로의 총합이 아니라 **시작점 대비 순 변위**다.
     * 경로 총합으로 재면 주머니 속에서 손가락이 잘게 흔들리기만 해도 누적되어 풀린다.
     * 순 변위는 왔다 갔다 하면 상쇄되므로 의도적인 스와이프만 통과한다.
     */
    SWIPE,

    /**
     * 옛 아이폰식 밀어서 잠금 해제.
     *
     * 화면 하단 트랙의 손잡이를 오른쪽 끝까지 끌면 풀린다. [SWIPE] 와 달리 화면이
     * 벗겨지지 않고, 대신 **무엇을 해야 하는지가 눈에 보인다.** 다른 제스처들은
     * 알려주지 않으면 알 수 없다.
     */
    SLIDE_TO_UNLOCK,
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

    /**
     * [Gesture.LONG_PRESS] 로 해제할 때 눌러야 하는 시간(ms).
     *
     * 짧으면 주머니에서 풀리고, 길면 답답하다. 사람마다 다른 감각이라 조절을 연다.
     */
    val holdMillis: Int = DEFAULT_HOLD_MILLIS,

    /** 상주 버블. 켜면 Blackout 모드도 오버레이 권한을 요구하게 된다. */
    val bubbleEnabled: Boolean = false,
    val bubbleEdge: Edge = Edge.RIGHT,
    /** 버블 지름(dp). [MIN_BUBBLE_DP]~[MAX_BUBBLE_DP]. */
    val bubbleSizeDp: Int = DEFAULT_BUBBLE_DP,
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
        get() = if (hasContent) brightnessForLevel else 0f

    /**
     * 표시 내용 유무와 무관하게 레벨에 대응하는 밝기.
     *
     * 설정 화면 미리보기용이다. [screenBrightness] 를 그대로 쓰면 시계·문장을 아직
     * 켜지 않은 상태에서 슬라이더를 움직여도 0 이라 아무 변화가 없다.
     */
    val brightnessForLevel: Float
        get() = when (textLevel.coerceIn(1, 5)) {
            1 -> 0.05f
            2 -> 0.15f
            3 -> 0.30f
            4 -> 0.50f
            else -> 0.80f
        }

    /** 표시할 것이 하나도 없으면 텍스트 레이아웃 자체를 건너뛴다. */
    val hasContent: Boolean
        get() = showClock || sentence.isNotBlank()
}

/**
 * 꾹 눌러 해제에 쓸 수 있는 시간 범위.
 *
 * 하한이 1초인 이유는 그보다 짧으면 주머니에서 스치기만 해도 풀리기 때문이다.
 * 0.1초 단위로 조절한다 — 이 정도 차이는 손끝에서 실제로 다르게 느껴진다.
 */
const val MIN_HOLD_MILLIS = 1_000
const val MAX_HOLD_MILLIS = 3_500
const val HOLD_STEP_MILLIS = 100
const val DEFAULT_HOLD_MILLIS = 1_500

/**
 * 버블 지름(dp)의 범위.
 *
 * 하한은 접근성 권장 터치 영역(48dp)의 1/3 이다. "거의 안 보이게 두고 싶다" 는 선택을
 * 막지 않기 위해서이며, 그 대가로 누르기 어려워지는 것은 사용자가 감수한다.
 * 상한은 화면 폭의 1/4 남짓이라 그 이상은 손가락보다 커서 의미가 없다.
 *
 * 하한을 더 내리지 말 것. 16dp 면 안쪽 막대가 이미 5.6dp 라, 여기서 더 줄이면
 * 버블인지 먼지인지 구분이 안 되고 꾹 누르기·드래그를 시작할 지점도 사라진다.
 */
const val MIN_BUBBLE_DP = 16
const val MAX_BUBBLE_DP = 96
const val BUBBLE_STEP_DP = 2
const val DEFAULT_BUBBLE_DP = 52

/**
 * 예전에는 1~5 단계로만 고를 수 있었다. 저장된 단계값을 dp 로 옮긴다.
 *
 * 단계를 늘리는 대신 dp 를 직접 저장하도록 바꿨다 — 범위를 넓히면서 단계 수를 유지하면
 * 눈금 간격이 벌어져 원하는 크기를 못 맞추게 된다.
 */
fun bubbleDpFromLegacyLevel(level: Int): Int = when (level.coerceIn(1, 5)) {
    1 -> 40
    2 -> 46
    3 -> 52
    4 -> 60
    else -> 70
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
