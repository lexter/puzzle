package com.lexterlabra.puzzle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import com.lexterlabra.puzzle.ui.theme.TabularNumbers

/** 44 dp round glass button with an icon (back, settings, leaderboard). */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(22.dp), tint = LocalPalette.current.text)
    }
}

/** Main call to action (Play, Start, Apply). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(29.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(if (enabled) 10.dp else 0.dp, shape, spotColor = palette.accent)
            .clip(shape)
            .background(palette.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
            Box(Modifier.width(10.dp))
        }
        Text(text, color = palette.onAccent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Secondary text button on glass ("Home", "Back to lobby"). */
@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
    ) {
        Text(text, Modifier.align(Alignment.Center), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.text)
    }
}

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Glass segmented control (Single/P2P, Host/Join, Light/Dark/System, grid tabs). */
@Composable
fun <T> SegmentedControl(segments: List<Segment<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, height: Int = 48, enabled: Boolean = true) {
    val palette = LocalPalette.current
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            segments.forEach { segment ->
                val isSelected = segment.value == selected
                Row(
                    Modifier
                        .weight(1f)
                        .height(height.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) palette.selectedFill else Color.Transparent)
                        .alpha(if (enabled || isSelected) 1f else 0.45f)
                        .selectable(selected = isSelected, enabled = enabled, role = Role.Tab) { onSelect(segment.value) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (isSelected && palette.isDark) palette.onAccent else palette.text
                    segment.icon?.let {
                        Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        Box(Modifier.width(6.dp))
                    }
                    Text(segment.label, color = color, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

/** Stopwatch pill shown at the top of time-based games. */
@Composable
fun TimerPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    GlassSurface(modifier.height(44.dp), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
            Text(text, style = TabularNumbers, fontSize = 22.sp, color = palette.text)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.secondaryText)
}

/** Thin progress bar for race progress. */
@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}
