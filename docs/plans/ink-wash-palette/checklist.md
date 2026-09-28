# 체크리스트

상태: **앱 전체 적용 완료 (2026-09-28).** 실기기 확인 대기.

- [x] 팔레트 스케일 설계 · 대비 검산
- [x] tokens.json 교체 · Tokens.kt 생성
- [x] 빌드 · lint
- [x] `ui/theme/Theme.kt` — `BlackScreenTheme`. 설정·Blackout·차폐 창·버블 창 네 루트에 적용
- [x] 생성기가 `res/values/tokens_colors.xml` 도 냄 → `themes.xml` 창 배경·시스템 바, 런처 아이콘, 알림 강조색
- [x] `colors.xml`(런처 배경 하나) 삭제 — 토큰 생성물로 대체
- [x] 런처 아이콘 틀·막대를 `#2E2E2E`/`#5A5A5A` → 가장 가까운 먹 톤 `wash800 #2A2A2A`/`wash600 #616161` 로. 눈에 띄는 변화는 아니다
- [ ] 실기기 확인 — 차폐 글자 paper 가 패널 0.30 에서 흰색처럼 읽히는가, 먹색 버블이 밝은 앱 위에서 너무 무겁지 않은가
