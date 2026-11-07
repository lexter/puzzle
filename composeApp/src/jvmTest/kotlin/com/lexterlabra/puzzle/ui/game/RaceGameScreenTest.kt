package com.lexterlabra.puzzle.ui.game

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.fakes.FakeConnection
import com.lexterlabra.puzzle.fakes.FakeSettingsStore
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.p2p.MatchController
import com.lexterlabra.puzzle.p2p.Role
import com.lexterlabra.puzzle.ui.theme.PuzzleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RaceGameScreenTest {
    @Test
    fun mirrorIsShownAndNotTappable() = runComposeUiTest {
        setContent {
            PuzzleTheme(Appearance.LIGHT) {
                OpponentMirror(PuzzleEngine.solved(4), ImageBitmap(200, 200), "Mia")
            }
        }
        onNodeWithTag("opponent-mirror").assertIsDisplayed()
        onNodeWithText("Mia").assertIsDisplayed()
        assertTrue(MIRROR_ALPHA == 0.7f)
    }

    @Test
    fun connectionLostDialogOffersWayBack() = runComposeUiTest {
        val (hostEnd, guestEnd) = FakeConnection.pair()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val controller = MatchController(Role.GUEST, guestEnd, "Me", "jvm", scope, { 0L }, { true })
        var left = false
        setContent {
            PuzzleTheme(Appearance.DARK) { RaceGameScreen(controller, FakeSettingsStore(), onLeave = { left = true }) }
        }
        hostEnd.close()
        waitUntil(timeoutMillis = 3_000) { runCatching { onNodeWithText("Connection lost").assertIsDisplayed() }.isSuccess }
        onNodeWithText("Back to lobby").performClick()
        assertTrue(left)
    }
}
