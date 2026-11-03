package com.lexterlabra.puzzle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.data.Appearance

fun resolveDark(appearance: Appearance, systemDark: Boolean): Boolean = when (appearance) {
    Appearance.LIGHT -> false
    Appearance.DARK -> true
    Appearance.SYSTEM -> systemDark
}

@Immutable
data class PuzzlePalette(
    val isDark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val secondaryText: Color,
    val background: Color,
    val backdropScrim: Color,
    val glassTint: Color,
    val glassTintSolid: Color,
    val glassBorder: Color,
    val selectedFill: Color,
    val opponentAccent: Color,
    val track: Color,
)

val LightPalette = PuzzlePalette(
    isDark = false,
    accent = Color(0xFF2563EB),
    onAccent = Color.White,
    text = Color(0xFF12203A),
    secondaryText = Color(0xFF3E4E68),
    background = Color(0xFFEAF0F7),
    backdropScrim = Color(0xFFEEF3FA).copy(alpha = 0.45f),
    glassTint = Color.White.copy(alpha = 0.52f),
    glassTintSolid = Color.White.copy(alpha = 0.82f),
    glassBorder = Color.White.copy(alpha = 0.75f),
    selectedFill = Color.White,
    opponentAccent = Color(0xFFE07A4F),
    track = Color(0xFF12203A).copy(alpha = 0.1f),
)

val DarkPalette = PuzzlePalette(
    isDark = true,
    accent = Color(0xFF2F66E8),
    onAccent = Color.White,
    text = Color(0xFFEEF3FB),
    secondaryText = Color(0xFFB4C0D4),
    background = Color(0xFF0B111C),
    backdropScrim = Color(0xFF080C16).copy(alpha = 0.62f),
    glassTint = Color(0xFF1C2436).copy(alpha = 0.55f),
    glassTintSolid = Color(0xFF1C2436).copy(alpha = 0.85f),
    glassBorder = Color.White.copy(alpha = 0.14f),
    selectedFill = Color(0xFF2F66E8),
    opponentAccent = Color(0xFFF2A27A),
    track = Color.White.copy(alpha = 0.12f),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Timer and counters: fixed-width digits so numbers don't jitter. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

@Composable
fun PuzzleTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val palette = if (resolveDark(appearance, isSystemInDarkTheme())) DarkPalette else LightPalette
    val scheme = if (palette.isDark) {
        darkColorScheme(primary = palette.accent, onPrimary = palette.onAccent, background = palette.background, onBackground = palette.text, surface = palette.background, onSurface = palette.text)
    } else {
        lightColorScheme(primary = palette.accent, onPrimary = palette.onAccent, background = palette.background, onBackground = palette.text, surface = palette.background, onSurface = palette.text)
    }
    val base = MaterialTheme.typography
    val typography = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalPalette provides palette, LocalContentColor provides palette.text, content = content)
    }
}
