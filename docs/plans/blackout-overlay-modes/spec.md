# spec — blackout-overlay-modes

## 목표

스마트폰 화면을 검게 덮어 "화면만 꺼진" 상태를 만드는 Android 앱을 구현한다.
완료 시 사용자는 퀵 설정 타일 한 번의 탭으로 화면을 완전한 검정으로 만들 수 있고,
설정에서 시계·문장 표시 여부와 차폐 방식(Blackout / Overlay)을 고를 수 있다.

## 배경 제약 (설계를 결정한 두 가지 하드 제약)

1. **임의 시점에 화면을 끄는 공개 API가 Android에 없다.** 그리고 물리적으로 화면을 끄면
   요구사항인 "문장/시간 표시"가 불가능해진다. 두 요구는 모순이므로,
   "화면을 끈다"가 아니라 **"화면을 검게 덮고 밝기를 0으로 내린다"**로 정의를 바꿨다.
   AMOLED 기기에서는 검은 픽셀이 발광하지 않아 체감상 꺼진 것과 거의 같다.

2. **`TYPE_APPLICATION_OVERLAY`는 상태바도 내비게이션 바도 덮을 수 없다.** Android 8(O)에서
   시스템 UI 위에 그리는 것이 의도적으로 금지되었고 Google이 버그가 아니라고 확인했다.
   `FLAG_LAYOUT_IN_SCREEN` / `FLAG_LAYOUT_NO_LIMITS` / `FLAG_LAYOUT_INSET_DECOR`
   어떤 조합으로도 뚫리지 않으며 루팅 없이 우회 수단이 없다.
   실측(Galaxy S23+ / Android 16): 오버레이 창 프레임이 `[0,94][1080,2214]` 로 상태바(94px) 아래에서 시작해 내비바(126px) 위에서 끝난다. z-order 상으로도 `NavigationBar`·`StatusBar` 가 오버레이보다 위에 있어 기하학·레이어 양쪽에서 막혀 있다.

이 두 제약 때문에 "다른 앱 계속 렌더링"과 "완전한 검정"을 동시에 얻을 수 없고,
**두 모드를 모두 구현해 사용자가 고르는 하이브리드**로 간다.

## 두 모드의 정의

|  | **Blackout** (기본값) | **Overlay** |
|---|---|---|
| 구현 | 풀스크린 Activity + 몰입 모드 | `TYPE_APPLICATION_OVERLAY` + Foreground Service |
| 상태바 / 내비바 | 완전히 숨김 | **둘 다 못 덮음** (실기기 확인) |
| 다른 앱 UI 렌더링 | 멈춤 | 계속 렌더링 |
| 다른 앱 백그라운드 동작 | 계속 동작 | 계속 동작 |
| 권한 | 없음 | 다른 앱 위에 표시 + 알림 |
| 상시 알림 | 없음 | 있음 (제거 불가) |

> 오해 방지: Blackout 모드에서도 음악 재생·다운로드/업로드·내비게이션·타이머·동기화·
> 포그라운드 서비스는 전부 그대로 돌아간다. 멈추는 것은 **직전 최상단 앱의 UI 렌더링뿐**
> (게임 진행, 영상 재생). Overlay 모드가 추가로 사는 것은 정확히 이 한 가지다.

## 범위

- **포함 (1차 — Blackout)**
  - 풀스크린 Activity, 시스템 바 완전 은닉, `screenBrightness = 0f`, `FLAG_KEEP_SCREEN_ON`
  - 표시 콘텐츠: 무표시(기본) / 시계 / 문장 / 시계+문장
  - 롱프레스 해제 제스처 + 진행 피드백
  - AMOLED 번인 방지 픽셀 시프트
  - 설정 화면 (DataStore 영속화)
  - 퀵 설정 타일 진입점

- **포함 (2차 — Overlay)**
  - `OverlayService` (LifecycleService + FGS `specialUse`)
  - `WindowManager` 오버레이 창, 터치 삼킴, Compose ViewTree owner 배선
  - `SYSTEM_ALERT_WINDOW` 권한 안내 플로우
  - 설정에서 모드 전환, 알림 액션으로 해제
  - 상태바가 남는다는 제약을 설정 화면에 명시

- **제외 (3차 이후, 이번 범위 아님)**
  - 자동 종료 타이머
  - 근접센서 포켓 모드 (`PROXIMITY_SCREEN_OFF_WAKE_LOCK`)
  - 문장 여러 개 순환 / 시간대별 문구
  - 홈 화면 위젯·바로가기
  - 배터리 최적화 예외 안내 (제조사별 가이드)
  - **iOS 전면 제외** (오버레이·백그라운드 실행이 OS 차원에서 차단)
  - **Google Play 출시 대응** (개인용 사이드로딩 확정)

## 완료 조건 (Definition of Done)

> 전 항목 실기기 검증 완료 — Galaxy S23+ (SM-S916N) / Android 16 / SDK 36 / AMOLED

### 1차 — Blackout
- [x] 앱 실행 → 차폐 시작 시 화면이 완전한 검정이 되고 상태바·내비바가 보이지 않는다
- [x] 차폐 중 화면이 자동으로 꺼지지 않는다 (`FLAG_KEEP_SCREEN_ON`)
- [x] 단일 탭으로는 해제되지 않고, 1.5초 롱프레스로만 해제된다
- [x] 롱프레스 중 원형 진행 피드백이 보인다
- [x] 설정에서 시계 표시를 켜면 검은 화면에 시각이 **읽을 수 있게** 보인다
      (최초 구현은 글자색과 패널 밝기를 둘 다 낮춰 판독 불가였다 — 이후 수정)
