# mobile-black-screen

화면만 꺼진 것처럼 보이되, 기기는 그대로 돌아가는 Android 앱.

기본 상태는 아무것도 표시하지 않는 완전한 검정이다. 설정에서 시계와 문장을 켤 수 있다.

<sub>Android 전용 · 개인용 사이드로딩 · minSdk 26</sub>

---

## 먼저 알아야 할 것

**이 앱은 화면을 끄지 않는다. 검게 덮고 밝기를 0 으로 내린다.**

Android 에는 임의 시점에 화면을 끄는 공개 API가 없다. 그리고 물리적으로 꺼 버리면
시계와 문장을 표시할 수 없어 요구사항 자체가 성립하지 않는다. 그래서 정의를 바꿨다.

AMOLED 기기에서는 검은 픽셀이 발광하지 않아 체감상 꺼진 것과 거의 같다.
**LCD 기기는 백라이트가 남아 이득이 훨씬 적고 완전히 검게 보이지도 않는다.**

## 세 가지 Screen Off 방식

|  | **전체 화면** (기본) | **오버레이** | **완전 검정** |
|---|---|---|---|
| 구현 | `TYPE_ACCESSIBILITY_OVERLAY` | `TYPE_APPLICATION_OVERLAY` | 풀스크린 Activity |
| **쓰던 앱** | **그대로 유지** | **그대로 유지** | **뒤로 밀림** |
| 상태바 / 내비바 | **전부 덮음** | 둘 다 남음 | 숨김 |
| 다른 앱 UI 렌더링 | 계속 렌더링 | 계속 렌더링 | 멈춤 |
| 권한 | 접근성 | 다른 앱 위에 표시 | **없음** |

**전체 화면이 이 앱이 하려던 동작이다.** 쓰던 앱은 건드리지 않고 화면만 어두워진다.
나머지 둘은 접근성 권한을 주고 싶지 않을 때의 절충안(오버레이)과,
권한을 하나도 주고 싶지 않을 때의 최후 수단(완전 검정)이다.

> **자주 오해하는 지점**
> 완전 검정 모드에서도 음악 재생·다운로드·업로드·내비게이션·타이머·동기화는
> 전부 그대로 돌아간다. Android 에서 백그라운드 앱은 죽지 않는다.
> 멈추는 것은 **직전까지 화면에 떠 있던 앱의 UI 렌더링뿐**이다(게임 진행, 영상 재생).
> 오버레이 모드가 추가로 사는 것은 정확히 이 한 가지다.

> **창 종류에 따라 덮을 수 있는 범위가 다르다**
> `TYPE_APPLICATION_OVERLAY`(오버레이 모드)는 Android 8(O)부터 시스템 UI 위에 그리는 것이
> 금지되어 상태바·내비바를 덮지 못한다. 실측상 창 프레임이 `[0,94][1080,2214]` 로 잘리고
> z-order 상으로도 두 바가 위에 있다.
> **반면 `TYPE_ACCESSIBILITY_OVERLAY`(전체 화면 모드)는 그 위까지 전부 덮는다.**
> 실측으로 상태바 영역 밝은 픽셀 0개를 확인했다.

## 기능

- **무표시 / 시계 / 문장** — 기본값은 무표시. 시계 형식은 24시간·12시간 중 선택
- **번인 방지** — 60초마다 글자 위치를 원 궤도로 미세하게 이동 (AMOLED 잔상 방지)
- **밝기 5단계** — 글자는 흰색 고정, 슬라이더가 패널 밝기(0.05~0.80)를 움직인다.
  표시할 내용이 없으면 밝기 0. 시계는 분 경계에 맞춰 분 단위로만 갱신
- **오작동 방지 해제** — 1.5초 롱프레스가 기본값. 진행 링으로 피드백.
  단일 탭과 뒤로 가기로는 풀리지 않는다. 더블 탭·3회 탭도 선택 가능
- **퀵 설정 타일** — 상태바를 내려 한 번 탭하면 바로 차폐

## 빌드 및 설치

