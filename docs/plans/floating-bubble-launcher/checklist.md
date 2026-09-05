# checklist — floating-bubble-launcher

> ## ⚠️ 이 문서는 완료된 과거 작업의 기록입니다
>
> 버블 자체는 계획대로 완성되었고 지금도 그대로 동작합니다. 다만 이후 변경으로
> 아래가 달라졌습니다. 현재 상태는 [architecture.md](../../architecture.md) 를 보세요.
>
> | 이 문서의 서술 | 현재 |
> |---|---|
> | 탭하면 `Mode.BLACKOUT` / `Mode.OVERLAY` 로 분기 | **`ScreenOff` 디스패처 경유, 기본은 `Mode.FULL`** |
> | 버블 크기 52dp 고정 | **5단계 조절(40~70dp)**, 크기 변경 시 재배치 |
> | "차폐 시작" 버튼 | **"Screen Off 시작"** |


> 작업 진행하면서 AI 가 순차적으로 체크. `[x]` 로 표시한 항목은 완료된 것으로 간주.
> 새 항목이 발견되면 적절한 단계에 추가하고 체크리스트를 유지한다.

**현재 상태: 26 / 26 완료 — 실기기 검증까지 끝. 남은 것은 문서 반영뿐**

> 검증 기기: Galaxy S23+ (SM-S916N) / Android 16 / SDK 36 / 밀도 420
> `assembleDebug` + `lintDebug` 0 errors, 크래시 0건

브랜치: `feat/floating-bubble`

## 0. 준비
- [x] 작업 브랜치 생성 → `feat/floating-bubble`
- [x] `docs/plans/blackout-overlay-modes/` 와 `CLAUDE.md` 를 읽고 기존 제약 확인

## 1. 서비스 리팩터링 (버블 없이, 기존 동작 유지)
> 이 단계가 끝난 시점에 **기존 기능이 그대로 동작해야 한다.** 여기서 회귀를 내면
> 이후 단계의 문제와 뒤섞여 원인을 못 찾는다.
- [x] `OverlayService` → `ScreenCoverService` 개명, 상태 필드 분리
      (`bubbleView` / `coverView`, 둘 다 null 일 때만 `stopSelf()`)
- [x] 액션 정의 — `SHOW_BUBBLE` / `HIDE_BUBBLE` / `START_COVER` / `STOP_COVER` / `STOP_ALL`
- [x] 알림 문구를 상태에 따라 전환 ("버블 실행 중" ↔ "차폐 중"), 알림은 항상 1개
- [x] **회귀 확인**: Overlay 모드 차폐 시작/해제, 알림 액션, 타일 — 전부 기존대로

## 2. 버블 창
- [x] `Settings` 에 `bubbleEnabled` / `bubbleEdge` / `bubbleYRatio` 추가
- [x] `bubble/BubbleWindow.kt` — 창 파라미터 (`WRAP_CONTENT`, `TOP|START` gravity)
- [x] `bubble/BubbleContent.kt` — 원형 Composable
- [x] 서비스에서 버블 창 add/remove
- [x] 탭 → 현재 모드대로 차폐 시작
      · ⚠️ Blackout 모드는 Activity 라 `OverlayLauncherActivity` 와 같은 문제가 없다.
        버블은 이미 보이는 오버레이 창이므로 FGS 제약도 해당 없음
- [x] 버블 바깥 터치가 아래 앱으로 통과하는지 확인

## 3. 드래그 · 스냅 · 위치 저장
- [x] 드래그로 `params.x/y` 갱신 (`windowManager.updateViewLayout`)
- [x] 탭/드래그 구분 — 이동 거리가 터치 슬롭 미만이면 탭
- [x] 놓으면 가까운 좌/우 가장자리로 스냅 (애니메이션)
- [x] `bubbleEdge` + `bubbleYRatio` 로 저장, 재시작 시 복원
- [x] 회전 시 화면 밖으로 나가지 않는지 확인

## 4. ✕ 타겟으로 제거
- [x] 드래그 시작 시 하단 중앙에 ✕ 타겟 창 표시
- [x] 버블 중심이 타겟 반경 안이면 시각 피드백 (확대/강조)
- [x] 거기서 놓으면 버블 제거 + `bubbleEnabled = false` 저장
- [x] 드래그 종료 시 타겟 창 제거 (누수 방지)

## 5. 유휴 페이드
- [x] 일정 시간 무입력 시 알파를 낮춤, 터치하면 복귀
- [x] ⚠️ 페이드는 `animateFloatAsState` 로 끝내고 상시 프레임 루프를 돌리지 말 것
      (차폐 화면의 번인 시프트를 애니메이션 없이 처리한 것과 같은 이유 — 전력)

