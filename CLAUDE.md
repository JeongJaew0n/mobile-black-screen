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

**함정 두 가지.**

- `BlackoutActivity` 는 `exported="false"` 라 `am start` 로 직접 띄울 수 없다
  (`SecurityException`). `MainActivity` 를 띄우고 버튼을 탭하거나 타일을 써야 한다.
- `cmd statusbar click-tile` 만으로는 `onStartListening` 이 돌지 않아 TileService 가
  바인딩되지 않는다. `cmd statusbar expand-settings` 를 먼저 호출해야 한다.
  앱 버그가 아니라 adb 아티팩트다.

## 구조의 핵심

**두 모드가 표시 콘텐츠를 하나의 Composable(`ui/BlackScreenContent.kt` 의
`BlackScreenRoot`)로 공유한다.** 차이는 "어떤 창에 올리는가"뿐이다.

- **Blackout** (`blackout/BlackoutActivity.kt`) — 풀스크린 Activity. 시스템 바를
  완전히 숨긴다. 권한 0개.
- **Overlay** (`overlay/OverlayService.kt`) — `TYPE_APPLICATION_OVERLAY` 창 +
  `specialUse` 포그라운드 서비스. 아래 앱이 계속 렌더링된다.

표시 내용을 고칠 때는 `BlackScreenRoot` 한 곳만 만지면 두 모드에 함께 반영된다.
모드별로 갈라 쓰지 말 것.

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
