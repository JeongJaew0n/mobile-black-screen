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
| Screen Off 방식 | **세 모드, 기본은 FULL** | 창 종류마다 덮을 수 있는 범위가 달라 하나로 합칠 수 없음 |
| 배포 | **개인용 / 사이드로딩** | Play 심사 리스크 전부 소거 |

> **한 번 크게 틀렸던 지점.** 초기 설계는 "오버레이는 상태바를 못 덮는다"를 근거로
> **Blackout(앱 전환)을 기본값**으로 삼았습니다. 그 명제는 `TYPE_APPLICATION_OVERLAY`
> 에만 해당하는데 모든 오버레이로 확대한 잘못된 일반화였고, 그 결과 사용자가 처음부터
> 요구한 동작("쓰던 앱은 그대로, 화면만 어두워짐")과 정반대인 모드를 기본값으로 두었습니다.
> `TYPE_ACCESSIBILITY_OVERLAY` 를 발견하고 FULL 모드로 바로잡았습니다(§4.1).

---

## 1. 세 모드의 정확한 차이

|  | **FULL** (기본) | **Overlay** | **Blackout** |
|---|---|---|---|
| 창 종류 | `TYPE_ACCESSIBILITY_OVERLAY` | `TYPE_APPLICATION_OVERLAY` | 풀스크린 Activity |
| **쓰던 앱** | **그대로 유지** | **그대로 유지** | **뒤로 밀림** |
| 상태바 / 내비바 | **전부 덮음** | 둘 다 남음 | 숨김 |
| 다른 앱 UI 렌더링 | 계속 | 계속 | 멈춤 |
| 권한 | 접근성 서비스 | 다른 앱 위에 표시 | **없음** |
| 상시 알림 | 없음 | 있음 | 없음 |

> **아래 앱이 계속 그리는 것은 의도된 동작입니다.** FULL/Overlay 에서 가려진 앱은
> 여전히 RESUMED 이고 매 프레임 렌더링합니다. 이걸 막으면 Activity 전환(Blackout)이
> 되어 버립니다. 전력만 보면 Blackout 이 유리하지만, 쓰던 앱을 밀어내지 않는 것이
> 이 앱의 목적이라 그 대가로 내줍니다.
>
> | | 아래 앱 렌더링 | 합성 |
> |---|---|---|
> | Blackout (Activity) | **멈춤** | — |
> | FULL / Overlay (반투명) | 계속 그림 | 섞음 |
>
> 창을 불투명으로 되돌리면 합성 한 단계를 아끼지만 밀어 올려 해제가 동작하지 않습니다.
> 아래 앱이 그리는 비용이 이미 깔려 있어 그 한 단계는 상대적으로 작습니다.

**FULL 이 이 앱이 하려던 동작입니다.** 나머지 둘은 절충안입니다 — 접근성 권한을 주기
싫으면 Overlay, 권한을 하나도 주기 싫으면 Blackout.

> **자주 오해하는 지점:** Blackout 모드에서도 음악 재생·다운로드·내비게이션·타이머·
> 동기화는 전부 그대로 돌아갑니다. Android에서 백그라운드 앱은 죽지 않습니다.
> 멈추는 것은 **직전 최상단 앱의 UI 렌더링뿐**입니다.

### 창 종류에 따른 제약 (실측)

**`TYPE_APPLICATION_OVERLAY`** 는 Android 8(O)부터 시스템 UI 위에 그리는 것이 금지되어
상태바·내비게이션 바를 덮지 못합니다. 실측(Galaxy S23+ / Android 16)상 창 프레임이
`[0,94][1080,2214]` 로 잘리고, z-order 상으로도 두 바가 위에 있습니다.
`FLAG_LAYOUT_NO_LIMITS` 를 포함해 어떤 플래그 조합으로도 뚫리지 않습니다.

