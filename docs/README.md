# docs

| 문서 | 내용 |
|---|---|
| [technical-feasibility.md](./technical-feasibility.md) | 기술 타당성 분석 — 플랫폼별 가능/불가능 판정, 구현 방식 비교, 하드 제약, 리스크 |
| [architecture.md](./architecture.md) | 구현 설계 — 세 모드 구조, 핵심 코드, 설정 스키마, 함정 목록 |
| [power-measurement.md](./power-measurement.md) | 전력 측정 — 방법, 실패한 시도들, **실측치**(2026-09-08) |
| [sharing-readiness.md](./sharing-readiness.md) | 개인용 전제가 깨지는 자리 — 남에게 공유했을 때 기능별로 생기는 문제와 공유 전 순서. **의견 문서** (2026-09-23) |
| [plans/](./plans/) | 작업별 계획·판단 기록. 뒤 작업이 앞 결정을 뒤집은 경우가 있다 |
| [glossary/](./glossary/) | 이 프로젝트에서만 통하는 용어 — Screen Off/차폐, FULL, 버블, 겨냥… |
| [troubleshootings/](./troubleshootings/) | 시간을 잡아먹은 오류의 증상·원인·해결. `reusable/` 은 다음 Android 프로젝트에서도 쓸 것 |

## 한 줄 요약

Android 전용. 화면을 "끄는" 게 아니라 "검게 덮는" 앱.
**FULL**(접근성 오버레이 — 쓰던 앱은 그대로, 상태바까지 검게)이 기본이자 유일하게 제공되는 모드다.
Overlay·Blackout 은 코드만 남기고 미제공. 현재 구조는 [architecture.md](./architecture.md).
