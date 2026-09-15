# spec — media-playback-bar

**상태: 설계 검토.** 구현 전. 사용자 확인이 필요한 결정은 마지막 절에 모았다.

## 목표

음악·동영상이 재생 중일 때 차폐 화면(과 설정 미리보기)에 **재생 바** 를 표시한다 —
제목 · 아티스트/앱 · 진행 막대 · `1:23 / 4:56`. 재생이 멈추면 사라진다.

## 범위

- **포함**
  - `showMedia` 설정 (기본 꺼짐 — 무표시가 기본이라는 원칙)
  - 알림 접근 권한 안내 카드 + 설정 이동
  - `MediaSessionManager` 로 활성 세션 관찰, 재생 중인 세션 하나 선택
  - 차폐·미리보기에 재생 바 (표시 전용)
  - 위치 계산은 로컬, 10초 주기 재그림, 상태 변화는 콜백 즉시
- **제외 (2차 후보)**
  - 재생/일시정지/이전/다음 제어
  - 앨범 아트
  - 권한 없을 때 `isMusicActive` 폴백 표시

## 완료 조건 (Definition of Done)

- [ ] 유튜브 재생 중 Screen Off → 제목·채널·진행 막대·시간이 뜬다
- [ ] 음악 앱(멜론/스포티파이 등 아무것이나) 재생 중 → 같은 형식으로 뜬다
- [ ] 곡이 바뀌면 10초를 기다리지 않고 즉시 바뀐다 (콜백)
- [ ] 일시정지 → 막대가 멈추고 시간이 고정된다. 정지/앱 종료 → 재생 바가 사라지고 밝기가 원래 규칙으로 돌아간다
- [ ] 재생 중이 아닐 때는 아무것도 그리지 않는다 (hasContent 에 영향 없음)
- [ ] 권한 없으면 카드가 뜨고, 허용하고 돌아오면 카드가 사라진다
- [ ] 알림 접근 콜백은 비어 있다 — 알림 내용을 읽는 코드가 없다 (코드 리뷰로 확인)
- [ ] 차폐가 떠 있지 않을 때 세션 관찰이 살아 있지 않다 (서비스 dumpsys 로 확인)
- [ ] 해제 제스처가 영향을 받지 않는다
- [ ] lint 0 errors

## 설계

### 1. 권한 — 알림 접근

```xml
<service android:name=".media.MediaListenerService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
</service>
```

- 클래스는 `NotificationListenerService` 를 상속하되 **콜백을 전부 비워 둔다.** 존재 이유는
  `MediaSessionManager.getActiveSessions(ComponentName)` 이 요구하는 컴포넌트 이름 하나다.
  접근성 서비스의 `onAccessibilityEvent = Unit` 과 같은 자세.
- 권한 확인: `NotificationManager.isNotificationListenerAccessGranted(componentName)` (API 27+; minSdk 26 이라
  26 은 `Settings.Secure.getString("enabled_notification_listeners")` 파싱 폴백).
- 설정 이동: `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. 돌아오면 `LifecycleResumeEffect` 가
  다시 확인한다 — 오버레이 권한과 같은 패턴.
- 설정 화면 문구: "재생 중인 곡을 알기 위해 필요합니다. 이 앱은 알림 내용을 읽지 않습니다 —
  재생 정보만 씁니다." 접근성 설명과 같은 톤.

### 2. 관찰 — `MediaWatcher`

```kotlin
class MediaWatcher(context: Context) {
    val nowPlaying: StateFlow<NowPlaying?>
    fun start()   // addOnActiveSessionsChangedListener + 각 세션에 Callback
    fun stop()    // 전부 해제
}

