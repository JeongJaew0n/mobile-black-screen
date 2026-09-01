# mobile-black-screen — 구현 설계

작성일: 2026-08-30
최종 갱신: 2026-08-31 — 실제 구현 결과에 맞춰 동기화 (툴체인, 회전 처리, 중계 Activity)
전제: [technical-feasibility.md](./technical-feasibility.md) 의 분석 결과
진행 상황: [plans/blackout-overlay-modes/checklist.md](./plans/blackout-overlay-modes/checklist.md)

---

## 0. 확정된 결정

| 항목 | 결정 | 근거 |
|---|---|---|
| 플랫폼 | **Android 단독** | iOS는 오버레이·백그라운드 실행이 OS 차원에서 차단 |
| 차폐 방식 | **하이브리드** (Blackout 기본 + Overlay 선택) | 상태바를 덮을 수 없는 제약 때문에 두 모드의 장단이 갈림 |
| 배포 | **개인용 / 사이드로딩** | Play 심사 리스크 전부 소거 |

**배포가 사이드로딩으로 확정되면서 원래 최대 리스크였던 `specialUse` 포그라운드 서비스 심사(R1)가 사라졌습니다.** `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE_SPECIAL_USE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`를 정책 눈치 보지 않고 쓸 수 있습니다.

---

## 1. 두 모드의 정확한 차이

|  | **Blackout** (기본) | **Overlay** |
|---|---|---|
| 구현 | 풀스크린 Activity + 몰입 모드 | `TYPE_APPLICATION_OVERLAY` + Foreground Service |
| 상태바 / 내비바 | **완전히 숨김** | **둘 다 못 덮음** (실기기 확인) |
| 다른 앱 UI 렌더링 | 멈춤 (pause/stop) | **계속 렌더링** |
| 다른 앱 백그라운드 동작 | **계속 동작** | 계속 동작 |
| 권한 | 없음 | 다른 앱 위에 표시 + 알림 |
| 상시 알림 | 없음 | 있음 (제거 불가) |
| 터치 | Activity가 수신 | 오버레이가 삼킴 |

> **자주 오해하는 지점:** Blackout 모드에서도 음악 재생·다운로드/업로드·내비게이션·타이머·동기화·포그라운드 서비스는 전부 그대로 돌아갑니다. Android에서 백그라운드 앱은 죽지 않습니다. 멈추는 건 **직전 최상단 앱의 UI 렌더링뿐**입니다 — 즉 게임 진행이나 영상 재생이 멈춥니다. Overlay 모드가 추가로 사는 건 정확히 이 한 가지입니다.

### 상태바 제약 (근거)

`TYPE_APPLICATION_OVERLAY`는 Android 8(O)부터 **시스템 UI 위에 그리는 것이 의도적으로 금지**되어 있습니다. `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`, `FLAG_LAYOUT_INSET_DECOR` 등 어떤 플래그 조합으로도 뚫리지 않으며, Google이 버그가 아닌 의도된 동작이라고 확인했습니다. 루팅 없이는 우회 수단이 없습니다.

> **실기기 정정:** 최초 설계에서는 "내비게이션 바 영역은 `FLAG_LAYOUT_NO_LIMITS` 로 덮인다"고 적었으나 **틀렸습니다.**
> 실측(Galaxy S23+ / Android 16): 오버레이 창 프레임이 `[0,94][1080,2214]` 로 상태바(94px) 아래에서 시작해 내비바(126px) 위에서 끝난다. z-order 상으로도 `NavigationBar`·`StatusBar` 가 오버레이보다 위에 있어 기하학·레이어 양쪽에서 막혀 있다.
> 즉 **상태바와 내비게이션 바 둘 다 남습니다.**

---

## 2. 기술 스택

```
Kotlin + Jetpack Compose
Gradle 9.7.1 / AGP 9.3.2 / Kotlin 2.4.10 (AGP 내장)
Compose BOM 2026.08.00  (Compose 1.12 / Material3 1.4)
compileSdk 37   (Compose 1.12 가 요구)
targetSdk  36   (의도적으로 유지 — 아래 참조)
minSdk     26   (TYPE_APPLICATION_OVERLAY 요구 하한)
설정 저장  DataStore (Preferences)
DI         불필요 (수동 주입으로 충분한 규모)
```

