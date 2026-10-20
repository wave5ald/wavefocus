package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onBackground = NightOnSurface,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    secondary = DaySecondary,
    tertiary = DayTertiary,
    background = DayBackground,
    surface = DaySurface,
    surfaceVariant = DaySurfaceVariant,
    onBackground = DayOnSurface,
    onSurface = DayOnSurface,
    onSurfaceVariant = DayOnSurfaceVariant,
    outline = DayOutline
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
