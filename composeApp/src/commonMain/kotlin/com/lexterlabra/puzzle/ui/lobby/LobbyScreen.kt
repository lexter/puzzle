package com.lexterlabra.puzzle.ui.lobby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.p2p.FoundHost
import com.lexterlabra.puzzle.p2p.MatchPhase
import com.lexterlabra.puzzle.platform.openAppSettings
import com.lexterlabra.puzzle.platform.rememberImagePicker
import com.lexterlabra.puzzle.ui.components.GlassButton
import com.lexterlabra.puzzle.ui.components.GlassIconButton
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.SectionLabel
import com.lexterlabra.puzzle.ui.components.Segment
import com.lexterlabra.puzzle.ui.components.SegmentedControl
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.game.MessageToast
import com.lexterlabra.puzzle.ui.game.PictureChip
import com.lexterlabra.puzzle.ui.game.UserMessage
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import kotlinx.coroutines.delay

const val SAME_NETWORK_NOTE = "Both devices must be on the same Wi-Fi or one phone's hotspot. No internet needed."
const val NO_HOSTS_TEXT = "No games found yet. Same Wi-Fi or hotspot? Local network access allowed?"

@Composable
fun LobbyScreen(viewModel: LobbyViewModel, onBack: () -> Unit, onRaceStarting: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val controller by viewModel.controller.collectAsState()
    val match = controller?.state?.collectAsState()?.value
    val scenic = scenicImage()
    val picker = rememberImagePicker(viewModel::setPicture)
    val palette = LocalPalette.current

    LaunchedEffect(match?.phase) {
        if (match?.phase == MatchPhase.COUNTDOWN || match?.phase == MatchPhase.RACING) {
            viewModel.handOff()
            onRaceStarting()
        }
        if (match?.phase == MatchPhase.LOST) viewModel.onMatchLost()
    }
    LaunchedEffect(state.message) {
        if (state.message != null) { delay(4_000); viewModel.dismissMessage() }
    }

    ScreenScaffold(state.customImage ?: scenic) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 560.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Text("Play nearby", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.text)
            }
            SegmentedControl(
                listOf(Segment(LobbyTab.HOST, "Host"), Segment(LobbyTab.JOIN, "Join")),
                selected = state.tab,
                onSelect = viewModel::selectTab,
                height = 44,
                enabled = match == null || match.phase == MatchPhase.LOST,
            )

            if (state.tab == LobbyTab.HOST) {
                HostCard(state, scenic, match?.peerName, match?.phase, match?.peerReady == true, picker.cameraAvailable, viewModel, picker::takePhoto, picker::pickFromGallery)
            } else {
                JoinList(state, viewModel::join, viewModel::retry, waitingForHost = match != null)
            }

            state.error?.let {
                Text(it, color = palette.text, fontSize = 14.sp)
                GlassButton("Try again", viewModel::retry)
            }
            Text(SAME_NETWORK_NOTE, Modifier.fillMaxWidth(), fontSize = 13.sp, color = palette.secondaryText, textAlign = TextAlign.Center)
        }
        MessageToast(
            text = state.message?.text,
            actionLabel = if (state.message == UserMessage.CameraDenied) "Settings" else null,
            onAction = { openAppSettings() },
            onDismiss = viewModel::dismissMessage,
        )
    }
}

@Composable
private fun HostCard(
    state: LobbyUiState,
    scenic: androidx.compose.ui.graphics.ImageBitmap,
    peerName: String?,
    phase: MatchPhase?,
    peerReady: Boolean,
    cameraAvailable: Boolean,
    viewModel: LobbyViewModel,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    val palette = LocalPalette.current
    val configured = phase == MatchPhase.CONFIGURED
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel("Match you're hosting")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(state.customImage ?: scenic, contentDescription = "Puzzle picture", contentScale = ContentScale.Crop, modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val modeLabel = if (state.mode == Mode.TIME_BASED) "Time-based" else "Free play"
                    Text("${state.grid}×${state.grid} · $modeLabel", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                    Text("Same shuffle for both players", fontSize = 14.sp, color = palette.secondaryText)
                }
            }
            if (!configured) {
                SegmentedControl(
                    (PuzzleState.MIN_SIZE..PuzzleState.MAX_SIZE).map { Segment(it, "$it") },
                    selected = state.grid,
                    onSelect = viewModel::setGrid,
                    height = 38,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PictureChip(Icons.Rounded.Landscape, "Scenic", state.customImage == null, true, Modifier.weight(1f), viewModel::useScenic)
                    PictureChip(Icons.Rounded.PhotoCamera, "Camera", false, cameraAvailable, Modifier.weight(1f), onCamera)
                    PictureChip(Icons.Rounded.PhotoLibrary, "Gallery", state.customImage != null, true, Modifier.weight(1f), onGallery)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.accent.copy(alpha = 0.12f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                Text("Visible as “${state.myName}” on this network", fontSize = 14.sp, color = palette.text)
            }
        }
    }

    when {
        peerName == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Waiting for a player to join…", color = palette.secondaryText, fontSize = 15.sp)
        }
        !configured -> PlayerRow(peerName, "wants to join", actionLabel = "Accept", onAction = viewModel::accept, secondary = "Decline" to viewModel::decline)
        else -> {
            PlayerRow(peerName, if (peerReady) "ready" else "getting ready…", actionLabel = null, onAction = {})
            PrimaryButton("Start race · 3-2-1", onClick = viewModel::startRace, enabled = peerReady)
        }
    }
}

@Composable
private fun JoinList(state: LobbyUiState, onJoin: (FoundHost) -> Unit, onRetry: () -> Unit, waitingForHost: Boolean) {
    val palette = LocalPalette.current
    if (waitingForHost) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Connected. Waiting for the host to start…", color = palette.secondaryText, fontSize = 15.sp)
        }
        return
    }
    SectionLabel("Games nearby")
    if (state.hosts.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Looking for games…", color = palette.secondaryText, fontSize = 15.sp)
        }
        if (state.noHostsHint) {
            Text(NO_HOSTS_TEXT, color = palette.text, fontSize = 14.sp)
            GlassButton("Retry", onRetry)
        }
    }
    state.hosts.forEach { host ->
        val busy = state.joining == host.name
        PlayerRow(host.name, if (busy) "connecting…" else "hosting a game", actionLabel = if (busy) null else "Join", onAction = { onJoin(host) })
    }
}

@Composable
private fun PlayerRow(name: String, status: String, actionLabel: String?, onAction: () -> Unit, secondary: Pair<String, () -> Unit>? = null) {
    val palette = LocalPalette.current
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(palette.opponentAccent), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF12203A))
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.text, maxLines = 1)
                Text(status, fontSize = 13.sp, color = palette.secondaryText)
            }
            secondary?.let { (label, action) ->
                Text(label, Modifier.clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = action).padding(horizontal = 12.dp, vertical = 12.dp), color = palette.secondaryText, fontWeight = FontWeight.SemiBold)
            }
            if (actionLabel != null) {
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(palette.accent).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(actionLabel, color = palette.onAccent, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
