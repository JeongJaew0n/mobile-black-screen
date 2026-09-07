package com.jjw.blackscreen.ui

import android.icu.text.DateTimePatternGenerator
import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.jjw.blackscreen.data.ClockStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MinuteMillis = 60_000L

/**
 * 로케일이 정하는 시각 서식.
 *
 * ICU 로 패턴을 만들고 **ICU 로 서식한다.** `java.time.DateTimeFormatter` 를 쓰면 안 된다 —
 * 일부 로케일의 12시간제 패턴에는 유연한 시간대 문자 `B`(*오후에*)가 들어오는데 Android 의
 * `java.time` 은 `B` 를 몰라 `ofPattern` 이 예외를 던진다.
 *
 * `MATCH_HOUR_FIELD_LENGTH` 가 없으면 스켈레톤의 `HH` 가 로케일 기본 폭으로 바뀐다.
 * Android 의 편의 함수 `DateFormat.getBestDateTimePattern` 은 이 옵션을 노출하지 않는다.
 */
fun ClockStyle.formatter(locale: Locale): SimpleDateFormat {
    val pattern = DateTimePatternGenerator.getInstance(locale)
        .getBestPattern(skeleton, DateTimePatternGenerator.MATCH_HOUR_FIELD_LENGTH)
    return SimpleDateFormat(pattern, locale)
}

/**
 * 앱별 언어까지 반영된 현재 로케일. 로케일이 바뀌면 Configuration 이 바뀌어 재구성된다.
 * `Locale.getDefault()` 로 폴백하지 말 것 — 관찰되지 않는 읽기라 Compose lint 가 에러로 막는다.
 * Configuration 의 로케일 목록은 비어 있지 않다.
 */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * 분 단위로만 갱신되는 시각 문자열.
 *
 * 초 단위 갱신은 이 앱에서 의미가 없고 전력만 소모하므로, 다음 분 경계까지 정확히
 * 기다렸다가 한 번만 갱신한다. 경계에 정렬해 두면 표시된 값이 실제 시각보다
 * 최대 1분 뒤처지는 일도 없다.
 */
@Composable
fun rememberClockText(style: ClockStyle): String {
    val locale = currentLocale()
    val formatter = remember(style, locale) { style.formatter(locale) }
    var text by remember(formatter) { mutableStateOf(formatter.format(Date())) }

    LaunchedEffect(formatter) {
        while (true) {
            val now = System.currentTimeMillis()
            delay(MinuteMillis - (now % MinuteMillis))
            text = formatter.format(Date())
        }
    }

    return text
}

/**
 * 설정 라디오에 붙는 예시. 고정 시각 09:05 를 현재 로케일로 서식한다.
 * 문자열 리소스에 `(오전 9:05)` 를 박아 두면 언어마다 손으로 맞춰야 한다.
 *
 * 한 자리 시각을 고른 이유: 24시간제 라벨에 `09:05` 가 찍히면 `MATCH_HOUR_FIELD_LENGTH` 가
 * 살아 있다는 뜻이고 `9:05` 면 죽은 것이다. 라벨 자체가 검증이 된다.
 */
@Composable
fun rememberClockSample(style: ClockStyle): String {
    val locale = currentLocale()
    return remember(style, locale) {
        val sample = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }.time
        style.formatter(locale).format(sample)
    }
}
