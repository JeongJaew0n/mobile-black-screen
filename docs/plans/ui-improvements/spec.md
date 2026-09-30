# UI 개선 — 무엇을 만드는가

`docs/ui-research.md` 1~7.

1. 화면 탭 맨 위 미리보기(늘 보임, 무표시면 안내 문구). 스크롤하면 55% 까지 축소
2. SwitchRow 행 전체 토글, 라디오 행 최소 48dp, `sys.touchTarget.min` 48dp
3. 자동 끄기·시계 형식 → FilterChip 한 줄(FlowRow)
4. 섹션을 `surfaceContainer` 카드로, 구분선 제거
5. 시작 버튼 + 안내 줄을 화면 아래에 고정
6. 탭 이름 `화면 / 동작` (en Display / Behavior, ja 表示 / 動作)
7. 글자 크기 200% 실기기 확인

## 완료 조건
- [ ] 빌드 · lint 0 errors · 토큰 검사 · 문자열 세 언어 일치 · 한글 리터럴 0
- [ ] 실기기: 1~6 스크린샷 확인, 7 은 `font_scale 2.0` 에서 설정·차폐 확인 후 원복
- [ ] 끝나면 화면 끄기(사용자 요청)
