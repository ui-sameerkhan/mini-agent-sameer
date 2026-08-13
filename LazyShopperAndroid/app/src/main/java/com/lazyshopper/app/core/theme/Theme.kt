package com.lazyshopper.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LsLightColors = lightColorScheme(
    primary = LsPrimary,
    onPrimary = LsBackground,
    secondary = LsAccent,
    onSecondary = LsBackground,
    background = LsBackground,
    onBackground = LsForeground,
    surface = LsSurface,
    onSurface = LsForeground,
    surfaceVariant = LsMuted,
    onSurfaceVariant = LsForeground,
    error = LsError,
)

private val LsDarkColors = darkColorScheme(
    primary = LsPrimaryLight,
    onPrimary = LsForeground,
    secondary = LsAccent,
    background = Color(0xFF141B13),
    onBackground = LsBackground,
    surface = Color(0xFF1E271C),
    onSurface = LsBackground,
    surfaceVariant = Color(0xFF2A342A),
    onSurfaceVariant = LsBackground,
    error = LsError,
)

@Composable
fun LazyShopperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) LsDarkColors else LsLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = LsTypography,
        content = content,
    )
}
