package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalistDarkColorScheme = darkColorScheme(
    primary = ChatAccentWhite,
    onPrimary = ChatDarkBg,
    primaryContainer = ChatUserBubble,
    onPrimaryContainer = ChatTextPrimary,
    secondary = ChatTextSecondary,
    onSecondary = ChatDarkBg,
    secondaryContainer = ChatSurfaceDock,
    onSecondaryContainer = ChatTextPrimary,
    tertiary = ChatTextPrimary,
    background = ChatDarkBg,
    onBackground = ChatTextPrimary,
    surface = ChatSurfaceDock,
    onSurface = ChatTextPrimary,
    surfaceVariant = ChatUserBubble,
    onSurfaceVariant = ChatTextSecondary,
    outline = ChatSubtleBorder
)

@Composable
fun AuralisAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalistDarkColorScheme,
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
