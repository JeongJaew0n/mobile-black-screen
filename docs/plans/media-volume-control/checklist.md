# checklist — media-volume-control

상태: **설계 완료. 실험·구현 미착수.**

## 0. 결정 (2026-09-21 확정)
- [x] `볼륨 0` 은 **차폐를 켤 때마다** 적용
- [x] **블루투스 출력 중에는 적용하지 않음** — 안전한 실패 방향은 "끄지 않는 것"
- [x] 설정 문구에 **"미디어 소리만"** 명시

## 1. 실험 (구현보다 먼저)
- [ ] `ADJUST_MUTE` 가 권한 없이 동작하는가
- [ ] 시스템 음소거가 `force-stop` 으로 풀리는가 → (a)/(b) 방식 결정
- [ ] `getAudioDevicesForAttributes(USAGE_MEDIA)` 로 블루투스 라우팅을 판단할 수 있는가
- [ ] `flags = 0` 으로 볼륨 팝업이 안 뜨는가
- [ ] 차폐 중 볼륨 키 조작 시 동작
- [ ] `isVolumeFixed` 인 기기 대응이 필요한가

## 2. 설정
- [ ] `MediaVolumeAction` enum + `Settings` 필드 + DataStore 키
- [ ] `설정` 탭에 라디오 3택 + 문자열(en/ko)
- [ ] "미디어 소리만 / 블루투스 제외" 안내 문구

## 3. 적용·복원
- [ ] `showCover()` 에 적용 — **블루투스 출력이면 건너뛴다**
- [ ] `hideCover()` · `onViewDetachedFromWindow` · `onDestroy()` 에 복원
      (**`onDestroy` 는 `hideCover()` 에 맡기지 말 것** — 조기 반환한다)
- [ ] 중복 적용 가드 (이미 적용 중이면 저장하지 않음)
- [ ] (b) 방식이면 저장값을 DataStore 에 두고, 앱 시작 시 남아 있으면 복원 후 삭제

## 4. 검증 (지시할 때)
- [ ] 세 선택지 각각의 동작
- [ ] 블루투스 연결 중 차폐 → 소리가 그대로 나온다
- [ ] 복원 경로 넷 전부 — 제스처 / 지문 / PIN 화면으로 창 걷힘 / 서비스 소멸
- [ ] 볼륨 팝업이 안 뜬다
- [ ] 재생 바는 정상 (소리만 없음)
- [ ] 강제 종료 후 앱 재실행 시 복원

## 5. 문서
- [ ] `architecture.md` §5 설정 스키마 + 적용/복원 지점, `glossary`, `CLAUDE.md` 설계 의도
- [ ] Overlay/Blackout 되살릴 때 같은 훅이 필요하다는 것을 §4.2 빚 목록에 추가
