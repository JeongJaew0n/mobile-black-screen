# checklist — i18n

상태: **구현 완료(en + ko). 실기기 검증 대기.**

## 0. 결정 대기

- [x] 첫 회차 언어 목록 — **en / ko 로 시작.** ja 시계 패턴은 문자열 없이도 `set-app-locales ja-JP` 로 ICU 결과를 볼 수 있어 ja 문자열은 요청 시 파일 하나로 추가

## 1. 문자열 밖의 로케일 의존 (먼저 — 문자열 이동보다 어렵다)

- [x] `ClockStyle(skeleton) { H24("HHmm"), H12("hmm") }` 도입, `ClockFormats` 제거
- [x] `android.icu.text.DateTimePatternGenerator.getBestPattern(skeleton, MATCH_HOUR_FIELD_LENGTH)` 로 패턴 파생
- [x] 서식은 `android.icu.text.SimpleDateFormat` — `java.time` 을 쓰지 않는다 (`B` 문자)
- [ ] **ko / en-US / ja × HHmm / hmm 여섯 값을 찍어 spec §2a 표를 실측값으로 교체.** ko `HH:mm` 앞자리 0 확인
- [x] DataStore 마이그레이션 `clock_format`(패턴) → `clock_style`(열거형)
- [x] `rememberClockText` 가 로케일 변경을 따라오는지 (`LocalConfiguration`)
- [x] 설정 라디오 예시를 고정 시각 서식으로 생성
- [x] `hold_duration_value` / `bubble_size_value` 서식 문자열
- [x] ~~`String.format(Locale.getDefault(), …)` 명시~~ → 숫자를 `%1$.1f` 로 리소스에 넘겨 `getString` 이 리소스 로케일로 서식. 코드에 `format` 이 없어졌다
- [x] `AimTargets` 하드코딩 2건 추출
- [x] `SlideToUnlockLayer` 를 `LocalLayoutDirection provides Ltr` 로 감싼다

## 2. 리소스 이동

- [x] `values/strings.xml` → 영어 47개
- [x] `values-ko/strings.xml` ← 지금 한국어 그대로
- [x] OS 용어 인용 문자열 5건은 플랫폼 표기 확인 후 작성 (spec §2c)
- [x] `resources.properties` + `generateLocaleConfig`. AGP 9.3.2 에서 동작 — 매니페스트에 `localeConfig="@xml/_generated_res_locale_config"` 가 붙고 en-US / ko (+디버그 의사 로케일) 가 들어간다
- [x] ~~안 되면 수동~~ 불필요

## 3. 빌드 설정

- [x] `debug { isPseudoLocalesEnabled = true }`
- [x] `lintDebug` 0 errors — `MissingTranslation` 끄지 않고

## 4. 검증 (사용자가 지시할 때)

- [ ] `set-app-locales en-US` — 화면·알림 영어. (런처·타일·접근성 설명은 안 바뀌는 게 정상)
- [ ] **기기 언어**를 영어로 — 런처 이름 *Black Screen*, 타일 이름, 접근성 목록 설명까지 영어
- [ ] `set-app-locales ko-KR` — 지금과 동일. **24시간제 `09:05` 앞자리 0 포함**
- [ ] 12시간제: ko `오후 1:05`, en `1:05 PM`, (ja 포함 시) `午後1:05`
- [ ] `en-XA` — 감싸이지 않은 글자 0, 잘림 0
- [ ] `ar-XB` — 설정 화면 뒤집힘. 트랙 손잡이는 **왼쪽에서 시작해 손가락 방향으로** 움직임
- [ ] 언어 바꾼 뒤 서비스 재시작 → 알림 채널 이름 갱신
- [ ] grep 하드코딩 0건
- [ ] 로케일 원복 (`--locales ""`)

## 5. 문서

- [x] `architecture.md` §5.1/5.2 추가, 함정 F20~F25
- [x] `CLAUDE.md` — 용어 절에 기본 로케일, 검증 레시피에 `set-app-locales` + grep, 함정 4건