- [x] 설정에서 문장을 입력하면 검은 화면에 그 문장이 보인다
- [x] 시계는 분 단위로만 갱신된다 (초 단위 갱신 없음)
- [x] 번인 방지가 켜져 있으면 60초마다 텍스트 위치가 미세하게 이동한다
- [x] 설정이 앱 재시작 후에도 유지된다
- [x] 퀵 설정 타일을 탭하면 즉시 차폐가 시작된다
- [x] 권한 요청이 단 한 번도 발생하지 않는다

### 2차 — Overlay
- [x] 설정에서 Overlay 모드 선택 시 권한 미허용이면 설정 화면으로 안내된다
- [x] Overlay 차폐 중 아래 앱이 계속 렌더링된다 (영상 재생으로 확인)
- [x] Overlay 차폐 중 터치가 아래 앱으로 통과되지 않는다
- [x] 롱프레스로 오버레이가 해제되고 서비스가 종료된다
- [x] 알림의 해제 액션으로도 종료된다
- [x] 서비스 종료 시 창이 누수 없이 제거된다

## 기술 스택 / 인터페이스

```
Kotlin + Jetpack Compose
Gradle 9.7.1 / AGP 9.3.2 / Kotlin 2.4.10 (AGP 내장)
Compose BOM 2026.08.00
compileSdk 37   (Compose 1.12 요구)
targetSdk  36   (미검증 동작 변경을 떠안지 않기 위해 의도적으로 유지)
minSdk     26   (TYPE_APPLICATION_OVERLAY 요구 하한)
설정 저장  DataStore (Preferences)
DI         사용 안 함 (수동 주입으로 충분한 규모)
```

### 모듈 구조
```
app/src/main/java/.../blackscreen/
├─ MainActivity.kt              설정 화면 (진입점)
├─ blackout/BlackoutActivity.kt Blackout 모드
├─ overlay/OverlayService.kt    LifecycleService + FGS(specialUse)
├─ overlay/OverlayWindow.kt     WindowManager 창 생성/해제
├─ ui/BlackScreenContent.kt     ★ 두 모드 공유 Composable
├─ ui/BurnInShift.kt            픽셀 시프트
├─ ui/UnlockGesture.kt          해제 제스처 + 피드백
├─ ui/settings/SettingsScreen.kt
├─ data/Settings.kt
├─ data/SettingsRepository.kt   DataStore 래퍼
└─ tile/BlackScreenTileService.kt
```

### 설정 스키마
```kotlin
data class Settings(
    val mode: Mode = Mode.BLACKOUT,       // BLACKOUT | OVERLAY
    val showClock: Boolean = false,       // 기본은 "아무것도 없음"
    val clockFormat: String = "HH:mm",
    val sentence: String = "",
    val textLevel: Int = 3,               // 1~5, 텍스트 그레이 밝기
    val burnInShiftEnabled: Boolean = true,
    val unlockGesture: Gesture = Gesture.LONG_PRESS,
)
```

### 권한 (Overlay 모드에서만)
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 의존성

- **JDK 21** — 설치 확인됨 (Temurin 21.0.10)
- **Android SDK — 설치 완료.** `brew install --cask android-commandlinetools`,
  SDK root `/opt/homebrew/share/android-commandlinetools`.
  `local.properties` 의 `sdk.dir` 로 연결한다(이 파일은 gitignore 대상이므로
  다른 머신에서는 다시 만들어야 한다).
- Gradle Wrapper — gradle 미설치 상태에서 `gradle wrapper` 를 못 써
  wrapper jar 와 스크립트를 직접 내려받아 커밋했다.
- **실기기 — 없음.** `adb` 는 사용 가능하지만 연결된 기기가 없다.
  DoD 의 동작 확인 항목은 전부 미검증 상태다.

## 비고 — 의도적으로 뺀 것과 가정

- **전력 소모 수치를 스펙에 못 박지 않았다.** 이 앱은 화면을 끄지 않으므로 디스플레이
  파이프라인이 계속 살아 있다. 실측 없이 숫자를 말할 근거가 없어, 추후
  `adb shell dumpsys batterystats` + Battery Historian으로 측정 후 반영한다.
- **LCD 기기에서는 백라이트가 남아 완전히 검게 보이지 않는다.** 회피 불가. AMOLED 최적화가
  전제이며 첫 실행 안내로 처리한다(3차).
- **볼륨 키 해제는 넣지 않는다.** Blackout 모드는 `onKeyDown`으로 받을 수 있지만 Overlay
  모드는 `FLAG_NOT_FOCUSABLE` 때문에 키 이벤트를 못 받아 모드 간 동작이 갈린다.
  일관성을 위해 제외.
- **부팅 후 자동 시작은 넣지 않는다.** Android 15+에서 백그라운드 FGS 시작 시 오버레이
  예외를 쓰려면 권한과 "이미 보이는 오버레이 창"이 둘 다 필요해
  `ForegroundServiceStartNotAllowedException`이 발생한다. 실익 대비 위험이 크다.
