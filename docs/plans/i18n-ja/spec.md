# 일본어 추가 — 무엇을 만드는가

## 범위
1. `app/src/main/res/values-ja/strings.xml` — 영어 원문 80개 전부
2. `overlay_running_desc` 문구 수정 (en · ko · ja)
3. 문서 — `architecture.md` §5.2, 용어집, `CLAUDE.md`

## 완료 조건
- [x] ja 키 집합 = en 키 집합 (빠짐·남음 0)
- [x] 서식 인자(`%1$s` `%2$s` `%1$.1f` `%1$d`)가 키마다 en 과 같다
- [x] `lintDebug` 0 errors — `MissingTranslation` 을 끄지 않고
- [x] 생성된 locale config 에 `ja` 가 들어간다 (`generateLocaleConfig`)

## 로케일 의존 — 이미 되어 있는 것 (1차 작업)
| 항목 | ja 결과 | 근거 |
|---|---|---|
| 12시간제 시계 | `午後1:05` | 1차 실측 (ICU `hmm`) |
| 날짜 | `2026年9月28日(月)` 식 | ICU 스켈레톤 — 문자열 없음 |
| `Hold duration %1$.1f s` | 리소스 로케일로 서식 | 코드에 `format` 없음 |

## OS 용어 (AOSP 일본어)
| 키 | 인용 항목 |
|---|---|
| `overlay_permission_needed`, `bubble_permission_note` | 他のアプリの上に重ねて表示 |
| `accessibility_needed`, `accessibility_open` | ユーザー補助 |
| `media_permission_needed`, `show_media_desc` | 通知へのアクセス |
