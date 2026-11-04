package com.lexterlabra.puzzle.ui.game

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.lexterlabra.puzzle.ui.components.Avatar
import com.lexterlabra.puzzle.ui.components.TugOfWar
import com.lexterlabra.puzzle.ui.theme.CandyCta
import com.lexterlabra.puzzle.ui.theme.DisplayStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.game.formatElapsed
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.image.PuzzleImage
import com.lexterlabra.puzzle.p2p.LossReason
import com.lexterlabra.puzzle.p2p.MatchController
import com.lexterlabra.puzzle.p2p.MatchNotice
import com.lexterlabra.puzzle.p2p.MatchPhase
import com.lexterlabra.puzzle.p2p.MatchSnapshot
import com.lexterlabra.puzzle.p2p.Role
import com.lexterlabra.puzzle.platform.rememberHaptics
import com.lexterlabra.puzzle.ui.board.BoardView
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ProgressTrack
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.layout.LayoutMode
import com.lexterlabra.puzzle.ui.layout.layoutModeFor
import com.lexterlabra.puzzle.ui.layout.rememberFoldInfo
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import com.lexterlabra.puzzle.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val MIRROR_ALPHA = 0.7f
private val MIRROR_MAX_SIDE_PANEL = 200.dp
private const val RIVAL_CLOSE_TILES = 4
private val MIRROR_WIDTH_COMPACT = 100.dp