Android SDK 와 JDK 17 이상이 필요하다. SDK 경로는 `local.properties` 에 적는다.

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

SDK 가 없다면 명령줄 도구만으로 충분하다.

```bash
brew install --cask android-commandlinetools
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

### 툴체인

| | 버전 |
|---|---|
| Gradle | 9.7.1 |
| AGP | 9.3.2 |
| Kotlin (AGP 내장) | 2.4.10 |
| Compose BOM | 2026.08.00 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 26 |

`compileSdk` 는 Compose 1.12 가 요구해 37 로 올렸지만 `targetSdk` 는 36 에 둔다.
targetSdk 를 올리면 새 OS 의 동작 변경을 옵트인하게 되는데, 그만큼 검증 범위가
넓어지기 때문이다. lint 의 `OldTargetApi` 경고 1건은 이 선택의 결과이며 의도된 것이다.

`android.builtInKotlin` 은 끄지 않는다. 외부 `org.jetbrains.kotlin.android` 플러그인은
AGP 9 의 새 DSL 이 `BaseExtension` 을 제거해 `ClassCastException` 으로 실패한다.

## 구조

```
app/src/main/java/com/jjw/blackscreen/
├─ MainActivity.kt              설정 화면 (진입점)
├─ blackout/BlackoutActivity.kt 완전 검정 모드
├─ overlay/
│   ├─ OverlayService.kt        LifecycleService + FGS(specialUse)
│   ├─ OverlayWindow.kt         WindowManager.LayoutParams 구성
│   └─ OverlayLauncherActivity.kt  투명 중계 Activity
├─ ui/
│   ├─ BlackScreenContent.kt    ★ 세 모드가 공유하는 Screen Off 화면
│   ├─ Clock.kt                 분 경계 정렬 시계
│   ├─ BurnInShift.kt           픽셀 시프트
│   ├─ UnlockGesture.kt         해제 제스처 + 진행 링
│   └─ settings/SettingsScreen.kt
├─ data/                        Settings + DataStore 래퍼
└─ tile/BlackScreenTileService.kt  퀵 설정 타일
```

## 검증 상태

Galaxy S23+ (SM-S916N) / Android 16 / SDK 36 / AMOLED 에서 완료 조건 17개를 모두 확인했다.
전 픽셀 `(0,0,0)`, 밝기 오버라이드 적용, 화면 꺼짐 방지, 해제 제스처 4종, 번인 이동,
분 단위 갱신, 설정 영속성, 오버레이 아래 앱 생존, 터치 비통과, 창 누수 없음.

**전력 소모는 아직 실측하지 않았다.** 이 앱은 화면을 끄지 않으므로 디스플레이
파이프라인이 계속 살아 있다. 수치를 말하려면 충전을 끊고 `dumpsys batterystats` 로
측정해야 한다.

## 문서

| 문서 | 내용 |
|---|---|
| [docs/technical-feasibility.md](docs/technical-feasibility.md) | 기술 타당성 분석 — 플랫폼별 판정, 방식 비교, 하드 제약 |
| [docs/architecture.md](docs/architecture.md) | 구현 설계 — 세 모드 구조, 핵심 코드, 함정 목록 |
| [docs/power-measurement.md](docs/power-measurement.md) | 전력 측정 — 방법, 함정, 현재 추정치 |
| [docs/plans/blackout-overlay-modes/](docs/plans/blackout-overlay-modes/) | 작업 계획 — 스펙, 맥락, 진행 체크리스트 |

## 하지 않은 것

- **iOS** — 다른 앱 위 오버레이와 백그라운드 임의 실행이 OS 차원에서 차단된다.
  화면을 끄거나 잠그는 공개 API도 없다. 요구사항을 그대로 옮길 수 없어 제외했다.
- **Google Play 출시** — 개인용 사이드로딩 전제라 `specialUse` FGS 심사 대응을 하지 않았다.
- 자동 종료 타이머, 근접센서 포켓 모드, 문장 순환, 홈 화면 위젯, 제조사별 배터리 최적화 안내