**`TYPE_ACCESSIBILITY_OVERLAY`** 는 그 위까지 전부 덮습니다. 접근성 서비스로 등록하는
것 자체가 자격이라 `SYSTEM_ALERT_WINDOW` 도 필요 없습니다. 실측으로 상태바 영역
(y 0~94) 밝은 픽셀 0개, 내비바 영역도 0개, 최상단 Activity 는 아래 앱 그대로임을
확인했습니다.

**제약을 말할 때는 어떤 창 종류에 대한 제약인지 반드시 붙일 것.**

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

Compose를 쓰는 이유는 **세 모드가 표시 콘텐츠(시계/문장/무표시)를 하나의 Composable로 공유**하기 위해서입니다. Activity 가 아닌 창(서비스·접근성 서비스)에 Compose를 얹으려면 owner 배선이 필요하지만(§4.1, §4.2) 콘텐츠 로직을 세 번 짜는 것보다 낫습니다.

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
│   ├─ BlackScreenContent.kt    ★ 세 모드 공유 Composable
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

### 4.1 FULL 모드 — 접근성 오버레이 (기본)

`TYPE_ACCESSIBILITY_OVERLAY` 는 상태바·내비게이션 바·시스템 다이얼로그 위까지 덮습니다.
**앱 전환이 없으므로 쓰던 앱이 그대로 유지됩니다.**

`AccessibilityService` 는 `LifecycleService` 가 아니라 Compose 에 필요한 owner 가 하나도
없습니다. 셋을 직접 구현하고 **RESUMED 까지 올립니다** — 프레임 클럭이 그 상태를 봅니다.

```kotlin
class ScreenOffAccessibilityService :
    AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override fun onCreate() {
        savedStateController.performRestore(null)   // ★ super 보다 먼저
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onServiceConnected() {
        instance = this
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
}
```

**창 크기를 명시적으로 지정합니다.** `MATCH_PARENT` 는 시스템 바를 제외한 부모 프레임에
맞춰져 위아래가 남습니다. 실제 디스플레이 크기를 직접 넣고 경계를 풉니다.

```kotlin
WindowManager.LayoutParams(
    displayWidth, displayHeight,                       // currentWindowMetrics.bounds
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN or
        FLAG_LAYOUT_NO_LIMITS or FLAG_KEEP_SCREEN_ON,
    PixelFormat.OPAQUE,
).apply {
    gravity = Gravity.TOP or Gravity.START; x = 0; y = 0
    screenBrightness = latestSettings.screenBrightness
    // 노치까지 덮는다. ALWAYS 상수는 API 30+
}
```

> **함정 두 가지.**
> - `showCover()` 는 suspend 가 아니라 그 자리에서 밝기를 알아야 합니다. 설정 수집기가
>   캐시해 둔 값(`latestSettings`)을 초기 파라미터에 씁니다. 이걸 빼먹으면 **첫 표시에만
>   밝기 오버라이드가 걸리지 않습니다.**
> - **앱을 재설치하면 접근성 서비스가 꺼집니다.** Android 의 정상 동작이며, 업데이트마다
>   사용자가 설정에서 다시 켜야 합니다.


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

### 4.2c 진입 분기 — `ScreenOff` 디스패처

진입점이 셋(설정 화면 버튼 / 버블 / 퀵 설정 타일)이라 모드 분기를 흩어 두면 금방 어긋납니다.
`ScreenOff.start(context, mode)` 하나로 모읍니다. 권한이 없으면 시작하지 않고
`NEEDS_ACCESSIBILITY` / `NEEDS_OVERLAY_PERMISSION` 을 돌려주어 호출부가 안내하게 합니다.

**타일에서 FULL 을 켤 때는 중계 Activity 를 거치지 않습니다.** 그것을 띄우는 순간
쓰던 앱이 뒤로 밀리는데, 그걸 피하는 것이 이 모드의 존재 이유이기 때문입니다.
중계 Activity 는 Overlay 모드에만 필요합니다(백그라운드 FGS 시작 제약 회피, §4.2b).


