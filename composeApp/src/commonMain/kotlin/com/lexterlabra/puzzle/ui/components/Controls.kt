package com.lexterlabra.puzzle.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.theme.CandyCta
import com.lexterlabra.puzzle.ui.theme.DisplayStyle
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import com.lexterlabra.puzzle.ui.theme.TabularNumbers

private val ButtonFace = 56.dp
private val ButtonDepth = 5.dp

/** 48 dp rounded-square frosted button with an icon (back, settings, leaderboard). */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null) {
    val shape = RoundedCornerShape(16.dp)
    GlassSurface(
        modifier
            .size(48.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        shape = shape,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(22.dp), tint = tint ?: LocalPalette.current.text)
    }
}

/**
 * Glossy candy button (Play, Find a rival, Start, Apply): strawberry → tangerine face with a
 * shine strip, sitting on a darker base that it sinks into while pressed.
 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed && enabled) 1.dp else ButtonDepth, tween(90), label = "candy-press")
    Box(modifier.fillMaxWidth().height(ButtonFace + ButtonDepth).alpha(if (enabled) 1f else 0.5f)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(ButtonFace).clip(shape).background(CandyCta.base))
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = ButtonDepth - lift)
                .fillMaxWidth()
                .height(ButtonFace)
                .clip(shape)
                .background(CandyCta.brush)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .fillMaxWidth(0.88f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent))),
            )
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Box(Modifier.width(10.dp))
                }
                Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** Secondary frosted button ("Home", "Play solo", "Back to lobby"). */
@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color? = null) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    GlassSurface(modifier.fillMaxWidth().height(52.dp).clip(shape).clickable(role = Role.Button, onClick = onClick), shape = shape) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTint ?: palette.text, modifier = Modifier.size(20.dp))
                Box(Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
        }
    }
}

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Segmented control (Time attack/Free play, Join/Host, Light/Dark/System, grid tabs). */
@Composable
fun <T> SegmentedControl(segments: List<Segment<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, height: Int = 46, enabled: Boolean = true) {
    val palette = LocalPalette.current
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            segments.forEach { segment ->
                val isSelected = segment.value == selected
                Row(
                    Modifier
                        .weight(1f)
                        .height(height.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) palette.selectedFill else Color.Transparent)
                        .alpha(if (enabled || isSelected) 1f else 0.45f)
                        .selectable(selected = isSelected, enabled = enabled, role = Role.Tab) { onSelect(segment.value) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (isSelected) palette.selectedText else palette.text
                    segment.icon?.let {
                        Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        Box(Modifier.width(6.dp))
                    }
                    Text(segment.label, color = color, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

/** Stopwatch pill shown at the top of time-based games. */
@Composable
fun TimerPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    GlassSurface(modifier.height(48.dp).border(1.5.dp, palette.accent.copy(alpha = 0.6f), shape), shape = shape) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
            Text(text, style = TabularNumbers.merge(DisplayStyle), fontSize = 21.sp, color = palette.text)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(horizontal = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = LocalPalette.current.secondaryText)
}

/** Thin progress bar. */
@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/**
 * Race bar: you fill from the left, the rival from the right, both toward a finish line in
 * the middle. Each side reaching the middle means that player's picture is complete.
 */
@Composable
fun TugOfWar(youFraction: Float, rivalFraction: Float, youLabel: String, rivalLabel: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val you = palette.accent
    val rival = palette.opponentAccent
    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(youLabel, Modifier.weight(1f), style = TabularNumbers, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = you, maxLines = 1)
            Text("FINISH", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = palette.secondaryText)
            Text(rivalLabel, Modifier.weight(1f), style = TabularNumbers, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = rival, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(palette.track)) {
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(
                        Modifier.fillMaxWidth(youFraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                            .background(Brush.horizontalGradient(listOf(you.copy(alpha = 0.45f), you))),
                    )
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                    Box(
                        Modifier.fillMaxWidth(rivalFraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                            .background(Brush.horizontalGradient(listOf(rival, rival.copy(alpha = 0.45f)))),
                    )
                }
            }
            Box(Modifier.align(Alignment.Center).width(3.dp).fillMaxHeight().background(palette.text))
        }
    }
}

/** Round initial badge for a player. */
@Composable
fun Avatar(name: String, color: Color, modifier: Modifier = Modifier, size: Int = 42, glow: Boolean = false) {
    Box(
        modifier
            .size(size.dp)
            .then(if (glow) Modifier.shadow(14.dp, CircleShape, ambientColor = color, spotColor = color) else Modifier)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).uppercase().ifEmpty { "?" }, style = DisplayStyle, fontSize = (size * 0.4f).sp, color = if (color.luminance() < 0.35f) Color.White else Color(0xFF1F0D1A))
    }
}