/** Peer-to-peer race. [onLeave] must end the match (MatchHub.end) and navigate away. */
@Composable
fun RaceGameScreen(controller: MatchController, settings: SettingsStore, onLeave: () -> Unit) {
    val match by controller.state.collectAsState()
    val appearance by settings.appearance.collectAsState(Appearance.SYSTEM)
    val scenic = scenicImage()
    val custom = remember(match.imageBytes) { match.imageBytes?.let { PuzzleImage.prepare(it).getOrNull() } }
    val image = custom ?: scenic
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val config = match.config
    val grid = config?.grid ?: PuzzleState.DEFAULT_SIZE
    val peer = match.peerName ?: "Opponent"
    val timed = config?.mode != Mode.FREE

    val elapsed by produceState(0L, match.phase, match.mySolvedMs) {
        while ((match.phase == MatchPhase.RACING || match.phase == MatchPhase.SOLO) && match.mySolvedMs == null) {
            value = controller.elapsedNow()
            delay(100)
        }
        value = match.mySolvedMs ?: controller.elapsedNow()
    }
    val countdown by produceState(0L, match.phase) {
        while (match.phase == MatchPhase.COUNTDOWN) {
            value = controller.millisUntilStart()
            delay(50)
        }
        value = 0L
    }
    LaunchedEffect(match.phase) { if (match.phase == MatchPhase.FINISHED && match.result?.iWon == true) haptics.success() }
    LaunchedEffect(match.notice) {
        if (match.notice != null) { delay(4_000); controller.dismissNotice() }
    }

    val myBoard = match.me ?: PuzzleEngine.solved(grid)
    val theirBoard = match.opponent ?: PuzzleEngine.solved(grid)

    ScreenScaffold(image) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = layoutModeFor(maxWidth.value.toInt(), maxHeight.value.toInt(), rememberFoldInfo())
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GameTopBar(onBack = onLeave, timerText = if (timed) formatElapsed(elapsed) else null, onSettings = { showSettings = true }, backLabel = "Leave match")
                StatusLine(peer, match, grid)

                val myBoardView: @Composable (Modifier) -> Unit = { modifier ->
                    GlassSurface(
                        modifier
                            .shadow(22.dp, RoundedCornerShape(28.dp), ambientColor = LocalPalette.current.accent, spotColor = LocalPalette.current.accent)
                            .border(3.dp, LocalPalette.current.accent, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        BoardView(
                            myBoard, image,
                            onTileTap = { haptics.tap(); controller.tapTile(it) },
                            modifier = Modifier.padding(12.dp),
                            interactive = (match.phase == MatchPhase.RACING || match.phase == MatchPhase.SOLO) && match.mySolvedMs == null,
                        )
                    }
                }
                val side: @Composable ColumnScope.() -> Unit = {
                    RaceProgress(myBoard, theirBoard, peer)
                    if (match.phase == MatchPhase.RACING && match.mySolvedMs == null) RivalCloseBanner(myBoard, theirBoard, peer)
                    PreRaceControls(controller.role, match, onStart = controller::startRace)
                }

                if (layout == LayoutMode.COMPACT) {
                    Column(Modifier.widthIn(max = 640.dp).align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // The opponent's board floats over the top-right corner of mine at 70% opacity.
                        BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                            val bounds = constraints
                            var drag by remember { mutableStateOf(Offset.Zero) }
                            var mirrorSize by remember { mutableStateOf(IntSize.Zero) }
                            myBoardView(Modifier.padding(top = 8.dp))
                            // Small, draggable copy of the rival's board, 70% opaque, starting over the top-right corner.
                            OpponentMirror(
                                theirBoard, image, peer,
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .width(MIRROR_WIDTH_COMPACT)
                                    .onSizeChanged { mirrorSize = it }
                                    .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, amount ->
                                            change.consume()
                                            val maxLeft = (bounds.maxWidth - mirrorSize.width).coerceAtLeast(0).toFloat()
                                            val maxDown = (bounds.maxHeight - mirrorSize.height).coerceAtLeast(0).toFloat()
                                            drag = Offset((drag.x + amount.x).coerceIn(-maxLeft, 0f), (drag.y + amount.y).coerceIn(0f, maxDown))
                                        }
                                    },
                                draggable = true,
                            )
                        }
                        side()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { myBoardView(Modifier) }
                        Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OpponentMirror(theirBoard, image, peer, Modifier.widthIn(max = MIRROR_MAX_SIDE_PANEL).align(Alignment.CenterHorizontally))
                            side()
                        }
                    }
                }
            }

            if (match.phase == MatchPhase.COUNTDOWN) VsCountdown(countdown, peer, image)
            MessageToast(
                text = when (match.notice) {
                    MatchNotice.IMAGE_FALLBACK -> "Couldn't send the photo, so you're racing on the scenic picture."
                    MatchNotice.REMATCH_REQUESTED -> "$peer wants a rematch."
                    null -> null
                },
                actionLabel = if (match.notice == MatchNotice.REMATCH_REQUESTED) "Rematch" else null,
                onAction = { controller.rematch(); controller.dismissNotice() },
                onDismiss = controller::dismissNotice,
            )
        }
    }

    val result = match.result
    ResultCard(
        visible = match.phase == MatchPhase.FINISHED && result != null,
        title = if (result?.iWon == true) "YOU WIN!" else "$peer won",
        celebrate = result?.iWon == true,
        lines = listOfNotNull(
            result?.let { if (timed) "Winning time ${formatElapsed(it.winnerTimeMs)}" else null },
            "${myBoard.moves} moves · ${grid}×$grid",
        ),
        highlight = null,
        primaryLabel = if (controller.role == Role.HOST) "Rematch" else "Ask for rematch",
        onPrimary = controller::rematch,
        secondaryLabel = "Leave",
        onSecondary = onLeave,
    )

    val raceStarted = match.raceStartedAt != null && match.me?.isSolved == false
    ResultCard(
        visible = match.phase == MatchPhase.LOST,
        title = when (match.lostReason) {
            LossReason.PEER_LEFT -> "$peer left"
            LossReason.VERSION_MISMATCH -> "Different app versions"
            else -> "Connection lost"
        },
        lines = listOf(
            if (match.lostReason == LossReason.VERSION_MISMATCH) "Both players need the same app version." else "This race can't continue.",
        ),
        highlight = null,
        primaryLabel = if (raceStarted) "Keep playing solo" else "Back to lobby",
        onPrimary = if (raceStarted) controller::continueSolo else onLeave,
        secondaryLabel = if (raceStarted) "Back to lobby" else "Home",
        onSecondary = onLeave,
    )

    ResultCard(
        visible = match.phase == MatchPhase.SOLO && match.me?.isSolved == true,
        title = "Solved!",
        lines = listOf("Unranked · ${formatElapsed(match.mySolvedMs ?: 0)} · ${myBoard.moves} moves"),
        highlight = null,
        primaryLabel = "Back to lobby",
        onPrimary = onLeave,
        secondaryLabel = "Home",
        onSecondary = onLeave,
    )

    SettingsSheet(
        visible = showSettings,
        gridSize = grid,
        appearance = appearance,
        gridLocked = controller.role != Role.HOST || match.phase == MatchPhase.RACING || match.phase == MatchPhase.COUNTDOWN,
        onApply = { newGrid, newAppearance ->
            showSettings = false
            if (newAppearance != appearance) scope.launch { settings.setAppearance(newAppearance) }
            val canReconfigure = controller.role == Role.HOST && config != null && newGrid != grid &&
                (match.phase == MatchPhase.CONFIGURED || match.phase == MatchPhase.FINISHED)
            if (canReconfigure) controller.configure(newGrid, config!!.mode, match.imageBytes)
        },
        onDismiss = { showSettings = false },
    )
}

