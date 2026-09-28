package com.jjw.blackscreen.ui

import androidx.compose.ui.graphics.Color
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.ui.theme.Sys

/**
 * 차폐 화면의 글자색. **항상 거의 흰색**(수묵화 `paper` `#FFFFE3`)이고 설정으로 바뀌지 않는다.
 *
 * 보이는 밝기는 프레임버퍼 값 × 패널 밝기다. 어둡게 만드는 손잡이가 둘이면 둘 다 깎아 놓고 왜 안
 * 보이는지 헤매게 된다 — 실제로 세 번 그랬다(`#333333`+패널 0, `#E6E6E6`+패널 0, `#E6E6E6`+패널 0.08).
 * 그래서 밝기 손잡이는 [Settings.screenBrightness] 하나로 통일했다.
 *
 * `data` 층이 아니라 여기 있는 이유: 색은 Compose 타입이고 디자인 토큰에서 온다. `ClockFont` →
 * `FontFamily` 매핑을 `ui/Clock.kt` 에 둔 것과 같은 원칙 — data 층은 Compose 도 테마도 모른다.
 */
val Settings.textColor: Color get() = Sys.Color.onCanvas
