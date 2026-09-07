# spec — i18n

## 목표

**언어를 하나 추가하는 일이 `values-xx/strings.xml` 한 파일을 쓰는 것으로 끝나게** 만든다.
문자열 밖에 숨어 있는 로케일 의존(시계 패턴, 숫자 서식, 어순, OS 용어)을 전부 걷어내고,
기본 로케일을 영어로 옮긴다.

## 범위

- **포함**
  - `values/` 를 영어로, 한국어를 `values-ko/` 로 이동 (47개)
  - 코드 안 하드코딩 2건 추출 (`AimTargets.kt`)
  - 문자열 밖에서 이어 붙이는 값 3건을 서식 문자열로 (`"초"`, `"dp"`, 시계 형식 예시)
  - **시계 형식을 패턴 저장 → 24/12시간제 열거형 저장으로** 바꾸고 패턴은 로케일에서 파생
  - `localeConfig` 선언 — Android 13+ 시스템 설정에 앱별 언어 항목이 생긴다
  - 디버그 빌드에 의사 로케일(`en-XA`, `ar-XB`) 활성화
  - lint 를 번역 누락에 대해 엄격하게 유지
  - 검증 레시피 (`cmd locale set-app-locales`)

- **제외**
  - 앱 안 언어 선택 UI (appcompat 필요 — context.md 판단 2)
  - 사용자가 입력한 "문장" 의 번역 (사용자 텍스트다)
  - `docs/`, 코드 주석, 커밋 메시지의 언어 변경 — **프로젝트 언어는 한국어로 유지**
  - 버블 가장자리(LEFT/RIGHT)와 밀어서 잠금 해제 방향의 RTL 미러링 (아래 §RTL)

## 완료 조건 (Definition of Done)

- [ ] **기기 언어**를 영어로 바꾸면 화면·알림·런처 이름·타일 이름·접근성 설명이 전부 영어다
- [ ] **앱별 언어**만 영어로 바꾸면 화면·알림은 영어이고, 런처·타일·접근성 설명은 기기 언어를
  따른다 — 이건 플랫폼 동작이다 (§2e). 버그로 적지 말 것
- [ ] 한국어로 되돌리면 지금과 글자 하나 다르지 않다 — **24시간제 `09:05` 의 앞자리 0 포함**
- [ ] 12시간제 시계가 한국어에서 `오후 1:05`, 영어에서 `1:05 PM` 으로 뜬다 — **패턴을 손으로 정한 곳이 없다**
- [ ] 설정의 시계 형식 라디오 예시가 현재 로케일로 서식된다
- [ ] `en-XA` 의사 로케일에서 잘리는 텍스트가 없고 `[ ]` 로 감싸이지 않은 글자가 없다
  (감싸이지 않았다 = 하드코딩이다)
- [ ] `ar-XB` 에서 설정 화면 레이아웃이 뒤집히고, 잠금 해제 트랙은 **손잡이가 왼쪽에서 시작해
  손가락 방향으로 움직인다** (§3 — 핀을 안 박으면 반대로 간다)
- [ ] `lintDebug` 0 errors. `MissingTranslation` 을 끄지 않고 통과한다
- [ ] grep 으로 Kotlin 안 한글 리터럴이 **주석 외에 0건**

## 설계

### 1. 리소스 배치

```
res/values/strings.xml        영어 (기본 폴백)
res/values-ko/strings.xml     한국어
res/values-xx/strings.xml     추가 언어는 여기만 늘린다
res/xml/locales_config.xml    지원 언어 목록 (AGP 자동 생성 — 아래)
```

AGP 의 `androidResources { generateLocaleConfig = true }` 를 켜면 `values-*` 를 보고
`locales_config.xml` 을 만들어 준다. 기본 로케일은 `res/resources.properties` 의
`unqualifiedResLocale=en-US` 로 알려 준다.

> ⚠️ 이 옵션은 AGP 8.1 에서 들어왔고 9.x 에서 DSL 위치가 바뀌었을 수 있다.
> 첫 빌드에서 확인할 것. 실패하면 `locales_config.xml` 을 손으로 쓰고 매니페스트
> `<application android:localeConfig="@xml/locales_config">` 를 직접 건다.

### 2. 문자열 밖에 있는 로케일 의존 — 이번 작업의 실제 내용

#### 2a. 시계 형식 (핵심)

```kotlin
// 저장값. 패턴이 아니다.
enum class ClockStyle(val skeleton: String) {
    H24("HHmm"),   // 앞자리 0 을 유지한다 — 아래
    H12("hmm"),
}

// 렌더 시점에 로케일에서 파생. ICU 로 만들고 ICU 로 서식한다.
fun ClockStyle.formatter(locale: Locale): android.icu.text.DateFormat {
    val pattern = android.icu.text.DateTimePatternGenerator.getInstance(locale)
        .getBestPattern(skeleton, DateTimePatternGenerator.MATCH_HOUR_FIELD_LENGTH)
    return android.icu.text.SimpleDateFormat(pattern, locale)
}
```