@Composable
private fun StatusLine(peer: String, match: MatchSnapshot, grid: Int) {
    val palette = LocalPalette.current
    val connected = match.phase != MatchPhase.LOST && match.phase != MatchPhase.SOLO
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) Color(0xFF22C55E) else Color(0xFFEF4444)))
        Text(
            if (connected) "  Connected · $peer" else "  Offline · solo",
            Modifier.weight(1f), fontSize = 14.sp, color = palette.secondaryText,
        )
        Text("${grid}×$grid · Moves ${match.me?.moves ?: 0}", style = TabularNumbers.copy(fontWeight = FontWeight.Normal), fontSize = 14.sp, color = palette.secondaryText)
    }
}

/** The other player's live board, 70% opaque and not tappable. */
@Composable
fun OpponentMirror(board: PuzzleState, image: ImageBitmap, name: String, modifier: Modifier = Modifier, draggable: Boolean = false) {
    val palette = LocalPalette.current
    GlassSurface(
        modifier
            .alpha(MIRROR_ALPHA)
            .testTag("opponent-mirror")
            .semantics { contentDescription = if (draggable) "$name's board, live. Drag to move it." else "$name's board, live" }
            .border(3.dp, palette.opponentAccent, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        solid = true,
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (draggable) Box(Modifier.align(Alignment.CenterHorizontally).size(width = 28.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(palette.text.copy(alpha = 0.6f)))
            BoardView(board, image, onTileTap = {}, interactive = false)
            Row {
                Text(name, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = palette.opponentAccent, maxLines = 1)
                Text("${board.tilesInPlace}/${board.tileCount}", style = TabularNumbers, fontSize = 12.sp, color = palette.text)
            }
        }
    }
}

@Composable
private fun RaceProgress(me: PuzzleState, opponent: PuzzleState, peer: String) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        TugOfWar(
            youFraction = me.tilesInPlace.toFloat() / me.tileCount,
            rivalFraction = opponent.tilesInPlace.toFloat() / opponent.tileCount,
            youLabel = "YOU · ${me.tilesInPlace}/${me.tileCount}",
            rivalLabel = "${peer.uppercase()} · ${opponent.tilesInPlace}/${opponent.tileCount}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/** Nudge shown when the rival is ahead and close to finishing. */
@Composable
private fun RivalCloseBanner(me: PuzzleState, opponent: PuzzleState, peer: String) {
    val left = opponent.tileCount - opponent.tilesInPlace
    if (left == 0 || left > RIVAL_CLOSE_TILES || opponent.tilesInPlace <= me.tilesInPlace) return
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.opponentAccent.copy(alpha = 0.16f))
            .border(1.dp, palette.opponentAccent.copy(alpha = 0.55f), shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = palette.opponentAccent, modifier = Modifier.size(18.dp))
        Text("  $peer is $left ${if (left == 1) "tile" else "tiles"} from done. Speed up!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
    }
}

@Composable
private fun PreRaceControls(role: Role, match: MatchSnapshot, onStart: () -> Unit) {
    if (match.phase != MatchPhase.CONFIGURED) return
    if (role == Role.HOST) {
        PrimaryButton("Start race · 3-2-1", onClick = onStart, enabled = match.peerReady)
    } else {
        Text("Waiting for the host to start…", Modifier.padding(horizontal = 6.dp), fontSize = 14.sp, color = LocalPalette.current.secondaryText)
    }
}

/** Full-screen VS splash with the 3-2-1 in the middle. */
@Composable
private fun VsCountdown(remainingMs: Long, peer: String, image: ImageBitmap) {
    val palette = LocalPalette.current
    val seconds = ((remainingMs + 999) / 1000).coerceAtLeast(1)
    val pop by animateFloatAsState(if (remainingMs % 1000 > 700) 1.25f else 1f, label = "count-pop")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).semantics { liveRegion = LiveRegionMode.Assertive }) {
        Row(Modifier.align(Alignment.TopStart).padding(top = 80.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar("You", palette.accent, size = 64, glow = true)
            Column(Modifier.padding(start = 12.dp)) {
                Text("YOU", style = DisplayStyle, fontSize = 26.sp, color = palette.accent)
                Text("Same photo · same shuffle", fontSize = 13.sp, color = Color.White)
            }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(170.dp)
                .shadow(30.dp, CircleShape, ambientColor = CandyCta.top, spotColor = CandyCta.top)
                .clip(CircleShape)
                .background(palette.background)
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$seconds",
                Modifier.scale(pop),
                style = DisplayStyle.copy(brush = Brush.linearGradient(listOf(palette.accent, palette.opponentAccent))),
                fontSize = 88.sp,
            )
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(bottom = 120.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(end = 12.dp), horizontalAlignment = Alignment.End) {
                Text(peer.uppercase(), style = DisplayStyle, fontSize = 26.sp, color = palette.opponentAccent, maxLines = 1)
                Text("First to finish wins", fontSize = 13.sp, color = Color.White)
            }
            Avatar(peer, palette.opponentAccent, size = 64, glow = true)
        }
    }
}
