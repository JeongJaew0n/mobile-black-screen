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
    /**
     * 수묵화(Figma 'Ink wash' — #4A4A4A · #CBCBCB · #FFFFE3 · #6D8196)를 이 앱에 맞게 펼친 팔레트. 피그마 원색 넷은
     * 설명에 '원색' 으로 표시했고, 나머지는 그 사이·양 끝을 메운 톤이다.
     */
    object Palette {
        /** 차폐 바탕. AMOLED 에서 발광하지 않는다 — 이 앱의 존재 이유라 팔레트가 바뀌어도 그대로다. */
        val black = androidx.compose.ui.graphics.Color(0xFF000000)

        /** 원색. 한지색. 흰색 대비 98.6% 밝기라 패널 0.30 에서도 흰 글자와 같이 읽히면서 푸른 기가 빠진다. */
        val paper = androidx.compose.ui.graphics.Color(0xFFFFFFE3)

        val wash200 = androidx.compose.ui.graphics.Color(0xFFDEDEDA)

        /** 원색. 옅은 먹. 어두운 표면 위의 기호·보조 텍스트. */
        val wash300 = androidx.compose.ui.graphics.Color(0xFFCBCBCB)

        val wash400 = androidx.compose.ui.graphics.Color(0xFF9E9E9E)

        val wash500 = androidx.compose.ui.graphics.Color(0xFF7C7C7C)

        val wash600 = androidx.compose.ui.graphics.Color(0xFF616161)

        /** 원색. 먹색. 떠 있는 표면. */
        val wash700 = androidx.compose.ui.graphics.Color(0xFF4A4A4A)

        val wash750 = androidx.compose.ui.graphics.Color(0xFF363636)

        val wash800 = androidx.compose.ui.graphics.Color(0xFF2A2A2A)

        val wash850 = androidx.compose.ui.graphics.Color(0xFF1F1F1F)

        /** 설정 화면의 바탕. 순검정보다 한 톤 위라 카드·시트가 떠 보인다. */
        val wash900 = androidx.compose.ui.graphics.Color(0xFF161616)

        val wash950 = androidx.compose.ui.graphics.Color(0xFF0E0E0E)

        val slate200 = androidx.compose.ui.graphics.Color(0xFFC9D3DE)

        /** 어두운 바탕 위 강조(Material primary). 먹색 계열 위에서 8.7:1. */
        val slate300 = androidx.compose.ui.graphics.Color(0xFFA7B6C6)

        /** 원색. 청회색. 스케일의 기준 톤. 흰 글자를 얹으면 4.0:1 로 모자라 강조 표면으로는 slate700 을 쓴다. */
        val slate500 = androidx.compose.ui.graphics.Color(0xFF6D8196)

        val slate700 = androidx.compose.ui.graphics.Color(0xFF4C5D70)

        val slate800 = androidx.compose.ui.graphics.Color(0xFF2C3845)

        val slate900 = androidx.compose.ui.graphics.Color(0xFF1E2630)

        val red200 = androidx.compose.ui.graphics.Color(0xFFF2B8B0)

        /** 지우기·닫기. 수묵화에 경고색이 없어 이전 팔레트의 값을 그대로 둔다 — '열기' 와 헷갈리면 안 된다. */
        val red500 = androidx.compose.ui.graphics.Color(0xFFC0392B)

        val red800 = androidx.compose.ui.graphics.Color(0xFF7A2419)

        val red900 = androidx.compose.ui.graphics.Color(0xFF5C1A12)

        /** paper 20%. 어두운 표면의 얇은 테두리. */
        val paper20 = androidx.compose.ui.graphics.Color(0x33FFFFE3)

        /** 먹색 88%. 뒤가 살짝 비쳐 '떠 있다' 는 인상. */
        val wash700a88 = androidx.compose.ui.graphics.Color(0xE04A4A4A)

        /** 짙은 먹 94%. 뒤로 물러나되 뒤 글자가 비치지 않는 표면. 75% 로는 설정 화면 위에서 라벨이 뒤 글자와 섞였다(실기기, 2026-09-28). */
        val wash800a94 = androidx.compose.ui.graphics.Color(0xF02A2A2A)

        val slate700a94 = androidx.compose.ui.graphics.Color(0xF04C5D70)

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

        val d48 = 48.dp

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

        val ms5000: Int = 5000
    }
}

