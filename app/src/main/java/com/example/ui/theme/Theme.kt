package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DesiDarkColorScheme = darkColorScheme(
    primary = DesiOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = DesiOrangeContainer,
    onPrimaryContainer = DesiOrangeLight,
    secondary = DesiAmberSecondary,
    onSecondary = Color.Black,
    secondaryContainer = DesiAmberContainer,
    onSecondaryContainer = DesiAmberSecondary,
    tertiary = DesiTealAccent,
    background = DesiDarkBg,
    onBackground = DesiTextPrimary,
    surface = DesiDarkSurface,
    onSurface = DesiTextPrimary,
    surfaceVariant = DesiDarkCard,
    onSurfaceVariant = DesiTextSecondary,
    outline = DesiDarkBorder
)

@Composable
fun DesiAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DesiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