### 4.3 Blackout 모드 — 풀스크린 Activity (최후 수단)

**이 모드만 쓰던 앱을 뒤로 밀어냅니다.** 권한이 하나도 필요 없다는 것이 유일한 장점이라,
접근성도 오버레이 권한도 주고 싶지 않은 경우의 최후 수단으로만 남겨 둡니다.

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


### 4.4 공유 콘텐츠

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

### 4.5 번인 방지

AMOLED에서 같은 위치에 시계를 몇 시간 띄우면 잔상이 남습니다.

```kotlin
@Composable
fun rememberBurnInShift(enabled: Boolean): IntOffset {
    // 60초마다 반경 24dp 원 궤도 위의 다음 지점으로 이동
    // 이동은 애니메이션 없이 즉시 (애니메이션은 전력 소모)
}
```

### 4.6 해제 제스처

단일 탭 해제는 주머니에서 바로 풀립니다. 기본값은 **1.5초 롱프레스**로 하고, 누르는 동안 원형 프로그레스를 서서히 페이드인해서 피드백을 줍니다.

| 옵션 | 비고 |
|---|---|
| 롱프레스 1.5초 (기본) | 가장 안전 |
| 더블탭 | 빠르지만 오작동 여지 |
| 3회 연속 탭 | 절충안 |

Blackout 모드에서는 볼륨 키(`onKeyDown`)로도 해제할 수 있지만, Overlay 모드는 `FLAG_NOT_FOCUSABLE` 때문에 키 이벤트를 받지 못합니다. **모드 간 동작이 달라지므로 키 해제는 넣지 않는 편이 일관됩니다.**

### 4.7 진입점 — 퀵 설정 타일

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

### 4.8 상주 버블

앱을 닫아도 떠 있는 원형 버튼. `ScreenCoverService` 가 차폐 창과 함께 소유하는
상태 기계로 관리합니다 — 서비스를 나누면 상시 알림이 두 개가 됩니다.

```
bubbleWanted      사용자가 켰는가 (차폐 중 감춰도 유지)
bubbleSuppressed  차폐 중이라 잠시 감췄는가
```

모든 상태 전이는 `syncBubble()` 하나를 거칩니다. `coverView` 와 `bubbleWanted` 가
둘 다 비면 그때만 `stopSelf()` 합니다.

- **창 크기를 버블 크기와 같게** 잡아 바깥 터치는 아래 앱으로 통과시킵니다.
  차폐 창이 `FLAG_NOT_TOUCHABLE` 을 안 쓰는 이유("터치를 삼켜야 해서")와
  버블이 안 쓰는 이유("자기 영역만 받으면 돼서")는 다릅니다.
- **위치는 픽셀이 아니라 가장자리(LEFT/RIGHT) + 세로 비율**로 저장합니다.
  픽셀로 두면 회전·해상도 변경 때 화면 밖으로 나갑니다.
- **크기를 바꾸면 창을 다시 배치**합니다. `WRAP_CONTENT` 라 크기는 알아서 커지지만,
  오른쪽 가장자리에서는 x 를 다시 계산하지 않으면 밀려납니다
  (52dp 위치 x=929 에서 70dp 가 되면 오른쪽 끝이 1113 → 33px 이탈).
- **Blackout 차폐 중에는 `BlackoutActivity` 가 서비스에 직접 알려** 버블을 감춥니다.
  오버레이 창은 어떤 Activity 가 앞인지 알 수 없습니다. FULL 은 접근성 오버레이가
  버블보다 위 레이어라 알아서 가려집니다.

---

## 5. 설정 항목

