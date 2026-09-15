# 지문이 계속 실패하면 차폐에서 빠져나올 수 없다

## 증상
해제 제스처를 '지문으로 해제' 로 두고 Screen Off. 지문 인식이 반복 실패하자 화면을 되찾을
방법이 없었다. 프롬프트의 '취소' 를 눌러도 프롬프트만 닫히고 차폐는 그대로다. 차폐가 상태바를
덮어 알림 그림자도 못 내리고, 설정 앱으로도 갈 수 없다.

## 원인
`BiometricPrompt` 에 `BIOMETRIC_WEAK` 만 허용하고 **기기 PIN/패턴 대체를 일부러 막아 두었다.**
당시 근거는 "지문으로 푸는 기능이고, PIN 화면은 차폐 아래 가려져 조작할 수 없다" 였다.
앞부분은 취향이고 뒷부분은 해결 가능한 문제였는데, 그걸 이유로 **유일한 탈출구를 없앤 것**이
잘못이었다.

## 응급 탈출 (갇혔을 때)
- **전원 버튼 길게 → 다시 시작.** 부팅 후 자동 시작을 안 하므로 차폐가 사라진다.
- adb 가 붙어 있다면: `adb shell am force-stop com.jjw.blackscreen`
  (접근성 서비스가 죽으면서 오버레이 창이 함께 내려간다)

## 해결
1. `DEVICE_CREDENTIAL` 을 허용해 PIN/패턴 대체를 연다(API 30+는 `setAllowedAuthenticators`,
   29는 `setDeviceCredentialAllowed`). **`DEVICE_CREDENTIAL` 과 `setNegativeButton` 은 함께 쓸 수
   없다** — 시스템이 그 자리에 'PIN 사용' 버튼을 넣는다.
2. PIN 화면이 차폐 아래 가려지지 않게, 인증 중에는 차폐를 거의 투명(alpha 0.12)하게 + 터치
   통과(`FLAG_NOT_TOUCHABLE`)로 바꾼다. **탈출구는 실제로 쓸 수 있어야 의미가 있다.**
3. 안전망: 복구 불가 오류(HW_UNAVAILABLE 1 / LOCKOUT_PERMANENT 9 / NO_BIOMETRICS 11 /
   HW_NOT_PRESENT 12)면 차폐를 자동 해제한다. 갇히는 것보다 열리는 쪽이 낫다.

## 재발 방지
**해제 수단을 새로 만들 때는 "그 수단이 실패하면 어떻게 빠져나오는가" 를 먼저 답할 것.**
차폐는 상태바까지 덮어 시스템 UI 로 우회할 수 없다 — 앱이 스스로 탈출구를 보장해야 한다.
