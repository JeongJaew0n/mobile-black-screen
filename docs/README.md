# docs

| 문서 | 내용 |
|---|---|
| [technical-feasibility.md](./technical-feasibility.md) | 기술 타당성 분석 — 플랫폼별 가능/불가능 판정, 구현 방식 비교, 하드 제약, 리스크 |
| [architecture.md](./architecture.md) | 구현 설계 — 두 모드 구조, 핵심 코드, 설정 스키마, 구현 순서, 함정 목록 |

## 한 줄 요약

Android 전용. 화면을 "끄는" 게 아니라 "검게 덮는" 앱.
**Blackout**(풀스크린·권한 0개·완전한 검정)을 기본으로, **Overlay**(다른 앱 계속 렌더링·상태바는 남음)를 선택 모드로 제공.
