# 체크리스트

상태: **완료 (2026-10-30).** 실기기 확인까지 끝. 타일만 미확인.

- [x] 토큰 `comp.mascot`·`comp.bubble.mugFraction`·`ref.duration.ms2000` → 생성. 런처 틀·막대 색(`frame`·`mark`)은 쓰는 곳이 없어져 지움
- [x] `design/mascot/build.py` — 표정 셋·머그·런처 전경/단색·타일. 오프라인 렌더(SVG 변환)로 시안과 같은지 확인
- [x] ① 제목 옆 ② 권한 카드 ③ 준비됐어요(en·ko·ja)
  - ③ 은 **권한 없이 앱을 떠났다가(ON_STOP) 돌아와서 풀렸을 때만.** 앱을 막 열 때 접근성 서비스가 늦게 붙어
    `connected` 가 false → true 로 바뀌는 것을 거르기 위해서다
- [x] ④ `BubbleIcon.MUG` + 이름표 세 파일. 견본이 여섯이 되자 고정 크기로는 411dp 폭에서도 넘쳐
  (64dp × 6 > 339dp) 칸을 균등 분할하고 원은 견본 크기까지만 크게 했다
- [x] ⑤ 타일·알림 ⑥ 런처 (`mipmap-anydpi` 단색 층을 `ic_launcher_monochrome` 으로)
- [x] 빌드 · lint(0 errors, 경고는 기존 것뿐) · 두 생성기 `--check` · Kotlin 한글 리터럴 0
- [x] 문서 — design/mascot/README, glossary(모락), architecture §3·§4.11·§5·§7, plans README, CLAUDE.md
- [x] 실기기 확인 (S23+ / Android 16, 무선 adb)
  - ① 제목 옆 모락 · ② 권한 카드의 식은 모락(이때 제목 모락 숨음) · ③ 권한 켜고 돌아오면 '준비됐어요' 약 2초
  - ③ 반대 경우 — 권한 변화 없이 나갔다 돌아오면 안 뜬다(같은 지점 픽셀 #000000)
  - ④ 견본 여섯이 한 줄에 들어감, 머그를 고르면 실제 버블에 뜸(유휴 페이드 상태로). 원래 '달' 로 되돌림
  - ⑤ 상태바 알림 아이콘이 김 한 가닥 · ⑥ 알림 카드에 런처 아이콘(수묵 모락)
  - 미확인: 퀵 설정 타일(사용자 패널에 없음 — 배치를 바꾸지 않았다), 테마 아이콘(단색 층)
  - 함정: `accessibility_enabled 0` 만으로는 삼성에서 서비스가 안 떨어졌다. `enabled_accessibility_services` 를 지워야
    카드가 떴다. 재설치 직후 꺼졌던 접근성은 시스템이 몇 초 뒤 스스로 다시 켰다
