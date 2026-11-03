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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.data.Appearance

fun resolveDark(appearance: Appearance, systemDark: Boolean): Boolean = when (appearance) {
    Appearance.LIGHT -> false
    Appearance.DARK -> true
    Appearance.SYSTEM -> systemDark
}

/**
 * "Candy Pop" palette. [accent] is the local player ("you", blueberry) and [opponentAccent]
 * is the rival (tangerine): they differ in hue and lightness so they read apart for everyone.
 */
@Immutable
data class PuzzlePalette(
    val isDark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val secondaryText: Color,
    val background: Color,
    val glassTint: Color,
    val glassTintSolid: Color,
    val glassBorder: Color,
    val selectedFill: Color,
    val selectedText: Color,
    val opponentAccent: Color,
    val track: Color,
    val glowA: Color,
    val glowB: Color,
    val gold: Color,
    val sprinkles: List<Color>,
) {
    val ctaTop: Color get() = CandyCta.top
    val ctaBottom: Color get() = CandyCta.bottom
    val ctaBase: Color get() = CandyCta.base
}

/** Strawberry → tangerine candy button, the same in both modes. White label passes 3:1 at 18 sp bold. */
object CandyCta {
    val top = Color(0xFFFF7AB6)
    val bottom = Color(0xFFFF6A3D)
    val base = Color(0xFFC23A1C)
    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))
    /** Text gradient for big celebratory titles. */
    val titleBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFF4FD1FF), Color(0xFFFFB3D9), Color(0xFFFF8A3D)))
}

private val SprinkleColors = listOf(
    Color(0xFFFF7AB6), Color(0xFF4FD1FF), Color(0xFFFFD84D), Color(0xFF7CF0B4), Color(0xFFFF8A3D), Color.White,
)

val DarkPalette = PuzzlePalette(
    isDark = true,
    accent = Color(0xFF4FD1FF),
    onAccent = Color(0xFF06141A),
    text = Color(0xFFF4F1FF),
    secondaryText = Color(0xFFEBCFF2),
    background = Color(0xFF2A0E4F),
    glassTint = Color.White.copy(alpha = 0.12f),
    glassTintSolid = Color(0xFF3B1868).copy(alpha = 0.97f),
    glassBorder = Color.White.copy(alpha = 0.24f),
    selectedFill = Color(0xFFF4F1FF),
    selectedText = Color(0xFF4A1D3F),
    opponentAccent = Color(0xFFFF8A3D),
    track = Color.White.copy(alpha = 0.12f),
    glowA = Color(0xFFFF4FA3).copy(alpha = 0.45f),
    glowB = Color(0xFFEA580C).copy(alpha = 0.30f),
    gold = Color(0xFFFFC94D),
    sprinkles = SprinkleColors,
)

val LightPalette = PuzzlePalette(
    isDark = false,
    accent = Color(0xFF0E7490),
    onAccent = Color.White,
    text = Color(0xFF4A1D3F),
    secondaryText = Color(0xFF7A4A63),
    background = Color(0xFFFFF1E6),
    glassTint = Color.White.copy(alpha = 0.82f),
    glassTintSolid = Color.White.copy(alpha = 0.97f),
    glassBorder = Color(0xFFFF4FA3).copy(alpha = 0.18f),
    selectedFill = Color(0xFF4A1D3F),
    selectedText = Color.White,
    opponentAccent = Color(0xFFC2410C),
    track = Color(0xFF4A1D3F).copy(alpha = 0.08f),
    glowA = Color(0xFFFF4FA3).copy(alpha = 0.22f),
    glowB = Color(0xFFEA580C).copy(alpha = 0.16f),
    gold = Color(0xFFB45309),
    sprinkles = SprinkleColors.dropLast(1),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/**
 * Display face for titles and big numbers. The design uses Fredoka (rounded, OFL): drop
 * `fredoka_semibold.ttf` / `fredoka_bold.ttf` into composeResources/font and point this at them.
 */
val DisplayFont: FontFamily = FontFamily.Default

/** Timer and counters: fixed-width digits so numbers don't jitter. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

/** Chunky title text (screen titles, "YOU WIN!", countdown). */
val DisplayStyle = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)

@Composable
fun PuzzleTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val palette = if (resolveDark(appearance, isSystemInDarkTheme())) DarkPalette else LightPalette
    val scheme = if (palette.isDark) {
        darkColorScheme(primary = palette.accent, onPrimary = palette.onAccent, secondary = palette.opponentAccent, background = palette.background, onBackground = palette.text, surface = palette.background, onSurface = palette.text)
    } else {
        lightColorScheme(primary = palette.accent, onPrimary = palette.onAccent, secondary = palette.opponentAccent, background = palette.background, onBackground = palette.text, surface = palette.background, onSurface = palette.text)
    }
    val base = MaterialTheme.typography
    val typography = base.copy(
        headlineLarge = base.headlineLarge.merge(DisplayStyle).copy(fontSize = 30.sp),
        titleLarge = base.titleLarge.merge(DisplayStyle),
    )
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalPalette provides palette, LocalContentColor provides palette.text, content = content)
    }
}
