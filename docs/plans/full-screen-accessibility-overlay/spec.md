# spec — full-screen-accessibility-overlay

## 목표

**쓰던 앱을 그대로 둔 채 화면 전체(상태바·내비게이션 바 포함)를 덮는** Screen Off 모드를
만들고 기본값으로 삼는다. 완료되면 사용자는 어떤 앱을 쓰던 중에도 버블이나 타일을 눌러
화면만 어둡게 만들 수 있고, 그 앱은 최상단에 그대로 남는다.

## 범위

- **포함**
  - `ScreenOffAccessibilityService` — `TYPE_ACCESSIBILITY_OVERLAY` 창 소유
  - `Mode.FULL` 추가 및 기본값 지정, `BLACKOUT` 을 최후 수단으로 격하
  - `ScreenOff` 디스패처 — 설정 버튼 / 버블 / 타일의 시작 분기를 한 곳으로
  - 설정 화면에 FULL 모드 + 접근성 권한 안내
  - 타일에서 FULL 을 켤 때 중계 Activity 를 거치지 않게

- **제외**
  - 접근성 서비스로 화면 내용을 읽는 기능 (`canRetrieveWindowContent=false`)
  - 부팅 후 자동 복원
  - 접근성이 꺼졌을 때 앱 안에서 배너로 안내하는 기능 (추후)

## 완료 조건 (Definition of Done)

- [x] 설정에 "전체 화면" 모드가 있고 기본값이다
- [x] 접근성 권한이 없으면 안내 카드와 함께 설정으로 보낸다
- [x] **다른 앱 위에서 버블을 눌러도 그 앱이 최상단에 그대로 남는다**
- [x] **상태바 영역이 덮인다** (밝은 픽셀 0개)
- [x] **내비게이션 바 영역이 덮인다** (밝은 픽셀 0개)
- [x] 시계·문장이 정상 렌더링된다
- [x] 해제 제스처가 동작한다
- [x] 밝기 설정이 첫 표시부터 적용된다
- [x] 크래시 0건

## 인터페이스

```kotlin
enum class Mode { FULL, OVERLAY, BLACKOUT }   // 기본 FULL

object ScreenOff {
    enum class Result { STARTED, NEEDS_ACCESSIBILITY, NEEDS_OVERLAY_PERMISSION }
    fun start(context: Context, mode: Mode): Result
}
```

`ScreenOffAccessibilityService` 는 `LifecycleOwner` / `SavedStateRegistryOwner` /
`ViewModelStoreOwner` 를 직접 구현하고 `LifecycleRegistry` 를 **RESUMED 까지** 올린다.

## 비고 — 주의할 점

- **창 크기를 명시적으로 지정할 것.** `MATCH_PARENT` 는 시스템 바를 제외한 부모 프레임에
  맞춰져 위아래가 남는다.
- **`showCover()` 는 suspend 가 아니다.** 밝기를 알아야 하므로 설정 수집기가 캐시해 둔
  값을 초기 파라미터에 쓴다. 빼먹으면 첫 표시에만 밝기가 안 걸린다.
- **타일에서 FULL 은 중계 Activity 를 쓰지 말 것.** 띄우는 순간 쓰던 앱이 밀린다.
- 접근성 서비스는 앱 재설치 시 꺼진다.
