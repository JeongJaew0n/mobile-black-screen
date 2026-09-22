# 디자인 토큰

`tokens.json` 이 이 앱의 모든 시각 값 — 색·불투명도·치수·글자 크기·시간 — 의 **유일한 원본**이다.
형식은 [W3C Design Tokens(DTCG)](https://tr.designtokens.org/format/). Kotlin 쪽
`app/src/main/java/com/jjw/blackscreen/ui/theme/Tokens.kt` 는 `build.py` 가 여기서 만든다.

```bash
python3 design/tokens/build.py            # tokens.json → Tokens.kt
python3 design/tokens/build.py --check    # 생성물이 원본과 어긋났으면 실패 (커밋 전에)
```

## 세 층

```
ref   원시값      palette.ink900  dimension.d8  opacity.o70  duration.ms400     이름에 의도가 없다
 ↑
sys   의도        color.canvas  color.floatingSurface  opacity.secondary       다른 제품에 가져가는 층
 ↑                typography.display  spacing.md  radius.pill  motion.fade
comp  부품        bubble.surface  mediaBar.lift  unlockRing.size                 이 앱 것
```

규칙 — **sys 는 ref 만 가리키고, comp 는 sys·ref 만 가리킨다.** 역방향은 없다. `build.py` 가 검사한다.
ref 밖에서 리터럴을 쓸 수 있는 것은 `number`(비율·배수) 뿐이다 — 비율에는 팔레트가 없다.

"디자인을 바꾼다" 는 두 가지다.

- **팔레트 교체** — `ref` 의 값을 바꾼다. `ink900` 을 `#1A1A22` 로 하면 떠 있는 표면이 전부 바뀐다.
- **의도 재배치** — `sys` 의 alias 를 바꾼다. `opacity.secondary` 를 `o55` 로 옮기면 보조 텍스트가 전부 한 단계 흐려진다.

`comp` 는 거의 건드릴 일이 없다. 특정 부품 하나만 예외로 만들 때만.

## 형식

| `$type` | `$value` | Kotlin |
|---|---|---|
| `color` | `"#RRGGBB"` 또는 `"#RRGGBBAA"` | `Color(0xAARRGGBB)` |
| `dimension` | `{"value": 8, "unit": "px"}` | `8.dp` — 경로에 `fontSize`·`letterSpacing` 이 있으면 `8.sp` |
| `duration` | `{"value": 400, "unit": "ms"}` | `400` (Int) |
| `number` | `0.7` / `50` | `0.7f` / `50` — JSON 이 실수면 Float, 정수면 Int |

- `$type` 은 그룹에 한 번 적으면 아래로 상속된다.
- alias 는 `"{ref.palette.white}"`. Kotlin 에서는 참조(`Ref.Palette.white`)로 나온다 — 값을 풀어 쓰지 않는다.
- `$description` 은 KDoc 이 된다. **"왜 이 값인가" 는 여기 적는다.** 코드 주석에 두면 토큰과 떨어진다.
- **`px` 는 논리 픽셀이다.** Android dp, iOS pt, Web px. DTCG 가 허용하는 단위가 px/rem 뿐이라
  이 관례를 쓴다 — Style Dictionary 의 android 변환도 같다.
- 반투명 색은 `#RRGGBBAA` 원시값으로 둔다(`white20`, `ink900a88`). `$extensions` 로 "색 + 불투명도"
  를 조합하는 방식은 이 생성기만 알아듣고 다른 도구는 불투명한 색으로 읽는다.

## 토큰이 아닌 것

디자이너가 바꾸는 값만 토큰이다. 아래는 코드에 남아 있고, 그래야 한다.

| 값 | 왜 |
|---|---|
| 제스처 임계값 — 겨냥 44dp, 탭 창 400ms, 밀어내기 0.28, 해제 지점 0.92 | 바꾸면 디자인이 아니라 **동작**이 바뀐다 |
| 번인 궤도 8dp×28dp / 60초 / 12스텝 | 패널 보호 파라미터 |
| 패널 밝기 0.30 | 전력 실측으로 정한 값 |
| 기호 내부 비율, 배터리 번개 좌표 | 아이콘의 **그림**이다. SVG path 를 토큰으로 빼지 않는 것과 같다 |
| 사용자 설정의 기본값·범위 | 설정이다 |

## 다른 제품에서 가져가기

1. `tokens.json` 을 복사한다.
2. `comp` 를 지우고 자기 부품을 쓴다. `ref`·`sys` 는 그대로 두거나 값만 바꾼다.
3. 생성은 `build.py` 를 가져가도 되고, [Style Dictionary](https://styledictionary.com/) 나
   Tokens Studio 에 그대로 먹여도 된다 — 형식이 표준이라 CSS 변수·Swift·Kotlin 어느 쪽으로든 나온다.

`sys` 의 이름은 이 앱을 모르는 사람이 읽어도 뜻이 통해야 한다. `canvas`, `onCanvas`, `floatingSurface`,
`secondary`, `disabled` 는 되고, `mediaBarTint` 는 안 된다 — 그런 건 `comp` 에 둔다.

## 생성기 (`build.py`)

Python 3 표준 라이브러리만 쓴다. 하는 일:

1. JSON 을 읽어 토큰을 평탄화하고 `$type` 을 상속시킨다.
2. 층 규칙·alias 대상·타입 일치를 검사한다. 하나라도 어긋나면 생성하지 않는다.
3. 그룹 → 중첩 `object`, 토큰 → `val`, `$description` → KDoc 으로 Kotlin 을 낸다.

`Color(...)` 를 완전한 이름(`androidx.compose.ui.graphics.Color`)으로 내는 이유: `object Color` 안에서
`Color(...)` 는 자기 자신을 가리켜 컴파일이 안 된다.
