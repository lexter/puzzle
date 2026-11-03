package com.lexterlabra.puzzle.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.game.formatElapsed
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.Segment
import com.lexterlabra.puzzle.ui.components.SegmentedControl
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import com.lexterlabra.puzzle.ui.theme.TabularNumbers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

const val EMPTY_LEADERBOARD = "No times yet. Finish a time-based game to set one."

private val sizes = (PuzzleState.MIN_SIZE..PuzzleState.MAX_SIZE).map { Segment(it, "$it×$it") }
private val medals = listOf(Color(0xFFFFD66B), Color(0xFFD9E1EC), Color(0xFFF2C2A0))

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel, onBack: () -> Unit) {
    val selected by viewModel.selectedSize.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val palette = LocalPalette.current

    ScreenScaffold(scenicImage()) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Text("Leaderboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.text)
            }
            SegmentedControl(sizes, selected, viewModel::select, height = 44)
            Text("Top 10 · $selected×$selected · time-based", Modifier.padding(horizontal = 8.dp), fontSize = 13.sp, color = palette.secondaryText)
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                if (rows.isEmpty()) {
                    Text(EMPTY_LEADERBOARD, Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = palette.secondaryText)
                } else {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        rows.forEachIndexed { index, score -> ScoreRow(index + 1, score) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(rank: Int, score: Score) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(medals.getOrNull(rank - 1) ?: palette.track), contentAlignment = Alignment.Center) {
            Text("$rank", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (rank <= 3) Color(0xFF12203A) else palette.text)
        }
        Text(formatElapsed(score.timeMs), Modifier.weight(1f), style = TabularNumbers, fontSize = 18.sp, color = palette.text)
        Text("${score.moves} moves", Modifier.width(84.dp), style = TabularNumbers.copy(fontWeight = FontWeight.Normal), fontSize = 14.sp, color = palette.secondaryText)
        Text(shortDate(score.playedAt), Modifier.width(56.dp), textAlign = TextAlign.End, fontSize = 13.sp, color = palette.secondaryText)
    }
}

internal fun shortDate(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${date.dayOfMonth}"
}