- `ClockFormats` 객체와 `H24`/`H12`/`LEGACY_H12` 상수는 사라진다.
- **마이그레이션**: 저장돼 있는 `"HH:mm"` → `H24`, `"a h:mm"` / `"h:mm a"` → `H12`.
  `ClockFormats.migrate()` 자리에서 한 번 하고, 새 키(`clock_style`)에 열거형 이름으로 쓴다.
- `rememberClockText(pattern)` 은 `rememberClockText(style)` 로. 로케일은
  `LocalConfiguration.current.locales[0]` 에서 받아 `remember` 키로 쓴다.
- 설정 라디오의 예시(`13:05`, `오후 1:05`)는 문자열에 박지 않고
  **고정 시각 13:05 를 현재 스타일·로케일로 서식해서** 만든다. 그래야 언어마다 맞다.

**왜 `android.text.format.DateFormat.getBestDateTimePattern` + `java.time` 이 아닌가.**
처음 설계는 그쪽이었다. 재검토에서 두 가지가 걸렸다.

1. **앞자리 0 이 사라진다.** `getBestDateTimePattern` 은 시간 필드 폭을 로케일 기본값에
   맞춘다. 한국어 24시간제 기본은 `H:mm` 이라 지금의 `09:05` 가 `9:05` 로 바뀐다.
   "한국어는 지금과 동일" 이라는 완료 조건을 깬다. `DateTimePatternGenerator` 에
   `MATCH_HOUR_FIELD_LENGTH` 를 주면 스켈레톤의 `HH` 가 그대로 살아남는다.
   Android 의 편의 함수는 이 옵션을 노출하지 않는다.
2. **`java.time.DateTimeFormatter` 는 ICU 패턴을 다 모른다.** 일부 로케일의 12시간제
   패턴에는 유연한 시간대 문자 `B`(*오후에*, *밤에*)가 들어오는데 Android 의 `java.time`
   은 `B` 를 지원하지 않아 `ofPattern` 이 예외를 던진다. ICU 가 만든 패턴은 ICU 로 서식한다.

`android.icu.*` 는 API 24+ 이고 minSdk 는 26 이다.

`getBestPattern` 결과의 CLDR 기준 예상값:

| 로케일 | `HHmm` | `hmm` |
|---|---|---|
| ko | `HH:mm` → 13:05 | `a h:mm` → 오후 1:05 |
| en-US | `HH:mm` → 13:05 | `h:mm a` → 1:05 PM |
| ja | `HH:mm` → 13:05 | `aK:mm` → 午後1:05 |

**구현 첫 단계에서 실기기로 여섯 값을 찍어 이 표를 실측값으로 바꿀 것.** 특히 ko 의 `HH:mm`
(앞자리 0)이 살아 있는지. 여기서 하나라도 어긋나면 2a 전체가 흔들린다.

#### 2b. 이어 붙이는 값

| 지금 | 바꿀 것 |
|---|---|
| `"${label}  ${seconds}초"` | `<string name="hold_duration_value">%1$s s</string>` (ko: `%1$s초`) + `String.format(Locale.getDefault(), "%.1f", …)` |
| `"${label}  ${dp}dp"` | `<string name="bubble_size_value">%1$d dp</string>` |
| `AimTargets` 의 `"앱 열기"` `"버블 삭제"` | `aim_open_app`, `aim_remove_bubble` |

`AimTargets` 는 서비스가 소유하는 창이다. `stringResource()` 는 Composable 안이라 그대로
쓸 수 있고, `ComposeView` 는 서비스 Context 의 Configuration 을 따르므로 로케일도 맞는다.

#### 2c. OS 용어를 인용하는 문자열

아래 넷은 시스템 설정 항목 이름을 인용한다. **각 언어의 Android 표기를 그대로 써야 한다.**
직역하면 사용자가 그 항목을 못 찾는다. 번역 파일 상단에 이 사실을 주석으로 남긴다.

| 키 | 인용하는 OS 항목 (en) |
|---|---|
| `overlay_permission_needed`, `bubble_permission_note` | *Display over other apps* |
| `accessibility_needed`, `accessibility_open` | *Accessibility* |
| `accessibility_description` | 시스템 접근성 목록에 그대로 노출 |

#### 2d. 앱 이름

`app_name` / `tile_label` 모두 번역한다 (en: *Black Screen*). 접근성 서비스 목록의
이름도 `@string/app_name` 을 쓰므로 함께 따라온다.

#### 2e. 앱별 언어가 닿지 않는 곳

앱별 언어(Android 13+)는 **우리 프로세스가 읽는 리소스**에만 적용된다. 다른 프로세스가
우리 APK 에서 꺼내 그리는 것은 **기기 언어**를 따른다.

