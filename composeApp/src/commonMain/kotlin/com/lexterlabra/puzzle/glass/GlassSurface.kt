package com.lexterlabra.puzzle.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lexterlabra.puzzle.image.PuzzleImage
import com.lexterlabra.puzzle.ui.theme.LocalPalette

/**
 * Full-screen backdrop: the current puzzle picture, heavily blurred, under a light/dark scrim.
 * The blur comes from drawing a tiny copy of the picture scaled up, so it works on every
 * platform version (Modifier.blur adds extra softness where the OS supports it).
 */
@Composable
fun GlassBackdrop(image: ImageBitmap, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalPalette.current
    val tiny = remember(image) { PuzzleImage.downscale(image, BACKDROP_SIDE) }
    Box(modifier.fillMaxSize().background(palette.background)) {
        Image(
            bitmap = tiny,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxSize().blur(24.dp),
        )
        Box(Modifier.fillMaxSize().background(palette.backdropScrim))
        content()
    }
}

/** Frosted panel used for every bar, card, chip and sheet. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    solid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .shadow(elevation = 6.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(if (solid) palette.glassTintSolid else palette.glassTint)
            .border(1.dp, palette.glassBorder, shape),
        content = content,
    )
}

private const val BACKDROP_SIDE = 32
