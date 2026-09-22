// ⚠️ 생성된 파일이다. 손으로 고치지 말 것.
//
// 원본: design/tokens/tokens.json (W3C Design Tokens 형식)
// 생성: python3 design/tokens/build.py
// 검사: python3 design/tokens/build.py --check
//
// 값을 바꾸려면 JSON 을 고치고 다시 생성한다. 여기서 바꾸면 다음 생성에서 사라진다.
// 층 규칙 — Ref 는 원시값, Sys 는 의도(다른 제품에 가져가는 층), Comp 는 이 앱의 부품.
// Sys 는 Ref 만, Comp 는 Sys·Ref 만 가리킨다.

package com.jjw.blackscreen.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Ref {
    object Palette {
        val black = androidx.compose.ui.graphics.Color(0xFF000000)

        val white = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

        /** 떠 있는 표면의 바탕색. 순검정보다 한 톤 밝아 검은 앱 위에서도 윤곽이 잡힌다. */
        val ink900 = androidx.compose.ui.graphics.Color(0xFF141414)

        val ink850 = androidx.compose.ui.graphics.Color(0xFF202020)

        val blue500 = androidx.compose.ui.graphics.Color(0xFF2F6FA8)

        val red500 = androidx.compose.ui.graphics.Color(0xFFC0392B)

        /** 흰색 20%. 어두운 표면의 얇은 테두리. */
        val white20 = androidx.compose.ui.graphics.Color(0x33FFFFFF)

        /** 흰색 70%. 어두운 표면 위의 기호·라벨 — 100% 면 표면에서 뜬다. */
        val white70 = androidx.compose.ui.graphics.Color(0xB3FFFFFF)

        /** ink900 88%. 뒤가 살짝 비쳐 '떠 있다' 는 인상. 불투명하면 화면에 붙은 딱지처럼 보인다. */
        val ink900a88 = androidx.compose.ui.graphics.Color(0xE0141414)

        /** ink850 75%. ink900a88 보다 밝고 더 비친다 — 뒤로 물러나 보여야 하는 표면. */
        val ink850a75 = androidx.compose.ui.graphics.Color(0xC0202020)

        val blue500a94 = androidx.compose.ui.graphics.Color(0xF02F6FA8)

        val red500a94 = androidx.compose.ui.graphics.Color(0xF0C0392B)
    }

    object Opacity {
        val o6 = 0.06f

        val o12 = 0.12f

        val o22 = 0.22f

        val o25 = 0.25f

        val o30 = 0.3f

        val o40 = 0.4f

        val o45 = 0.45f

        val o55 = 0.55f

        val o70 = 0.7f

        val o85 = 0.85f

        val full = 1.0f
    }

    object Dimension {
        val d1 = 1.dp

        val d1_5 = 1.5f.dp

        val d2 = 2.dp

        val d3 = 3.dp

        val d4 = 4.dp

        val d5 = 5.dp

        val d6 = 6.dp

        val d8 = 8.dp

        val d10 = 10.dp

        val d12 = 12.dp

        val d14 = 14.dp

        val d16 = 16.dp

        val d18 = 18.dp

        val d20 = 20.dp

        val d24 = 24.dp

        val d28 = 28.dp

        val d30 = 30.dp

        val d32 = 32.dp

        val d40 = 40.dp

        val d44 = 44.dp

        val d46 = 46.dp

        val d52 = 52.dp

        val d56 = 56.dp

        val d64 = 64.dp

        val d72 = 72.dp

        val d84 = 84.dp

        val d112 = 112.dp

        val d180 = 180.dp

        val d300 = 300.dp
    }

    object FontSize {
        val f12 = 12.sp

        val f13 = 13.sp

        val f14 = 14.sp

        val f15 = 15.sp

        val f16 = 16.sp

        val f17 = 17.sp

        val f18 = 18.sp

        val f22 = 22.sp

        val f64 = 64.sp
    }

    object LetterSpacing {
        val wide = 1.sp
    }

    object Duration {
        val ms150: Int = 150

        val ms160: Int = 160

        val ms400: Int = 400

        val ms3000: Int = 3000
    }
}

/** 의도 층. 다른 제품에 가져가는 것은 이 층과 ref 다. 이름은 '어디에 쓰는가' 가 아니라 '무엇을 뜻하는가' 로 짓는다. */
object Sys {
    object Color {
        /** 차폐 화면의 바탕. 이 앱의 기본 상태는 이 색 하나만 보이는 것이다. */
        val canvas = Ref.Palette.black

