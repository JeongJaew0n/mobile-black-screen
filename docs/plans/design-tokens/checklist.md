# 체크리스트

상태: **완료 (2026-09-23).** 206 토큰. 시각 변화 0. 값 통합은 아래 후보로 남김.

## 준비

- [x] 시각 리터럴 전수 조사 (context.md 표)
- [x] 토큰 / 비토큰 경계 확정 (context.md "토큰이 아닌 것")
- [x] 용어집에 디자인 토큰 · ref/sys/comp · 캔버스 · 떠 있는 표면 등재

## 구현

- [x] `design/tokens/tokens.json` (DTCG, ref/sys/comp)
- [x] `design/tokens/build.py` (생성 · `--check`)
- [x] `ui/theme/Tokens.kt` 생성 — 옛 최상위 토큰 4개 제거
- [x] 교체: `BlackScreenContent` `UnlockGesture` `MediaBar` `Battery` `BubbleContent` `BubbleGlyph` `AimTargets` `SettingsScreen` `Settings.textColor`
- [x] 시각 변화 0 확인 — comp 토큰 93개를 끝까지 풀어 조사 때 리터럴과 대조, 전부 일치. 컴파일 경고 0(--rerun-tasks). **실기기 확인은 안 했다** — 지시가 있을 때. 값이 같으니 화면도 같아야 하지만, 교체 중 문맥을 잘못 짚은 자리가 있다면 그건 대조표로 안 잡힌다
- [x] `assembleDebug` · `lintDebug` 0 errors · 한글 리터럴 0
- [x] `design/tokens/README.md` — 형식 · 층 규칙 · 다른 제품에서 가져가는 법

## 문서

- [x] `architecture.md` §3 트리 · 새 절(디자인 토큰)
- [x] `CLAUDE.md` — "두 곳 이상 반복되는 값만" 규칙을 새 규칙으로 교체. 생성물 수정 금지 · `--check`
- [x] `docs/plans/README.md` 인덱스

## 이번에 하지 않은 것 (다음 단계 후보 — 눈으로 보고 정할 것)

값 통합은 디자인 결정이라 추출과 섞지 않았다. 아래는 같은 의도로 보이는데 값이 갈라진 것들.

| 후보 | 지금 | 어디 |
|---|---|---|
| 흐린 선 | 0.22 vs 0.25 | 밀어서 잠금 해제 트랙 테두리 vs 재생 진행 막대 바탕 |
| 빠른 반응 | 150ms vs 160ms | 겨냥 칩 확대 vs 밀어내기 마무리 |
| 작은 글자 | 13 / 14 / 15sp | 재생 부제 / 권한 안내 / 트랙 안내·칩 라벨 |
| 하단 여백 | 72dp | `mediaBar.bottomMargin` 과 `slideTrack.bottomMargin` 이 같은 값을 **따로** 갖고 있다 — 같은 자리라 sys 로 올릴 만하다 |
| 설정 화면 간격 6·10dp | `captionGap` `rowPaddingVertical` `swatchPadding` `fontChipPaddingVertical` | `sys.spacing` 눈금(2·4·8·12…)에 없어 comp 에 직접 걸었다. 눈금을 4 배수로 정리할지 |

그리고 **Material `colorScheme` 을 `sys.color` 로 채우는 것** — 설정 화면 디자인을 정할 때.
