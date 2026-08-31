package com.jjw.blackscreen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MinuteMillis = 60_000L

/**
 * 분 단위로만 갱신되는 시각 문자열.
 *
 * 초 단위 갱신은 이 앱에서 의미가 없고 전력만 소모하므로, 다음 분 경계까지 정확히
 * 기다렸다가 한 번만 갱신한다. 경계에 정렬해 두면 표시된 값이 실제 시각보다
 * 최대 1분 뒤처지는 일도 없다.
 */
@Composable
fun rememberClockText(pattern: String): String {
    val formatter = remember(pattern) {
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }
    var text by remember(formatter) { mutableStateOf(LocalTime.now().format(formatter)) }

    LaunchedEffect(formatter) {
        while (true) {
            val now = System.currentTimeMillis()
            delay(MinuteMillis - (now % MinuteMillis))
            text = LocalTime.now().format(formatter)
        }
    }

    return text
}
