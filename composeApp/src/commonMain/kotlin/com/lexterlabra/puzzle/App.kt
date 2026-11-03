package com.lexterlabra.puzzle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.data.ScoreRepository
import com.lexterlabra.puzzle.data.ScoreStore
import com.lexterlabra.puzzle.data.SettingsRepository
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.data.createDataStore
import com.lexterlabra.puzzle.game.Players
import com.lexterlabra.puzzle.platform.PlatformBackHandler
import com.lexterlabra.puzzle.ui.game.GameScreen
import com.lexterlabra.puzzle.ui.game.GameViewModel
import com.lexterlabra.puzzle.ui.landing.LandingScreen
import com.lexterlabra.puzzle.ui.leaderboard.LeaderboardScreen
import com.lexterlabra.puzzle.ui.leaderboard.LeaderboardViewModel
import com.lexterlabra.puzzle.ui.nav.Navigator
import com.lexterlabra.puzzle.ui.nav.Screen
import com.lexterlabra.puzzle.ui.theme.PuzzleTheme

const val APP_NAME = "Slide Puzzle"

/** Hand-wired dependencies, one instance per process. */
class AppContainer(val settings: SettingsStore, val scores: ScoreStore)

object AppGraph {
    val container: AppContainer by lazy {
        val store = createDataStore()
        AppContainer(SettingsRepository(store), ScoreRepository(store))
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
            onLeaderboard = { navigator.push(Screen.Leaderboard) },
        )
        Screen.Leaderboard -> LeaderboardScreen(
            viewModel = viewModel { LeaderboardViewModel(container.scores) },
            onBack = { navigator.pop() },
        )
        is Screen.Game -> GameScreen(
            viewModel = viewModel { GameViewModel(screen.mode, container.settings, container.scores) },
            onBack = { navigator.pop() },
            onHome = { navigator.popToRoot() },
            settings = container.settings,
        )
        is Screen.Lobby -> TODO("Task 13")
        Screen.Race -> TODO("Task 13")
    }
}
