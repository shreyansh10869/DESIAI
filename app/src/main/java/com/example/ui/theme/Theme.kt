package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuralisDarkColorScheme = darkColorScheme(
    primary = AuralisCyan,
    onPrimary = Color(0xFF090A0F),
    primaryContainer = DesiOrangeContainer,
    onPrimaryContainer = DesiOrangeLight,
    secondary = AuralisPurple,
    onSecondary = Color.White,
    secondaryContainer = DesiAmberContainer,
    onSecondaryContainer = AuralisPurple,
    tertiary = DesiTealAccent,
    background = AuralisObsidianBg,
    onBackground = DesiTextPrimary,
    surface = AuralisSurfaceSolid,
    onSurface = DesiTextPrimary,
    surfaceVariant = AuralisCardSolid,
    onSurfaceVariant = DesiTextSecondary,
    outline = AuralisHairlineBorder
)

@Composable
fun AuralisAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuralisDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun DesiAITheme(
    content: @Composable () -> Unit
) {
    AuralisAITheme(content = content)
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    AuralisAITheme(content = content)
}
