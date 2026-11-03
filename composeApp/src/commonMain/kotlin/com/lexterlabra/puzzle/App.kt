package com.lexterlabra.puzzle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.data.ScoreRepository
import com.lexterlabra.puzzle.data.ScoreStore
import com.lexterlabra.puzzle.data.SettingsRepository
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.data.createDataStore
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.Players
import com.lexterlabra.puzzle.image.PhotoSelection
import com.lexterlabra.puzzle.ui.photo.PhotoScreen
import com.lexterlabra.puzzle.ui.photo.PhotoViewModel
import com.lexterlabra.puzzle.platform.PlatformBackHandler
import com.lexterlabra.puzzle.p2p.MatchHub
import com.lexterlabra.puzzle.ui.game.GameScreen
import com.lexterlabra.puzzle.ui.game.RaceGameScreen
import com.lexterlabra.puzzle.ui.lobby.LobbyScreen
import com.lexterlabra.puzzle.ui.lobby.LobbyViewModel
import com.lexterlabra.puzzle.ui.game.GameViewModel
import com.lexterlabra.puzzle.ui.landing.LandingScreen
import com.lexterlabra.puzzle.ui.leaderboard.LeaderboardScreen
import com.lexterlabra.puzzle.ui.leaderboard.LeaderboardViewModel
import com.lexterlabra.puzzle.ui.nav.Navigator
import com.lexterlabra.puzzle.ui.nav.Screen
import com.lexterlabra.puzzle.ui.theme.PuzzleTheme

const val APP_NAME = "Slide Puzzle"

/** Hand-wired dependencies, one instance per process. */
class AppContainer(val settings: SettingsStore, val scores: ScoreStore, val matchHub: MatchHub, val photos: PhotoSelection = PhotoSelection())

object AppGraph {
    val container: AppContainer by lazy {
        val store = createDataStore()
        AppContainer(SettingsRepository(store), ScoreRepository(store), MatchHub())
    }
}

@Composable
fun App(container: AppContainer = AppGraph.container) {
    val appearance by container.settings.appearance.collectAsState(Appearance.SYSTEM)
    PuzzleTheme(appearance) {
        val navigator: Navigator = viewModel { Navigator() }
        val entry = navigator.current
        PlatformBackHandler(enabled = navigator.canPop) { navigator.pop() }
        CompositionLocalProvider(LocalViewModelStoreOwner provides entry) {
            key(entry) { ScreenContent(entry.screen, navigator, container) }
        }
    }
}

@Composable
private fun ScreenContent(screen: Screen, navigator: Navigator, container: AppContainer) {
    when (screen) {
        Screen.Landing -> LandingScreen(
            onPlay = { players, mode ->
                navigator.push(if (players == Players.SINGLE) Screen.Game(mode) else Screen.Lobby(mode))
            },
            onPhoto = { mode -> navigator.push(Screen.Photo(mode)) },
            onLeaderboard = { navigator.push(Screen.Leaderboard) },
        )
        is Screen.Photo -> PhotoScreen(
            viewModel = viewModel { PhotoViewModel(container.settings, container.photos) },
            onBack = { navigator.pop() },
            onRace = { navigator.replace(Screen.Lobby(screen.mode)) },
            onSolo = { navigator.replace(Screen.Game(screen.mode)) },
        )
        Screen.Leaderboard -> LeaderboardScreen(
            viewModel = viewModel { LeaderboardViewModel(container.scores) },
            onBack = { navigator.pop() },
        )
        is Screen.Game -> GameScreen(
            viewModel = viewModel { GameViewModel(screen.mode, container.settings, container.scores, initialImage = container.photos.chosen.value?.image) },
            onBack = { navigator.pop() },
            onHome = { navigator.popToRoot() },
            settings = container.settings,
        )
        is Screen.Lobby -> LobbyScreen(
            viewModel = viewModel { LobbyViewModel(screen.mode, container.settings, container.matchHub, initialPhoto = container.photos.chosen.value) },
            onBack = { navigator.pop() },
            onRaceStarting = { navigator.replace(Screen.Race) },
        )
        Screen.Race -> {
            val controller by container.matchHub.controller.collectAsState()
            val leave = {
                val mode = controller?.state?.value?.config?.mode ?: Mode.TIME_BASED
                container.matchHub.end()
                navigator.replace(Screen.Lobby(mode))
            }
            PlatformBackHandler(onBack = leave)
            val current = controller
            if (current == null) LaunchedEffect(Unit) { navigator.pop() }
            else RaceGameScreen(current, container.settings, onLeave = leave)
        }
    }
}
