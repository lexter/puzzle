package com.lexterlabra.puzzle.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.lexterlabra.puzzle.glass.GlassBackdrop
import com.lexterlabra.puzzle.resources.Res
import com.lexterlabra.puzzle.resources.scenic_default
import org.jetbrains.compose.resources.imageResource

/** The bundled default picture (square, 1024 px). */
@Composable
fun scenicImage(): ImageBitmap = imageResource(Res.drawable.scenic_default)

/** Blurred-picture backdrop + safe-area padding shared by every screen. */
@Composable
fun ScreenScaffold(backdrop: ImageBitmap, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlassBackdrop(backdrop, modifier) {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
        )
    }
}
