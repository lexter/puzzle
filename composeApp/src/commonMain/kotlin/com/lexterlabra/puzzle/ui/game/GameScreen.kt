package com.lexterlabra.puzzle.ui.game

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.formatElapsed
import com.lexterlabra.puzzle.glass.GlassSurface
import com.lexterlabra.puzzle.platform.openAppSettings
import com.lexterlabra.puzzle.platform.rememberHaptics
import com.lexterlabra.puzzle.platform.rememberImagePicker
import com.lexterlabra.puzzle.ui.board.BoardView
import com.lexterlabra.puzzle.ui.components.PrimaryButton
import com.lexterlabra.puzzle.ui.components.ScreenScaffold
import com.lexterlabra.puzzle.ui.components.SectionLabel
import com.lexterlabra.puzzle.ui.components.scenicImage
import com.lexterlabra.puzzle.ui.layout.LayoutMode
import com.lexterlabra.puzzle.ui.layout.layoutModeFor
import com.lexterlabra.puzzle.ui.layout.rememberFoldInfo
import com.lexterlabra.puzzle.ui.theme.LocalPalette
import com.lexterlabra.puzzle.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private const val TIMER_REFRESH_MS = 100L
private const val MESSAGE_MS = 4_000L

@Composable
fun GameScreen(viewModel: GameViewModel, settings: SettingsStore, onBack: () -> Unit, onHome: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val appearance by settings.appearance.collectAsState(Appearance.SYSTEM)
    val scenic = scenicImage()
    val image = state.customImage ?: scenic
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val picker = rememberImagePicker(viewModel::setPicture)
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val elapsed by produceState(state.elapsedMs, state.phase, state.pausedHidden) {
        while (state.phase == Phase.PLAYING && !state.pausedHidden) {
            value = viewModel.elapsedNow()
            delay(TIMER_REFRESH_MS)
        }
        value = viewModel.elapsedNow()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }
    LaunchedEffect(state.phase) { if (state.phase == Phase.WON) haptics.success() }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_MS)
            viewModel.dismissMessage()
        }
    }

    ScreenScaffold(image) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = layoutModeFor(maxWidth.value.toInt(), maxHeight.value.toInt(), rememberFoldInfo())
            val timerText = if (state.mode == Mode.TIME_BASED) formatElapsed(elapsed) else null
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GameTopBar(onBack, timerText, onSettings = { showSettings = true })
                val board: @Composable (Modifier) -> Unit = { modifier ->
                    GameBoard(state, image, modifier) { id ->
                        haptics.tap()
                        viewModel.tapTile(id)
                    }
                }
                val controls: @Composable ColumnScope.() -> Unit = {
                    InfoRow(state)
                    if (state.phase == Phase.READY) {
                        PictureRow(
                            customSelected = state.customImage != null,
                            cameraAvailable = picker.cameraAvailable,
                            onScenic = viewModel::useScenic,
                            onCamera = picker::takePhoto,
                            onGallery = picker::pickFromGallery,
                        )
                        PrimaryButton("Start", onClick = viewModel::start, icon = Icons.Rounded.PlayArrow, enabled = !state.loadingPhoto)
                    } else if (state.phase == Phase.PLAYING) {
                        Text(
                            "Tap any tile in the gap's row or column to slide it.",
                            Modifier.padding(horizontal = 6.dp),
                            fontSize = 13.sp,
                            color = LocalPalette.current.secondaryText,
                        )
                    }
                }
                if (layout == LayoutMode.COMPACT) {
                    Column(Modifier.widthIn(max = 640.dp).align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        board(Modifier.weight(1f, fill = false).align(Alignment.CenterHorizontally))
                        controls()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { board(Modifier) }
                        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = controls)
                    }
                }
            }
            if (state.loadingPhoto) CircularProgressIndicator(Modifier.align(Alignment.Center))
            MessageToast(
                text = state.message?.text,
                actionLabel = if (state.message == UserMessage.CameraDenied) "Settings" else null,
                onAction = { openAppSettings() },
                onDismiss = viewModel::dismissMessage,
            )
        }
    }

    ResultCard(
        visible = state.phase == Phase.WON,
        title = "Solved!",
        lines = listOfNotNull(
            if (state.mode == Mode.TIME_BASED) "Time ${formatElapsed(state.elapsedMs)}" else null,
            "${state.puzzle.moves} moves · ${state.puzzle.size}×${state.puzzle.size}",
        ),
        highlight = if (state.newBest) "New best!" else null,
        primaryLabel = "Play again",
        onPrimary = viewModel::playAgain,
        secondaryLabel = "Home",
        onSecondary = onHome,
    )

    SettingsSheet(
        visible = showSettings,
        gridSize = state.puzzle.size,
        appearance = appearance,
        gridLocked = false,
        onApply = { grid, newAppearance ->
            showSettings = false
            if (grid != state.puzzle.size) viewModel.setGridSize(grid)
            if (newAppearance != appearance) scope.launch { settings.setAppearance(newAppearance) }
        },
        onDismiss = { showSettings = false },
    )
}

@Composable
private fun InfoRow(state: GameUiState) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        val modeLabel = if (state.mode == Mode.TIME_BASED) "Time-based" else "Free play"
        Text("${state.puzzle.size}×${state.puzzle.size} · $modeLabel", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
        Text("Moves ${state.puzzle.moves}", style = TabularNumbers.copy(fontWeight = FontWeight.Normal), fontSize = 15.sp, color = palette.secondaryText)
    }
}

@Composable
private fun GameBoard(state: GameUiState, image: ImageBitmap, modifier: Modifier, onTap: (Int) -> Unit) {
    GlassSurface(modifier, shape = RoundedCornerShape(28.dp)) {
        Box(Modifier.padding(12.dp)) {
            BoardView(state.puzzle, image, onTileTap = onTap, interactive = state.phase == Phase.PLAYING)
            if (state.pausedHidden) {
                GlassSurface(Modifier.matchParentSize(), shape = RoundedCornerShape(16.dp), solid = true) {
                    Text("Paused", Modifier.align(Alignment.Center), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.text)
                }
            }
        }
    }
}

@Composable
fun PictureRow(customSelected: Boolean, cameraAvailable: Boolean, onScenic: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Picture")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PictureChip(Icons.Rounded.Landscape, "Scenic", selected = !customSelected, enabled = true, Modifier.weight(1f), onScenic)
            PictureChip(Icons.Rounded.PhotoCamera, "Camera", selected = false, enabled = cameraAvailable, Modifier.weight(1f), onCamera)
            PictureChip(Icons.Rounded.PhotoLibrary, "Gallery", selected = customSelected, enabled = true, Modifier.weight(1f), onGallery)
        }
    }
}
