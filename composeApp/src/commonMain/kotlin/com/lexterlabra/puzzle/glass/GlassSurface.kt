package com.lexterlabra.puzzle.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import kotlin.random.Random

/**
 * Full-screen candy backdrop: base color, two soft color glows and scattered sprinkles.
 * [image] is accepted for API compatibility (screens pass the current picture) but the
 * candy style keeps the backdrop calm so the board itself stays the hero.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun GlassBackdrop(image: ImageBitmap, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalPalette.current
    val sprinkles = remember { buildSprinkles() }
    Box(modifier.fillMaxSize().background(palette.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(palette.glowA, Color.Transparent), center = Offset(size.width * 0.1f, size.height * 0.08f), radius = size.width * 0.75f),
                radius = size.width * 0.75f, center = Offset(size.width * 0.1f, size.height * 0.08f),
            )
            drawCircle(
                Brush.radialGradient(listOf(palette.glowB, Color.Transparent), center = Offset(size.width * 1.0f, size.height * 0.55f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 1.0f, size.height * 0.55f),
            )
            val w = 14.dp.toPx()
            val h = 5.dp.toPx()
            sprinkles.forEach { s ->
                val center = Offset(s.x * size.width, s.y * size.height)
                rotate(s.angle, pivot = center) {
                    drawRoundRect(
                        color = palette.sprinkles[s.colorIndex % palette.sprinkles.size].copy(alpha = if (palette.isDark) 0.55f else 0.45f),
                        topLeft = Offset(center.x - w / 2, center.y - h / 2),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(h / 2),
                    )
                }
            }
        }
        content()
    }
}

private class Sprinkle(val x: Float, val y: Float, val angle: Float, val colorIndex: Int)

private fun buildSprinkles(): List<Sprinkle> {
    val random = Random(SPRINKLE_SEED)
    return List(SPRINKLE_COUNT) { Sprinkle(random.nextFloat(), random.nextFloat(), random.nextFloat() * 180f, random.nextInt(6)) }
}

/** Frosted candy panel used for every bar, card, chip and sheet. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    solid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .then(if (solid) Modifier.shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f)) else Modifier)
            .clip(shape)
            .background(if (solid) palette.glassTintSolid else palette.glassTint)
            .border(1.dp, palette.glassBorder, shape),
        content = content,
    )
}

private const val SPRINKLE_COUNT = 26
private const val SPRINKLE_SEED = 7
