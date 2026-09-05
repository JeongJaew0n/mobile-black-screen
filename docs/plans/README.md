# 작업 기록

작업 하나당 폴더 하나. 각 폴더는 세 파일로 이루어진다.

- **spec.md** — 무엇을 만들 것인가 (범위·완료 조건)
- **context.md** — 왜 시작했는가 (원 요청·결정 근거·기각된 대안). **사후에 고쳐 쓰지 않는다**
- **checklist.md** — 어디까지 했는가

시간순이며, **뒤 작업이 앞 작업의 결정을 뒤집은 경우가 있다.**
각 폴더 상단 배너에 무엇이 바뀌었는지 적어 두었다.
현재 상태를 알고 싶으면 계획 문서가 아니라 [architecture.md](../architecture.md) 를 볼 것.

| 순서 | 작업 | 상태 |
|---|---|---|
| 1 | [blackout-overlay-modes](./blackout-overlay-modes/) — 두 모드 차폐 앱 | 완료 (일부 결정 뒤집힘) |
| 2 | [floating-bubble-launcher](./floating-bubble-launcher/) — 상주 버블 | 완료 |
| 3 | [full-screen-accessibility-overlay](./full-screen-accessibility-overlay/) — FULL 모드 | 완료 |

## 왜 과거 문서를 고쳐 쓰지 않는가

계획 문서는 "그때 무엇을 알고 있었고 왜 그렇게 판단했는가"의 기록이다.
지금 기준으로 다시 쓰면 **판단이 틀렸던 이유가 사라진다.**

실제로 이 프로젝트에서 가장 큰 실수는
"오버레이는 상태바를 못 덮는다"를 창 종류 구분 없이 일반화한 것이었다.
그 경위가 1번 폴더에 남아 있어야 같은 실수를 반복하지 않는다.