data class NowPlaying(
    val title: String, val subtitle: String,   // 아티스트 없으면 앱 이름
    val appLabel: String,
    val positionMs: Long, val durationMs: Long, val playbackSpeed: Float,
    val updatedAtMs: Long,                      // PlaybackState.lastPositionUpdateTime
    val playing: Boolean,
)
```

- **세션 선택**: `PlaybackState.STATE_PLAYING` 인 것 우선, 여럿이면 `getActiveSessions` 순서(최근 활성).
  재생 중인 게 없으면 `null` — 일시정지 상태는 표시하되 `playing=false`.
  (일시정지도 보여 주는 이유: 잠깐 멈춘 곡이 무엇인지 보는 용도가 있다. 정지(`STATE_STOPPED`/`NONE`)는 숨긴다.)
- **수명**: 차폐 창이 뜰 때 `start()`, 내릴 때 `stop()`. 미리보기는 설정 화면이 RESUMED 인 동안만.
  차폐 없이 백그라운드에서 세션을 듣지 않는다 — 전력과 원칙 둘 다.
- **소유자**: `ScreenOffAccessibilityService` 가 차폐와 함께 소유한다(버블·차폐를 `ScreenCoverService` 가
  함께 소유하는 것과 같은 논리 — 서비스를 늘리지 않는다). 미리보기는 `MainActivity` 가 따로 만든다.
- 위치 계산: `position + (elapsedRealtime − updatedAt) × speed`, `duration` 으로 clamp.
  `duration ≤ 0`(라이브·길이 미상)이면 막대 대신 경과 시간만.

### 3. 표시 — `MediaBar`

시계·날짜·배터리 블록 **아래**, 문장 위. `BlackScreenContent` 의 Column 에 한 항목 더.
글자는 흰색, 시계 글꼴 계열, 크기 16sp/13sp. 번인 궤도를 함께 탄다.

```
        18:45
     9월 13일 (일)
       ▭ 60%

  제목이 길면 한 줄로 자름…
  아티스트 · YouTube
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  1:23 / 4:56
```

- 막대: 폭 60%, 두께 2dp, 흰색 0.25 위에 진행분 흰색. 손잡이 없음(제어 없으니).
- 재그림 주기: `playing` 일 때만 10초 `delay` 루프(`LaunchedEffect`). 일시정지면 루프 없음.
  콜백이 오면 `NowPlaying` 이 바뀌어 즉시 재구성된다.
- `hasContent`: `showMedia && nowPlaying != null` 일 때만 참에 기여한다 — 재생 중이 아니면
  이 기능은 밝기를 올리지 않는다. **`Settings.hasContent` 는 순수 설정값만 보는 프로퍼티라
  재생 상태를 알 수 없다.** 렌더 시점에 `settings.hasContent || (settings.showMedia && nowPlaying != null)`
  로 합쳐 밝기를 정한다. 접근성 서비스의 밝기 결정 지점을 이 합성값으로 바꿔야 한다.

### 4. 설정 화면

`화면 표시` 탭, 배터리 아래:
- `SwitchRow` "재생 중인 음악·동영상 표시"
- 켜져 있고 권한 없으면 `PermissionCard` "알림 접근 권한이 필요합니다 / 권한 허용하기"
- 미리보기 상자에도 재생 바가 뜬다 (설정 화면이 RESUMED 인 동안 관찰)

### 5. 전력

- 세션 콜백은 이벤트 기반 — 곡이 바뀌지 않으면 아무 일도 없다.
- 10초 루프 = 8시간에 2,880 프레임. 시계의 480 프레임(분당 1)보다 많지만 0.1Hz 다.
  실측 해상도(17.5 mA/30분)로는 안 잡힐 것이다. 신경 쓰이면 30초로.
- 아래에서 유튜브가 영상을 계속 그리는 것은 **이 기능의 비용이 아니다** — 차폐의 설계 의도이고
  이미 그렇게 돌고 있다.

## 함정 예상

- **`getActiveSessions` 는 권한 없으면 `SecurityException`** 을 던진다. 권한 확인을 먼저 하고,
  그래도 `runCatching`.
- **YouTube 는 광고 중에 메타데이터가 바뀌거나 빈다.** 제목이 비면 앱 이름만 보인다.
- **일부 앱은 `duration` 을 0 이나 -1 로 준다**(라이브·팟캐스트 스트림). 막대를 숨기고 경과만.
- **알림 접근을 켜면 시스템이 앱을 재시작시킬 수 있다.** 접근성처럼 재설치 때 꺼질 수 있다 —
  검증 레시피에 재활성화 명령을 추가해야 한다:
  `cmd notification allow_listener <pkg>/<svc>`.
- **`MediaController.Callback` 은 등록한 스레드의 Looper 로 온다.** 메인에서 등록.

## 사용자 확인이 필요한 결정

1. **알림 접근 권한을 하나 더 받는 것** — 이게 없으면 "재생 중" 참/거짓밖에 못 보여 준다. 받는가?
2. **1차는 표시만** — 재생/일시정지/다음 곡 버튼은 2차. 동의하는가?
3. **일시정지 상태도 보여 주는가** — 제안은 "보여 준다(멈춘 막대)", 정지는 숨김.
4. **위치** — 시계 블록 아래(제안) vs 화면 하단 고정. 하단 고정은 번인 궤도를 따로 태워야 하고
   잠금 해제 트랙이 있던 자리라 시각적으로 무거워진다.
5. **재그림 주기 10초** — 더 뜸하게(30초) 해도 되는가.