**targetSdk 를 36 에 두는 이유:** compileSdk 는 Compose 1.12 때문에 37 로 올렸지만,
targetSdk 를 올리면 새 OS 의 동작 변경을 옵트인하게 된다. 실기기 검증 수단이 없는 상태에서
확인하지 못한 변경을 떠안지 않는다. lint 의 `OldTargetApi` 경고 1건은 이 선택의 결과다.

**AGP 9 의 내장 Kotlin 을 쓴다.** 외부 `org.jetbrains.kotlin.android` 플러그인은 AGP 9 의
새 DSL(`android.newDsl=true` 기본값)이 `BaseExtension` 을 제거해 `ClassCastException` 으로
실패한다. `org.jetbrains.kotlin.plugin.compose` 만 적용하면 된다.

Compose를 쓰는 이유는 **두 모드가 표시 콘텐츠(시계/문장/무표시)를 하나의 Composable로 공유**하기 위해서입니다. 오버레이에 Compose를 얹는 건 약간의 배선이 필요하지만(§4.2) 콘텐츠 로직을 두 번 짜는 것보다 낫습니다.

> **참고 (선택):** 사이드로딩 전용이라면 `targetSdk`를 33으로 낮춰 `foregroundServiceType` 선언 의무 자체를 회피할 수도 있습니다. 다만 최신 OS 동작에서 벗어나므로 권장하지 않고, targetSdk 36 + `specialUse`로 정직하게 가는 것을 기본으로 둡니다.

---

## 3. 모듈 구조

```
app/src/main/java/.../blackscreen/
├─ MainActivity.kt              설정 화면 (진입점)
├─ blackout/
│   └─ BlackoutActivity.kt      Blackout 모드 — 풀스크린 + 몰입
├─ service/
│   └─ ScreenCoverService.kt    ★ 차폐 창 + 버블 창을 함께 소유하는 상태 기계
├─ overlay/
│   ├─ OverlayWindow.kt         차폐 창 LayoutParams
│   └─ OverlayLauncherActivity.kt  투명 중계 Activity (§4.2b)
├─ bubble/
│   ├─ BubbleWindow.kt          버블 창 LayoutParams + 가장자리 좌표 계산
│   ├─ BubbleContent.kt         원형 버블 (탭/드래그/유휴 페이드)
│   └─ RemoveTarget.kt          드래그 중 뜨는 ✕ 타겟
├─ ui/
│   ├─ BlackScreenContent.kt    ★ 두 모드 공유 Composable
│   ├─ Clock.kt                 분 경계 정렬 시계
│   ├─ BurnInShift.kt           픽셀 시프트 로직
│   ├─ UnlockGesture.kt         해제 제스처 + 진행 피드백
│   └─ settings/SettingsScreen.kt
├─ data/
│   ├─ Settings.kt              설정 데이터 클래스
│   └─ SettingsRepository.kt    DataStore 래퍼
└─ tile/
    └─ BlackScreenTileService.kt  퀵 설정 타일
```

---

## 4. 핵심 구현

### 4.1 Blackout 모드

```kotlin
class BlackoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 시스템 바 완전히 숨김
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // 2) 밝기 최소 + 화면 꺼짐 방지
        window.attributes = window.attributes.apply { screenBrightness = 0f }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent { BlackScreenRoot(onUnlock = { finish() }) }
    }
}
```

`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`는 가장자리 스와이프 시 시스템 바가 잠깐 나타나는 동작입니다. 비-sticky 모드는 바가 나타난 뒤 계속 남으므로, 몰입 유지를 위해 이 값이 맞습니다.

**Manifest:**
```xml
<activity
    android:name=".blackout.BlackoutActivity"
    android:theme="@style/Theme.Blackout"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"
    android:excludeFromRecents="true"
    android:launchMode="singleTask" />
```

