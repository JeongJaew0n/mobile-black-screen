# UI 연구 — 무엇을 어떻게 고치면 좋은가

> 2026-10-01. **의견 문서다.** 서칭 → 후보 → 반박 서칭 → 이 코드베이스에 대 보는 탐색 을 세 번 돌렸다.
> `docs/ux-research.md` 가 "무엇을 할 수 있어야 하는가(UX)" 였다면 이 문서는 "어떻게 보이고 어떻게
> 눌리는가(UI)" 다. 차폐 화면은 이미 규칙이 강하게 서 있어(검정·흰 글자·밝기 0.30) 제안 대부분이 **설정 화면**에 있다.
> "코드 확인" 은 소스를 읽어 사실로 확인한 것, "미검증" 은 기기에서 안 해 본 것이다.
>
> **2026-10-01 — 1~7 전부 구현.** `docs/plans/ui-improvements/`.

## 결론 먼저

| 순위 | 제안 | 근거 종류 | 비용 |
|---|---|---|---|
| 1 | **미리보기를 화면 표시 탭 맨 위로** (접히는 머리) | 코드 확인 + 패턴 | 중간 |
| 2 | **행 전체를 누르게 · 48dp 로** — 스위치 행, 라디오 행, 재생 버튼 | 코드 확인 + 가이드 | 작음 |
| 3 | **짧은 단일 선택은 칩 한 줄로** — 자동 끄기(6개), 시계 형식(2개) | 가이드 | 작음 |
| 4 | **설정을 묶음 카드로** — 구분선+라벨 대신 `surfaceContainer` 카드 | 플랫폼 관례 | 중간 |
| 5 | **시작 버튼을 아래로** (하단 고정 바) | 연구 + 플랫폼 관례 | 중간 |
| 6 | 탭 이름 `화면 표시 / 설정` → `화면 / 동작` | 판단 | 작음 |
| 7 | 글자 크기 200% 에서 한 번 보기 | 플랫폼 변화 | 테스트만 |

**뺀 것:** 밤 모드 붉은 글자, 번짐(halation)을 이유로 글자·바탕 톤 낮추기, M3 Expressive 버튼 그룹, 버블 불투명도 조정. 이유는 아래.

---

## 1. 미리보기를 맨 위로

