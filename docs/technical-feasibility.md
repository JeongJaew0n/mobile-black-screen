# mobile-black-screen — 기술 타당성 분석

작성일: 2026-08-30
최종 갱신: 2026-08-30
상태: **분석 완료 · 방향 확정** → 구현 설계는 [architecture.md](./architecture.md) 참조

> **확정 사항**
> - 플랫폼: **Android 단독** (iOS 제외)
> - 차폐 방식: **하이브리드** — Blackout(풀스크린) 기본 + Overlay 선택
> - 배포: **개인용 / 사이드로딩** → Play 심사 리스크(R1) 소거

---

## 1. 요구사항 정리

원문 요구:

> 스마트폰에서 화면만 닫고, 실제로 백그라운드에서는 모두 다 돌아가고 있는 앱.
> 말 그대로 '화면만 꺼진' 상태.
> 기본적으로는 아무것도 없음. 대신 세팅으로 문장이나 시간을 넣을 수 있음.

여기서 **"모두 다 돌아가고 있다"는 두 가지로 해석**될 수 있고, 이 둘의 기술적 난이도는 완전히 다릅니다.

| 해석 | 의미 | Android | iOS |
|---|---|---|---|
| **A. 기기 전체 유지** | 다른 앱들(음악, 통화, 다운로드, 게임 등)이 계속 돌아가는 상태에서 화면만 덮음 | ✅ 가능 (오버레이) | ❌ 불가능 |
| **B. 이 앱 자신만 유지** | 이 앱이 화면 꺼진 상태에서도 계속 동작 | ✅ 가능 (Foreground Service) | ⚠️ 매우 제한적 |

**핵심 결론 먼저:**

- **Android: 해석 A/B 모두 구현 가능.** 단, "진짜 화면 OFF"는 아니고 **"화면은 켜져 있으나 검게 덮인 상태"**가 현실적인 최선입니다.
- **iOS: 해석 A는 OS 정책상 원천 불가능.** 자기 앱 안에서만 검은 화면을 띄우는 수준으로 축소됩니다.
- 즉 이 앱은 **Android 퍼스트로 설계해야 하고, iOS는 "같은 앱"이 아니라 "많이 축소된 다른 앱"**이 됩니다.

---

## 2. "화면을 끈다"는 것의 실제 선택지

물리적으로 화면을 완전히 끄면 아무것도 표시할 수 없습니다. 그런데 요구사항에는 "문장이나 시간을 넣을 수 있다"가 있으므로, **표시가 필요한 시점에는 화면이 켜져 있어야 합니다.** 이 모순을 어떻게 푸느냐가 설계의 중심입니다.

### 방식 1 — 검은 화면 + 밝기 최소 (권장)

```kotlin
val lp = window.attributes
lp.screenBrightness = 0.0f   // 0.0 ~ 1.0, 앱 창에 한정된 밝기 오버라이드
window.attributes = lp
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```

- `screenBrightness`는 0.0~1.0 범위이며 **해당 창이 앞에 있는 동안 사용자 설정 밝기를 덮어씁니다.** 일반 앱 권한으로 가능합니다.
- **화면은 기술적으로 "ON"** 입니다. 백라이트가 최저로 내려갈 뿐입니다.
- **AMOLED 기기에서는 검은 픽셀이 실제로 발광하지 않으므로** 체감상 꺼진 것과 거의 같고 전력 소모도 매우 낮습니다. **LCD 기기에서는 백라이트가 남아 어둡게 빛납니다.**
- 시간/문장 표시가 자연스럽게 가능합니다.
- ⚠️ 화면이 켜져 있으므로 **터치가 그대로 들어옵니다.** 주머니 오작동 방지 로직이 필수입니다.

