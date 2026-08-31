# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 이 앱이 하는 일

화면을 검게 덮어 "꺼진 것처럼" 보이게 하는 Android 앱. **화면을 실제로 끄지 않는다** —
Android 에 임의 시점에 화면을 끄는 공개 API가 없고, 물리적으로 끄면 이 앱의 기능인
시계·문장 표시가 불가능해진다. 대신 전면 검정 + `screenBrightness = 0f` 로 처리한다.

## 명령

```bash
# SDK 경로 (gitignore 대상이라 새 머신에서는 직접 만들어야 한다)
echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties

./gradlew assembleDebug
./gradlew lintDebug          # 0 errors 를 유지할 것. 경고는 OldTargetApi 1건만 정상
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

테스트 소스셋(`app/src/test`, `app/src/androidTest`)이 없다. 검증은 실기기 + adb 로 한다.

## 실기기 검증 레시피

이 앱은 "화면이 검게 덮였는가"가 핵심이라 UI 테스트로 잡기 어렵다. 아래 방법이 실제로
동작함이 확인되었다.

```bash
# 순수 검정인지 — 스크린샷을 받아 픽셀값이 전부 0 인지 확인
adb exec-out screencap -p > /tmp/s.png

# 밝기 오버라이드가 우리 창에 걸렸는지
adb shell dumpsys display | grep mWindowManagerBrightnessOverride

# 오버레이 창 / 서비스 존재 여부 (해제 후 0 이어야 누수 없음)
adb shell dumpsys window windows | grep -oE "Window\{[0-9a-f]+ u0 com.jjw.blackscreen[^}]*\}"
adb shell dumpsys activity services com.jjw.blackscreen | grep -E "isForeground|types="

# 롱프레스 해제 (2초). 단일 탭·뒤로 가기로는 풀리면 안 된다
adb shell input swipe 540 1200 540 1200 2000