/** 의도 층. 다른 제품에 가져가는 것은 이 층과 ref 다. 이름은 '어디에 쓰는가' 가 아니라 '무엇을 뜻하는가' 로 짓는다. */
object Sys {
    object Color {
        /** 차폐 화면의 바탕. 이 앱의 기본 상태는 이 색 하나만 보이는 것이다. */
        val canvas = Ref.Palette.black

        /**
         * 캔버스 위의 글자·기호. 거의 흰색이어야 한다 — 보이는 밝기는 프레임버퍼 값 × 패널 밝기라, 회색으로 낮추면 패널 0.30 에서 읽히지 않는다.
         * 실제로 세 번 그랬다. 흑백 대비 19:1 미만은 여기 쓰지 말 것.
         */
        val onCanvas = Ref.Palette.paper

        /** 다른 앱 위에 떠 있는 UI 의 표면 — 버블, 설정의 버블 견본. */
        val floatingSurface = Ref.Palette.wash700a88

        /**
         * 떠 있는 UI 중 뒤로 물러나 보여야 하는 것 — 겨냥 칩의 비활성 상태. 물러남은 투명도가 아니라 어둠으로 낸다 — 투명하면 뒤 글자가 라벨과 섞인다.
         * 라벨(wash300) 대비 8.8:1.
         */
        val floatingSurfaceMuted = Ref.Palette.wash800a94

        /** 떠 있는 표면의 얇은 테두리. 항상 stroke.hairline 굵기로 쓴다. */
        val floatingBorder = Ref.Palette.paper20

        /** 떠 있는 표면 위의 기호·라벨. 먹색 위 5.5:1. */
        val floatingContent = Ref.Palette.wash300

        /** '열기·진행' 계열 강조. 겨냥 위(앱 열기). paper 글자 대비 6.7:1. */
        val accentPositive = Ref.Palette.slate700a94

        /** '지우기·닫기' 계열 강조. 겨냥 아래(버블 닫기). */
        val accentDestructive = Ref.Palette.red500a94

        /** 여기부터는 앱 화면(설정)의 역할. 이름은 Material 3 색 역할과 같다 — colorScheme 에 그대로 꽂힌다. */
        val background = Ref.Palette.wash900

        val onBackground = Ref.Palette.paper

        val surface = Ref.Palette.wash900

        /** surface 위 17.8:1. */
        val onSurface = Ref.Palette.paper

        val surfaceVariant = Ref.Palette.wash800

        /** 설명 문구·보조 텍스트. surface 위 11.2:1. */
        val onSurfaceVariant = Ref.Palette.wash300

        val surfaceContainerLowest = Ref.Palette.wash950

        val surfaceContainerLow = Ref.Palette.wash900

        val surfaceContainer = Ref.Palette.wash850

        val surfaceContainerHigh = Ref.Palette.wash800

        val surfaceContainerHighest = Ref.Palette.wash750

        /** 버튼·선택 표시·스위치. surface 위 8.7:1. */
        val primary = Ref.Palette.slate300

        val onPrimary = Ref.Palette.slate900

        val primaryContainer = Ref.Palette.slate800

        val onPrimaryContainer = Ref.Palette.slate200

        val secondary = Ref.Palette.wash300

        val onSecondary = Ref.Palette.wash800

        /** 선택된 탭·칩의 바탕. */
        val secondaryContainer = Ref.Palette.wash750

        val onSecondaryContainer = Ref.Palette.wash200

        /** 입력칸·라디오 테두리. surface 위 4.3:1 — 비텍스트 기준 3:1 을 넘는다. */
        val outline = Ref.Palette.wash500

        /** 구분선. */
        val outlineVariant = Ref.Palette.wash750

        val error = Ref.Palette.red200

        val onError = Ref.Palette.red900

        /** 권한 안내 카드의 바탕. */
        val errorContainer = Ref.Palette.red800

        val onErrorContainer = Ref.Palette.red200

        /** 스낵바. */
        val inverseSurface = Ref.Palette.wash200

        val inverseOnSurface = Ref.Palette.wash850

        val inversePrimary = Ref.Palette.slate700

        val scrim = Ref.Palette.black
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
        /** Android 권장 최소 터치 영역 48dp(약 9mm). 예전 44 는 iOS 값(44pt)이었다 — docs/ui-research.md §2. */
        val min = Ref.Dimension.d48
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

