package com.lexterlabra.puzzle.ui.landing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.APP_NAME
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.Players
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.board.BoardView
import com.lexterlabra.puzzle.ui.components.GlassButton
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.Segment
import com.lexterlabra.puzzle.ui.components.SegmentedControl
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.theme.CandyCta
import com.lexterlabra.puzzle.ui.theme.DisplayStyle
import com.lexterlabra.puzzle.ui.theme.LocalPalette

private val PreviewMine = PuzzleState(4, listOf(5, 1, 2, 4, 9, 6, 3, 8, 13, 10, 7, 11, 0, 14, 15, 12))
private val PreviewRival = PuzzleState(4, listOf(1, 2, 3, 0, 4, 5, 6, 7, 8, 9, 11, 10, 12, 13, 14, 15))

/**
 * Race-first landing that fits on one phone screen (it only scrolls in landscape or on very small windows): racing a friend nearby is the hero and solo play
 * is the quieter second option. Photos are picked in the lobby or the solo game.
 */
@Composable
fun LandingScreen(onPlay: (Players, Mode) -> Unit, onLeaderboard: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(Mode.TIME_BASED) }
    val scenic = scenicImage()
    val palette = LocalPalette.current

    ScreenScaffold(scenic) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 560.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SWEET & OFFLINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, color = palette.secondaryText)
                    Text(APP_NAME, style = DisplayStyle, fontSize = 30.sp, color = palette.text)
                }
                GlassIconButton(Icons.Rounded.EmojiEvents, "Leaderboard", onLeaderboard, tint = palette.gold)
            }

            SegmentedControl(
                segments = listOf(Segment(Mode.TIME_BASED, "Time attack", Icons.Rounded.Timer), Segment(Mode.FREE, "Free play", Icons.Rounded.AllInclusive)),
                selected = mode,
                onSelect = { mode = it },
            )

            RaceCard(scenic) { onPlay(Players.P2P, mode) }
            GlassButton("Play solo", onClick = { onPlay(Players.SINGLE, mode) }, icon = Icons.Rounded.Person)
            Text("Pick your own photo in the lobby or before a solo game.", Modifier.fillMaxWidth(), fontSize = 13.sp, color = palette.secondaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun RaceCard(image: ImageBitmap, onFindRival: () -> Unit) {
    val palette = LocalPalette.current
    val pulse = rememberInfiniteTransition(label = "vs")
    val vsScale by pulse.animateFloat(1f, 1.08f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "vs-scale")
    val dot by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot")

    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(palette.track).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(palette.accent.copy(alpha = dot)))
                    Text("  Play nearby", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                }
                Text("WI-FI · NO INTERNET", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = palette.secondaryText, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Column {
                Text("Race a friend", style = DisplayStyle, fontSize = 25.sp, color = palette.text)
                Text("Same photo, same shuffle. First to finish wins.", fontSize = 15.sp, color = palette.secondaryText)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MiniBoard(PreviewMine, image, palette.accent, "YOU", alpha = 1f)
                Box(
                    Modifier
                        .size(56.dp)
                        .scale(vsScale)
                        .shadow(10.dp, CircleShape, spotColor = CandyCta.bottom)
                        .clip(CircleShape)
                        .background(CandyCta.brush),
                    contentAlignment = Alignment.Center,
                ) { Text("VS", style = DisplayStyle, fontSize = 18.sp, color = Color.White) }
                MiniBoard(PreviewRival, image, palette.opponentAccent, "RIVAL", alpha = 0.85f)
            }
            PrimaryButton("Find a rival", onClick = onFindRival, icon = Icons.Rounded.Wifi)
        }
    }
}

@Composable
private fun MiniBoard(state: PuzzleState, image: ImageBitmap, ring: Color, label: String, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(132.dp)
                .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = ring, spotColor = ring)
                .clip(RoundedCornerShape(16.dp))
                .background(LocalPalette.current.glassTintSolid)
                .border(2.5.dp, ring, RoundedCornerShape(16.dp))
                .padding(6.dp),
        ) {
            BoardView(state, image, onTileTap = {}, interactive = false, modifier = Modifier.alpha(alpha))
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = ring)
    }
}
