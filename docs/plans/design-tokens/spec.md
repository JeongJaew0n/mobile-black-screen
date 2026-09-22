# 디자인 토큰 — 무엇을 만드는가

## 범위

1. `design/tokens/tokens.json` — W3C DTCG 형식의 **유일한 원본**. ref / sys / comp 세 층.
2. `design/tokens/build.py` — JSON → `ui/theme/Tokens.kt` 생성기. 표준 라이브러리만.
   `--check` 로 생성물이 원본과 어긋났는지 검사.
3. `ui/theme/Tokens.kt` — 생성물. **손으로 고치지 않는다.**
4. 코드의 시각 리터럴을 토큰 참조로 교체. `ui/`·`bubble/`·`data/Settings.textColor`.
5. 문서: `architecture.md`·`CLAUDE.md`·용어집·`design/tokens/README.md`(다른 제품에서 가져가는 법).

## 완료 조건

- [x] `app/src/main/java` 에 색·알파·dp·sp·ms 시각 리터럴이 **토큰 아닌 것 목록**(context.md) 외에 없다
- [x] `assembleDebug` · `lintDebug` 0 errors
- [x] `python3 design/tokens/build.py --check` 통과
- [x] **시각 변화 0** — 교체 전후 값이 전부 같다 (리팩터링이다)
- [x] `Tokens.kt` 의 옛 최상위 토큰(`FloatingSurface` 등) 참조가 코드에 없다

## 원본 형식

DTCG(<https://tr.designtokens.org/format/>). 토큰은 `$value` 를 가진 노드, 그룹은 나머지.
`$type` 은 그룹에서 상속된다. alias 는 `"{ref.palette.white}"`.

| `$type` | `$value` | Kotlin |
|---|---|---|
| `color` | `#RRGGBB` 또는 `#RRGGBBAA` | `Color(0xAARRGGBB)` |
| `dimension` | `{"value": 8, "unit": "px"}` | `8.dp` — 경로에 `fontSize`·`letterSpacing` 이 있으면 `8.sp` |
| `duration` | `{"value": 400, "unit": "ms"}` | `400` (Int, ms) |
| `number` | `0.7` / `50` | `0.7f` / `50` |

**`px` 는 논리 픽셀이다.** Android 에선 dp, iOS 에선 pt, 웹에선 px. DTCG 가 허용하는 단위가
px/rem 뿐이라 이렇게 쓴다 — Style Dictionary 의 android 변환도 같은 관례다.

## Kotlin 모양

```kotlin
object Ref  { object Palette { val ink900 = Color(0xFF141414) } object Dimension { val d8 = 8.dp } … }
object Sys  { object Color { val canvas = Ref.Palette.black; val floatingSurface = Ref.Palette.ink900a88 } … }
object Comp { object Bubble { val surface = Sys.Color.floatingSurface; val idleOpacity = Sys.Opacity.idle } … }
```

- 그룹 → 중첩 `object`(PascalCase), 토큰 → `val`(이름 그대로).
- alias 는 **Kotlin 참조**로 낸다(`Sys.Color.canvas = Ref.Palette.black`). 값을 풀어 쓰면
  JSON 의 층 구조가 생성물에서 사라진다.
- `$description` 은 KDoc 으로. 근거가 토큰과 함께 이동한다.
- `Color` 생성자는 완전한 이름으로 낸다 — `object Color` 안에서 `Color(…)` 가 자기를 가리킨다.

## 토큰 목록

### ref — 원시값

| 그룹 | 토큰 |
|---|---|
| `palette` | `black` `white` `ink900`(#141414) `ink850`(#202020) `blue500`(#2F6FA8) `red500`(#C0392B) · 반투명: `white20` `white70` `ink900a88` `ink850a75` `blue500a94` `red500a94` |
| `opacity` | `o6` `o12` `o22` `o25` `o30` `o40` `o45` `o55` `o70` `o85` `full` |
| `dimension` | `d1` `d1_5` `d2` `d3` `d4` `d5` `d6` `d8` `d10` `d12` `d14` `d16` `d18` `d20` `d24` `d28` `d30` `d32` `d40` `d44` `d46` `d52` `d56` `d64` `d72` `d84` `d112` `d180` `d300` |
| `fontSize` | `f12` `f13` `f14` `f15` `f16` `f17` `f18` `f22` `f64` |
| `letterSpacing` | `wide`(1) |
| `duration` | `ms150` `ms160` `ms400` `ms3000` |

반투명 색을 원시값으로 둔 이유: `$extensions` 로 "색 + 불투명도" 를 조합하면 이 생성기만 알아듣고
다른 도구는 불투명한 색으로 읽는다. `#RRGGBBAA` 는 표준이라 어디서든 같다.

### sys — 의도 (다른 제품에 가져가는 층)

| 그룹 | 토큰 → ref |
|---|---|
| `color` | `canvas`→black · `onCanvas`→white · `floatingSurface`→ink900a88 · `floatingSurfaceMuted`→ink850a75 · `floatingBorder`→white20 · `floatingContent`→white70 · `accentPositive`→blue500a94 · `accentDestructive`→red500a94 |
| `opacity` | `full` `strong`(.85) `secondary`(.70) `hint`(.55) `medium`(.45) `idle`(.40) `disabled`(.30) `weak`(.25) `hairline`(.22) `scrim`(.12) `faint`(.06) |
| `typography` | `display`(64) `headline`(22) `body`(18) `title`(17) `labelLarge`(16) `label`(15) `captionLarge`(14) `caption`(13) `captionSmall`(12) · `letterSpacingWide` |
| `spacing` | `xxs`(2) `xs`(4) `sm`(8) `md`(12) `lg`(16) `xl`(20) `xxl`(24) `xxxl`(32) |
| `radius` | `small`(8) `card`(12) `pill`(50%) |
| `stroke` | `hairline`(1) `emphasis`(2) |
| `motion` | `fast`(150) `fade`(400) `idleDelay`(3000) |
| `touchTarget` | `min`(44) |

`opacity` 가 열한 개인 이유: 이 앱의 글자색은 설정이 정하는 `tint` 라 색을 고정할 수 없고,
"tint 대비 몇 %" 로만 단계를 둘 수 있다. 값이 많은 것은 통합을 안 한 결과다 — checklist 참조.

### comp — 부품 (이 앱 것)

`content` · `dimmedScrim` · `unlockRing` · `slideDismiss` · `slideTrack` · `fingerprintButton` ·
`mediaBar` · `mediaControls` · `battery` · `bubble` · `aimChip` · `preview` · `settings`.
전부 sys 또는 ref 를 가리킨다. 목록은 `tokens.json` 이 곧 문서다.

## 다른 제품에서 가져가는 법

`design/tokens/README.md` 에 적는다. 요지: `tokens.json` 을 복사해 `comp` 를 지우고 자기 부품을
쓴다. `ref`·`sys` 는 그대로 두거나 값만 바꾼다. 생성은 `build.py` 를 쓰든 Style Dictionary 를
쓰든 상관없다 — 형식이 표준이다.