### Round 1 — 패턴
삼성 잠금 화면 편집기, 워치 페이스 편집기는 **실시간 미리보기를 화면에서 가장 눈에 띄는 자리**에 두고,
그 아래에서 요소를 고른다([XDA — Samsung 잠금 화면](https://www.xda-developers.com/customize-lock-screen-samsung-device/),
[SamMobile — One UI 5 잠금 화면](https://www.sammobile.com/news/one-ui-5-0-feature-focus-new-custom-lock-screen-options/),
[Jetpack Watch Face 에디터](https://android-developers.googleblog.com/2021/12/develop-watch-faces-with-stable-jetpack.html)).
바꾸는 것과 결과가 한 화면에 같이 있어야 한다.

### Round 2 — 이 앱에는 더 필요하다
이 앱의 "화면 표시" 탭은 **그 자체가 차폐 화면 편집기**다. 시계·글꼴·날짜·배터리·문장을 고르는 이유는
차폐 화면이 어떻게 보일지다.

### Round 3 — 코드 확인
`SettingsScreen.kt` 의 화면 표시 탭 순서: 시계 스위치 → 시계 형식 → 글꼴 → 날짜 → 날짜 형식 → 배터리 →
재생 표시 → 문장 → **미리보기(맨 끝)**. 그리고 미리보기는 `hasContent || mediaPreview` 일 때만 보인다.

→ 기본 상태(아무것도 표시 안 함)에서 시계를 켜면 미리보기가 **화면 밖 맨 아래에** 생긴다. 켠 결과를 보려면
스크롤해야 하고, 글꼴 칩을 바꿀 때마다 다시 내려가야 한다.

**제안**
- 미리보기를 탭 콘텐츠 **맨 위**로. 스크롤하면 높이가 줄어드는 접히는 머리(180dp → 96dp 정도)로 해서
  아래 옵션이 밀리지 않게.
- 아무것도 표시 안 할 때도 미리보기를 보여 준다 — 검은 상자 하나와 "아무것도 표시하지 않습니다".
  이 앱의 기본 상태가 그것이고, 사용자가 **무엇을 켜야 하는지** 알게 된다.
- 기존 "실제 화면은 이보다 어둡습니다" 캡션은 그대로 미리보기 아래에.

## 2. 행 전체를 누르게 · 48dp

### Round 1 — 가이드
Android 권장 터치 영역은 **48×48dp**(약 9mm)다([Android 접근성 도움말](https://support.google.com/accessibility/android/answer/7101858?hl=en)).
Compose 의 `RadioButton`·`Switch` 는 **스스로 눌릴 때만** 이 최소 크기를 강제한다
([Compose API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)).

### Round 2 — 이 앱의 짜임이 그 예외에 걸린다
행 자체를 누르게 하려고 `RadioButton(onClick = null)` 로 두면 버튼은 최소 크기를 강제하지 않는다. 그러면 행
높이는 **글자 한 줄 + 위아래 여백**이 된다.

### Round 3 — 코드 확인 셋
| 위치 | 지금 | 문제 |
|---|---|---|
| `SwitchRow` | `Row` 에 클릭 없음. `Switch` 만 눌린다 | **라벨을 눌러도 안 켜진다.** 긴 설명이 붙은 행(버블·재생 표시)일수록 누를 자리가 스위치 하나로 좁다. TalkBack 은 라벨과 스위치를 따로 읽는다 |
| `RadioRow` | `.padding(vertical = 6dp)` + 글자 한 줄 | 스크린샷 기준 행 간격 약 100px ≈ **38dp**. 48dp 미만 |
| `Comp.MediaControls.buttonSize` | `sys.touchTarget.min` = **44dp**, 설명 "접근성 권장 최소 터치 영역" | 44 는 **iOS** 값(44pt)이다. Android 는 48dp. 토큰 설명이 틀렸다 |

**제안**
- `SwitchRow` 를 `Modifier.toggleable(value, role = Role.Switch)` 로 행 전체에 걸고 `Switch(onCheckedChange = null)`.
  행 전체가 한 번에 읽히고 눌린다.
- `RadioRow`·`ModeRow` 에 `heightIn(min = 48.dp)` — 토큰으로 `comp.settings.rowMinHeight`.
- `sys.touchTarget.min` 을 **48dp** 로. 재생 버튼 셋이 함께 커진다. 차폐 하단 여백은 충분하다.

## 3. 짧은 단일 선택은 칩 한 줄로

### Round 1
Material 3 은 라디오를 **5개 이하**의 단일 선택에 권한다([M3 Radio button](https://m3.material.io/components/radio-button/guidelines)).
자동 끄기는 6개(디버그 7개)라 설정 탭이 세로로 길어졌다. 대안으로 세그먼트 버튼([M3 Segmented button](https://m3.material.io/components/segmented-buttons/guidelines))이 떠올랐다.

### Round 2 — 세그먼트 버튼은 물러났다
M3 Expressive 업데이트에서 세그먼트 버튼은 더 이상 권장되지 않고 **연결된 버튼 그룹**이 대신한다
([M3 Segmented button](https://m3.material.io/components/segmented-buttons/guidelines),
[Joe Birch — ButtonGroup](https://joebirch.co/android/exploring-jetpack-compose-buttongroup/)).
그런데 Compose 에서 그건 `@ExperimentalMaterial3ExpressiveApi` 다
([ButtonGroup API](https://composables.com/jetpack-compose/androidx.compose.material3/material3/components/ButtonGroup/api)).
M3 의 또 다른 대안인 **필터 칩 단일 선택**은 안정 API 다([M3 Chips](https://m3.material.io/components/chips/guidelines)).

### Round 3 — 이 앱에 대 보기
- 실험 API 는 버전마다 바뀔 수 있다. 이 프로젝트는 "검증 범위를 넘는 변경을 들이지 않는다"(`targetSdk` 고정 원칙)를
  지켜 왔다 → **안정 API 인 `FilterChip` + `FlowRow`**.
- `FlowRow` 는 글자 크기를 키우면 줄바꿈된다 — 7번(글자 200%)과도 맞는다.
- 대상: 자동 끄기(`사용 안 함 · 15분 · 30분 · 1시간 · 2시간`), 시계 형식(`24시간제 · 오전·오후`).
- **라디오를 그대로 둘 곳:** 소리(3개, 각각 설명이 붙어 있어 칩에 못 넣는다), 해제(4개, 고르면 아래에 누르는
  시간 슬라이더가 붙는다).

## 4. 설정을 묶음 카드로

### Round 1
Android 16 의 M3 Expressive 설정 개편은 **모든 페이지를 컨테이너로 묶어** 항목을 그룹 짓는다
([9to5Google — Android 16 QPR1](https://9to5google.com/2025/09/03/android-16-qpr1-pixel/),
[Android Authority](https://www.androidauthority.com/material-3-expressive-more-android-settings-3574011/)).
삼성 One UI 설정도 둥근 카드 묶음이다 — 이 앱 사용자가 매일 보는 모양이다.

### Round 2 — 어둠 속에서 경계를 어떻게 내나
M3 는 다크 테마에서 그림자 대신 **톤 차이**로 표면을 가른다([M3 Elevation](https://m3.material.io/styles/elevation/applying-elevation),
[M3 Color roles](https://m3.material.io/styles/color/roles)). 지금 설정 화면은 섹션을 **구분선 + 라벨**로만 가른다.
배경 `#161616` 위의 선이라 묶음이 약하다.

### Round 3 — 토큰에 이미 있다
수묵화 토큰에 `surfaceContainer`(`#1F1F1F`)·`surfaceContainerHigh`(`#2A2A2A`) 가 이미 있다. 새 색이 필요 없다.
섹션마다 `surfaceContainer` 카드(반지름 `sys.radius.card` 12dp) 하나. 구분선은 없앤다.
권한 카드(`errorContainer`)와 겹칠 때 색 역할이 달라 구분된다(M3 권고 그대로).

## 5. 시작 버튼을 아래로

### Round 1
사람들의 49% 는 한 손으로 폰을 쥔다. 엄지가 편한 곳은 **화면 아래 가운데**, 닿기 어려운 곳은 위쪽 구석이다
([Hoober — UXmatters](https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php),
[Smashing — Thumb Zone](https://www.smashingmagazine.com/2016/09/the-thumb-zone-designing-for-mobile-users/)).
삼성 One UI 는 화면을 **보는 영역(위)** 과 **조작 영역(아래)** 으로 나누고, 버튼은 아래에 둔다
([One UI 디자인 가이드](https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf)).

### Round 2 — 반박
이 앱을 켜는 주 경로는 **타일과 버블**이다. 설정 화면의 시작 버튼은 설정을 바꾼 뒤 확인하는 용도가 크다.
위에 있어도 치명적이지 않다.

### Round 3 — 그래도 옮길 이유
- 1번(미리보기를 맨 위로)을 하면 위쪽이 미리보기 자리가 된다. **보는 것은 위, 누르는 것은 아래** — One UI 의 구분과 그대로 맞는다.
- UX 개선에서 붙인 "3번 탭하면 풀립니다 · 1시간 뒤 화면이 꺼집니다" 줄이 버튼과 함께 아래에 고정되면,
  어느 탭을 어디까지 스크롤해도 늘 보인다.
- 하단 고정 바는 edge-to-edge 에서 내비게이션 바 인셋을 받아야 한다(`WindowInsets.navigationBars`).

## 6. 탭 이름

`화면 표시 / 설정` — 설정 화면 안에 "설정" 탭이 있다. 둘 다 설정이다. 나눈 기준은 "무엇이 보이는가 / 어떻게
동작하는가" 였다(코드 주석). **`화면 / 동작`** 이 그 기준을 그대로 말한다. en `Display / Behavior`, ja `表示 / 動作`.
근거는 판단뿐이라 순위를 낮게 둔다.

## 7. 글자 크기 200%

Android 14 부터 글자 크기를 **200%** 까지 키울 수 있고, 비선형이라 작은 글자는 크게, 큰 글자는 조금만 커진다
(57sp 가 약 112%)([Android 14 features](https://developer.android.com/about/versions/14/features),
[Flutter 이관 문서](https://docs.flutter.dev/release/breaking-changes/android-14-nonlinear-text-scaling-migration)).

이 앱에 대 보면:
- 차폐 시계 64sp 는 거의 안 커진다 — 괜찮다.
- 날짜 22sp, 해제 안내 17sp, 재생 바 12~16sp, 겨냥 칩 15sp 는 크게 커진다. **재생 바와 제어 버튼이 겹치는지**,
  겨냥 칩 라벨이 한 줄에 드는지 봐야 한다. 미검증.
- 시작 버튼 아래 줄은 지금도 두 줄로 넘어간다(자동 끄기를 켜면). 200% 면 서너 줄 — 5번의 하단 바 높이에 영향.

`adb shell settings put system font_scale 2.0` 으로 한 번 보고 되돌리면 된다.

---

## 뺀 것과 이유

| 후보 | 뺀 이유 | 회차 |
|---|---|---|
| **밤 모드 붉은 글자** (iOS StandBy 식) | 1회차에 StandBy 의 붉은 야간 모드를 봤다([Apple](https://support.apple.com/guide/iphone/use-standby-iph878d77632/ios)). 2회차에서 근거가 약해졌다. ① 전력: OLED 적색 소자는 녹색과 같고 청색보다 적게 먹지만([US 특허 자료](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/9347647)), 이 앱은 글자 픽셀의 전력이 측정 해상도 아래였다(시계 켬·끔 183.3 mA 동일). ② 수면: 적색광은 청색광보다 멜라토닌을 덜 억제하지만, 그 효과를 보려면 **3 lux 미만으로 어두워야** 한다([PMC 연구](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC12113466/), [Calm](https://www.calm.com/blog/red-light-sleep)). ③ 이 앱의 규칙: 글자는 거의 흰색이어야 패널 0.30 에서 읽힌다. 적색은 휘도가 흰색의 약 1/4 이라 같은 함정에 걸릴 가능성이 높다. **해 보려면 기기에서 먼저 읽히는지 봐야 한다** | 2 |
| **번짐(halation)을 이유로 바탕을 회색·글자를 #DCDCDC 로** | 순검정 위 순백 글자는 번져 보인다는 권고가 많다([rs999](https://www.rs999.in/blog/halation-bloom-in-dark-mode-graphics-why-your-white-text-vibrates-on-black-and-the-anti-glow-fix-pros-use), [bushe](https://bushe.co/blog/dark-mode-hurts-eyes-how-to-fix/)). 그러나 ① 차폐 바탕은 **발광하지 않는 순검정이 존재 이유**다 ② 글자를 회색으로 낮춰 세 번 안 보였다 ③ 글자는 이미 순백이 아니라 `#FFFFE3`(한지색)이고 패널 밝기가 0.30 이라 휘도 자체가 낮다. **설정 화면**은 이미 권고대로다 — 바탕 `#161616`, 글자 대비 17.8:1(권고 15~18:1) | 2 |
| **M3 Expressive 연결 버튼 그룹** | 세그먼트 버튼의 후계지만 Compose 에서 실험 API 다. 안정 API 인 필터 칩으로 같은 일을 한다(3번) | 3 |
| **버블 불투명도 60~70% 로 고정** | 플로팅 버튼 앱들은 60~70% 를 권한다([Floatify](https://floatifyapp.com/blog/customize-android-floating-button)). 이 앱은 만질 때 100%, 3초 뒤 40% 로 흐려진다 — 쓸 때 또렷하고 안 쓸 때 물러난다. 고정값보다 낫다 | 2 |

## 차폐 화면은 왜 거의 안 건드리나

차폐 화면의 규칙은 전부 실측에서 나왔다 — 순검정(전력), 거의 흰 글자(패널 0.30 가독성), 번인 궤도(패널 보호),
밝기 상수(0.30 과 0 의 전력 동일). 이번 서칭에서 이 규칙들을 뒤집을 근거는 나오지 않았다. 오히려 번짐·붉은
글자 논의는 "왜 지금 값인가" 를 다시 확인시켰다. 차폐 화면에서 남은 것은 7번(큰 글자에서 겹침) 하나다.

## 구현 순서 제안

2번 → 3번 → 1번 → 4번 → 5번 → 6번. 2·3번은 작고 독립적이다. 1·4·5번은 설정 화면의 짜임을 함께 바꾸니 한 번에
하는 게 낫다(미리보기가 위로, 버튼이 아래로, 가운데가 카드 묶음으로). 7번은 1·4·5 뒤에 한 번 본다.
