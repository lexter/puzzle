package com.lexterlabra.puzzle.ui.game

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.data.ScoreStore
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.game.GameClock
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.image.ImageTooSmall
import com.lexterlabra.puzzle.image.PuzzleImage
import com.lexterlabra.puzzle.platform.PickResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Clock

enum class Phase { READY, SHUFFLING, COUNTDOWN, PLAYING, WON }

/** The visible scramble when a game starts: [SHUFFLE_STEPS] slides, one every [SHUFFLE_STEP_MS]. */
const val SHUFFLE_STEP_MS = 100L
const val SHUFFLE_STEPS = 50

/** After the scramble, a 3-2-1 count runs before play (and the timer) starts. */
const val COUNTDOWN_FROM = 3
const val COUNTDOWN_STEP_MS = 1_000L

sealed interface UserMessage {
    val text: String

    data object PhotoTooSmall : UserMessage { override val text = "That photo is too small. Pick a bigger one." }
    data object PhotoUnreadable : UserMessage { override val text = "That photo couldn't be opened." }
    data object CameraDenied : UserMessage { override val text = "Camera access is off. You can turn it on in Settings." }
}

data class GameUiState(
    val puzzle: PuzzleState,
    val mode: Mode,
    val phase: Phase = Phase.READY,
    val customImage: ImageBitmap? = null,
    val elapsedMs: Long = 0L,
    val pausedHidden: Boolean = false,
    val loadingPhoto: Boolean = false,
    val message: UserMessage? = null,
    val newBest: Boolean = false,
    /** The number on screen during [Phase.COUNTDOWN]; 0 otherwise. */
    val countdown: Int = 0,
)

class GameViewModel(
    private val mode: Mode,
    private val settings: SettingsStore,
    private val scores: ScoreStore,
    private val clock: GameClock = GameClock(),
    private val seedSource: () -> Long = { Random.nextLong() },
    /** Board frames played during the start-of-game scramble; the last one is the puzzle. */
    private val shuffleFrames: (size: Int, seed: Long) -> List<PuzzleState> = { size, seed -> PuzzleEngine.shuffleWalk(size, seed, SHUFFLE_STEPS) },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    initialImage: ImageBitmap? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState(PuzzleEngine.solved(PuzzleState.DEFAULT_SIZE), mode, customImage = initialImage))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var shuffleJob: Job? = null

    init {
        viewModelScope.launch {
            val size = settings.gridSize.first()
            _state.update { if (it.phase == Phase.READY) it.copy(puzzle = PuzzleEngine.solved(size)) else it }
        }
    }

    /** Live stopwatch value; the screen polls it while playing. */
    fun elapsedNow(): Long = clock.elapsedMs

    fun start() {
        val current = _state.value
        if (current.phase != Phase.READY) return
        val frames = shuffleFrames(current.puzzle.size, seedSource())
        _state.update { it.copy(phase = Phase.SHUFFLING, newBest = false) }
        shuffleJob = viewModelScope.launch {
            for (frame in frames) {
                _state.update { it.copy(puzzle = frame.copy(moves = 0)) }
                delay(SHUFFLE_STEP_MS)
            }
            for (count in COUNTDOWN_FROM downTo 1) {
                _state.update { it.copy(phase = Phase.COUNTDOWN, countdown = count) }
                delay(COUNTDOWN_STEP_MS)
            }
            _state.update { it.copy(phase = Phase.PLAYING, countdown = 0) }
            if (mode == Mode.TIME_BASED) clock.start() else clock.reset()
        }
    }

    fun tapTile(tileId: Int) {
        val current = _state.value
        if (current.phase != Phase.PLAYING || current.pausedHidden) return
        val next = PuzzleEngine.slideTile(current.puzzle, tileId)
        if (next === current.puzzle) return
        if (!next.isSolved) {
            _state.update { it.copy(puzzle = next) }
            return
        }
        clock.pause()
        val elapsed = clock.elapsedMs
        _state.update { it.copy(puzzle = next, phase = Phase.WON, elapsedMs = elapsed) }
        if (mode == Mode.TIME_BASED) {
            viewModelScope.launch {
                val best = scores.record(Score(next.size, elapsed, next.moves, now()))
                _state.update { it.copy(newBest = best) }
            }
        }
    }

    fun setGridSize(size: Int) {
        val clamped = size.coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE)
        viewModelScope.launch { settings.setGridSize(clamped) }
        resetTo(clamped)
    }

    fun setPicture(result: PickResult) {
        when (result) {
            PickResult.Cancelled -> Unit
            PickResult.PermissionDenied -> _state.update { it.copy(message = UserMessage.CameraDenied) }
            is PickResult.Picked -> viewModelScope.launch {
                _state.update { it.copy(loadingPhoto = true) }
                val prepared = withContext(workDispatcher) { PuzzleImage.prepare(result.bytes) }
                _state.update { state ->
                    prepared.fold(
                        onSuccess = { state.copy(customImage = it, loadingPhoto = false) },
                        onFailure = {
                            val message = if (it is ImageTooSmall) UserMessage.PhotoTooSmall else UserMessage.PhotoUnreadable
                            state.copy(loadingPhoto = false, message = message)
                        },
                    )
                }
            }
        }
    }

    fun useScenic() = _state.update { it.copy(customImage = null) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun onBackground() {
        val current = _state.value
        if (current.phase != Phase.PLAYING || mode != Mode.TIME_BASED || current.pausedHidden) return
        clock.pause()
        _state.update { it.copy(pausedHidden = true, elapsedMs = clock.elapsedMs) }
    }

    fun onForeground() {
        if (!_state.value.pausedHidden) return
        clock.resume()
        _state.update { it.copy(pausedHidden = false) }
    }

    fun playAgain() = resetTo(_state.value.puzzle.size)

    private fun resetTo(size: Int) {
        shuffleJob?.cancel()
        shuffleJob = null
        clock.reset()
        _state.update { it.copy(puzzle = PuzzleEngine.solved(size), phase = Phase.READY, elapsedMs = 0L, pausedHidden = false, newBest = false, countdown = 0) }
    }
}
