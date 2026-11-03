# 모락 마스코트 적용 — spec

## 범위

| # | 자리 | 표정 | 구현 |
|---|---|---|---|
| ① | 설정 화면 제목 옆 | 기본 | `R.drawable.morak_base`, `Comp.Mascot.titleSize`. 화면 맨 위 권한 카드가 떠 있으면 숨긴다(한 화면에 모락 하나) |
| ② | 권한 안내 카드(전부) | 식음 | `morak_sad`, 카드 왼쪽, `Comp.Mascot.cardSize`. 문구는 그대로 |
| ③ | 맨 위 권한이 막 켜졌을 때 | 따끈 | `morak_happy` + "준비됐어요" 2초. 문자열 `permission_ready` en·ko·ja |
| ④ | 버블 기호 | 얼굴 없음 | `BubbleIcon.MUG` 를 **맨 뒤에** 추가(저장값이 이름이라 순서 무관하지만 기본값 BAR 유지). `morak_mug` |
| ⑤ | 퀵 설정 타일 · 알림 작은 아이콘 | — | `ic_tile` 을 S자 김 한 가닥으로 |
| ⑥ | 런처 아이콘 | 기본 | 적응형 아이콘 전경 = 수묵 모락, 단색(테마 아이콘) 층 = 선만 |

넣지 않는다: 차폐 화면, 해제 안내, 지문 프롬프트, 미리보기.

## 생성기 `design/mascot/build.py`

- 표준 라이브러리만. 입력은 스크립트 안의 도형(원본 `cast.js` 의 경로와 같은 좌표, 200×200).
- 출력(전부 생성물, 손으로 고치지 않는다): `res/drawable/morak_base.xml`, `morak_happy.xml`, `morak_sad.xml`,
  `morak_mug.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_tile.xml`.
- 흔들림: 곡선을 점으로 펴고 2옥타브 값 노이즈로 ±2.5 밀기. 표정별 시드 고정.
- `--check`: 생성물이 스크립트와 어긋나면 실패.

## 토큰

`comp.mascot` — 색(선·몸통·빗금·하트·띠·테·커피·식은 커피·볼·그늘·눈물·김) 은 전부 기존 ref 별칭,
크기 `titleSize`(d32)·`cardSize`(d48)·`toastSize`(d64), 토스트 `duration`(ms2000 신설)·`surface`·`radius`.
`comp.bubble.mugFraction` — 머그는 몸통이 넓어 다른 기호(0.5)보다 크게 그려야 같은 무게로 보인다.

## 완료 조건

- `assembleDebug`, `lintDebug` 0 errors, `build.py --check` 둘 다 통과.
- 접근성: 그림은 전부 장식(`contentDescription = null`). ③ 문구는 `paneTitle` 로 읽힌다. (처음엔 liveRegion 이었다 — 리뷰에서 Compose 가 새로 나타난 노드의 liveRegion 에는 이벤트를 안 보낸다는 것을 소스로 확인해 바꿨다.)
- 실기기 확인은 지시가 있을 때.
