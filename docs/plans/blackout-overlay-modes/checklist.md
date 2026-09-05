# checklist — blackout-overlay-modes

> ## ⚠️ 이 문서는 완료된 과거 작업의 기록입니다
>
> 이후 **[full-screen-accessibility-overlay](../full-screen-accessibility-overlay/)** 로
> 뒤집힌 내용이 있습니다. 현재 상태는 [architecture.md](../../architecture.md) 를 보세요.
>
> | 이 문서의 서술 | 현재 |
> |---|---|
> | 모드 두 개(Blackout / Overlay), 기본은 Blackout | **세 개(FULL / Overlay / Blackout), 기본은 FULL** |
> | "오버레이는 상태바를 덮을 수 없다 — 우회 불가" | **`TYPE_APPLICATION_OVERLAY` 에 한한 제약.** 접근성 오버레이는 덮는다 |
> | 글자색을 저휘도 그레이로 | **글자는 흰색 고정**, 밝기는 패널 하나로만 조절 |
> | UI 용어 `차폐` | **`Screen Off`** (코드·문서 내부 용어는 `차폐` 유지) |
> | 기본 해제 제스처 1.5초 롱프레스 | **3회 연속 탭** |


> 작업 진행하면서 AI 가 순차적으로 체크. `[x]` 로 표시한 항목은 완료된 것으로 간주.
> 새 항목이 발견되면 적절한 단계에 추가하고 체크리스트를 유지한다.

**현재 상태: 31 / 36 완료 — 1·2차 구현 및 실기기 검증 완료. 남은 것은 전력 실측과 마무리**
> ✅ `assembleDebug` 성공, `lintDebug` 0 errors / 1 warning (의도적 targetSdk 36 선택).
> ✅ **DoD 17개 전부 실기기 검증 완료** — Galaxy S23+ / Android 16 / SDK 36.
> ⚠️ 남은 공백: **전력 실측만 미완** (USB 충전 중이라 방전 데이터 확보 불가).

## 0. 준비
- [x] spec.md / context.md 다시 한 번 읽고 어긋난 곳 없는지 확인
- [x] **Android SDK 확보** — `brew install --cask android-commandlinetools`
      · SDK root: `/opt/homebrew/share/android-commandlinetools` (`local.properties` 에 `sdk.dir` 기록, gitignore 대상)
      · 설치: `platform-tools`, `platforms;android-36`, `platforms;android-37.0`, `build-tools;36.0.0`, `build-tools;37.0.0`
      · 라이선스 동의 완료
- [x] Gradle Wrapper 확보 (gradle 미설치 → wrapper jar 직접 내려받기)
      · Gradle **9.1.0** 고정 (AGP 9.0 이 요구하는 최소·기본 버전). `./gradlew --version` 동작 확인
- [x] 작업 브랜치 생성 → `feat/blackout-overlay-modes`

### 확정된 툴체인 (빌드로 검증됨)

| | 버전 |
|---|---|
| Gradle | 9.7.1 |
| AGP | 9.3.2 |
| Kotlin (KGP, 내장) | 2.4.10 |
| Compose BOM | 2026.08.00 (Compose 1.12 / Material3 1.4) |
| compileSdk | 37 |
| targetSdk | **36** |
| minSdk | 26 |
| JDK | 17 타깃 (설치본 21) |

**targetSdk 를 36 에 두는 이유:** compileSdk 37 은 Compose 1.12 가 요구해서 올렸지만,
targetSdk 를 올리면 새 OS 의 동작 변경을 옵트인하게 된다. 실기기 검증 수단이 확보되기
전까지는 확인하지 못한 변경을 떠안지 않는다. lint 의 `OldTargetApi` 경고 1건은 이 선택의
결과이며 의도된 것이다.

#### 두 번 뒤집힌 결정 (기록)

**1) 내장 Kotlin.** 처음에는 `android.builtInKotlin=false` 로 끄고 외부
`org.jetbrains.kotlin.android` 플러그인을 적용했으나, AGP 9 의 새 DSL
(`android.newDsl=true` 기본값)이 `BaseExtension` 을 제거해 실패했다.

```
class ...ApplicationExtensionImpl$AgpDecorated_Decorated cannot be cast to
class com.android.build.gradle.BaseExtension
```

→ `org.jetbrains.kotlin.plugin.compose` 만 적용하고 `kotlin.android` 는 제거하는 것이 정답.
`kotlin { compilerOptions { jvmTarget } }` 블록도 내장 Kotlin 이 관리하므로 함께 제거했다.

