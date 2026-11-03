package com.lexterlabra.puzzle.ui.board

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.lexterlabra.puzzle.game.PuzzleEngine
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BoardViewTest {
    private val image = ImageBitmap(400, 400)

    @Test
    fun rapidTapsApplyInOrder() = runComposeUiTest {
        var state by mutableStateOf(PuzzleEngine.solved(4))
        setContent {
            BoardView(state, image, onTileTap = { state = PuzzleEngine.slideTile(state, it) }, modifier = Modifier.size(320.dp))
        }
        mainClock.autoAdvance = false
        onNodeWithTag("tile-15").performClick()
        onNodeWithTag("tile-14").performClick()
        mainClock.autoAdvance = true
        waitForIdle()
        val expected = PuzzleEngine.slide(PuzzleEngine.slide(PuzzleEngine.solved(4), 14), 13)
        assertEquals(expected.tiles, state.tiles)
        val bounds = (1..15).map { onNodeWithTag("tile-$it").fetchSemanticsNode().boundsInRoot }
        assertEquals(15, bounds.toSet().size)
    }

    @Test
    fun nonInteractiveIgnoresTaps() = runComposeUiTest {
        var taps = 0
        setContent {
            BoardView(PuzzleEngine.solved(4), image, onTileTap = { taps++ }, modifier = Modifier.size(120.dp), interactive = false)
        }
        onNodeWithTag("tile-15").performClick()
        assertEquals(0, taps)
    }
}