        /**
         * 캔버스 위의 글자·기호. 항상 흰색이다 — 보이는 밝기는 프레임버퍼 값 × 패널 밝기라, 글자를 회색으로 낮추면 패널 밝기 0.30 에서 읽히지 않는다.
         * 실제로 세 번 그랬다.
         */
        val onCanvas = Ref.Palette.white

        /** 다른 앱 위에 떠 있는 UI 의 표면 — 버블, 설정의 버블 견본. */
        val floatingSurface = Ref.Palette.ink900a88

        /** 떠 있는 UI 중 뒤로 물러나 보여야 하는 것 — 겨냥 칩의 비활성 상태. */
        val floatingSurfaceMuted = Ref.Palette.ink850a75

        /** 떠 있는 표면의 얇은 테두리. 항상 stroke.hairline 굵기로 쓴다. */
        val floatingBorder = Ref.Palette.white20

        /** 떠 있는 표면 위의 기호·라벨. */
        val floatingContent = Ref.Palette.white70

        /** '열기·진행' 계열 강조. 겨냥 위(앱 열기). */
        val accentPositive = Ref.Palette.blue500a94

        /** '지우기·닫기' 계열 강조. 겨냥 아래(버블 닫기). */
        val accentDestructive = Ref.Palette.red500a94
    }

    /** 글자색(tint)에 곱하는 상대 불투명도. 이 앱의 글자색은 설정이 정하므로 색을 고정할 수 없고 '몇 %' 로만 단계를 둘 수 있다. */
    object Opacity {
        val full = Ref.Opacity.full

        /** 거의 불투명한 채움 — 손잡이처럼 잡을 것. */
        val strong = Ref.Opacity.o85

        /** 보조 텍스트. 제목보다 한 단계 낮춘다. */
        val secondary = Ref.Opacity.o70

        /** 안내 문구 · 표시 위의 작은 표식. 있어야 하지만 주인공은 아니다. */
        val hint = Ref.Opacity.o55

        /** 또렷해야 하는 테두리. */
        val medium = Ref.Opacity.o45

        /** 한동안 손대지 않은 떠 있는 UI. */
        val idle = Ref.Opacity.o40

        /** 누를 수 없는 버튼. */
        val disabled = Ref.Opacity.o30

        /** 채워지기 전의 바탕 막대. */
        val weak = Ref.Opacity.o25

        /** 윤곽만 잡는 테두리. */
        val hairline = Ref.Opacity.o22

        /** 아래를 거의 다 보여 줘야 하는 덮개. 0.55 로는 PIN 입력 화면의 숫자가 안 보였다 — 탈출구는 실제로 쓸 수 있어야 의미가 있다. */
        val scrim = Ref.Opacity.o12

        /** 있는지 겨우 느껴지는 채움. 알약 버튼의 바탕. */
        val faint = Ref.Opacity.o6
    }

    object Typography {
        /** 시계. */
        val display = Ref.FontSize.f64

        /** 날짜 · 배터리 잔량. 시계 아래 줄. */
        val headline = Ref.FontSize.f22

        /** 문장 · 글꼴 견본. */
        val body = Ref.FontSize.f18

        /** 버튼 라벨. */
        val title = Ref.FontSize.f17

        /** 재생 제목 · 칩 기호. */
        val labelLarge = Ref.FontSize.f16

        /** 칩 라벨 · 트랙 안내. */
        val label = Ref.FontSize.f15

        /** 권한 안내. */
        val captionLarge = Ref.FontSize.f14

        /** 재생 부제. */
        val caption = Ref.FontSize.f13

        /** 재생 시간. */
        val captionSmall = Ref.FontSize.f12

        /** 짧은 버튼 라벨을 조금 벌려 또렷하게. */
        val letterSpacingWide = Ref.LetterSpacing.wide
    }

    object Spacing {
        val xxs = Ref.Dimension.d2

        val xs = Ref.Dimension.d4

        val sm = Ref.Dimension.d8

        val md = Ref.Dimension.d12

        val lg = Ref.Dimension.d16

        val xl = Ref.Dimension.d20

        val xxl = Ref.Dimension.d24

        val xxxl = Ref.Dimension.d32
    }

    object Radius {
        /** 누르는 행. */
        val small = Ref.Dimension.d8

        /** 미리보기 상자 · 글꼴 견본 칩. */
        val card = Ref.Dimension.d12

        /** 알약. 반지름을 %로 — 높이의 절반. */
        val pill = 50
    }

    object Stroke {
        /** 윤곽만 잡는 테두리. */
        val hairline = Ref.Dimension.d1