### 방식 2 — 시스템 오버레이 (해석 A 구현용)

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```
`WindowManager` + `TYPE_APPLICATION_OVERLAY` (API 26+)로 다른 앱 위에 전체 화면 검은 창을 띄웁니다.

- 아래에서 **다른 앱이 그대로 실행됩니다.** "화면만 꺼진" 요구에 가장 근접한 방식입니다.
- 권한은 런타임 요청이 아니라 `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` 설정 화면으로 사용자를 보내야 합니다. 허들이 높습니다.
- **한계:**
  - **상태바도 내비게이션 바도 덮을 수 없습니다. 우회 불가능한 하드 제약입니다.** Android 8(O)에서 `TYPE_APPLICATION_OVERLAY`가 시스템 UI 위에 그리지 못하도록 의도적으로 막혔고, Google이 버그가 아닌 설계라고 확인했습니다. `FLAG_LAYOUT_IN_SCREEN` / `FLAG_LAYOUT_NO_LIMITS` / `FLAG_LAYOUT_INSET_DECOR` 어떤 조합으로도 뚫리지 않습니다.
    **실측(Galaxy S23+ / Android 16): 오버레이 창 프레임이 `[0,94][1080,2214]` 로 상태바(94px) 아래에서 시작해 내비바(126px) 위에서 끝난다. z-order 상으로도 `NavigationBar`·`StatusBar` 가 오버레이보다 위에 있어 기하학·레이어 양쪽에서 막혀 있다.**
    결과적으로 상단에 시계·배터리·신호 아이콘이, 하단에 내비게이션 바가 그대로 남아 "화면이 꺼진" 착시가 깨집니다.
  - 볼륨 다이얼로그, 알림 셰이드, 잠금 화면 위에도 못 덮습니다.
  - Android 12+ 부터 다른 앱이 `hideOverlayWindows()`로 오버레이를 강제로 숨길 수 있고, 설정/비밀번호 입력 등 **민감 화면에서는 OS가 자동으로 오버레이를 숨깁니다.**
  - `FLAG_NOT_TOUCHABLE`로 터치를 아래 앱에 통과시키면, 반대로 **해제 제스처를 받을 방법이 사라집니다.** 통과/차단 중 하나를 골라야 합니다.

### 방식 3 — 진짜 화면 OFF

| 수단 | 동작 | 판정 |
|---|---|---|
| `DevicePolicyManager.lockNow()` | Device Admin 등록 후 즉시 잠금. **진짜로 꺼짐** | 문장/시간 표시 불가 · Play 심사 매우 까다로움 → ❌ |
| `PROXIMITY_SCREEN_OFF_WAKE_LOCK` | 근접센서가 가려지면 화면이 실제로 꺼짐 (공개 API, `WAKE_LOCK` 권한) | 센서를 계속 가려야 함 → "포켓 모드" 부가 기능으로만 ⚠️ |
| 임의 시점 화면 끄기 API | **존재하지 않음** | ❌ |

> 참고: `PROXIMITY_SCREEN_OFF_WAKE_LOCK`은 다른 wake lock과 달리 기기 잠자기 자체를 막지 않지만, 이 락으로 화면이 꺼져 있는 동안은 사용자 활동으로 간주되어 잠들지 않습니다.

**→ 결론: 방식 1을 기본으로 하고, 방식 2를 "전체 화면 모드" 옵션으로, 방식 3(근접센서)을 부가 기능으로 배치합니다.**

---

## 3. "백그라운드에서 계속 돌아간다" — Android

### 3.1 Foreground Service

프로세스가 죽지 않으려면 포그라운드 서비스가 사실상 유일한 합법 수단입니다.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".BlackScreenService"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="screen_dimming_overlay" />
</service>
```

**제약 (중요):**
- targetSdk 34(Android 14) 이상은 **`foregroundServiceType` 선언이 필수**입니다.
- 이 앱의 용도는 기존 타입(미디어/위치/데이터동기화 등) 어디에도 안 맞으므로 **`specialUse`를 써야 합니다.**
- `specialUse`는 **Google Play Console의 App content 페이지에서 사용 사유를 자유 서술로 제출해야 하고, 심사자가 이를 검토합니다.** → **출시 리스크가 여기 집중됩니다.** 사유를 설득력 있게 쓰지 못하면 반려됩니다.
- 상시 표시되는 알림이 하나 남습니다(제거 불가). "아무것도 없는 화면" 컨셉과 미묘하게 충돌합니다.

### 3.2 Doze / 배터리 최적화 / 제조사 킬러

- Doze 모드와 App Standby가 백그라운드 동작을 죽입니다. 포그라운드 서비스로 상당 부분 완화되지만 완전하지 않습니다.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 안내가 필요합니다. **단, 이 권한은 Play 정책상 정당한 사유가 있는 앱에만 허용됩니다.**
- **삼성/샤오미/오포 등 제조사는 AOSP보다 훨씬 공격적으로 백그라운드 앱을 종료합니다.** 기기별 예외 설정 안내 화면이 실무적으로 필요합니다.

### 3.3 CPU 유지

- `PARTIAL_WAKE_LOCK`: 화면이 꺼져도 CPU를 유지합니다. 다만 이 앱은 방식 1을 쓰면 화면이 켜진 상태이므로 대개 불필요합니다. **배터리 소모 원인이 되므로 기본 OFF 권장.**

---

## 4. iOS 분석

### 4.1 불가능한 것

