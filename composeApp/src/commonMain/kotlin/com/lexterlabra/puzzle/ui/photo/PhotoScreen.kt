package com.lexterlabra.puzzle.ui.photo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.platform.openAppSettings
import com.lexterlabra.puzzle.platform.rememberImagePicker
import com.lexterlabra.puzzle.ui.components.GlassButton
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.game.MessageToast
import com.lexterlabra.puzzle.ui.game.UserMessage
import com.lexterlabra.puzzle.ui.theme.DisplayStyle
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/** "Make your puzzle": the photo is the hero, with the tile grid drawn over it. */
@Composable
fun PhotoScreen(viewModel: PhotoViewModel, onBack: () -> Unit, onRace: () -> Unit, onSolo: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val scenic = scenicImage()
    val image = state.photo?.image ?: scenic
    val picker = rememberImagePicker(viewModel::setPicture)
    val palette = LocalPalette.current

    LaunchedEffect(state.message) {
        if (state.message != null) { delay(4_000); viewModel.dismissMessage() }
    }

    ScreenScaffold(image) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 520.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Column {
                    Text("Make your puzzle", style = DisplayStyle, fontSize = 22.sp, color = palette.text)
                    Text("Any photo becomes a ${state.grid}×${state.grid} board", fontSize = 13.sp, color = palette.secondaryText)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(6.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = palette.accent, spotColor = palette.accent)
                    .border(3.dp, palette.accent, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .semantics { contentDescription = "Puzzle picture preview, cut into ${state.grid} by ${state.grid} tiles" },
            ) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                GridOverlay(state.grid, palette.accent)
                if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (PuzzleState.MIN_SIZE..PuzzleState.MAX_SIZE).forEach { n ->
                    GridChip(n, n == state.grid, Modifier.weight(1f)) { viewModel.setGrid(n) }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SourceTile(Icons.Rounded.PhotoLibrary, "Gallery", enabled = true, onClick = picker::pickFromGallery)
                Shutter(enabled = picker.cameraAvailable, onClick = picker::takePhoto)
                SourceTile(Icons.Rounded.Landscape, "Scenic", enabled = true, onClick = viewModel::useScenic)
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton("Race with this photo", onClick = onRace, icon = Icons.Rounded.Bolt, enabled = !state.loading)
            GlassButton("Play solo", onClick = onSolo, icon = Icons.Rounded.Person)
        }
        MessageToast(
            text = state.message?.text,
            actionLabel = if (state.message == UserMessage.CameraDenied) "Settings" else null,
            onAction = { openAppSettings() },
            onDismiss = viewModel::dismissMessage,
        )
    }
}

/** White cut lines plus corner brackets in the player's color. */
@Composable
private fun GridOverlay(n: Int, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val cell = size.width / n
        val line = Color.White.copy(alpha = 0.85f)
        for (i in 1 until n) {
            drawLine(line, Offset(i * cell, 0f), Offset(i * cell, size.height), strokeWidth = 1.5.dp.toPx())
            drawLine(line, Offset(0f, i * cell), Offset(size.width, i * cell), strokeWidth = 1.5.dp.toPx())
        }
        val inset = 12.dp.toPx()
        val arm = 24.dp.toPx()
        val stroke = 4.dp.toPx()
        listOf(
            Offset(inset, inset) to Offset(1f, 1f),
            Offset(size.width - inset, inset) to Offset(-1f, 1f),
            Offset(inset, size.height - inset) to Offset(1f, -1f),
            Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f),
        ).forEach { (corner, dir) ->
            drawLine(accent, corner, Offset(corner.x + arm * dir.x, corner.y), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(accent, corner, Offset(corner.x, corner.y + arm * dir.y), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun GridChip(n: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(if (selected) palette.accent else palette.glassTint)
            .border(1.dp, if (selected) palette.accent else palette.glassBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$n×$n", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) palette.onAccent else palette.text, maxLines = 1)
    }
}

@Composable
private fun SourceTile(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    GlassSurface(Modifier.size(width = 92.dp, height = 66.dp).alpha(if (enabled) 1f else 0.45f).clip(shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick), shape = shape) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
        }
    }
}

/** Big round camera button. */
@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(80.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(18.dp, CircleShape, ambientColor = palette.accent, spotColor = palette.accent)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF4FD1FF), Color(0xFFFF4FA3))))
            .border(4.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Take a photo" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}
