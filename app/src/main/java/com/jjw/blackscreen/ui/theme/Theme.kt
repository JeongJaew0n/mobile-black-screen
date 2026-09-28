package com.jjw.blackscreen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * 수묵화 팔레트를 Material 3 색 역할에 꽂은 것. 값은 전부 [Sys.Color] 에서 온다 —
 * 여기서 색을 새로 정하지 않는다. 바꾸려면 `design/tokens/tokens.json`.
 *
 * 항상 어두운 쪽 하나다. 설정 화면이 눈부시면 "화면을 끈다" 는 용도와 어긋난다.
 */
private val InkWashColorScheme = darkColorScheme(
    primary = Sys.Color.primary,
    onPrimary = Sys.Color.onPrimary,
    primaryContainer = Sys.Color.primaryContainer,
    onPrimaryContainer = Sys.Color.onPrimaryContainer,
    inversePrimary = Sys.Color.inversePrimary,
    secondary = Sys.Color.secondary,
    onSecondary = Sys.Color.onSecondary,
    secondaryContainer = Sys.Color.secondaryContainer,
    onSecondaryContainer = Sys.Color.onSecondaryContainer,
    // 3차 강조는 따로 두지 않는다 — 이 앱의 강조는 청회색 하나다.
    tertiary = Sys.Color.primary,
    onTertiary = Sys.Color.onPrimary,
    tertiaryContainer = Sys.Color.primaryContainer,
    onTertiaryContainer = Sys.Color.onPrimaryContainer,
    background = Sys.Color.background,
    onBackground = Sys.Color.onBackground,
    surface = Sys.Color.surface,
    onSurface = Sys.Color.onSurface,
    surfaceVariant = Sys.Color.surfaceVariant,
    onSurfaceVariant = Sys.Color.onSurfaceVariant,
    surfaceTint = Sys.Color.primary,
    surfaceBright = Sys.Color.surfaceContainerHighest,
    surfaceDim = Sys.Color.surfaceContainerLowest,
    surfaceContainerLowest = Sys.Color.surfaceContainerLowest,
    surfaceContainerLow = Sys.Color.surfaceContainerLow,
    surfaceContainer = Sys.Color.surfaceContainer,
    surfaceContainerHigh = Sys.Color.surfaceContainerHigh,
    surfaceContainerHighest = Sys.Color.surfaceContainerHighest,
    inverseSurface = Sys.Color.inverseSurface,
    inverseOnSurface = Sys.Color.inverseOnSurface,
    error = Sys.Color.error,
    onError = Sys.Color.onError,
    errorContainer = Sys.Color.errorContainer,
    onErrorContainer = Sys.Color.onErrorContainer,
    outline = Sys.Color.outline,
    outlineVariant = Sys.Color.outlineVariant,
    scrim = Sys.Color.scrim,
)

/**
 * 이 앱의 모든 Compose 루트가 이걸로 감싼다 — 설정 화면, Blackout, 차폐 창, 버블 창.
 * 차폐·버블은 색을 대부분 직접 지정하지만, 감싸 두면 Material 기본값(보라)이 새어 나올 자리가 없다.
 */
@Composable
fun BlackScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = InkWashColorScheme, content = content)
}
