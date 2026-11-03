# uiautomator 덤프에 화면에 없는 UI 가 있다

> 범용 정본: 전역 스킬 `android-device-test` 의 `references/platform/uiautomator-dump-shows-ui-not-on-screen.md`. 이 파일은 이 앱에서 겪은 원문 기록이다.

## 환경
Galaxy S23+ / Android 16. adb 셸의 `uiautomator dump`·`uiautomator events`.

## 증상
2026-10-30. 접근성 권한이 켜져 있어 화면에는 권한 카드가 없는데, `uiautomator dump` 에는 카드가 있었다.
다시 덤프해도 같았다. 카드(약 320px)만큼 아래 노드 좌표가 밀려, 덤프는 탭을 y=663 이라 했고 화면의 탭은 y=340 이었다.
"덤프가 사라진 노드를 붙들고 있다" 로 오진했다.

## 원인
uiautomator 는 UiAutomation 으로 붙는데, 기본값이 **붙어 있는 동안 다른 접근성 서비스를 끄는 것(suppress)** 이다.
`UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` 를 줘야 안 끄는데, 셸의 `uiautomator dump`·`events` 에는 줄 방법이 없다.

이 앱은 접근성 서비스가 붙어 있는지(`ScreenOffAccessibilityService.connected`)를 보고 권한 카드를 그린다. 그래서 덤프하는 순간
서비스가 빠지고, 앱이 **실제로** 카드를 그리고, 덤프가 그걸 찍었다. 덤프가 끝나면 서비스가 다시 붙어 카드가 사라진다.
덤프 앞뒤의 스크린샷에 카드가 없었던 이유다.

2026-11-03 재현:
- `uiautomator events` 를 켜 둔 동안 `dumpsys accessibility` 의 `Bound services` 에서 이 앱 서비스가 빠졌다. 실제 화면
  (스크린샷)에도 권한 카드가 떴다. 끄자 몇 초 안에 다시 붙고 카드가 사라졌다.
- `uiautomator dump` 도 도는 동안 서비스가 빠졌다(`dumpsys accessibility` 로 확인). 그 덤프 자체는 사용자가 그새 다른
  앱으로 넘어가 이 앱 화면을 담지 못했다 — 덤프에 카드가 찍히는 것은 10-30 의 덤프가 보여 준다.

## 해결
- 보이는지는 스크린샷 픽셀로 판정하고, 누를 좌표도 스크린샷에서 확인한다. 덤프 좌표는 덤프가 바꿔 놓은 화면의 좌표일 수 있다.
- 이 앱이 받는 접근성 이벤트를 보려고 `uiautomator events` 를 쓰지 않는다. 붙어 있는 내내 서비스가 꺼져, 권한이 없는
  상태의 앱을 보게 된다. 대신 시험 빌드에서 서비스의 `onAccessibilityEvent` 를 `Log.d` 로 찍는다. 2026-11-03 에
  '준비됐어요' 의 `PANE_APPEARED` 를 이렇게 확인했다(서비스 `eventTypes` 가 `TYPE_WINDOW_STATE_CHANGED` 라 그대로 받는다).
- 차폐 중에는 덤프하지 않는다. 차폐는 이 서비스가 띄운 접근성 오버레이라, 서비스가 빠지면 같이 떨어질 수 있다(실측은 안 했다).

## 재발 방지
덤프 중에 `adb shell dumpsys accessibility | grep "Bound services"` 를 보면 이 함정인지 바로 안다 — 대상 서비스가 빠지면 이것이다.
CLAUDE.md 실기기 함정 · architecture.md §8 "실기기 검증 시 속기 쉬운 것" 에 한 줄씩 있다.
