# 체크리스트

상태: **구현 완료 (2026-10-30). 실기기 확인 대기.**

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
- [ ] 실기기 확인 (지시 시) — 제목·카드·토스트(접근성 껐다 켜기 필요)·버블 머그·타일·알림·런처·테마 아이콘