**2) 버전 선택 근거가 틀렸음.** 최초에 "AGP 최신 안정판은 9.0.1 이고 Compose BOM
2026.08.00 이 요구하는 AGP 9.1.1 은 안정판이 아니다"라고 판단해 BOM 을 2026.06.01 로
낮췄다. 이는 **사실이 아니었다** — lint 의 버전 점검이 AGP 9.3.2 가 나와 있음을 알려주었고,
`sdkmanager --list` 로 platform 37 도 안정판으로 제공됨을 확인했다.
현재는 전부 최신 조합으로 올렸고 빌드·lint 모두 통과한다.

## 1. 구현 — 1차 Blackout (권한 0개)
- [x] 프로젝트 스캐폴딩: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
      `gradle/libs.versions.toml`, `.gitignore`
- [x] `AndroidManifest.xml` — Application, `MainActivity`, `BlackoutActivity`
      (`theme=Theme.Black`, `screenOrientation=nosensor`, `excludeFromRecents`, `launchMode=singleTask`)
- [x] `data/Settings.kt` — 설정 데이터 클래스 + enum (`Mode`, `Gesture`)
- [x] `data/SettingsRepository.kt` — DataStore Preferences 래퍼
- [x] `ui/BlackScreenContent.kt` — 두 모드 공유 Composable (무표시/시계/문장)
      · 시계는 **분 경계에 정렬된 타이머**로 분 단위만 갱신
      · ~~텍스트 색은 저휘도 그레이~~ → **오설계였음.** 패널 밝기와 곱해져 판독 불가.
        글자는 흰색 고정, `textLevel` 은 패널 밝기(0.05~0.80)를 움직인다
- [x] `ui/BurnInShift.kt` — 60초마다 반경 24dp 원 궤도로 즉시 이동 (애니메이션 없음)
- [x] `ui/Clock.kt` — 분 경계 정렬 타이머 (체크리스트 최초 작성 시 누락되어 추가)
- [x] `ui/UnlockGesture.kt` — 1.5초 롱프레스 + 원형 진행 피드백 페이드인
- [x] `blackout/BlackoutActivity.kt` — 몰입 모드 + `screenBrightness=0f` + `FLAG_KEEP_SCREEN_ON`
      · `WindowInsetsControllerCompat`로 `systemBars()` hide
      · `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`
- [x] `ui/settings/SettingsScreen.kt` + `MainActivity.kt` — 설정 UI 및 차폐 시작 버튼
- [x] `tile/BlackScreenTileService.kt` — 퀵 설정 타일
      · ⚠️ `startActivityAndCollapse(PendingIntent)` 사용 (Intent 오버로드는 API 34+ 예외)

## 2. 구현 — 2차 Overlay
- [x] Manifest에 권한 4종 + `OverlayService` (`foregroundServiceType=specialUse` + `<property>`)
- [x] `overlay/OverlayWindow.kt` — `WindowManager.LayoutParams` 구성
      · `TYPE_APPLICATION_OVERLAY`
      · `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS | FLAG_KEEP_SCREEN_ON`
      · ⚠️ `FLAG_NOT_TOUCHABLE` **사용 금지** (터치를 삼켜야 함)
      · `PixelFormat.OPAQUE`, `screenBrightness = 0f`
- [x] `overlay/OverlayService.kt` — `LifecycleService` + `SavedStateRegistryOwner` + `ViewModelStoreOwner`
      · ⚠️ `savedStateController.performRestore(null)`을 **`super.onCreate()` 이전**에 호출
      · `ComposeView`에 `setViewTreeLifecycleOwner` / `SavedStateRegistryOwner` / `ViewModelStoreOwner` 배선
      · `onDestroy`에서 `removeView` + `viewModelStore.clear()` (누수 방지, try/catch)
- [x] 포그라운드 알림 (최소 우선순위) + 해제 액션
- [x] `Settings.canDrawOverlays()` 확인 → `ACTION_MANAGE_OVERLAY_PERMISSION` 안내 플로우
- [x] `POST_NOTIFICATIONS` 런타임 권한 요청 (API 33+)
- [x] 설정 화면에 모드 전환 + **"Overlay 모드는 상태바를 덮지 못한다"** 명시
- [x] 타일에서 현재 모드에 따라 Activity / Service 분기
      · 타일은 `onClick` 이 suspend 가 아니라 DataStore 를 그 자리에서 못 읽는다.
        `onStartListening` 에서 모드를 미리 읽어 둔다.
