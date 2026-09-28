# 체크리스트

상태: **구현 완료 (2026-09-28).** 테스트는 사용자 지시 후.

## 구현
- [x] `values-ja/strings.xml` — 80개
- [x] `overlay_running_desc` 수정 (en · ko · ja)
- [x] 키·서식 인자 대조 — ko·ja 모두 빠짐 0 · 남음 0 · 서식 불일치 0
- [x] 빌드 · lint 0 errors · locale config 에 `ja` 포함

## 검증 (지시할 때)
- [ ] `adb shell cmd locale set-app-locales com.jjw.blackscreen --user current --locales ja-JP` — 설정 화면·차폐·알림
- [ ] 긴 설명문이 잘리지 않는지 (버블 설명·소리 안내)
- [ ] **삼성 일본어 OS 표기 확인** — 기기 언어를 일본어로 바꿔 설정 앱의 실제 항목 이름과 대조. 기기 전체 언어라 지시 없이 하지 않는다
- [ ] 원복 `--locales ""`