```kotlin
data class Settings(
    val mode: Mode = Mode.FULL,              // FULL | OVERLAY | BLACKOUT
    val showClock: Boolean = false,          // 기본은 "아무것도 없음"
    val clockFormat: String = "a h:mm",      // 한국어는 오전/오후가 앞
    val sentence: String = "",
    val textLevel: Int = 3,                  // 1~5 → 패널 밝기 0.05~0.80
    val burnInShiftEnabled: Boolean = true,
    val unlockGesture: Gesture = Gesture.TRIPLE_TAP,
    val bubbleEnabled: Boolean = false,
    val bubbleEdge: Edge = Edge.RIGHT,
    val bubbleSizeLevel: Int = 3,            // 1~5 → 40~70dp
    val bubbleYRatio: Float = 0.5f,
)
```

### 밝기 손잡이는 하나다

보이는 밝기는 **프레임버퍼 값 × 패널 밝기**입니다. 둘 다 깎아 놓으면 왜 안 보이는지
헤매게 됩니다 — 실제로 세 번 그랬습니다.

```
#333333 + 패널 0.00  →  판독 불가
#E6E6E6 + 패널 0.00  →  판독 불가 (곱셈의 한쪽이 0이면 소용없음)
#E6E6E6 + 패널 0.08  →  여전히 판독 불가
```

그래서 **글자는 항상 흰색 고정**이고, `textLevel` 은 창의 `screenBrightness` 만
움직입니다. 표시할 내용이 없으면 밝기는 무조건 0 입니다 — 그게 이 앱의 기본 상태입니다.

⚠️ **이 값은 `screencap` 으로 검증할 수 없습니다.** 프레임버퍼에는 패널 밝기가 담기지
않아 픽셀값이 찍혀도 눈에는 안 보일 수 있습니다. 반드시 실제 화면을 봐야 합니다.

### 설정 저장은 스냅샷이 아니라 변환 함수로

`SettingsScreen.onChange` 는 `(Settings) -> Settings` 를 받습니다. 화면이 들고 있는
스냅샷을 통째로 저장하면, DataStore 가 아직 값을 안 뱉은 시점(앱을 막 연 직후)에
항목 하나만 바꿔도 나머지가 전부 기본값으로 덮입니다. 실제로 그 버그를 냈습니다.

---

## 6. 전력

**아직 실측하지 못했습니다.** 시도했고 두 번 다 실패했습니다 —
방법과 함정은 [power-measurement.md](./power-measurement.md) 참조.

현재 있는 것은 추정치입니다(신뢰도 ±50%).

| 상태 | 추정 전류 | 8시간 |
|---|---|---|
| 실제 화면 꺼짐 | 5~20 mA | 약 2% |
| **Screen Off (이 앱)** | **70~110 mA** | **12~20%** |
| 일반 화면 켜짐 | 120~140 mA | 21~25% |

**밝기 슬라이더는 전력에 거의 영향이 없을 것으로 봅니다.** AMOLED 는 검은 픽셀이
발광하지 않는데 이 앱은 화면의 대부분이 검정이고 시계 몇 글자만 켜집니다.
전력의 대부분은 **"화면이 기술적으로 켜져 있다"** 는 사실 자체(SoC·디스플레이
파이프라인·터치 패널이 계속 깨어 있음)에서 나옵니다.

---

## 7. 구현 이력

1. **Blackout 모드** — 풀스크린 Activity, 권한 0개
2. **Overlay 모드** — `SYSTEM_ALERT_WINDOW` + `specialUse` 포그라운드 서비스
3. **상주 버블** — 서비스 통합(`ScreenCoverService`), 드래그·✕ 제거·유휴 페이드
4. **FULL 모드** — 접근성 오버레이. 기본값을 여기로 옮기고 Blackout 을 최후 수단으로 격하
5. **버블 크기 조절** — 5단계

작업별 배경과 결정 근거는 `docs/plans/` 의 각 폴더에 있습니다.

### 아직 하지 않은 것

자동 종료 타이머, 근접센서 포켓 모드, 문장 여러 개 순환, 홈 화면 위젯,
제조사별 배터리 최적화 안내, 부팅 후 자동 복원.

