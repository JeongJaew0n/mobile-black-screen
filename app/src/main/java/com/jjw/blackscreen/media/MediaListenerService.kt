package com.jjw.blackscreen.media

import android.service.notification.NotificationListenerService

/**
 * 알림 접근 권한의 **자리만** 차지하는 서비스. 콜백은 전부 비어 있다.
 *
 * `MediaSessionManager.getActiveSessions()` 는 알림 접근이 허용된 `NotificationListenerService`
 * 의 컴포넌트 이름을 요구한다 — 잠금화면 미디어 컨트롤이 쓰는 것과 같은 경로다. 이 클래스가
 * 존재하는 이유는 그 이름 하나다. 접근성 서비스의 `onAccessibilityEvent = Unit` 과 같은 자세다.
 *
 * 알림 접근에는 접근성의 `canRetrieveWindowContent=false` 같은 "읽지 않는다" 플래그가 없다.
 * 콜백을 비워 두는 것이 유일한 약속이고, 설정 화면 문구가 그 사실을 사용자에게 말한다.
 * **여기에 알림을 읽는 코드를 넣지 말 것.**
 */
class MediaListenerService : NotificationListenerService()
