# checklist — media-volume-control

상태: **설계 완료. 실험·구현 미착수.**

## 0. 결정 대기
- [ ] `볼륨 0` 을 차폐 켤 때마다 적용할지 (제안: 켤 때마다)
- [ ] 블루투스 출력도 끌지 (제안: 구분하지 않음)

## 1. 실험 (구현보다 먼저)
- [ ] `ADJUST_MUTE` 가 권한 없이 동작하는가
- [ ] 시스템 음소거가 `force-stop` 으로 풀리는가 → (a)/(b) 방식 결정
- [ ] 블루투스 연결 시 어느 볼륨이 바뀌는가
- [ ] `flags = 0` 으로 볼륨 팝업이 안 뜨는가
- [ ] 차폐 중 볼륨 키 조작 시 동작
- [ ] `isVolumeFixed` 인 기기 대응이 필요한가

## 2. 설정
- [ ] `MediaVolumeAction` enum + `Settings` 필드 + DataStore 키
- [ ] `설정` 탭에 라디오 3택 + 문자열(en/ko)

## 3. 적용·복원
- [ ] `showCover()` 에 적용
- [ ] `hideCover()` · `onViewDetachedFromWindow` · `onDestroy()` 에 복원
      (**`onDestroy` 는 `hideCover()` 에 맡기지 말 것** — 조기 반환한다)
- [ ] 중복 적용 가드 (이미 적용 중이면 저장하지 않음)
- [ ] (b) 방식이면 저장값을 DataStore 에 두고, 앱 시작 시 남아 있으면 복원 후 삭제

## 4. 검증 (지시할 때)
- [ ] 세 선택지 각각의 동작
- [ ] 복원 경로 넷 전부 — 제스처 / 지문 / PIN 화면으로 창 걷힘 / 서비스 소멸
- [ ] 볼륨 팝업이 안 뜬다
- [ ] 재생 바는 정상 (소리만 없음)
- [ ] 강제 종료 후 앱 재실행 시 복원

## 5. 문서
- [ ] `architecture.md` §5 설정 스키마 + 적용/복원 지점, `glossary`, `CLAUDE.md` 설계 의도
- [ ] Overlay/Blackout 되살릴 때 같은 훅이 필요하다는 것을 §4.2 빚 목록에 추가