---

## 8. 함정 목록

실제로 한 번씩 물린 것들입니다.

| # | 함정 | 대응 |
|---|---|---|
| F1 | Service 에서 `performRestore` 호출 순서 | `super.onCreate()` **이전** |
| F2 | `startActivityAndCollapse(Intent)` API 34+ 예외 | `PendingIntent` 오버로드 |
| F3 | 오버레이가 민감 화면에서 자동으로 숨겨짐 | 회피 불가 |
| F4 | Android 12+ 다른 앱이 `hideOverlayWindows()` 로 숨김 | 회피 불가 |
| F5 | 제조사의 공격적 백그라운드 종료 | 배터리 최적화 예외 안내 |
| F6 | 회전 시 재생성 → 깜빡임 | `configChanges`. **`screenOrientation` 은 Android 16+ 에서 무시됨** |
| F7 | 창 누수 | `onDestroy` 에서 해제, `runCatching` |
| F8 | LCD 기기에서 기대와 다름 | 회피 불가 (AMOLED 전제) |
| F9 | 타일 `onClick` 은 suspend 아님 | `onStartListening` 에서 모드 선읽기 |
| F10 | 타일에서 FGS 직접 시작 → Android 15+ 예외 | Overlay 모드만 중계 Activity 경유 |
| F11 | AGP 9 + 외부 `kotlin.android` → `ClassCastException` | 내장 Kotlin 사용 |
| F12 | 코루틴 안에서 창 추가 → 경합으로 창 2개 | 동기 플래그(`bubbleAdding`) |
| F13 | 오버레이 좌표는 화면이 아니라 **부모 프레임 기준** | `usableSize()` — 인셋 제외 |
| F14 | Compose `animate()` 를 서비스에서 호출 → 프레임 클럭 없어 크래시 | 단순 루프(`slideBubbleX`) |
| F15 | `LayoutParams.y` 에 dp 를 그대로 넣음 | `* density` |
| F16 | 포그라운드 아닐 때 `startForegroundService` → 크래시 | `LifecycleResumeEffect` + `runCatching` |
| F17 | 접근성 오버레이 첫 표시에 밝기 미적용 | 수집기가 캐시한 값을 초기 파라미터에 |
| F18 | 재설치·`force-stop` 시 접근성 서비스 꺼짐 | Android 정상 동작. **강제 종료로도 꺼지므로 실사용에서도 FULL 이 조용히 멈춘다** |
| F19 | 호출 안 하는 Composable 은 컴파일·lint 통과 | "빌드 성공" 이 아니라 **화면에 떴는지**로 확인 |

### 실기기 검증 시 속기 쉬운 것

| 증상 | 실제 원인 |
|---|---|
| "롱프레스 해제가 회귀했다" | adb 테스트 탭이 **해제 제스처 설정을 바꿔 놓음**. `unlock_gesture` 부터 확인할 것 |
| "버블 페이드가 동작 안 한다" | 평균 밝기로 측정. 뒤 배경이 어두우면 안 움직임. **고대비 지점**(흰 막대)을 볼 것 |
| "접근성창이 안 뜬다" | 직전 재설치·`force-stop` 이 서비스를 꺼 놓음. `accessibility_enabled` 부터 확인할 것 |
| "새로 넣은 UI 가 안 보인다" | 호출하는 분기를 빠뜨림. 미사용 함수는 빌드가 잡아 주지 않는다 |
| "글자가 보인다"(실제로는 안 보임) | `screencap` 은 프레임버퍼라 **패널 밝기가 반영되지 않음** |
| "타일이 동작 안 한다" | `cmd statusbar click-tile` 만으로는 `onStartListening` 이 안 돎. `expand-settings` 선행 |
| "차폐가 안 걸린다" | 화면이 잠겨 있어 탭이 잠금화면에 떨어짐. `isKeyguardShowing` 확인 |
