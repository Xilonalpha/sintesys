package com.synthesis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentGreen,
    tertiary = AccentAmber,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AccentRed
)

@Composable
fun SynthesisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
