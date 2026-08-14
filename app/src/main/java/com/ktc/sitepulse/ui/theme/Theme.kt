package com.ktc.sitepulse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SitePulseColorScheme = lightColorScheme(
    primary = SpBrandBlueMid,
    onPrimary = Color.White,
    primaryContainer = SpBrandBlueSoft,
    onPrimaryContainer = SpBrandBlueDark,
    secondary = SpBrandGold,
    onSecondary = Color.Black,
    background = SpPaper,
    onBackground = SpInk,
    surface = SpCard,
    onSurface = SpInk,
    surfaceVariant = SpBrandBlueSoft,
    onSurfaceVariant = SpMuted,
    outline = SpLine,
    error = SpRed,
    errorContainer = SpRedSoft,
)

private val SitePulseTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, letterSpacing = 0.3.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp),
)

// Generous corner radii (vs. Material3's tighter 4/8/12dp defaults) — the single biggest lever
// for reading as a modern consumer-grade product instead of an out-of-the-box Material sample.
// Flows automatically into every Card, Button, TextField, and Dialog app-wide.
private val SitePulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SitePulseTheme(content: @Composable () -> Unit) {
    // Deliberately light-only, matching the original app (no dark-mode CSS was defined).
    MaterialTheme(
        colorScheme = SitePulseColorScheme,
        typography = SitePulseTypography,
        shapes = SitePulseShapes,
        content = content,
    )
}
