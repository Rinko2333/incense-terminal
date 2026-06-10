package com.rinko.incenseterminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IncenseColorScheme = darkColorScheme(
    background = IncenseColors.Background,
    surface = IncenseColors.Background,
    onBackground = IncenseColors.PrimaryText,
    onSurface = IncenseColors.PrimaryText,
    primary = IncenseColors.Accent,
    secondary = IncenseColors.Smoke,
    tertiary = IncenseColors.Ember,
    error = IncenseColors.Ember,
    surfaceVariant = IncenseColors.Background,
    onSurfaceVariant = IncenseColors.DimText,
    outline = IncenseColors.DimText
)

@Composable
fun IncenseTerminalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IncenseColorScheme,
        typography = IncenseTypography,
        content = content
    )
}