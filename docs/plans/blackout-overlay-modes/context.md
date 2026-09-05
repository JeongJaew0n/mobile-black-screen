# context — blackout-overlay-modes

> **기록 보존용 문서입니다.** 여기 적힌 판단 중 일부는 이후 뒤집혔습니다 —
> 무엇이 왜 뒤집혔는지는 [full-screen-accessibility-overlay/context.md](../full-screen-accessibility-overlay/context.md) 에 있습니다.

## 사용자의 원 요청

> "스마트폰에서 화면만 닫고, 실제로 백그라운드에서는 모두 다 돌아가고 있는 앱을 만들거야.
> 말 그대로 '화면만 꺼진' 상태인 앱인거지.
> 해당 앱에서는 기본적으로는 아무것도 없는 걸 상정해. 대신 세팅으로 문장이나 시간을 넣을 수 있지.
> 일단 기술적으로 가능한지 분석해서 docs폴더 만들고 md 파일 만들어줘."

이어서 두 가지를 확정했다.

> "1. 다른 앱들이 계속 돌아가는 상태에서 화면만 덮기야. 2. 안드로이드에서 할거야."

## 왜 이걸 지금 하는가

새 프로젝트다. 저장소(`mobile-black-screen`)는 초기 커밋 하나만 있는 빈 상태였고,
기술 타당성 분석을 먼저 수행한 뒤 구현에 착수하는 흐름이다.

요구의 핵심은 "화면이 꺼진 것처럼 보이지만 기기는 계속 살아 있는 상태"를 만드는 것이다.
용도는 명시되지 않았으나 침대 옆 시계, 화면 잠금 대용, 배터리 절약, 문구 표시 등이
자연스러운 사용 맥락이다.

## 결정된 방향

**Android 전용. 화면을 "끄는" 게 아니라 "검게 덮는" 앱. Blackout(풀스크린) 모드를 기본값으로
하고 Overlay 모드를 선택지로 함께 제공하는 하이브리드.**

## 이 방향에 도달한 과정

분석 과정에서 두 개의 하드 제약이 순서대로 드러났고, 그때마다 방향이 조정되었다.

### 1차 조정 — "화면 끄기"에서 "검게 덮기"로

Android에는 임의 시점에 화면을 끄는 공개 API가 없다. 있는 것은
`DevicePolicyManager.lockNow()`(Device Admin 필요, 진짜 잠금)와
`PROXIMITY_SCREEN_OFF_WAKE_LOCK`(근접센서를 가려야 함)뿐이다.

더 근본적으로, **화면을 물리적으로 끄면 요구사항의 "문장/시간 표시"가 불가능해진다.**
두 요구가 모순이므로 정의를 "화면을 검게 덮고 밝기를 0으로 내린다"로 바꿨다.
AMOLED에서는 검은 픽셀이 발광하지 않아 체감상 꺼진 것과 거의 같다.

### 2차 조정 — 오버레이 단독에서 하이브리드로

사용자가 "다른 앱들이 계속 돌아가는 상태에서 화면만 덮기"를 선택해 오버레이 방식으로
확정한 뒤, **`TYPE_APPLICATION_OVERLAY`가 상태바를 덮을 수 없다**는 사실이 확인되었다.
Android 8(O)에서 의도적으로 막힌 동작이고 Google이 버그가 아니라고 확인했으며 우회 수단이 없다.

이로 인해 두 방식의 장단이 정확히 상충하게 되었다.

- **Overlay**: 다른 앱이 계속 렌더링됨 ↔ 상단에 밝은 띠(시계·배터리·신호·알림 아이콘)가 남음
- **Blackout**: 완전한 검정 ↔ 최상단 앱의 UI 렌더링이 멈춤

여기서 중요한 사실 하나가 판단을 갈랐다. **Blackout 모드에서도 음악 재생·다운로드·
내비게이션·타이머·동기화 등 백그라운드 동작은 전부 유지된다.** Android에서 백그라운드 앱은
죽지 않는다. 멈추는 것은 최상단 앱의 화면 렌더링(게임 진행, 영상 재생)뿐이다.
즉 대부분의 "백그라운드에서 다 돌아간다" 요구는 Blackout만으로도 충족된다.

이 제약을 사용자에게 설명하고 다시 물은 결과 **하이브리드**를 선택했다.

## 기각된 대안

