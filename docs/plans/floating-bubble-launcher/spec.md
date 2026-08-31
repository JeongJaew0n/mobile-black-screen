# spec — floating-bubble-launcher

## 목표

다른 앱을 쓰는 도중에도 화면 차폐를 한 번의 탭으로 켤 수 있는, **앱을 닫아도 계속 떠 있는
작은 원형 버튼(버블)** 을 만든다. 완료되면 사용자는 앱을 열거나 알림창을 내리지 않고
화면 어디서든 버블을 눌러 차폐를 시작할 수 있다.

## 배경 — 왜 지금 구조에 그냥 못 얹는가

두 가지가 걸린다.

1. **오버레이는 Activity 보다 위에 그려진다.** 따라서 Blackout 모드로 화면을 덮어도
   검은 화면 한가운데에 버블이 그대로 떠 있게 되어 "완전한 검정"이 깨진다.
   그런데 오버레이 창은 지금 어떤 Activity 가 앞에 있는지 알 방법이 없다
   (접근성 서비스 없이는). 따라서 **`BlackoutActivity` 가 서비스에 직접 알려야 한다.**

2. **`OverlayService` 의 수명이 차폐에 묶여 있다.** 현재 `onUnlock = { stopSelf() }` 이므로
   버블을 같은 서비스에 넣으면 차폐를 풀 때 버블까지 사라진다.

## 결정된 방향

**`OverlayService` 를 상태를 가진 단일 서비스로 리팩터링한다.**

서비스를 따로 두면 기존 검증 코드를 덜 건드리지만 상시 알림이 두 개가 된다.
"아무것도 없는 화면"을 지향하는 앱에서 알림 두 개는 어긋나므로 통합을 택한다.

```
ScreenCoverService (기존 OverlayService 를 개명·확장)
├─ bubbleView : View?   버블 오버레이 창
├─ coverView  : View?   전체 차폐 오버레이 창 (Overlay 모드에서만)
└─ 둘 다 null 이 되는 순간에만 stopSelf()
```

액션: `SHOW_BUBBLE` / `HIDE_BUBBLE` / `START_COVER` / `STOP_COVER` / `STOP_ALL`
알림 문구는 상태에 따라 바뀐다 — "버블 실행 중" ↔ "차폐 중".

## 범위

- **포함**
  - 원형 버블 오버레이 창 (앱을 닫아도 유지)
  - 탭 → 현재 모드(`Mode.BLACKOUT` / `Mode.OVERLAY`)대로 차폐 시작
  - 드래그 이동 + 가까운 좌우 가장자리로 스냅 + 위치 영속화
  - 드래그 중 하단에 ✕ 타겟 표시, 거기 놓으면 버블 제거
  - 유휴 시 반투명으로 흐려짐 (터치하면 복귀)
  - 차폐 중 버블 자동 숨김 (Blackout·Overlay 양쪽)
  - 설정 화면에 버블 켜기 스위치
  - `ScreenCoverService` 로의 서비스 통합 리팩터링

- **제외**
  - **부팅 후 자동 복원.** `specialUse` 는 `BOOT_COMPLETED` 에서 시작이 허용되지만
    (Android 15+ 가 막은 것은 dataSync·mediaPlayback·mediaProjection·phoneCall)
    사용자가 수동 재실행을 택했다. `RECEIVE_BOOT_COMPLETED` 를 추가하지 않는다.
  - 버블 롱프레스 메뉴 (드래그와 제스처가 충돌한다)
  - 버블 크기·색상 커스터마이징
  - 버블에서 차폐 외 다른 기능 실행

## 완료 조건 (Definition of Done)

- [ ] 설정에서 버블을 켜면 화면 위에 원형 버튼이 나타난다
- [ ] 앱을 완전히 종료(`force-stop` 아님, 최근앱에서 제거)해도 버블이 유지된다
- [ ] 다른 앱 위에서도 버블이 보인다
- [ ] 버블 바깥 영역의 터치는 아래 앱으로 정상 통과한다 (버블이 화면을 막지 않는다)
- [ ] 버블을 탭하면 현재 모드대로 차폐가 시작된다
- [ ] **Blackout 모드로 차폐 시 버블이 보이지 않는다** (전 픽셀 0 유지)
- [ ] **Overlay 모드로 차폐 시에도 버블이 보이지 않는다**
- [ ] 차폐를 해제하면 버블이 원래 위치에 다시 나타난다
- [ ] 버블을 드래그해 옮길 수 있고, 놓으면 가까운 좌/우 가장자리로 붙는다
- [ ] 위치가 앱 재시작 후에도 유지된다
- [ ] 드래그를 시작하면 하단에 ✕ 타겟이 나타나고, 거기 놓으면 버블이 사라진다
- [ ] ✕ 로 제거해도 다른 기능(타일, 앱 내 차폐 시작)은 정상 동작한다
- [ ] 일정 시간 손대지 않으면 버블이 흐려지고, 터치하면 다시 진해진다
- [ ] 상시 알림은 **하나만** 뜬다 (버블+차폐 동시에도)
- [ ] 버블 해제 후 창·서비스가 모두 정리된다 (누수 없음)
- [ ] 기존 회귀 없음 — CLAUDE.md 의 검증 레시피 전 항목 재통과

## 인터페이스 / 데이터

### Settings 추가
```kotlin
val bubbleEnabled: Boolean = false,
val bubbleEdge: Edge = Edge.RIGHT,   // LEFT | RIGHT
val bubbleYRatio: Float = 0.5f,      // 화면 높이 대비 0..1
```
좌표를 픽셀로 저장하면 회전·해상도 변경 때 화면 밖으로 나간다.
**가장자리 + 세로 비율**로 저장한다.

### 버블 창 파라미터
```kotlin
WindowManager.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    TYPE_APPLICATION_OVERLAY,
    FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT,
).apply { gravity = Gravity.TOP or Gravity.START }
```
창 크기가 버블 크기와 같으므로 **그 바깥 터치는 자동으로 아래 앱에 간다.**
전체 차폐 창과 달리 여기서는 `FLAG_NOT_TOUCHABLE` 을 쓰지 않는 이유가 다르다 —
차폐 창은 "터치를 삼켜야 해서", 버블은 "자기 영역만 받으면 되어서"다.

## 의존성

새 권한이 없다. `SYSTEM_ALERT_WINDOW` 는 Overlay 모드에서 이미 쓰고 있다.
다만 **지금까지 Blackout 모드는 권한 0개였는데, 버블을 켜는 순간 권한이 필요해진다.**
설정 화면에서 이 점을 알려야 한다.

## 비고 — 주의할 점

- **`BlackoutActivity` 에서 서비스로 숨김 신호를 보낼 때, 버블이 꺼져 있으면 보내면 안 된다.**
  `startService` 는 죽어 있는 서비스를 새로 띄우므로 알림만 잠깐 떴다 사라진다.
  `bubbleEnabled` 를 확인한 뒤에만 보낸다.
- 민감 화면(비밀번호 입력 등)과 `hideOverlayWindows()` 를 호출하는 앱 위에서는
  버블이 자동으로 숨겨진다. 회피 불가이며 정상 동작이다.
- 탭과 드래그를 구분해야 한다. 이동 거리가 터치 슬롭 미만이면 탭으로 처리한다.
- 포그라운드 서비스인 이상 **상시 알림은 없앨 수 없다.** 중요도를 최저로 두는 것이 한계다.
- 이 기능은 퀵 설정 타일과 목적이 겹친다. 차이는 알림창을 내리지 않아도 된다는 점뿐이고,
  대가로 상시 오버레이 창과 알림을 계속 유지한다.
