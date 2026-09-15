# checklist — media-playback-bar

상태: **구현 완료(빌드·lint 통과). 실기기 검증 대기.**

## 0. 결정 대기
- [x] 알림 접근 권한 추가 — 받는다. 권한 상실 감지 + 안내 문구 포함
- [x] 1차 표시, 2차 제어 (레이아웃은 제어 자리 전제)
- [x] 일시정지 표시 — 한다
- [x] 위치 — 하단 고정
- [x] 재그림 주기 — 10초

## 1. 권한
- [x] `MediaListenerService` (콜백 비움) + 매니페스트
- [x] 권한 확인 + `ACTION_NOTIFICATION_LISTENER_SETTINGS` 이동 + `PermissionCard`
- [x] 권한 상실 감지 3지점(설정 RESUME / 차폐 표시 시 / SecurityException) + 차폐 하단 안내 문구
- [x] 검증 레시피: `cmd notification allow_listener`

## 2. 관찰
- [x] `MediaWatcher` — 세션 목록 변화 + 세션 콜백 → `StateFlow<NowPlaying?>`
- [x] 세션 선택 규칙 (PLAYING 우선, 정지 숨김)
- [x] 수명: 차폐 뜰 때 start / 내릴 때 stop (접근성 서비스 소유)

## 3. 표시
- [x] `MediaBar` Composable — 하단 고정, 제목·부제·막대·시간, 자체 번인 궤도, 2차 제어 자리
- [x] 10초 루프는 playing 일 때만
- [x] 밝기 결정에 `nowPlaying` 합성 (`hasContent` 만으로는 부족)
- [x] 미리보기 상자에도

## 4. 설정
- [x] `showMedia` + 스위치 + 권한 카드 + 문자열(en/ko)

## 5. 검증 (지시할 때)
- [ ] 유튜브 / 음악 앱 각각
- [ ] 곡 전환 즉시 반영, 일시정지, 정지 후 사라짐
- [ ] 권한 거부 → 카드, 허용 후 복귀 → 카드 사라짐
- [ ] 차폐 없을 때 관찰 꺼짐
- [ ] 해제 제스처 무영향

## 6. 문서
- [x] `architecture.md` §4·§5·함정, `glossary` (재생 바, NowPlaying), `CLAUDE.md` 권한·레시피