- **iOS 지원** — 기각. 다른 앱 위 오버레이 개념이 OS에 없고, 백그라운드 임의 실행도 차단된다.
  화면을 끄거나 잠그는 공개 API도 없다. 무음 오디오를 재생해 백그라운드를 유지하는 편법이
  알려져 있으나 App Store 심사 거절 사유라 채택하지 않았다. iOS 17+ StandBy 모드가 이미
  유사한 기능을 OS 차원에서 제공하는 점도 고려했다.
- **`DevicePolicyManager.lockNow()`로 진짜 화면 끄기** — 기각. 진짜로 꺼지지만 그러면
  문장/시간을 표시할 수 없어 요구사항과 정면 충돌한다. Device Admin 등록도 필요하다.
- **오버레이 단독** — 기각. 상태바가 남아 "화면이 꺼진" 착시가 깨진다.
  (Overlay 자체는 선택 모드로 살렸고, 기본값 자리만 Blackout에 내줬다.)
- **풀스크린 단독** — 기각. 사용자가 명시적으로 "다른 앱들이 계속 돌아가는 상태"를
  요구했으므로 그 경로를 완전히 없앨 수는 없다.
- **오버레이에 `FLAG_NOT_TOUCHABLE` 적용(터치를 아래 앱으로 통과)** — 기각.
  주머니 속 오작동이 그대로 아래 앱에 전달되고, 동시에 해제 제스처를 받을 방법이 사라진다.
  화면이 검어서 어차피 아래 앱을 조작할 수도 없다.
- **크로스플랫폼 프레임워크(Flutter / React Native)** — 기각. 이 앱의 핵심 가치가 전부
  오버레이·포그라운드 서비스·WakeLock·밝기 오버라이드 같은 플랫폼 밀착 API에 있어
  결국 네이티브 플러그인을 직접 짜게 된다. iOS를 뺀 시점에서 이점이 사라졌다.
- **`targetSdk`를 33으로 낮춰 `foregroundServiceType` 의무 회피** — 기각.
  사이드로딩 전용이라 기술적으로는 가능하지만 최신 OS 동작에서 벗어난다.
  targetSdk 36 + `specialUse`로 정직하게 간다.
- **볼륨 키 해제** — 기각. Blackout은 되지만 Overlay는 `FLAG_NOT_FOCUSABLE` 때문에
  키 이벤트를 못 받아 모드 간 동작이 갈린다.
- **부팅 후 자동 시작** — 기각. Android 15+에서 백그라운드 FGS 시작 시
  `ForegroundServiceStartNotAllowedException` 위험. 실익 대비 위험이 크다.

## 제약 / 합의 사항

### 기술적 제약 (회피 불가)
- 임의 시점에 화면을 끄는 공개 API 없음
- `TYPE_APPLICATION_OVERLAY`는 상태바·**내비게이션 바**·알림 셰이드·잠금 화면·볼륨 다이얼로그를 덮지 못함 (실기기 확인)
- Android 12+ 다른 앱이 `hideOverlayWindows()`로 우리 오버레이를 숨길 수 있음
- 설정·비밀번호 입력 등 민감 화면에서는 OS가 오버레이를 자동으로 숨김
- LCD 기기는 백라이트가 남아 완전히 검게 보이지 않음 (AMOLED 최적화가 전제)
- 제조사(삼성·샤오미 등)의 공격적 백그라운드 종료

### 환경 제약
- **Android SDK 미설치.** `ANDROID_HOME`/`ANDROID_SDK_ROOT` unset, `adb`·`gradle` 없음.
  JDK 21(Temurin)만 설치됨. 코드 작성은 가능하나 **컴파일·설치·실기기 검증 불가.**

### 사용자가 명시한 선호
- 기본 상태는 **아무것도 표시하지 않는 완전한 검정**
- 문장과 시간은 **설정으로 켜는 옵션**이지 기본값이 아님
- 배포는 **개인용 사이드로딩** → Play 심사 대응 불필요

## 관련 자료

- `docs/technical-feasibility.md` — 기술 타당성 분석. 플랫폼별 판정, 방식 비교, 리스크
- `docs/architecture.md` — 구현 설계. 두 모드 구조, 핵심 코드, 함정 목록
- 근거가 된 외부 문서
  - [Android O is Breaking Apps that Overlay on top of the Status Bar](https://www.xda-developers.com/android-o-is-breaking-apps-that-overlay-on-top-of-the-status-bar/)
  - [issuetracker #37140075 — TYPE_APPLICATION_OVERLAY should allow drawing on status bar](https://issuetracker.google.com/issues/37140075)
  - [Behavior changes: Android 15](https://developer.android.com/about/versions/15/behavior-changes-15)
  - [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