| 우리가 그린다 → 앱별 언어 | 남이 그린다 → 기기 언어 |
|---|---|
| 설정 화면, 차폐 화면의 시계·힌트 | 런처의 앱 이름 |
| 알림 제목·본문 (우리가 만들어 넘긴다) | 퀵 설정 타일의 이름 (`tile_label`) |
| 버블 겨냥 목표 텍스트 | 시스템 접근성 목록의 이름·설명 (`accessibility_description`) |

검증 레시피의 `set-app-locales` 로는 오른쪽 열을 확인할 수 없다. 오른쪽은 기기 언어를
실제로 바꿔서 본다.

#### 2f. 서비스가 소유하는 창은 만들 때의 로케일로 굳는다

Activity 는 로케일이 바뀌면 재생성되지만 서비스는 아니다. 서비스가 붙인 `ComposeView` 가
Configuration 변경을 따라오는지는 보장이 없다.

지금 구조에서는 문제가 없다 — **텍스트를 가진 창은 전부 쓸 때마다 새로 만든다.**
차폐 창은 Screen Off 마다, 겨냥 목표 창은 꾹 누를 때마다, 알림은 갱신마다.
오래 사는 버블 창에는 텍스트가 없다.

**규칙: 버블 창에 텍스트를 넣지 말 것.** 넣는 순간 언어를 바꾼 뒤 옛 언어가 남는다.

### 3. RTL

`supportsRtl="true"` 는 이미 켜져 있고 설정 화면은 Compose 가 `start`/`end` 기준으로
알아서 뒤집는다. **의도적으로 뒤집지 않는 것**:

- **버블 가장자리** `Edge.LEFT` / `RIGHT` — 창 좌표(픽셀)라 Compose 의 방향과 무관하다.
  사용자가 물리적으로 끌어다 놓은 위치다. 손댈 것 없음.
- **밀어서 잠금 해제 트랙** — 왼→오른쪽으로 미는 물리 제스처다. **여기는 손대야 한다.**
- **위로 밀어 올리기 / 겨냥 위·아래** — 세로 방향이라 무관하다.

**트랙은 핀을 박지 않으면 반대로 움직인다.** `SlideToUnlockLayer` 는 손잡이를
`Alignment.CenterStart` 로 놓고 `Modifier.offset { }` 로 옮기는데, 둘 다 RTL 에서 미러링된다.
그런데 `detectHorizontalDragGestures` 의 delta 는 물리 방향이다. 결과: 손잡이가 오른쪽에서
시작해 **손가락과 반대로** 간다. 트랙 전체를

```kotlin
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { … }
```

로 감싸 방향을 고정한다. `absoluteOffset` 으로 바꾸는 것만으로는 `CenterStart` 가 남는다.

`ar-XB` 검증 때 버블·트랙이 안 뒤집힌 것을 "버그" 로 적지 말 것. 반대로 **트랙 손잡이가
오른쪽에서 시작하면 그게 버그다.**

### 4. 검증

```bash
# 앱별 언어 강제 (Android 13+). 기기 언어를 안 바꿔도 된다
adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales en-US
adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales ko-KR
adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales en-XA   # 의사 로케일
adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales ar-XB   # RTL 의사
adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales ""      # 원복

# Kotlin 안 하드코딩 (주석 제외). 0 이어야 한다
grep -rn '"[^"]*[가-힣][^"]*"' app/src/main/java --include='*.kt' | grep -v '^\S*:\s*\(//\|\*\)'
```

의사 로케일은 `buildTypes.debug { isPseudoLocalesEnabled = true }` 로 켠다.
`en-XA` 는 글자를 `[Ţĥîš]` 처럼 감싸고 30% 늘린다 — **감싸이지 않은 글자 = 추출 누락**,
잘림 = 레이아웃이 고정폭이라는 뜻이다.

⚠️ 시계는 스크린샷으로 로케일 순서를 확인할 수 있지만 **밝기는 여전히 안 된다**
(CLAUDE.md). 순서만 본다.

### 5. lint

`values-*` 가 생기는 순간 `MissingTranslation` 이 error 로 뜬다. **끄지 않는다.**
번역이 빠진 채 빌드가 통과하는 것이 이 검사의 존재 이유다. 대신:

- 번역이 필요 없는 문자열(예: 없음 — `Screen Off` 도 문장 안에 있다)은
  `translatable="false"` 로 표시한다. 지금은 해당 없음.
- `ExtraTranslation`(기본에 없는 키가 번역에만 있음)도 함께 0 을 유지한다.

## 언어 목록 — 결정 필요

이 설계는 언어 수에 무관하다. 첫 회차 제안:

| 순서 | 언어 | 근거 |
|---|---|---|
| 1 | en (기본) | 폴백. 반드시 |
| 2 | ko | 지금 상태 유지 |
| 3 | ja | `aK:mm` 처럼 시계 패턴이 갈리는 언어라 **2a 가 맞게 됐는지 검증하는 값**이 있다 |

그 이상은 요청이 있을 때 `values-xx/` 하나씩 추가한다.
