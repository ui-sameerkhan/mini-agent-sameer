package com.ktc.sitepulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SitePulseColorScheme = lightColorScheme(
    primary = SpGreenMid,
    onPrimary = Color.White,
    primaryContainer = SpGreenSoft,
    onPrimaryContainer = SpGreenDark,
    secondary = SpAmberMid,
    onSecondary = Color.White,
    background = SpPaper,
    onBackground = SpInk,
    surface = SpCard,
    onSurface = SpInk,
    surfaceVariant = SpGreenSoft,
    onSurfaceVariant = SpMuted,
    outline = SpLine,
    error = SpRed,
    errorContainer = SpRedSoft,
)

private val SitePulseTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp),
)

@Composable
fun SitePulseTheme(content: @Composable () -> Unit) {
    // Deliberately light-only, matching the original app (no dark-mode CSS was defined).
    MaterialTheme(
        colorScheme = SitePulseColorScheme,
        typography = SitePulseTypography,
        content = content,
    )
}
