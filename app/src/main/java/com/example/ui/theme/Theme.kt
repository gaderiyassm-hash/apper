package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF28230D),
    onPrimaryContainer = AccentGold,
    secondary = AccentBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF142442),
    onSecondaryContainer = Color(0xFFADC8FF),
    tertiary = TradeUpGreen,
    onTertiary = Color(0xFF000000),
    background = TerminalBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderLight,
    error = TradeDownRed,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun ApperTheme(
    darkTheme: Boolean = true, // Trading terminal defaults to dark for high clarity
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias for template references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ApperTheme(content = content)
}
