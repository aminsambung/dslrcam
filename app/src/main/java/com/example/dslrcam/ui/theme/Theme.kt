package com.example.dslrcam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun DSLRCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
