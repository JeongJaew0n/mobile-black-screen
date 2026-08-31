package com.jjw.blackscreen.overlay

import android.graphics.PixelFormat
import android.view.WindowManager

/**
 * 오버레이 창 파라미터.
 *
 * 플래그 선택 이유:
 * - `FLAG_NOT_FOCUSABLE` : 터치는 받되 아래 앱의 키/IME 포커스는 뺏지 않는다.
 * - `FLAG_LAYOUT_NO_LIMITS` : 다른 기기·버전을 위해 남겨 두지만 **실측상 효과가 없다.**
 *   Galaxy S23+ / Android 16 에서 창 프레임은 `[0,94][1080,2214]` 로, 상태바(94px) 아래에서
 *   시작해 내비바(126px) 위에서 끝난다. z-order 상으로도 `NavigationBar`·`StatusBar` 가
 *   오버레이보다 위라 기하학·레이어 양쪽에서 막혀 있다. Android 8(O)부터
 *   `TYPE_APPLICATION_OVERLAY` 가 시스템 UI 위에 그리는 것은 의도적으로 금지되어 있다.
 * - `FLAG_KEEP_SCREEN_ON` : 화면이 꺼지면 표시 중인 시계·문장도 사라진다.
 * - `PixelFormat.OPAQUE` : 완전히 가려진 아래 레이어의 합성을 컴포지터가 건너뛸 수 있다.
 *
 * `FLAG_NOT_TOUCHABLE` 은 **의도적으로 쓰지 않는다.** 터치를 아래 앱으로 통과시키면
 * 주머니 속 오작동이 그대로 아래 앱에 전달되고, 동시에 해제 제스처를 받을 방법이 사라진다.
 */
internal fun overlayLayoutParams(): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.OPAQUE,
    ).apply {
        // 최상단 가시 창의 값이 적용된다.
        screenBrightness = 0f
    }
