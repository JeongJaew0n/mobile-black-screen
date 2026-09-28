# UX 연구 — 무엇을 고치면 좋은가

> 2026-09-28. **의견 문서다.** 조사 → 제안 → 그 제안을 다시 조사해 반박 을 세 번 돌렸다.
> 각 제안이 어느 회차에서 어떻게 바뀌었는지 남겼다 — 결론만 남기면 왜 그 결론인지가 사라진다.
> "미검증" 은 실제로 해 보지 않은 것이다.
>
> **2026-09-29 — 1·2·3·6 구현.** `docs/plans/ux-improvements/`. 4(재생 컨트롤 숨기기)·5(전화) 는 보류.

## 결론 먼저 — 우선순위

| 순위 | 제안 | 왜 | 비용 |
|---|---|---|---|
| 1 | **해제 방법을 보이게** — 시작 버튼 아래 한 줄 + 차폐 중 첫 탭에 안내를 잠깐 | 지금은 화면에 단서가 0. 처음 켠 사람은 못 푼다 | 작음 |
| 2 | **자동 종료 타이머** | 실측상 배터리에 효과가 있는 유일한 손잡이. 차폐 183 mA → 실제 꺼짐 96 mA | 중간 |
| 3 | **권한 안내 고치기** — 제한된 설정 3단계, 설치 출처로 미리 안내, 조용한 실패 없애기 | 남에게 주면 기본 모드가 안 켜진다 | 중간 |
| 4 | **재생 컨트롤을 탭해야 보이게** (선택 항목) | 주머니 속 오작동으로 곡이 넘어간다 | 작음 |
| 5 | **전화가 오면 차폐가 어떻게 되는지 실험** | 모른다. 가려지면 전화를 못 받는다 | 실험 먼저 |
| 6 | `볼륨 0` 선택의 되돌릴 수 없음을 더 드러내기 | 실제로 테스트 중 8 → 0 사고가 났다 | 작음 |

**뺀 것:** 근접 센서 포켓 모드, 시계까지 "탭해야 보이게", 튜토리얼 카드 넘기기, 전화 알림 읽기. 이유는 아래.

---

## 1. 해제 방법을 보이게

