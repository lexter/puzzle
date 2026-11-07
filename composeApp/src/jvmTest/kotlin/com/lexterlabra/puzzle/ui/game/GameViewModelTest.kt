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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    /** One tap (tile 1) away from solved. */
    private val nearlySolved: (Int, Long) -> com.lexterlabra.puzzle.game.PuzzleState =
        { n, _ -> PuzzleEngine.slide(PuzzleEngine.solved(n), 1) }

    private val walk: (Int, Long) -> List<com.lexterlabra.puzzle.game.PuzzleState> =
        { n, seed -> PuzzleEngine.shuffleWalk(n, seed, SHUFFLE_STEPS) }

    private fun vm(mode: Mode = Mode.TIME_BASED, shuffler: (Int, Long) -> com.lexterlabra.puzzle.game.PuzzleState? = { _, _ -> null }) =
        GameViewModel(
            mode, settings, scores, clock = GameClock { t }, seedSource = { 99L },
            shuffleFrames = { n, seed -> shuffler(n, seed)?.let { listOf(it) } ?: walk(n, seed) },
            now = { 1_000L }, workDispatcher = dispatcher,
        )

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
        vm.start()
        assertEquals(Phase.SHUFFLING, vm.state.value.phase)
        vm.tapTile(vm.state.value.puzzle.tiles.first { it != 0 })
        assertEquals(0, vm.state.value.puzzle.moves)
        val frames = walk(4, 99L)
        advanceTimeBy(frames.size * SHUFFLE_STEP_MS - 1)
        assertEquals(Phase.SHUFFLING, vm.state.value.phase)
        advanceTimeBy(1); runCurrent()
        assertEquals(Phase.COUNTDOWN, vm.state.value.phase)
        assertEquals(3, vm.state.value.countdown)
        // No moves and no clock during the countdown.
        vm.tapTile(vm.state.value.puzzle.tiles.first { it != 0 })
        assertEquals(0, vm.state.value.puzzle.moves)
        assertEquals(0L, vm.elapsedNow())
        advanceTimeBy(COUNTDOWN_STEP_MS); runCurrent()
        assertEquals(2, vm.state.value.countdown)
        advanceTimeBy(COUNTDOWN_STEP_MS); runCurrent()
        assertEquals(1, vm.state.value.countdown)
        advanceTimeBy(COUNTDOWN_STEP_MS - 1)
        assertEquals(Phase.COUNTDOWN, vm.state.value.phase)
        advanceUntilIdle()
        assertEquals(Phase.PLAYING, vm.state.value.phase)
        assertEquals(0, vm.state.value.countdown)
        assertEquals(0, frames.last().gapIndex)
        assertEquals(walk(4, 99L).last().tiles, vm.state.value.puzzle.tiles)
        assertEquals(0, vm.state.value.puzzle.moves)
        t = 2_000
        assertEquals(2_000L, vm.elapsedNow())
    }

    @Test
    fun changingGridDuringShuffleCancelsIt() = runTest {
        val vm = vm()
        vm.start()
        advanceTimeBy(1_000)
        vm.setGridSize(5)
        advanceUntilIdle()
        assertEquals(Phase.READY, vm.state.value.phase)
        assertEquals(PuzzleEngine.solved(5), vm.state.value.puzzle)
    }

    @Test
    fun solvingRecordsTimeBasedScore() = runTest {
        val vm = vm(shuffler = nearlySolved)
        vm.start(); advanceUntilIdle(); t = 5_000
        vm.tapTile(1)
        assertEquals(Phase.WON, vm.state.value.phase)
        assertEquals(listOf(Score(4, 5_000, 1, 1_000L)), scores.recorded)
        assertTrue(vm.state.value.newBest)
    }

    @Test
    fun freePlayNeverRecords() = runTest {
        val vm = vm(mode = Mode.FREE, shuffler = nearlySolved)
        vm.start(); advanceUntilIdle(); vm.tapTile(1)
        assertEquals(Phase.WON, vm.state.value.phase)
        assertTrue(scores.recorded.isEmpty())
    }

    @Test
    fun tapsIgnoredBeforeStart() = runTest {
        val vm = vm()
        vm.tapTile(1)
        assertEquals(PuzzleEngine.solved(4), vm.state.value.puzzle)
    }

    @Test
    fun changingGridMidGameResetsWithoutScore() = runTest {
        val vm = vm()
        vm.start(); advanceUntilIdle(); t = 3_000
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
        vm.start(); advanceUntilIdle(); t = 1_000
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
        vm.start(); advanceUntilIdle(); vm.tapTile(1); vm.playAgain()
        assertEquals(Phase.READY, vm.state.value.phase)
        assertEquals(0L, vm.elapsedNow())
    }
}