        /** 선택된 것의 테두리. */
        val emphasis = Ref.Dimension.d2
    }

    object Motion {
        /** 손가락에 바로 반응하는 것 — 커짐·색 바뀜. */
        val fast = Ref.Duration.ms150

        /** 천천히 흐려지고 돌아오는 것. */
        val fade = Ref.Duration.ms400

        /** 이만큼 손대지 않으면 유휴로 본다. */
        val idleDelay = Ref.Duration.ms3000
    }

    object TouchTarget {
        /** 접근성 권장 최소 터치 영역. */
        val min = Ref.Dimension.d44
    }
}

/** 부품 층. 이 앱의 것이다. 색·치수·시간은 sys 또는 ref 를 가리킨다 — 리터럴은 비율(number)만 허용한다. 비율에는 팔레트가 없다. */
object Comp {
    /** 차폐 화면 한가운데의 시계·날짜·배터리·문장 블록. */
    object Content {
        /** 정중앙에서 위로 화면 높이의 15%. 손에 들었을 때 시선이 닿는 높이. */
        val verticalBias = -0.3f

        val clockSize = Sys.Typography.display

        val dateSize = Sys.Typography.headline

        val sentenceSize = Sys.Typography.body

        /** 문장 좌우 여백. 긴 문장이 화면 끝까지 닿지 않게. */
        val sentenceInset = Ref.Dimension.d40

        /** 시계 → 날짜 → 배터리 사이. */
        val gapSmall = Sys.Spacing.sm

        /** 블록 → 문장 사이. 문장은 한 덩어리가 아니라 따로 읽힌다. */
        val gapLarge = Sys.Spacing.xxl
    }

    object DimmedScrim {
        /** 지문 프롬프트·PIN 입력이 떠 있는 동안 차폐의 알파. */
        val opacity = Sys.Opacity.scrim
    }

    /** 꾹 눌러 해제의 진행 링. */
    object UnlockRing {
        val size = Ref.Dimension.d64

        val stroke = Ref.Dimension.d3

        /** 화면 높이 대비 중앙에서 아래로. 정중앙은 시계·문장이 쓴다. */
        val verticalOffsetFraction = 0.28f

        /** 진행률에 곱해 알파를 앞당긴다. 초반에도 링이 보이게. */
        val fadeInBoost = 1.4f
    }

    object SlideDismiss {
        /** 손을 뗀 뒤 남은 거리를 마저 밀어내는 시간. */
        val duration = Ref.Duration.ms160
    }

    /** 밀어서 잠금 해제 트랙. 지금은 미제공(Gesture.available=false)이지만 코드가 남아 있다. */
    object SlideTrack {
        val height = Ref.Dimension.d56

        val knob = Ref.Dimension.d46

        val inset = Ref.Dimension.d5

        val bottomMargin = Ref.Dimension.d72

        /** 넓으면 화면을 가로지르는 막대처럼 보여 답답하다. */
        val widthFraction = 0.66f

        val borderOpacity = Sys.Opacity.hairline

        val hintSize = Sys.Typography.label

        val hintOpacity = Sys.Opacity.hint

        /** 손잡이 진행률에 곱해 안내 문구를 빨리 물린다. */
        val hintFade = 1.6f

        val knobOpacity = Sys.Opacity.strong

        /** 손잡이 안의 가로 막대 — 앱의 막대 모티프. */
        val knobMarkWidth = Ref.Dimension.d14

        val knobMarkHeight = Ref.Dimension.d2

        val knobMarkOpacity = Sys.Opacity.hint
    }

    /** 지문 해제의 '잠금 해제' 알약 버튼. 재생 컨트롤이 있을 때만 보인다. */
    object FingerprintButton {
        /** 위에서 62.5%. 시계 블록(~45%)과 재생 컨트롤(~78%)의 중간. */
        val verticalBias = 0.25f

        val borderOpacity = Sys.Opacity.medium

        val fillOpacity = Sys.Opacity.faint

        val paddingHorizontal = Ref.Dimension.d30

        val paddingVertical = Ref.Dimension.d14

        val fontSize = Sys.Typography.title

        val letterSpacing = Sys.Typography.letterSpacingWide
    }

    /** 하단 재생 바 — 제목 · 부제 · 진행 막대 · 시간. */
    object MediaBar {
        /** 예전 밀어서 잠금 해제 트랙이 있던 자리. */
        val bottomMargin = Ref.Dimension.d72

        val widthFraction = 0.8f

        val titleSize = Sys.Typography.labelLarge