### Round 1 — 문제
해제는 일부러 단일 탭이 아니다(주머니). 그런데 3회 탭·꾹 누르기·더블 탭은 **화면에 아무 단서가 없다.**
코드 주석도 인정한다 — "다른 제스처들은 알려주지 않으면 알 수 없다". 숨은 제스처의 발견성 문제는
오래된 비판이다([NN/g 제스처](https://www.nngroup.com/topic/gestures/), Norman & Nielsen
*Gestural interfaces: a step backward in usability*). 경쟁 앱 Jet Black Screen 은 보이는 '잠금 해제'
버튼을 두고([Play](https://play.google.com/store/apps/details?id=com.infinityplus.jet.blackscreen&hl=en_US)),
이 앱도 지문 모드에서만 버튼이 있다.

→ 제안: 온보딩 튜토리얼을 넣는다.

### Round 2 — 반박
NN/g 연구상 **카드 넘기기 튜토리얼은 과업 수행을 개선하지 않는다.** 앱을 실제보다 복잡해 보이게 하고
기억에 남지 않는다. 대신 필요한 순간에 보여 주는 **상황별 도움말**이 낫다
([Onboarding Tutorials vs. Contextual Help](https://www.nngroup.com/articles/onboarding-tutorials/),
[Mobile-App Onboarding](https://www.nngroup.com/articles/mobile-app-onboarding/)).

그리고 사람들이 이미 아는 패턴이 있다 — 삼성 AOD 의 **"탭하면 10초 표시"**
([Samsung](https://www.samsung.com/us/support/answer/ANS10001976/)). 검은 화면을 한 번 탭하면
잠깐 보였다 사라진다. 이 앱 사용자 다수가 삼성이다.

→ 수정: 튜토리얼 대신 두 곳에 상황별로.

### Round 3 — 이 앱의 제약에 대 보기
- 차폐 중 **표시할 내용이 없으면 밝기는 0** 이다. 안내 문구를 띄워도 안 보인다. 안내가 떠 있는 동안
  밝기를 올려야 하고, 밝기를 정하는 곳은 `effectiveBrightness()` 하나여야 한다(`CLAUDE.md`).
  지문 프롬프트가 이미 같은 길을 쓴다.
- 3회 탭의 **첫 탭이 곧 안내 트리거**다. 첫 탭에 안내가 뜨고, 이어지는 두 탭이 해제한다 — 두 동작이
  충돌하지 않는다. 더블 탭·꾹 누르기도 같다.
- 주머니 속 탭으로 안내가 뜨면 밝기 0.30 이 몇 초 켜진다. 주머니 안이라 보는 사람이 없고, 실측상
  0.30 과 0 의 전력 차이는 측정 해상도 아래였다(183.3 mA 동일). 비용이 거의 없다.

**최종안**
- 설정 화면 "Screen Off 시작" 버튼 **바로 아래 한 줄**: "3번 탭하면 풀립니다" — 지금 고른 해제 방법으로.
  누르기 직전에 읽히는 자리다.
- 차폐 중 **첫 탭** → 화면 가운데 아래에 "3번 더 탭하면 풀립니다"(남은 횟수) 를 5초. 그동안만 밝기 0.30.
- 차폐 창은 켤 때마다 새로 만들어 로케일 문제가 없다. 버블 창에는 넣지 않는다(텍스트 금지 규칙).

## 2. 자동 종료 타이머

### Round 1
경쟁 앱과 "Sleep Timer" 류가 공통으로 가진 기능이다
([Sleep Timer: Media, Screen Off](https://play.google.com/store/apps/details?id=com.okak.sleeptimer&hl=en_US),
[Screen Suite](https://play.google.com/store/apps/details?id=com.cozyme.app.screenoff&hl=en)).
이 앱은 "아직 하지 않은 것" 목록에 있다.

### Round 2
켜 두고 잊으면 밤새 간다. 이 앱의 대가는 **8시간에 17%p**(`power-measurement.md`). 공유 문제
문서(`sharing-readiness.md` §2-3)에서도 위험으로 짚었다.

### Round 3 — 실측과 대 보기
이 앱에서 배터리를 움직이는 것은 밝기가 아니라 **화면이 켜져 있다는 사실**이다.

| 상태 | 전류 |
|---|---|
| Screen Off + 시계 (패널 0.30) | 183.3 mA |
| Screen Off 무표시 (패널 0.00) | 183.3 mA |
| 실제 화면 꺼짐 | 96.1 mA |

타이머가 끝나면 차폐를 풀고 접근성 서비스의 `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`(API 28+)으로
**화면을 진짜로 끈다.** 차폐만 풀면 기기 꺼짐 시간이 이어받는데, 그게 길면(이 테스트 기기는 24시간)
화면이 켜진 채 남는다 — 구현 계획 때 추가한 판단. 표에서 유일하게 87 mA 를 아끼는 길이다. 실패 방향도 안전하다 — 타이머가
잘못 돌면 차폐가 풀릴 뿐 갇히지 않는다(`CLAUDE.md` "실패하면 어떻게 빠져나오는가").

- 접근성 서비스 안에서 코루틴 `delay` 로 충분하다. 화면이 켜져 있어 Doze 에 안 들어가고, 알람 권한이 필요 없다.
- 선택지: 끔(기본) / 15분 / 30분 / 1시간 / 2시간. 재생 중이면 "재생이 끝나면" 도 후보.
- **끝날 때 미디어를 멈출지는 따로 정해야 한다.** 멈추면 수면 타이머, 안 멈추면 화면 타이머다. 사용자 결정.

## 3. 권한 안내

### Round 1
Android 13+ 에서 사이드로딩 앱은 접근성·알림 접근 토글이 **"제한된 설정"** 으로 막힌다
([Android Police](https://www.androidpolice.com/android-13-blocks-accessibility-services-sideloaded-apps/),
[Esper](https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis)).
앱은 이걸 안내하지 않는다. `adb install` 은 이 제한에 안 걸려 개발 중엔 못 봤다.

Play 정책상 접근성 사용은 **앱 안에서, 설정으로 보내기 직전에** "무엇을 왜 하는지" 를 밝히고
동의·거절 두 선택지를 줘야 한다([Prominent disclosure](https://support.google.com/googleplay/android-developer/answer/11150561?hl=en),
[AccessibilityService API 정책](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)).
Play 에 안 올려도 좋은 설계다.

### Round 2 — 더 나빠졌다
- **Android 15 의 Enhanced Confirmation Mode** 가 제한을 더 조였다. 신뢰하지 않는 출처에서 설치한 앱이
  접근성·알림 리스너를 켜려 하면 ECM 대화상자가 뜬다
  ([Android Authority](https://www.androidauthority.com/android-15-restricted-settings-sideloading-3481098/),
  [Android Police](https://www.androidpolice.com/android-15-enhanced-confirmation-mode-sideloading/)).
- **Android 16 은 통화 중에 접근성 권한 켜기를 막는다**
  ([inspire2rise](https://www.inspire2rise.com/android-16-blocks-sensitive-settings-changes-during-calls.html)).
  "권한 켜는 중인데 안 된다" 의 원인이 하나 더 생겼다.
- 푸는 순서가 직관과 반대다 — **토글을 먼저 한 번 눌러 경고를 봐야** 앱 정보 ⋮ 메뉴에 "제한된 설정
  허용" 이 나타난다([Kvaesitso 문서](https://kvaesitso.mm20.de/docs/user-guide/troubleshooting/restricted-settings.html)).
  순서를 모르면 메뉴를 찾아도 없다.

### Round 3 — 미리 알 수 있는가
설치 출처는 `PackageManager.getInstallSourceInfo(...).installingPackageName` 으로 권한 없이 읽힌다
(API 30+, [InstallSourceInfo](https://developer.android.com/reference/android/content/pm/InstallSourceInfo)).
스토어(`com.android.vending`, `com.sec.android.app.samsungapps`)가 아니고 API 33+ 면 **제한될 가능성이
높다**고 보고 안내를 미리 띄울 수 있다. adb 설치는 `null` 이라 개발자에게는 안 뜬다 — 그게 맞다.

"제한됐는지" 자체를 확인하는 공개 API 는 못 찾았다. 그래서 **추정으로 안내만** 하고 막지는 않는다.

**최종안**
- 접근성 카드를 3단계로: ① "Screen Off 가 화면 전체를 덮으려면 접근성 권한이 필요합니다 — 화면 내용은
  읽지 않습니다" + [설정 열기] [나중에] ② (사이드로딩 + API 33+ 일 때만) "'제한된 설정' 경고가 뜨면:
  경고를 닫고 → 앱 정보 → ⋮ → 제한된 설정 허용" + [앱 정보 열기] ③ 통화 중이면 "통화가 끝난 뒤 켜 주세요"(Android 16).
- **설명 없이 설정이 열리는 것 고치기** — 타일·버블로 FULL 을 켰는데 접근성이 꺼져 있으면 세 진입점 모두
  접근성 설정을 연다(처음 쓴 판단 "아무 일도 안 일어난다" 는 틀렸다 — 구현 계획 때 코드로 확인).
  다만 **왜 열렸는지 말하지 않는다.** 토스트 한 줄을 붙인다. 재설치·강제 종료 뒤 흔히 겪는다
  (`troubleshootings/reusable/accessibility-service-silently-disabled-by-reinstall.md`).
- 알림 접근(재생 바)도 같은 벽이다. 같은 카드 구조를 재사용.

## 4. 재생 컨트롤을 탭해야 보이게 (선택)

### Round 1
주머니 오작동 방지는 경쟁 앱의 대표 기능이다 — Jet Black Screen 의 Pocket Mode, Blackr 의 "주머니 속
의도치 않은 터치 방지"([Blackr](https://play.google.com/store/apps/details?id=com.neximolabs.blackr&hl=en_US)).
→ 제안: 근접 센서로 주머니를 감지해 입력을 막는다.

### Round 2 — 근접 센서는 믿을 수 없다
- S23 계열 등 최근 삼성은 **물리 근접 센서 대신 빛 센서 기반 가상 센서**를 쓰고, 주머니 감지가 불안정하다는
  보고가 많다([Samsung Community S23 FE](https://r2.community.samsung.com/t5/Galaxy-S/The-S23-Fe-pocket-mode-sensor-is-malfunctioning/td-p/16274250)).
- 삼성 자체의 "오터치 방지" 도 주머니에서 스크린샷 165장·긴급 전화가 눌렸다는 불만이 이어진다
  ([S24+ 커뮤니티](https://eu.community.samsung.com/t5/galaxy-s24-series/s24-issues-when-in-pocket/td-p/9855159/page/2),
  [S23](https://us.community.samsung.com/t5/Galaxy-S23/Accidental-Protection-isn-t-stopping-my-phone-from-coming-on-in/td-p/2616147)).
  OS 가 못 하는 걸 앱이 센서로 하기는 어렵다.

→ 수정: 센서 대신 **입력 자체를 주머니에 강하게.** 이 앱에서 주머니에 약한 것은 해제(3회 탭이라 강함)가
아니라 **늘 떠 있는 재생 컨트롤 버튼 셋**(44dp)이다.

### Round 3
컨트롤을 첫 탭 뒤 10초만 보이게 하면(1번 제안과 같은 트리거) 주머니 속 무작위 터치가 버튼을 맞힐
확률이 크게 준다 — 먼저 빈 곳을 한 번 치고, 10초 안에 44dp 과녁을 맞혀야 한다. 대가는 일시정지에
탭 한 번이 더 든다는 것. **사람마다 갈려 선택 항목으로.** 기본은 지금처럼 늘 보이기.

## 5. 전화가 오면 — 실험 먼저

`TYPE_ACCESSIBILITY_OVERLAY` 는 시스템 대화상자 위까지 덮는다. 화면이 켜진 상태의 전화는 전체 화면이
아니라 **헤드업 알림**으로 오는데, 그것도 차폐 아래일 가능성이 높다. 소리는 나지만 받으려면 먼저 해제해야
한다. **실제로 걸어 본 적이 없다.**

- PIN 화면은 시스템이 오버레이를 걷어 주는 것을 실측했다. 전화도 그런 보호 대상인지 모른다.
- 권한 없이 "전화가 울린다" 를 알 방법으로 `AudioManager` 모드 변화(`MODE_RINGTONE`)를 찾았지만, 서드파티
  앱에서 권한 없이 믿고 쓸 수 있는지 **문서로 확인하지 못했다**
  ([OnModeChangedListener](https://developer.android.com/reference/android/media/AudioManager.OnModeChangedListener)).
- 이미 가진 알림 접근으로 "전화 알림" 을 보는 방법은 **쓰지 않는다.** 알림 접근을 받으면서 한 약속이 "알림을
  읽지 않는다" 이고, 콜백을 비워 두는 것이 그 약속의 유일한 증거다(`CLAUDE.md`).

→ 순서: ① 전화를 걸어 실제로 가려지는지 본다 ② 가려지면 `MODE_RINGTONE` 감지를 실기기에서 시험 ③ 되면
"전화가 오면 풀기" 선택 항목. `READ_PHONE_STATE` 는 권한이 하나 더 늘어 최후 수단.

## 6. `볼륨 0` 을 더 드러내기

테스트 중 `볼륨 0` 이 켜져 있는 줄 모르고 차폐를 켜 미디어 볼륨을 8 → 0 으로 내렸다. 되돌아오지 않는
설정이라 일어난 일이다. 설명문은 이미 "0 그대로입니다" 라고 한다.

- 이 선택이 켜져 있을 때 **시작 버튼 아래 줄**(1번과 같은 자리)에 "미디어 볼륨을 0 으로 내립니다" 를 함께.
- 누르기 직전에 보이는 자리라 설정 화면을 뒤지지 않아도 안다.

작다. 1번을 만들면 같은 자리에 한 줄 더하는 일이다.

---

## 뺀 것과 이유

| 후보 | 뺀 이유 | 회차 |
|---|---|---|
| 근접 센서 포켓 모드 | 삼성 가상 센서가 불안정하고, OS 의 오터치 방지도 실패한다 | 2 |
| 시계·날짜까지 "탭해야 보이게" | AOD 가 그러는 이유는 배터리인데, 이 앱은 밝기 0.30 과 0 의 전력이 같다. 얻는 것 없이 시계를 못 보게 된다 | 3 |
| 첫 실행 튜토리얼 카드 | NN/g — 과업 수행을 개선하지 않는다. 상황별 도움말로 대체 | 2 |
| 알림 접근으로 전화 감지 | "알림을 읽지 않는다" 약속을 깬다 | 2 |

## 참고 — 이 앱 자신의 기록
- `docs/power-measurement.md` — 183.3 / 183.3 / 96.1 mA
- `docs/sharing-readiness.md` — 남에게 줄 때의 문제. 이 문서의 3번·5번과 겹친다
- `CLAUDE.md` — 밝기는 `effectiveBrightness()` 하나, 버블 창 텍스트 금지, 해제 수단의 탈출구