- [x] `overlay/OverlayLauncherActivity.kt` — 계획에 없던 항목, 구현 중 필요해져 추가
      · 포그라운드 서비스를 백그라운드에서 시작하면 Android 15+ 에서
        `ForegroundServiceStartNotAllowedException` 이 날 수 있다. 오버레이 예외를 쓰려면
        "이미 보이는 오버레이 창"이 필요한데 시작 시점엔 당연히 없다.
        투명 Activity 를 한 단계 끼워 앞에 있는 상태에서 서비스를 시작해 이 경로를 피한다.

## 3. 검증
- [x] 컴파일 통과 (`./gradlew assembleDebug`) — `app-debug.apk` 생성 확인
- [x] `./gradlew lintDebug` 통과 — 0 errors / 1 warning (의도적 targetSdk 36 선택에 대한 `OldTargetApi`)
- [x] 실기기 설치 후 Blackout 완료 조건 11개 확인 (spec.md의 DoD)
      · 검증 기기: **Galaxy S23+ (SM-S916N) / Android 16 / SDK 36 / AMOLED**
      · 전 픽셀 (0,0,0) 확인 (1080×2340 샘플링) — 시스템 바 흔적 없음
      · `mWindowManagerBrightnessOverride=0.0`, 태그가 BlackoutActivity 로 확인
      · 화면 꺼짐 방지: 타임아웃을 15초로 낮춘 뒤 40초 무입력 → `mScreenState=ON` 유지 (설정 원복 완료)
      · 해제 제스처: 단일 탭 / 뒤로 가기 / 0.8초 누름 → 유지, 2.0초 누름 → 해제
      · 롱프레스 진행 링 시각 확인 (하단 1/4 지점, 부분 호)
      · 번인 이동: 65초 후 무게중심 dx=-6.5px dy=+31.2px
      · 분 단위 갱신: 같은 구간에서 23:29 → 23:30 전환 확인
- [x] 실기기에서 Overlay 완료 조건 6개 확인 (spec.md의 DoD)
      · FGS `types=0x40000000` = `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`, `uidState: TOP`
      · **아래 앱 생존**: HOME 키로 런처가 RESUMED 된 상태에서 오버레이 창 유지 확인
      · 터치 비통과: 오버레이 위 탭이 아래 런처에 전달되지 않음
      · 알림 "해제" 액션 → 창 1→0, 서비스 1→0, 알림 제거 확인
      · 권한 미허용 시 `Settings$OverlaySettingsActivity` 로 정확히 안내됨
      · **상태바·내비바 둘 다 안 덮임** (프레임 `[0,94][1080,2214]`, z-order 상 바가 위)
      · 아래 앱 렌더링 지속은 **영상 재생**으로 확인
      · 터치 비통과 확인
- [x] 회귀: 모드 전환 왕복, 설정 변경 후 재시작, 창 누수 (`dumpsys window`) 확인
      · 설정 영속성: `force-stop` 후 재실행에도 시계 ON·문장 보존
      · 창 누수 없음: 해제 후 남은 창은 MainActivity 뿐
      · 크래시 로그 0건 (`logcat -b crash`)
- [x] 퀵 설정 타일 실동작 확인
      · `START ... BlackoutActivity with LAUNCH_SINGLE_TASK from uid 10898` 로그로 확증
      · ⚠️ `cmd statusbar click-tile` 만으로는 `onStartListening` 이 안 돌아 실패한다.
        QS 패널을 먼저 띄워야 서비스가 바인딩된다 — adb 아티팩트이지 앱 버그가 아니다.
- [ ] 전력 실측 — `adb shell dumpsys batterystats` + Battery Historian
      **미완.** adb 연결이 USB 충전을 겸하고 있어 방전 데이터를 얻을 수 없다.
      무선 디버깅으로 전환하거나, 충전기를 뽑고 20~30분 방치 후 측정해야 한다.

## 4. 마무리
- [x] 커밋 (이 저장소에는 별도 커밋 규약이 없어 Conventional Commits 형식을 따름)
- [ ] checklist 의 미체크 항목이 남았으면 사유 메모
- [ ] 다음 세션이 픽업할 수 있도록 spec.md / context.md 의 변경분 반영
- [x] `docs/architecture.md`와 실제 구현이 어긋난 부분 동기화
