# 마스코트 모락

김 나는 머그잔. 결점이 "금방 식는다" 라서 화면을 식히는(끄는) 이 앱의 얼굴이다.
원본은 밝은 종이 위 손그림 SVG(선 `#2b2326`, 민트 몸통)인데, 어두운 앱에서는 외곽선이 묻혀
**수묵 모락** — 선을 paper 로, 면을 먹으로 뒤집은 것 — 을 쓴다. 결정 기록은 `docs/plans/morak-mascot/`.

```bash
python3 design/mascot/build.py            # 생성
python3 design/mascot/build.py --check    # 생성물이 어긋났으면 실패 (커밋 전에)
```

## 무엇을 만드나

| 파일 (`app/src/main/res/drawable/`) | 쓰는 곳 |
|---|---|
| `morak_base.xml` | 설정 화면 제목 옆 |
| `morak_sad.xml` | 권한 안내 카드 — 아직 켤 수 없음 = 식음 |
| `morak_happy.xml` | 권한을 켜고 돌아왔을 때 2초 '준비됐어요' |
| `morak_mug.xml` | 버블 기호 '머그' — 얼굴 없음(46dp 안에서 뭉개진다) |
| `ic_launcher_foreground.xml` | 런처 아이콘 전경 |
| `ic_launcher_monochrome.xml` | 테마 아이콘 층 — 선만, 시스템이 칠한다 |
| `ic_tile.xml` | 퀵 설정 타일 · 알림 작은 아이콘 — S자 김 한 가닥 |

전부 생성물이다. **손으로 고치지 말 것.** 도형은 `build.py`, 색은 `design/tokens/tokens.json` 의 `comp.mascot`.

## 왜 생성하나

원본 SVG 의 손그림 느낌은 두 가지 SVG 기능에서 나오는데 **VectorDrawable 에는 둘 다 없다.**

| 원본 | VectorDrawable 에서 |
|---|---|
| `feTurbulence` + `feDisplacementMap` — 선이 흔들린다 | 곡선을 점으로 펴고 2옥타브 값 노이즈로 ±2.5 밀어 **경로에 굽는다.** 표정마다 시드 고정 |
| `pattern` 빗금 채우기 | 모양을 `<clip-path>` 로 걸고 그 안에 35° 띠를 깐다 |
| 색면을 (-5, 4) 어긋나게 찍는 리소 효과 | 좌표에 그대로 더한다 |

세 표정은 같은 틀(합친 경계)을 써서 표정을 바꿔도 잔이 제자리에 있다.

## 넣지 않는 곳

차폐 화면 · 미리보기 · 해제 안내 · 지문 프롬프트. 기본이 완전한 검정이고, 무엇이든 그리는 순간 패널 밝기가
0 에서 올라간다. 미리보기는 실제 차폐와 같은 그림이어야 한다.