> **정정:** 처음에는 `screenOrientation="nosensor"` 로 회전 자체를 막으려 했으나,
> **Android 16 부터 고정 화면 방향 지정은 대부분의 경우 무시됩니다**(lint `DiscouragedApi`).
> 방향을 고정하는 대신 `configChanges` 로 **재생성 자체를 막는** 쪽이 모든 버전에서
> 동작하며, 애초의 목적(회전 시 검은 화면 깜빡임 방지)에도 정확히 부합합니다.

### 4.2 Overlay 모드

**창 파라미터:**
```kotlin
val params = WindowManager.LayoutParams(
    MATCH_PARENT, MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    FLAG_NOT_FOCUSABLE or        // 아래 앱의 키/IME 포커스를 뺏지 않음
    FLAG_LAYOUT_IN_SCREEN or
    FLAG_LAYOUT_NO_LIMITS or     // 다른 기기·버전 대비. 실측상 효과 없음
    FLAG_KEEP_SCREEN_ON,
    PixelFormat.OPAQUE           // 불투명 → 컴포지터가 아래 레이어 합성을 건너뜀
).apply {
    screenBrightness = 0f        // 최상단 가시 창의 값이 적용됨
}
```

설계 포인트 세 가지:

- **`FLAG_NOT_TOUCHABLE`을 쓰지 않습니다.** 터치를 아래 앱으로 통과시키면 주머니 속 오작동이 그대로 아래 앱에 전달되고, 동시에 해제 제스처를 받을 방법이 사라집니다. **오버레이가 터치를 삼키는 게 맞습니다.** 어차피 화면이 검어서 아래 앱을 조작할 수는 없습니다.
- **`FLAG_NOT_FOCUSABLE`은 유지합니다.** 터치는 받되 키 입력 포커스는 아래 앱에 남겨둡니다.
- **`PixelFormat.OPAQUE`** 를 쓰면 SurfaceFlinger가 완전히 가려진 아래 레이어의 합성을 생략할 수 있어 전력에 유리합니다.

**Service에서 Compose 쓰기 (배선 필요):**

`ComposeView`는 `LifecycleOwner` / `SavedStateRegistryOwner` / `ViewModelStoreOwner` 를 ViewTree에서 찾습니다. Activity가 아닌 Service의 창에는 이게 없으므로 직접 붙여야 합니다.

```kotlin
class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    override fun onCreate() {
        savedStateController.performRestore(null)   // ★ super.onCreate() 보다 먼저
        super.onCreate()

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent { BlackScreenRoot(onUnlock = { stopSelf() }) }
        }
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        windowManager.removeView(view)
        viewModelStore.clear()
        super.onDestroy()
    }
}
```

> **함정:** `performRestore(null)`을 `super.onCreate()` 이후에 호출하면 라이프사이클이 이미 `CREATED`로 이동해 있어 예외가 납니다. 반드시 먼저 호출해야 합니다.

**Manifest:**
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".overlay.OverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Full-screen black overlay that visually blanks the display
                       while other apps keep running." />