        val subtitleSize = Sys.Typography.caption

        val subtitleOpacity = Sys.Opacity.secondary

        val subtitleGap = Sys.Spacing.xxs

        /** 부제 → 진행 막대. */
        val barGap = Ref.Dimension.d10

        /** 손잡이 없는 얇은 막대. 제어가 없으니 잡을 것도 없다. */
        val barHeight = Ref.Dimension.d2

        val trackOpacity = Sys.Opacity.weak

        val timeGap = Sys.Spacing.md

        val timeSize = Sys.Typography.captionSmall

        val timeOpacity = Sys.Opacity.secondary

        val permissionHintSize = Sys.Typography.captionLarge

        val permissionHintOpacity = Sys.Opacity.hint
    }

    /** 이전 · 재생/일시정지 · 다음. */
    object MediaControls {
        /** 재생 바 위로 얹는 높이 — 바의 높이만큼. */
        val lift = Ref.Dimension.d84

        val gap = Ref.Dimension.d28

        val buttonSize = Sys.TouchTarget.min

        val disabledOpacity = Sys.Opacity.disabled
    }

    /** 배터리 그림 + 퍼센트 한 줄. */
    object Battery {
        val glyphWidth = Ref.Dimension.d30

        val glyphHeight = Ref.Dimension.d14

        val glyphStroke = Ref.Dimension.d1_5

        val gap = Sys.Spacing.sm

        val fontSize = Sys.Typography.headline
    }

    /** 상주 버블. 크기는 사용자 설정이라 여기 없다. */
    object Bubble {
        val surface = Sys.Color.floatingSurface

        val border = Sys.Color.floatingBorder

        val borderWidth = Sys.Stroke.hairline

        val content = Sys.Color.floatingContent

        /** 기호는 버블 지름의 절반 안에. 가장자리까지 차면 테두리와 붙어 답답하다. */
        val glyphFraction = 0.5f

        /** 버블 크기에 비례시키지 않는다 — 16dp 에서도 2dp 는 보이고, 96dp 에서 8dp 는 둔하다. */
        val glyphStroke = Ref.Dimension.d2

        val idleOpacity = Sys.Opacity.idle

        val idleDelay = Sys.Motion.idleDelay

        val fadeDuration = Sys.Motion.fade
    }

    /** 버블을 꾹 누른 뒤 위/아래에 뜨는 두 목표. */
    object AimChip {
        val surfaceIdle = Sys.Color.floatingSurfaceMuted

        val surfaceUp = Sys.Color.accentPositive

        val surfaceDown = Sys.Color.accentDestructive

        val contentActive = Sys.Color.onCanvas

        val contentIdle = Sys.Color.floatingContent

        /** 화면 위아래 끝에서 띄우는 거리. */
        val edgeMargin = Ref.Dimension.d112

        val paddingHorizontal = Ref.Dimension.d18

        val paddingVertical = Sys.Spacing.md

        val gap = Sys.Spacing.sm

        val iconSize = Sys.Typography.labelLarge

        val labelSize = Sys.Typography.label

        val activeScale = 1.12f

        val scaleDuration = Sys.Motion.fast
    }

    /** 설정 화면의 차폐 미리보기 상자. */
    object Preview {
        /** 시계(display)와 문장 한 줄이 들어가는 최소치. */
        val height = Ref.Dimension.d180

        val heightWithMedia = Ref.Dimension.d300

        val radius = Sys.Radius.card

        val mediaBottomMargin = Sys.Spacing.lg
    }

    /** 설정 화면. 색·글꼴은 Material 이 정하고 여기는 우리 고유 요소만. */
    object Settings {
        val screenPadding = Sys.Spacing.xl

        /** 라디오·스위치 행의 위아래. */
        val rowPaddingVertical = Ref.Dimension.d6

        val rowRadius = Sys.Radius.small

        val fontChipRadius = Sys.Radius.card

        val fontChipPaddingHorizontal = Sys.Spacing.sm

        val fontChipPaddingVertical = Ref.Dimension.d10

        val fontChipSample = Sys.Typography.body

        /** 설명 문구를 위 요소에 붙이는 간격. */
        val captionGap = Ref.Dimension.d6

        /** 버블 기호 견본의 지름(dp). 단위 없는 수인 이유: 기호를 그리는 함수가 dp 정수를 받는다. */
        val swatchSize = 52

        val swatchPadding = Ref.Dimension.d6

        val selectedBorder = Sys.Stroke.emphasis

        val idleBorder = Sys.Stroke.hairline
    }
}
