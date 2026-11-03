package com.lexterlabra.puzzle.ui.landing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.Players
import com.lexterlabra.puzzle.ui.theme.PuzzleTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LandingScreenTest {
    @Test
    fun playSingleTimeBasedByDefault() = runComposeUiTest {
        var played: Pair<Players, Mode>? = null
        setContent { PuzzleTheme(Appearance.LIGHT) { LandingScreen(onPlay = { p, m -> played = p to m }, onLeaderboard = {}) } }
        onNodeWithText("Play").performScrollTo().performClick()
        assertEquals(Players.SINGLE to Mode.TIME_BASED, played)
    }

    @Test
    fun playP2PFreePlay() = runComposeUiTest {
        var played: Pair<Players, Mode>? = null
        setContent { PuzzleTheme(Appearance.LIGHT) { LandingScreen(onPlay = { p, m -> played = p to m }, onLeaderboard = {}) } }
        onNodeWithText("Peer to Peer").performClick()
        onNodeWithText("Free play").performScrollTo().performClick()
        onNodeWithText("Play").performScrollTo().performClick()
        assertEquals(Players.P2P to Mode.FREE, played)
    }

    @Test
    fun trophyOpensLeaderboard() = runComposeUiTest {
        var opened = false
        setContent { PuzzleTheme(Appearance.DARK) { LandingScreen(onPlay = { _, _ -> }, onLeaderboard = { opened = true }) } }
        onNodeWithContentDescription("Leaderboard").performClick()
        assertTrue(opened)
    }
}