</service>
```

**권한 요청 흐름:** 런타임 권한이 아니므로 설정 화면으로 보내야 합니다.
```kotlin
if (!Settings.canDrawOverlays(this)) {
    startActivity(Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:$packageName".toUri()
    ))
}
```

**시작 순서 (Android 15+ 주의):** Android 15부터 백그라운드에서 FGS를 시작할 때 오버레이 예외를 쓰려면 `SYSTEM_ALERT_WINDOW` 권한과 **이미 보이는 오버레이 창**이 둘 다 있어야 하고, 아니면 `ForegroundServiceStartNotAllowedException`이 납니다. 우리 흐름은 사용자가 앱/타일에서 직접 시작하므로 앱이 포그라운드 상태라 해당되지 않지만, 나중에 "부팅 후 자동 시작" 같은 걸 붙이면 걸립니다.

### 4.2b 오버레이 시작 경로 — 투명 중계 Activity

구현 중에 발견해 추가한 부분입니다.

퀵 설정 타일에서 곧바로 `startForegroundService` 를 호출하면 **백그라운드에서 FGS 를
시작하는 것**이 됩니다. Android 15+ 에서 오버레이 예외를 쓰려면 `SYSTEM_ALERT_WINDOW`
권한과 **이미 보이는 오버레이 창**이 둘 다 필요한데, 시작 시점에는 당연히 창이 없습니다.
`ForegroundServiceStartNotAllowedException` 이 날 수 있는 경로입니다.

`OverlayLauncherActivity` — 투명 테마의 빈 Activity — 를 한 단계 끼워 넣어,
Activity 가 앞에 있는 상태에서 서비스를 시작하고 즉시 `finish()` 합니다.
이 경로는 백그라운드 시작이 아니므로 제약을 통째로 피합니다.

```kotlin
class OverlayLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) OverlayService.start(this)
        else startActivity(Intent(this, MainActivity::class.java).addFlags(FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
```

### 4.3 공유 콘텐츠

```kotlin
@Composable
fun BlackScreenContent(settings: Settings) {
    val offset = rememberBurnInShift(settings.burnInShiftEnabled)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.align(Alignment.Center).offset { offset }) {
            if (settings.showClock) {
                Text(formatNow(settings.clockFormat), color = settings.textColor)
            }
            if (settings.sentence.isNotBlank()) {
                Text(settings.sentence, color = settings.textColor)
            }
        }
    }
}
```

- **시계 갱신은 분 단위**로. 초 단위 갱신은 전력만 먹고 이 앱에선 의미가 없습니다. 분 경계에 정렬된 타이머(다음 정각까지 `delay`)를 씁니다.
- **글자는 흰색 고정.** 어둡게 하는 몫은 창의 `screenBrightness` 하나가 전담합니다.
  글자색과 패널 밝기를 둘 다 낮추면 곱해져서 판독이 불가능해집니다(실제로 세 번 겪음).

### 4.4 번인 방지

AMOLED에서 같은 위치에 시계를 몇 시간 띄우면 잔상이 남습니다.

```kotlin
@Composable
fun rememberBurnInShift(enabled: Boolean): IntOffset {
    // 60초마다 반경 24dp 원 궤도 위의 다음 지점으로 이동
    // 이동은 애니메이션 없이 즉시 (애니메이션은 전력 소모)
}
```

### 4.5 해제 제스처

단일 탭 해제는 주머니에서 바로 풀립니다. 기본값은 **1.5초 롱프레스**로 하고, 누르는 동안 원형 프로그레스를 서서히 페이드인해서 피드백을 줍니다.

| 옵션 | 비고 |
|---|---|
| 롱프레스 1.5초 (기본) | 가장 안전 |
| 더블탭 | 빠르지만 오작동 여지 |
| 3회 연속 탭 | 절충안 |

Blackout 모드에서는 볼륨 키(`onKeyDown`)로도 해제할 수 있지만, Overlay 모드는 `FLAG_NOT_FOCUSABLE` 때문에 키 이벤트를 받지 못합니다. **모드 간 동작이 달라지므로 키 해제는 넣지 않는 편이 일관됩니다.**

### 4.6 진입점 — 퀵 설정 타일

실사용에서 가장 중요한 부분입니다. 앱을 열어서 버튼을 누르는 건 번거롭고, **상태바를 내려 타일 한 번 탭**이 자연스럽습니다.

```kotlin
class BlackScreenTileService : TileService() {
    override fun onClick() {
        // Android 14+ 에서 startActivityAndCollapse(Intent)는 예외를 던짐.
        // 반드시 PendingIntent 오버로드를 사용.
        startActivityAndCollapse(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, BlackoutActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
```

> **함정:** `startActivityAndCollapse(Intent)`는 API 34에서 deprecated이자 호출 시 `UnsupportedOperationException`을 던집니다. `PendingIntent` 버전을 써야 합니다.

---

## 5. 설정 항목

```kotlin
data class Settings(
    val mode: Mode = Mode.BLACKOUT,          // BLACKOUT | OVERLAY
    val showClock: Boolean = false,          // 기본은 "아무것도 없음"
    val clockFormat: String = "HH:mm",
    val sentence: String = "",
    val textLevel: Int = 3,                  // 1~5 → 패널 밝기 0.05 ~ 0.80
    val burnInShiftEnabled: Boolean = true,
    val unlockGesture: Gesture = Gesture.LONG_PRESS,
)
```

`autoStopMinutes` 는 자동 종료 타이머와 함께 3차로 미뤘습니다(§7).
DataStore 에 저장된 enum 이름이 사라진 상수를 가리키면 기본값으로 되돌립니다.

기본값은 요구사항대로 **아무것도 표시하지 않는 완전한 검정**입니다.

---

## 6. 전력에 대한 정직한 기대치

- 이 앱은 화면을 **끄지 않습니다.** 디스플레이 파이프라인은 계속 살아 있습니다.
- AMOLED + 전면 검정 + 밝기 0이면 발광 픽셀이 거의 없어 소모가 낮지만 **0은 아닙니다.**
- LCD 기기는 백라이트가 남아 이득이 훨씬 적고, 보기에도 완전히 검지 않습니다.
- **구체적인 수치는 측정 전에는 말할 수 없습니다.** 개발 중 `adb shell dumpsys batterystats` + Battery Historian으로 실제 소모율을 측정하고, 필요하면 `autoStopMinutes` 기본값을 그 결과에 맞춰 정합니다.

---

## 7. 구현 순서

### 1차 — Blackout 모드 (권한 0개)
1. 프로젝트 스캐폴딩 (Compose, DataStore)
2. `BlackoutActivity` — 몰입 모드 + 밝기 0 + KEEP_SCREEN_ON
3. `BlackScreenContent` — 무표시 / 시계 / 문장
4. 롱프레스 해제 + 프로그레스 피드백
5. 번인 방지 픽셀 시프트
6. 설정 화면
7. 퀵 설정 타일

이 시점에서 **권한 하나 없이 완결된 앱**이 됩니다.

### 2차 — Overlay 모드
8. `OverlayService` + FGS + 창 배선
9. Compose ViewTree owner 배선
10. 권한 안내 플로우
11. 설정에서 모드 전환 + 알림 액션 해제
12. 상태바가 남는다는 점을 설정 화면에서 명시

### 3차 — 선택
13. 자동 종료 타이머
14. 근접센서 포켓 모드 (`PROXIMITY_SCREEN_OFF_WAKE_LOCK` — 진짜 화면 OFF)
15. 문장 여러 개 순환 / 시간대별 문구
16. 홈 화면 위젯·바로가기
17. 배터리 최적화 예외 안내 (제조사별 가이드)

---

## 8. 남은 함정 목록

| # | 함정 | 대응 |
|---|---|---|
| F1 | Service에서 `performRestore` 호출 순서 | `super.onCreate()` 이전 |
| F2 | `startActivityAndCollapse(Intent)` API 34+ 예외 | `PendingIntent` 오버로드 |
| F3 | 오버레이가 설정·비밀번호 등 민감 화면에서 자동으로 숨겨짐 | 회피 불가. 설정 화면에 명시 |
| F4 | Android 12+ 다른 앱이 `hideOverlayWindows()`로 우리 오버레이를 숨길 수 있음 | 회피 불가 |
| F5 | 제조사(삼성·샤오미 등)의 공격적 백그라운드 종료 | 배터리 최적화 예외 안내 |
| F6 | 화면 회전 시 Activity 재생성 → 깜빡임 | `configChanges` 로 재생성 차단. **`screenOrientation` 은 Android 16+ 에서 무시되므로 쓰면 안 된다** |
| F7 | 오버레이 창 누수 (`removeView` 누락) | `onDestroy`에서 반드시 해제, try/catch |
| F8 | LCD 기기에서 기대와 다른 결과 | 첫 실행 시 안내 |
| F9 | 타일 `onClick` 은 suspend 가 아니라 DataStore 를 그 자리에서 못 읽음 | `onStartListening` 에서 모드를 미리 읽어 둠 |
| F10 | 타일에서 FGS 직접 시작 → Android 15+ 예외 위험 | 투명 중계 Activity 경유 (§4.2b) |
| F11 | AGP 9 + 외부 `kotlin.android` 플러그인 → `ClassCastException` | 내장 Kotlin 사용, `kotlin.android` 제거 |
