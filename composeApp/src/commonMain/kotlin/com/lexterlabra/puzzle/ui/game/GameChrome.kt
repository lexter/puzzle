package com.lexterlabra.puzzle.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.components.GlassButton
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.Segment
import com.lexterlabra.puzzle.ui.components.SegmentedControl
import com.lexterlabra.puzzle.ui.components.TimerPill
import com.lexterlabra.puzzle.ui.theme.CandyCta
import com.lexterlabra.puzzle.ui.theme.DisplayStyle
import com.lexterlabra.puzzle.ui.theme.LocalPalette

/** Back · stopwatch (time-based only) · settings. */
@Composable
fun GameTopBar(onBack: () -> Unit, timerText: String?, onSettings: () -> Unit, backLabel: String = "Back") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (timerText != null) TimerPill(timerText, Icons.Rounded.Timer)
        }
        GlassIconButton(Icons.Rounded.Settings, "Game settings", onSettings)
    }
}

/** Picture source chip (Scenic / Camera / Gallery). */
@Composable
fun PictureChip(icon: ImageVector, label: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    GlassSurface(
        modifier
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, palette.accent, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        shape = shape,
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) palette.accent else palette.text, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = palette.text)
        }
    }
}

/** Dimmed full-screen layer that hosts a bottom sheet or a centered card. */
@Composable
fun Overlay(visible: Boolean, onDismiss: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = onDismiss != null) { onDismiss?.invoke() },
            content = content,
        )
    }
}

const val GRID_LOCKED_NOTE = "Only the host can change this."

@Composable
fun SettingsSheet(
    visible: Boolean,
    gridSize: Int,
    appearance: Appearance,
    gridLocked: Boolean,
    onApply: (gridSize: Int, appearance: Appearance) -> Unit,
    onDismiss: () -> Unit,
) {
    Overlay(visible, onDismiss) {
        var pickedGrid by remember(gridSize, visible) { mutableIntStateOf(gridSize) }
        var pickedAppearance by remember(appearance, visible) { mutableStateOf(appearance) }
        val palette = LocalPalette.current
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            GlassSurface(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .semantics { contentDescription = "Game settings" },
                shape = RoundedCornerShape(34.dp),
                solid = true,
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(palette.track))
                    Text("Game settings", style = DisplayStyle, fontSize = 22.sp, color = palette.text)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row {
                            Text("Puzzle size", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                            Text("$pickedGrid×$pickedGrid · ${pickedGrid * pickedGrid - 1} tiles", fontSize = 14.sp, color = palette.secondaryText)
                        }
                        (PuzzleState.MIN_SIZE..PuzzleState.MAX_SIZE).chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { n -> SizePill(n, n == pickedGrid, enabled = !gridLocked, Modifier.weight(1f)) { pickedGrid = n } }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (gridLocked) Text(GRID_LOCKED_NOTE, fontSize = 13.sp, color = palette.secondaryText)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                        SegmentedControl(
                            segments = listOf(
                                Segment(Appearance.LIGHT, "Light", Icons.Rounded.LightMode),
                                Segment(Appearance.DARK, "Dark", Icons.Rounded.DarkMode),
                                Segment(Appearance.SYSTEM, "System", Icons.Rounded.PhoneAndroid),
                            ),
                            selected = pickedAppearance,
                            onSelect = { pickedAppearance = it },
                            height = 46,
                        )
                    }

                    if (!gridLocked) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = palette.secondaryText, modifier = Modifier.size(18.dp))
                            Text("Changing size reshuffles the board and resets the timer.", fontSize = 13.sp, color = palette.secondaryText)
                        }
                    }
                    PrimaryButton("Apply", onClick = { onApply(pickedGrid, pickedAppearance) })
                }
            }
        }
    }
}

@Composable
private fun SizePill(n: Int, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(52.dp)
            .alpha(if (enabled || selected) 1f else 0.45f)
            .clip(shape)
            .background(if (selected) palette.accent else palette.track)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text("$n×$n", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (selected) palette.onAccent else palette.text)
    }
}

/** Result card. [title] e.g. "Solved!", "You win!", "Mia won". */
@Composable
fun ResultCard(
    visible: Boolean,
    title: String,
    lines: List<String>,
    highlight: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    celebrate: Boolean = false,
) {
    Overlay(visible, onDismiss = null) {
        val palette = LocalPalette.current
        if (celebrate) Confetti(Modifier.fillMaxSize())
        GlassSurface(Modifier.align(Alignment.Center).widthIn(max = 420.dp).fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(30.dp), solid = true) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (celebrate) {
                    Text(title, style = DisplayStyle.copy(brush = CandyCta.titleBrush), fontSize = 40.sp, textAlign = TextAlign.Center)
                } else {
                    Text(title, style = DisplayStyle, fontSize = 28.sp, color = palette.text, textAlign = TextAlign.Center)
                }
                lines.forEach { Text(it, fontSize = 17.sp, color = palette.secondaryText, textAlign = TextAlign.Center) }
                if (highlight != null) Text(highlight, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.accent)
                Spacer(Modifier.height(4.dp))
                PrimaryButton(primaryLabel, onPrimary)
                GlassButton(secondaryLabel, onSecondary)
            }
        }
    }
}

/** Short message at the bottom with an optional action. */
@Composable
fun BoxScope.MessageToast(text: String?, actionLabel: String? = null, onAction: () -> Unit = {}, onDismiss: () -> Unit) {
    AnimatedVisibility(text != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp), enter = fadeIn(), exit = fadeOut()) {
        val palette = LocalPalette.current
        GlassSurface(Modifier.widthIn(max = 480.dp).clickable(onClick = onDismiss), shape = RoundedCornerShape(18.dp), solid = true) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text.orEmpty(), Modifier.weight(1f, fill = false), fontSize = 14.sp, color = palette.text)
                if (actionLabel != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(actionLabel, Modifier.clickable(role = Role.Button, onClick = onAction).padding(6.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.accent)
                }
            }
        }
    }
}

/** Falling candy-colored confetti, drawn behind the win card. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val pieces = remember { List(CONFETTI_COUNT) { i -> ConfettiPiece(i) } }
    val time by rememberInfiniteTransition(label = "confetti").animateFloat(
        0f, 1f, infiniteRepeatable(tween(CONFETTI_LOOP_MS, easing = LinearEasing)), label = "confetti-t",
    )
    Canvas(modifier) {
        pieces.forEach { p ->
            val t = (time * p.speed + p.phase) % 1f
            val x = p.x * size.width + kotlin.math.sin((t * 6.28f) + p.phase * 10f) * 18.dp.toPx()
            val y = -20.dp.toPx() + t * (size.height + 40.dp.toPx())
            rotate(t * 540f + p.phase * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = palette.sprinkles[p.colorIndex % palette.sprinkles.size],
                    topLeft = Offset(x - 4.dp.toPx(), y - 6.dp.toPx()),
                    size = Size(if (p.colorIndex % 2 == 0) 8.dp.toPx() else 6.dp.toPx(), if (p.colorIndex % 3 == 0) 14.dp.toPx() else 9.dp.toPx()),
                )
            }
        }
    }
}

private class ConfettiPiece(index: Int) {
    private val random = kotlin.random.Random(index * 31 + 7)
    val x = random.nextFloat()
    val phase = random.nextFloat()
    val speed = 0.7f + random.nextFloat() * 0.6f
    val colorIndex = index
}

private const val CONFETTI_COUNT = 36
private const val CONFETTI_LOOP_MS = 4_000
