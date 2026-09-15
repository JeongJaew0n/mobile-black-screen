# checklist — media-playback-bar

상태: **설계 검토 중.** 사용자 결정(spec 마지막 절 5개) 대기. 구현 미착수.

## 0. 결정 대기
- [ ] 알림 접근 권한 추가에 동의
- [ ] 1차 표시 전용(제어 없음)
- [ ] 일시정지 표시 여부
- [ ] 위치 (시계 아래 / 하단)
- [ ] 재그림 주기 (10초 / 30초)

## 1. 권한
- [ ] `MediaListenerService` (콜백 비움) + 매니페스트
- [ ] 권한 확인 + `ACTION_NOTIFICATION_LISTENER_SETTINGS` 이동 + `PermissionCard`
- [ ] 검증 레시피: `cmd notification allow_listener`

## 2. 관찰
- [ ] `MediaWatcher` — 세션 목록 변화 + 세션 콜백 → `StateFlow<NowPlaying?>`
- [ ] 세션 선택 규칙 (PLAYING 우선, 정지 숨김)
- [ ] 수명: 차폐 뜰 때 start / 내릴 때 stop (접근성 서비스 소유)

## 3. 표시
- [ ] `MediaBar` Composable — 제목·부제·막대·시간, 번인 궤도
- [ ] 10초 루프는 playing 일 때만
- [ ] 밝기 결정에 `nowPlaying` 합성 (`hasContent` 만으로는 부족)
- [ ] 미리보기 상자에도

## 4. 설정
- [ ] `showMedia` + 스위치 + 권한 카드 + 문자열(en/ko)

## 5. 검증 (지시할 때)
- [ ] 유튜브 / 음악 앱 각각
- [ ] 곡 전환 즉시 반영, 일시정지, 정지 후 사라짐
- [ ] 권한 거부 → 카드, 허용 후 복귀 → 카드 사라짐
- [ ] 차폐 없을 때 관찰 꺼짐
- [ ] 해제 제스처 무영향

## 6. 문서
- [ ] `architecture.md` §4·§5·함정, `glossary` (재생 바, NowPlaying), `CLAUDE.md` 권한·레시피
