package com.jetro.mt5dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = ObsidianBg,
    primaryContainer = AccentCyanDark,
    secondary        = AccentViolet,
    onSecondary      = ObsidianBg,
    tertiary         = AccentEmerald,
    background       = ObsidianBg,
    onBackground     = TextPrimary,
    surface          = ObsidianSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline          = ObsidianBorder,
    error            = LossRed,
    onError          = ObsidianBg,
)

@Composable
fun JetroMT5Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