# FLAG_KEEP_SCREEN_ON 검증: 타임아웃을 낮추고 그보다 오래 방치. 반드시 원복할 것
adb shell settings put system screen_off_timeout 15000
adb shell settings put system screen_off_timeout 600000
```

**함정들.** 아래는 전부 실제로 한 번씩 속았던 것들이다.

- `BlackoutActivity` 는 `exported="false"` 라 `am start` 로 직접 띄울 수 없다
  (`SecurityException`). `MainActivity` 를 띄우고 버튼을 탭하거나 타일을 써야 한다.
- `cmd statusbar click-tile` 만으로는 `onStartListening` 이 돌지 않아 TileService 가
  바인딩되지 않는다. `cmd statusbar expand-settings` 를 먼저 호출해야 한다.
  앱 버그가 아니라 adb 아티팩트다.
- **테스트 탭이 설정을 조용히 바꾼다.** 화면이 잠긴 줄 모르고 보낸 탭이 해제 제스처를
  바꿔 놓아 "롱프레스 회귀"로 오진한 적이 있다. 제스처를 검증하기 전에
  `unlock_gesture` 저장값부터 확인할 것.
- **DataStore 의 boolean 은 `strings` 로 true/false 를 구분할 수 없다.** 키 이름만 보인다.
  동작으로 검증할 것.
- **설정 화면 스크롤 위치가 매번 달라진다.** 스크롤 후 스크린샷으로 좌표를 다시 잡을 것.
- **미세한 시각 변화를 평균 밝기로 재지 말 것.** 버블 페이드는 뒤 배경이 어두우면
  평균이 안 움직인다. 고대비 지점(흰 막대)의 픽셀값을 봐야 한다.

## 구조의 핵심

**두 모드가 표시 콘텐츠를 하나의 Composable(`ui/BlackScreenContent.kt` 의
`BlackScreenRoot`)로 공유한다.** 차이는 "어떤 창에 올리는가"뿐이다.

- **Blackout** (`blackout/BlackoutActivity.kt`) — 풀스크린 Activity. 시스템 바를
  완전히 숨긴다. 버블을 쓰지 않으면 권한 0개.
- **Overlay** — `TYPE_APPLICATION_OVERLAY` 창. 아래 앱이 계속 렌더링된다.

표시 내용을 고칠 때는 `BlackScreenRoot` 한 곳만 만지면 두 모드에 함께 반영된다.
모드별로 갈라 쓰지 말 것.

### 창은 전부 `service/ScreenCoverService` 가 소유한다

`specialUse` 포그라운드 서비스 하나가 **차폐 창과 버블 창을 함께** 관리하는 상태 기계다.
서비스를 나누지 말 것 — 상시 알림이 두 개가 되고, 그건 이 앱의 컨셉과 어긋난다.

```
bubbleWanted     사용자가 버블을 켰는가 (차폐 중 숨겨도 유지)
bubbleSuppressed 차폐 중이라 잠시 감췄는가
coverView        차폐 창
bubbleView       버블 창
```

모든 상태 전이는 `syncBubble()` 하나를 거친다. 창을 직접 붙이거나 떼지 말 것.
`coverView` 와 `bubbleWanted` 가 둘 다 비면 그때만 `stopSelf()` 한다 —
예전처럼 차폐 해제에서 `stopSelf()` 하면 버블까지 죽는다.

## 우회 불가능한 제약 (건드리지 말 것)

**오버레이는 상태바도 내비게이션 바도 덮지 못한다.** Android 8(O)부터 의도적으로
금지되었고 Google 이 버그가 아니라고 확인했다. `FLAG_LAYOUT_NO_LIMITS` 를 포함해
어떤 플래그 조합으로도 뚫리지 않는다. 실측(Galaxy S23+ / Android 16)상 창 프레임은
`[0,94][1080,2214]` 이고 z-order 상으로도 두 바가 오버레이보다 위다.
"내비바만이라도 덮어보자"는 시도는 이미 실패했으니 반복하지 말 것.

`OverlayWindow.kt` 의 `FLAG_LAYOUT_NO_LIMITS` 는 다른 기기·버전을 위해 남겨둔 것이고
이 기기에서는 효과가 없다.

## 코드를 고칠 때 걸리는 함정

- **`OverlayService.onCreate()` 에서 `savedStateController.performRestore(null)` 은
  `super.onCreate()` 보다 먼저** 호출해야 한다. super 가 라이프사이클을 `CREATED` 로
  옮긴 뒤에 복원하면 예외가 난다. Service 에 Compose 를 얹으려면
  Lifecycle / SavedStateRegistry / ViewModelStore owner 세 개를 직접 배선해야 한다.
- **오버레이에 `FLAG_NOT_TOUCHABLE` 을 쓰지 말 것.** 터치를 아래 앱으로 통과시키면
  주머니 속 오작동이 그대로 전달되고 해제 제스처를 받을 방법도 사라진다.
  오버레이가 터치를 삼키는 것이 의도된 설계다.
- **타일에서 포그라운드 서비스를 직접 시작하지 말 것.** 백그라운드 FGS 시작은
  Android 15+ 에서 `ForegroundServiceStartNotAllowedException` 위험이 있다.
  투명한 `OverlayLauncherActivity` 를 거쳐 Activity 가 앞에 있는 상태로 시작한다.
- **`TileService.onClick` 은 suspend 가 아니라** DataStore 를 그 자리에서 읽을 수 없다.
  `onStartListening` 에서 모드를 미리 읽어 둔다.
- **`startActivityAndCollapse(Intent)` 는 API 34+ 에서 예외를 던진다.**
  `PendingIntent` 오버로드를 쓴다. API 33 이하 분기는 lint 오탐이라 suppress 되어 있다.
- **화면 회전은 `screenOrientation` 이 아니라 `configChanges` 로 처리한다.**
  고정 방향 지정은 Android 16 부터 무시된다. 목적은 재생성으로 인한 깜빡임 방지다.
- **오버레이 창 좌표는 화면 전체가 아니라 부모 프레임 기준이다.** 실측상 부모 프레임은
  `[0,94][1080,2214]` 였다. 화면 전체 높이로 위치를 계산하면 상태바 높이만큼 밀린다.
  `ScreenCoverService.usableSize()` 를 쓸 것.
- **Compose 의 `animate()` 를 서비스에서 호출하지 말 것.** 코루틴 컨텍스트에
  `MonotonicFrameClock` 을 요구해서 `lifecycleScope` 에서 부르면
  `IllegalStateException` 으로 크래시한다. 창 좌표 애니메이션은 단순 루프로 한다
  (`slideBubbleX`).
- **`LayoutParams.y` 는 픽셀이다.** dp 숫자를 그대로 넣는 실수를 이미 한 번 했다.
- **코루틴 안에서 창을 붙일 때는 동기 플래그로 중복을 막을 것.** `addBubble()` 은
  저장된 위치를 읽느라 코루틴 안에서 `addView` 하는데, 그 사이 재호출되면 창이 두 개 생긴다.
- **`android.builtInKotlin` 을 끄지 말 것.** 외부 `org.jetbrains.kotlin.android`
  플러그인은 AGP 9 의 새 DSL 이 `BaseExtension` 을 제거해 `ClassCastException` 으로
  실패한다. `kotlin.plugin.compose` 만 적용한다.

## 설계 의도 (바꾸기 전에 읽을 것)

- **기본값은 아무것도 표시하지 않는 완전한 검정이다.** 시계와 문장은 옵션이다.
- 텍스트는 **순백을 쓰지 않는다.** 밝기가 0 으로 눌린 상태라 저휘도 그레이(5단계)로
  충분하고, AMOLED 번인 부담도 줄어든다.
- 시계는 **분 경계에 정렬해 분 단위로만** 갱신한다. 초 단위 갱신은 전력만 먹는다.
- 60초마다 글자 위치를 원 궤도로 옮긴다(번인 방지). 애니메이션 없이 즉시 이동한다 —
  프레임을 계속 돌리면 전력을 먹고, 밝기 0 상태라 어차피 보이지 않는다.
- 해제는 **단일 탭이 아니다.** 주머니에서 바로 풀리기 때문이다. 1.5초 롱프레스가
  기본값이고 진행 링으로 피드백한다. 뒤로 가기로도 풀리지 않는다.
- **볼륨 키 해제를 넣지 않는다.** Blackout 은 되지만 Overlay 는 `FLAG_NOT_FOCUSABLE`
  때문에 키 이벤트를 못 받아 모드 간 동작이 갈린다.
- **부팅 후 자동 시작을 넣지 않는다.** Android 15+ 의 백그라운드 FGS 제약에 걸린다.

## targetSdk 를 올리지 말 것

`compileSdk` 는 37(Compose 1.12 요구), **`targetSdk` 는 36 에 고정**한다.
targetSdk 를 올리면 새 OS 의 동작 변경을 옵트인하게 되는데 검증 범위를 넘어선다.
lint 의 `OldTargetApi` 경고 1건은 이 선택의 결과이며 의도된 것이다.

## 배포 전제

**개인용 사이드로딩.** Google Play 심사 대상이 아니므로 `specialUse` 포그라운드 서비스
사유서나 권한 정당화가 필요 없다. iOS 는 오버레이·백그라운드 실행이 OS 차원에서
차단되어 범위에서 제외되었다.

## 작업 기록

`docs/plans/blackout-overlay-modes/` 에 spec / context / checklist 가 있다.
작업을 이어갈 때는 checklist 부터 읽는다. 진행하며 체크박스를 갱신할 것.
`docs/technical-feasibility.md` 와 `docs/architecture.md` 에 판단 근거와 함정 목록이 있다.