## 6. 차폐 중 버블 숨김
- [x] Overlay 모드: 차폐 창을 add 할 때 서비스가 스스로 버블을 숨김
- [x] Blackout 모드: `BlackoutActivity.onStart` → `HIDE_BUBBLE`,
      `onDestroy` → `SHOW_BUBBLE`
- [x] ⚠️ **`bubbleEnabled` 가 false 면 신호를 보내지 말 것.**
      `startService` 는 죽어 있는 서비스를 새로 띄워 알림만 깜빡이게 만든다
- [x] 차폐 해제 후 버블이 원래 위치로 복귀하는지 확인

## 7. 설정 화면
- [x] "버블" 섹션 + 켜기 스위치
- [x] 버블을 켜면 `SYSTEM_ALERT_WINDOW` 권한이 필요함을 안내
      (Blackout 모드는 지금까지 권한 0개였다 — 이 변화를 명시할 것)
- [x] 권한 미허용 시 기존 오버레이 권한 플로우 재사용

## 8. 검증
- [x] `./gradlew assembleDebug` + `lintDebug` 0 errors
- [x] spec.md 의 DoD 16개 실기기 확인
      · **Blackout 차폐 중 전 픽셀 0 유지** (버블이 안 보이는지 픽셀로 확증)
      · 앱을 최근앱에서 제거해도 버블 유지
      · 알림 1개만 유지
- [x] CLAUDE.md 의 기존 검증 레시피 전 항목 재통과 (회귀 없음)
- [x] 창 누수 — 버블·차폐·✕ 타겟 모두 제거 후 `dumpsys window` 0

## 9. 마무리
- [ ] 커밋 (Conventional Commits)
- [ ] `CLAUDE.md` 갱신 — 서비스 구조 변경, 버블 관련 함정 추가
- [ ] `docs/architecture.md` 에 버블 구조 반영
- [ ] 미체크 항목이 남았으면 사유 메모


---

## 구현 중 실제로 물린 것 (다음 세션을 위한 기록)

| # | 증상 | 원인 | 대응 |
|---|---|---|---|
| B1 | 버블 창이 **정확히 겹쳐 2개** 생성 | `addBubble()` 이 저장 위치를 읽으려 코루틴 안에서 창을 붙이는데, `bubbleView` 가 그 안에서야 세팅돼 동시 호출 시 경합 | 동기적으로 세우는 `bubbleAdding` 플래그 추가 |
| B2 | 버블이 의도보다 **94px 아래** 배치 | 오버레이 창 좌표는 화면 전체가 아니라 **부모 프레임(`[0,94][1080,2214]`) 기준**. 전체 높이로 비율 계산해서 상태바 높이만큼 밀림 | `usableSize()` — `currentWindowMetrics` 에서 시스템 바 인셋을 뺀 크기 사용 |
| B3 | 드래그 종료 시 **크래시** `IllegalStateException: A MonotonicFrameClock is not available` | Compose 의 `animate()` 는 컨텍스트에 프레임 클럭을 요구한다. 서비스의 `lifecycleScope` 에는 없다 | 프레임워크 의존 없는 단순 루프(`slideBubbleX`)로 교체 |
| B4 | `RemoveTarget` 위치가 어긋남 | `LayoutParams.y` 에 dp 숫자를 그대로 넣음 (픽셀이어야 함) | `* density` 적용 |

## 검증 중 겪은 함정 (앱 버그 아님)

- **adb 테스트 탭이 설정을 조용히 바꾼다.** 화면이 잠긴 줄 모르고 보낸 탭들이 해제 제스처를
  `3회 연속 탭`으로 바꿔 놓아, 롱프레스 해제가 "회귀한 것처럼" 보였다.
  main 브랜치 APK로 대조해서야 코드 문제가 아님을 확인했다.
  **제스처를 검증하기 전에 `unlock_gesture` 저장값부터 확인할 것.**
- **DataStore 의 boolean 은 `strings` 로 true/false 를 구분할 수 없다.** 키 이름만 보인다.
  `bubble_enabled` 값을 그렇게 판정하려다 잘못된 결론을 냈다. 동작으로 검증할 것.
- **설정 화면 스크롤 위치가 매번 달라** 고정 좌표 탭이 빗나간다. 스크롤 후 반드시
  스크린샷으로 좌표를 다시 확인할 것.
- **페이드 같은 미세한 시각 변화는 평균 밝기로 측정하면 놓친다.** 버블 뒤 배경이
  버블만큼 어두우면 차이가 안 난다. 흰 막대 같은 고대비 지점의 픽셀값을 봐야 한다.
  (185 → 86 으로 확인)