    /** 차폐 중 해제 제스처의 첫 동작에 잠깐 뜨는 안내('2번 더 탭하면 풀립니다'). 떠 있는 동안만 밝기를 올린다. */
    object UnlockHint {
        /** 지문 '잠금 해제' 버튼과 같은 자리 — 시계 블록과 재생 컨트롤 사이. 진행 링 자리(중앙 아래 28%)는 재생 컨트롤과 겹친다. */
        val verticalBias = 0.25f

        val fontSize = Sys.Typography.title

        val inset = Ref.Dimension.d40

        /** AOD '탭하면 10초 표시' 보다 짧게 — 읽을 문장이 한 줄뿐이다. */
        val duration = Ref.Duration.ms5000
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

        /** 잰 재생 바 높이 위에 띄우는 간격. lift 는 평소 크기 바의 하한으로만 쓴다. */
        val barGap = Sys.Spacing.md

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

        /** 글자 크기 배율을 따라 상자를 키우는 한도. 200% 에서 고정 높이면 배터리 줄이 잘렸다. */
        val maxFontGrowth = 1.5f

        /** 스크롤하면 미리보기를 이 비율까지 **통째로** 줄인다. 높이만 줄이면 시계(64sp)가 잘려 고장처럼 보인다. */
        val collapsedScale = 0.55f

        /** 무표시일 때 상자 안 안내 문구. */
        val emptyOpacity = Sys.Opacity.secondary
    }

    /** 런처 아이콘 — 검은 바탕 위 어두운 사각 틀과 가로 막대. 앱 목록에서 '꺼진 화면' 처럼 조용해야 해서 밝은 톤을 쓰지 않는다. */
    object LauncherIcon {
        val background = Sys.Color.canvas

        /** 예전 #2E2E2E 에 가장 가까운 먹 톤. */
        val frame = Ref.Palette.wash800

        /** 예전 #5A5A5A 에 가장 가까운 먹 톤. */
        val mark = Ref.Palette.wash600
    }

    object Notification {
        /** 상시 알림의 강조색(아이콘 원·액션). 설정 화면의 primary 와 같다. */
        val accent = Sys.Color.primary
    }

    /** 설정 화면. 색·글꼴은 Material 이 정하고 여기는 우리 고유 요소만. */
    object Settings {
        val screenPadding = Sys.Spacing.xl

        /** 행 전체가 누르는 자리다. 라디오를 onClick=null 로 두면 버튼이 최소 크기를 강제하지 않아 행이 38dp 였다. */
        val rowMinHeight = Sys.TouchTarget.min

        val groupPaddingHorizontal = Sys.Spacing.lg

        val groupPaddingVertical = Sys.Spacing.sm

        /** 묶음 카드 사이. 구분선 대신 톤 차이와 이 간격으로 가른다(M3 다크 테마). */
        val groupGap = Sys.Spacing.md

        val groupRadius = Sys.Radius.card

        val chipGap = Sys.Spacing.sm

        val bottomBarPadding = Sys.Spacing.md

        /** 라디오·스위치 행의 위아래. */
        val rowPaddingVertical = Ref.Dimension.d6

        val rowRadius = Sys.Radius.small

        val fontChipRadius = Sys.Radius.card

        /** 칸이 같은 폭으로 나뉘어 좌우 여백은 최소만. 8 이면 카드 안에서 필기체 칸이 두 줄로 깨졌다. */
        val fontChipPaddingHorizontal = Sys.Spacing.xs

        val fontChipGap = Ref.Dimension.d6

        val fontChipPaddingVertical = Ref.Dimension.d10

        /** 다섯 칸이 한 줄에 같은 폭으로 들어가야 한다. 18 이면 고정폭 글꼴(콜론 양옆이 넓다)이 잘렸다. */
        val fontChipSample = Sys.Typography.labelLarge

        /** 설명 문구를 위 요소에 붙이는 간격. */
        val captionGap = Ref.Dimension.d6

        /** 버블 기호 견본의 지름(dp). 단위 없는 수인 이유: 기호를 그리는 함수가 dp 정수를 받는다. */
        val swatchSize = 52

        val swatchPadding = Ref.Dimension.d6

        val selectedBorder = Sys.Stroke.emphasis

        val idleBorder = Sys.Stroke.hairline
    }
}