| 요구 | 판정 | 이유 |
|---|---|---|
| 다른 앱 위에 검은 화면 덮기 | ❌ | iOS에 시스템 오버레이 개념 자체가 없음 |
| 프로그램으로 화면 끄기 / 잠그기 | ❌ | 공개 API 없음 (Apple 개발자 포럼에서 반복 확인된 사항) |
| 앱을 백그라운드에서 계속 실행 | ❌ | 임의의 지속 실행 불가 |

iOS의 백그라운드는 **허용된 모드(오디오, 위치, VoIP, BLE, 외부 액세서리, background fetch, `BGProcessingTask`)에서만** 동작합니다. 백그라운드 진입 시 수 초, 요청 시 수 분이 한계이고, **앱이 백그라운드 + 화면 꺼짐 + 비충전 상태면 백그라운드 태스크가 정지합니다.**

> 무음 오디오를 재생해 백그라운드를 유지하는 편법이 널리 알려져 있으나, **App Store 심사 거절 사유**이므로 채택하지 않습니다.

### 4.2 가능한 것

- 앱이 **포그라운드인 동안** 전체 검은 화면 + 시계/문장 표시 → 가능
- `UIScreen.main.brightness = 0.0` → 밝기를 최저로 (시스템 밝기를 실제로 변경하므로, 앱 종료 시 원복 처리 필요)
- `UIApplication.shared.isIdleTimerDisabled = true` → 자동 잠금 방지
- 잠금 화면 표시가 필요하면 **WidgetKit 위젯 / Live Activity** (업데이트 빈도에 강한 제약)
- iOS 17+의 **StandBy 모드**(가로 거치 + 충전 시)가 이 앱 컨셉과 거의 같은 기능을 OS 차원에서 이미 제공합니다.

### 4.3 심사 리스크

**App Store Review Guideline 4.2 (Minimum Functionality)** — 단순히 검은 화면만 띄우는 앱은 반려 가능성이 높습니다. 시계 스타일·문장 커스터마이징·위젯·알람 등 **명확한 부가 가치가 있어야 합니다.**

---

## 5. "문장 / 시간 표시" 기능의 기술 이슈

| 이슈 | 대응 |
|---|---|
| **AMOLED 번인** — 같은 위치에 시계를 오래 띄우면 잔상이 남음 | 수십 초~수 분 주기로 **픽셀 시프트**(표시 위치를 미세 이동). 색은 순백 대신 저휘도 그레이(`#303030` 수준) |
| **의도치 않은 터치** | 해제는 단일 탭이 아니라 **더블탭 / 롱프레스 / 스와이프 패턴**으로. 오버레이 모드에서는 특히 필수 |
| **전력** | 초 단위 갱신 대신 **분 단위 갱신**, 애니메이션 최소화, `TextClock` 또는 분 경계 정렬 타이머 사용 |
| **표시 자체를 끄고 싶을 때** | 완전 무표시 모드가 기본값 (요구사항 그대로) |

---

## 6. 권장 아키텍처

### 6.1 스택

**Android 네이티브 (Kotlin + Jetpack Compose) 우선을 권장합니다.**

이유: 이 앱의 핵심 가치가 전부 **오버레이 / 포그라운드 서비스 / WakeLock / 밝기 오버라이드** 같은 플랫폼 밀착 API에 있습니다. Flutter나 React Native를 쓰더라도 이 부분은 결국 네이티브 플러그인을 직접 작성하게 되므로, 크로스플랫폼의 이점이 거의 없습니다. 게다가 iOS 쪽은 기능이 완전히 달라져 코드 공유 대상 자체가 얇습니다.

- iOS를 나중에 붙일 경우: **별도 SwiftUI 앱**으로, "포그라운드 전용 블랙 클럭" 스코프로 축소.
- 설정 저장: DataStore (Preferences)

### 6.2 동작 모드 3단계

| 모드 | 구현 | 권한 | "다른 앱 유지" |
|---|---|---|---|
| **Simple** (기본) | 풀스크린 Activity + `screenBrightness=0` + `KEEP_SCREEN_ON` | 없음 | ✗ |
| **Overlay** | `TYPE_APPLICATION_OVERLAY` + Foreground Service | `SYSTEM_ALERT_WINDOW` | ✓ |
| **Pocket** | `PROXIMITY_SCREEN_OFF_WAKE_LOCK` | `WAKE_LOCK` | ✓ (진짜 OFF) |

**권한이 필요 없는 Simple 모드를 기본값으로 두고, 사용자가 필요할 때만 권한 허들을 넘게 하는 것**이 설치 이탈과 심사 리스크를 동시에 줄이는 구조입니다.

