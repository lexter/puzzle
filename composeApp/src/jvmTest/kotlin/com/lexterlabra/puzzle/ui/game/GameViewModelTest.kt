package com.lexterlabra.puzzle.ui.game

import androidx.compose.ui.graphics.ImageBitmap
import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.fakes.FakeScoreStore
import com.lexterlabra.puzzle.fakes.FakeSettingsStore
import com.lexterlabra.puzzle.game.GameClock
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.image.ImageCodec
import com.lexterlabra.puzzle.platform.PickResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private var t = 0L
    private val scores = FakeScoreStore()
    private val settings = FakeSettingsStore(grid = 4)

    /** One tap (tile 15) away from solved. */
    private val nearlySolved: (Int, Long) -> com.lexterlabra.puzzle.game.PuzzleState =
        { n, _ -> PuzzleEngine.slide(PuzzleEngine.solved(n), n * n - 2) }

    private fun vm(mode: Mode = Mode.TIME_BASED, shuffler: (Int, Long) -> com.lexterlabra.puzzle.game.PuzzleState = PuzzleEngine::shuffled) =
        GameViewModel(mode, settings, scores, clock = GameClock { t }, seedSource = { 99L }, shuffler = shuffler, now = { 1_000L }, workDispatcher = dispatcher)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun readyShowsSolvedBoardOfSavedSize() = runTest {
        settings.gridSize.value = 6
        val vm = vm()
        assertEquals(Phase.READY, vm.state.value.phase)
        assertEquals(PuzzleEngine.solved(6), vm.state.value.puzzle)
    }

    @Test
    fun startShufflesAndRunsClock() = runTest {
        val vm = vm()
        vm.start(); t = 2_000
        assertEquals(Phase.PLAYING, vm.state.value.phase)
        assertEquals(PuzzleEngine.shuffled(4, 99L).tiles, vm.state.value.puzzle.tiles)
        assertEquals(2_000L, vm.elapsedNow())
    }

    @Test
    fun solvingRecordsTimeBasedScore() = runTest {
        val vm = vm(shuffler = nearlySolved)
        vm.start(); t = 5_000
        vm.tapTile(15)
        assertEquals(Phase.WON, vm.state.value.phase)
        assertEquals(listOf(Score(4, 5_000, 1, 1_000L)), scores.recorded)
        assertTrue(vm.state.value.newBest)
    }

    @Test
    fun freePlayNeverRecords() = runTest {
        val vm = vm(mode = Mode.FREE, shuffler = nearlySolved)
        vm.start(); vm.tapTile(15)
        assertEquals(Phase.WON, vm.state.value.phase)
        assertTrue(scores.recorded.isEmpty())
    }

    @Test
    fun tapsIgnoredBeforeStart() = runTest {
        val vm = vm()
        vm.tapTile(15)
        assertEquals(PuzzleEngine.solved(4), vm.state.value.puzzle)
    }

    @Test
    fun changingGridMidGameResetsWithoutScore() = runTest {
        val vm = vm()
        vm.start(); t = 3_000
        vm.setGridSize(6)
        assertEquals(Phase.READY, vm.state.value.phase)
        assertEquals(PuzzleEngine.solved(6), vm.state.value.puzzle)
        assertEquals(0L, vm.elapsedNow())
        assertEquals(6, settings.gridSize.value)
        assertTrue(scores.recorded.isEmpty())
    }

    @Test
    fun backgroundHidesBoardAndPausesClock() = runTest {
        val vm = vm()
        vm.start(); t = 1_000
        vm.onBackground()
        assertTrue(vm.state.value.pausedHidden)
        t = 11_000
        vm.onForeground()
        assertFalse(vm.state.value.pausedHidden)
        assertEquals(1_000L, vm.elapsedNow())
    }

    @Test
    fun tinyPhotoShowsMessageKeepsImage() = runTest {
        val vm = vm()
        vm.setPicture(PickResult.Picked(ImageCodec.encodeJpeg(ImageBitmap(40, 40), 90)))
        assertEquals(UserMessage.PhotoTooSmall, vm.state.value.message)
        assertNull(vm.state.value.customImage)
    }

    @Test
    fun goodPhotoBecomesPicture() = runTest {
        val vm = vm()
        vm.setPicture(PickResult.Picked(ImageCodec.encodeJpeg(ImageBitmap(300, 200), 90)))
        assertNotNull(vm.state.value.customImage)
        assertEquals(200, vm.state.value.customImage?.width)
        vm.useScenic()
        assertNull(vm.state.value.customImage)
    }

    @Test
    fun cameraDeniedShowsMessage() = runTest {
        val vm = vm()
        vm.setPicture(PickResult.PermissionDenied)
        assertEquals(UserMessage.CameraDenied, vm.state.value.message)
    }

    @Test
    fun playAgainReturnsToReady() = runTest {
        val vm = vm(shuffler = nearlySolved)
        vm.start(); vm.tapTile(15); vm.playAgain()
        assertEquals(Phase.READY, vm.state.value.phase)
        assertEquals(0L, vm.elapsedNow())
    }
}
