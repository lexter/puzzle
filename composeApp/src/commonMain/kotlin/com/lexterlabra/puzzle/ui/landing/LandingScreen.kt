package com.lexterlabra.puzzle.ui.landing

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.APP_NAME
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.Players
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.board.BoardView
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.SectionLabel
import com.lexterlabra.puzzle.ui.components.Segment
import com.lexterlabra.puzzle.ui.components.SegmentedControl
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.theme.LocalPalette

@Composable
fun LandingScreen(onPlay: (Players, Mode) -> Unit, onLeaderboard: () -> Unit) {
    var players by rememberSaveable { mutableStateOf(Players.SINGLE) }
    var mode by rememberSaveable { mutableStateOf(Mode.TIME_BASED) }
    val scenic = scenicImage()
    val preview = remember { PuzzleEngine.slide(PuzzleEngine.solved(4), 14) }
    val palette = LocalPalette.current

    ScreenScaffold(scenic) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 560.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OFFLINE · PLAY SOLO OR NEARBY", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = palette.secondaryText)
                    Text(APP_NAME, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = palette.text)
                }
                GlassIconButton(Icons.Rounded.EmojiEvents, "Leaderboard", onLeaderboard)
            }

            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                BoardView(
                    preview, scenic, onTileTap = {},
                    modifier = Modifier.padding(18.dp).fillMaxWidth().align(Alignment.Center),
                    interactive = false,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Players")
                SegmentedControl(
                    segments = listOf(Segment(Players.SINGLE, "Single", Icons.Rounded.Person), Segment(Players.P2P, "Peer to Peer", Icons.Rounded.Group)),
                    selected = players,
                    onSelect = { players = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeCard(Icons.Rounded.Timer, "Time-based", "Stopwatch, ranked on the leaderboard", mode == Mode.TIME_BASED, Modifier.weight(1f)) { mode = Mode.TIME_BASED }
                    ModeCard(Icons.Rounded.AllInclusive, "Free play", "No timer, play at your pace", mode == Mode.FREE, Modifier.weight(1f)) { mode = Mode.FREE }
                }
            }

            PrimaryButton("Play", onClick = { onPlay(players, mode) }, icon = Icons.Rounded.PlayArrow)
        }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    GlassSurface(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, palette.accent, shape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick),
        shape = shape,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) palette.accent else palette.text, modifier = Modifier.size(26.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
            Text(subtitle, fontSize = 13.sp, color = palette.secondaryText)
        }
    }
}