### 6.3 권한 목록 (Android)

```xml
<!-- Simple 모드: 없음 -->
<!-- Overlay 모드 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- Pocket 모드 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<!-- 선택 -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

---

## 7. 리스크 정리

| # | 리스크 | 심각도 | 대응 |
|---|---|---|---|
| R1 | **`specialUse` FGS가 Play 심사에서 반려** | 높음 | Simple 모드를 기본으로 두어 FGS 없이도 앱이 성립하게 설계. 사유 문구를 사전 준비 |
| R2 | **LCD 기기에서 "꺼진 것처럼" 보이지 않음** | 중간 | 기기 디스플레이 타입 감지 후 안내. AMOLED 최적화임을 스토어 설명에 명시 |
| R3 | **제조사별 백그라운드 강제 종료** | 중간 | 기기별 설정 가이드 화면 제공 |
| R4 | **오버레이가 시스템 UI/민감 화면에서 뚫림** | 중간 | 완벽한 차폐가 불가능함을 UX에서 인정하고 설명 |
| R5 | **iOS에서 요구사항의 핵심이 구현 불가** | 높음 | iOS는 별도 스코프로 분리. 또는 1차 범위에서 제외 |
| R6 | **AMOLED 번인** | 중간 | 픽셀 시프트 + 저휘도 |
| R7 | **주머니 속 오작동 터치** | 중간 | 복합 제스처 해제 + 근접센서 연동 |

---

## 8. 확인이 필요했던 사항 — 해소됨

| 질문 | 답 | 영향 |
|---|---|---|
| 타깃 플랫폼 | **Android 단독** | iOS 스코프 전면 제외. §4는 기록용으로만 남김 |
| "모두 다 돌아간다"의 의미 | **해석 A** (다른 앱이 계속 돌아가는 상태에서 화면만 덮기) | 오버레이가 필요. 단, 상태바 제약 발견 후 **하이브리드**로 조정 |
| 배포 경로 | **개인용 / 사이드로딩** | **R1(`specialUse` 심사) 소거.** 권한을 정책 눈치 없이 사용 가능 |
| 최소 API 레벨 | **26** | `TYPE_APPLICATION_OVERLAY` 요구 하한 |
| "문장"의 성격 | 1차는 단일 고정 문장 | 다중 순환·시간대별 문구는 3차로 이연 |

### 하이브리드로 조정된 이유

해석 A(오버레이)를 선택했지만, 이후 **상태바를 덮을 수 없다는 하드 제약**(§2 방식 2)이 확인되었습니다. 이로 인해 두 방식의 장단이 정확히 상충합니다.

- **Overlay**: 다른 앱이 계속 렌더링됨 ↔ 상단에 밝은 띠가 남음
- **Blackout**: 완전한 검정 ↔ 최상단 앱의 UI 렌더링이 멈춤

여기서 짚어야 할 건, **Blackout 모드에서도 음악·다운로드·내비게이션·타이머·동기화 등 백그라운드 동작은 전부 그대로 유지된다**는 점입니다. 멈추는 것은 최상단 앱의 화면 렌더링(게임 진행, 영상 재생)뿐입니다. 즉 대부분의 "백그라운드에서 다 돌아간다" 요구는 Blackout만으로도 충족됩니다.

따라서 **두 모드를 모두 구현하고 기본값을 Blackout으로 두는 하이브리드**를 채택했습니다.

---

## 9. 결론

**요구사항은 Android에서 실현 가능하며, 다만 "화면을 끈다"가 아니라 "화면을 검게 덮는다"로 정의를 바꿔야 정확합니다.** AMOLED 기기에서는 이 둘의 체감 차이가 거의 없습니다.

두 개의 하드 제약이 설계를 결정했습니다.

1. **임의 시점에 화면을 끄는 공개 API가 없습니다.** 그리고 진짜로 끄면 요구사항의 "문장/시간 표시"가 불가능해집니다 — 둘은 모순입니다.
2. **오버레이는 상태바를 덮을 수 없습니다.** 따라서 "다른 앱 계속 렌더링"과 "완전한 검정"은 동시에 얻을 수 없고, 둘 중 하나를 고르는 모드 전환으로 풀어야 합니다.

iOS는 다른 앱 위에 무언가를 덮거나 백그라운드에서 임의로 실행하는 것이 OS 수준에서 차단되어 있어 원래 요구사항을 그대로 옮길 수 없으므로, **이번 범위에서 제외합니다.**

구체적인 구현 설계·코드·함정 목록은 **[architecture.md](./architecture.md)** 에 정리했습니다.
