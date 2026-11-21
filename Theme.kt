package com.premiabrygady.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgColor = Color(0xFF0F1115)
val CardColor = Color(0xFF181B22)
val BorderColor = Color(0xFF2A2F3A)
val TextColor = Color(0xFFE8EAF0)
val AccentColor = Color(0xFFF5A524)
val GreenColor = Color(0xFF3DDC97)
val RedColor = Color(0xFFFF6B6B)

private val DarkColors = darkColorScheme(
    primary = AccentColor,
    onPrimary = Color(0xFF1A1206),
    background = BgColor,
    onBackground = TextColor,
    surface = CardColor,
    onSurface = TextColor,
    surfaceVariant = CardColor,
    onSurfaceVariant = TextColor,
    outline = BorderColor,
    error = RedColor,
    onError = Color(0xFF2A0A0A)
)

@Composable
fun PremiaBrygadyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
