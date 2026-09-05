# checklist — full-screen-accessibility-overlay

**현재 상태: 완료.** 실기기 검증까지 마치고 main 에 병합됨 (`685e838`).

> 검증 기기: Galaxy S23+ (SM-S916N) / Android 16 / SDK 36 / 밀도 420

## 구현
- [x] `accessibility/ScreenOffAccessibilityService.kt` — 접근성 오버레이 창
- [x] `res/xml/accessibility_service_config.xml` — 화면 내용 읽지 않음
- [x] Manifest 서비스 등록 (`BIND_ACCESSIBILITY_SERVICE`)
- [x] `Mode.FULL` 추가, 기본값 지정, `BLACKOUT` 을 최후 수단으로 격하
- [x] `ScreenOff` 디스패처 — 설정 버튼 / 버블 / 타일 분기 통합
- [x] 타일에서 FULL 은 중계 Activity 없이 직접 호출
- [x] 설정 화면 FULL 모드 + 접근성 안내 카드

## 검증
- [x] 런처 위에서 버블 탭 → **최상단 Activity 가 런처 그대로**
- [x] 상태바 영역(y 0~94) 밝은 픽셀 **0개**
- [x] 내비바 영역(y 2214~) 밝은 픽셀 **0개**
- [x] 시계 렌더링, 3회 탭 해제, 패널 밝기 0.30 적용
- [x] `assembleDebug` + `lintDebug` 0 errors
- [x] 크래시 0건

## 함께 고친 것
- [x] 접근성 오버레이 첫 표시에 밝기 오버라이드가 걸리지 않던 문제
- [x] 버블 되살리기가 포그라운드 아닐 때 실행되어
      `ForegroundServiceStartNotAllowedException` 으로 죽던 문제

## 남은 것
- [ ] 접근성 서비스가 꺼져 있을 때 앱 안에서 배너로 안내
      (재설치마다 꺼지므로 실사용에서 걸릴 가능성이 있다)
