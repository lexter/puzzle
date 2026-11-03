package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleEngine
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.p2p.MatchMessage.MatchConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Role { HOST, GUEST }

enum class MatchPhase { WAITING, CONNECTED, CONFIGURED, COUNTDOWN, RACING, FINISHED, LOST, SOLO }

enum class LossReason { DISCONNECTED, PEER_LEFT, VERSION_MISMATCH }

enum class MatchNotice { IMAGE_FALLBACK, REMATCH_REQUESTED }

data class RaceResult(val iWon: Boolean, val myTimeMs: Long?, val winnerTimeMs: Long)

/** Everything the lobby and race screens need, in one value. */
data class MatchSnapshot(
    val phase: MatchPhase = MatchPhase.WAITING,
    val peerName: String? = null,
    val config: MatchConfig? = null,
    /** JPEG of the host's photo, or null for the bundled scenic picture. */
    val imageBytes: ByteArray? = null,
    val peerReady: Boolean = false,
    val me: PuzzleState? = null,
    val opponent: PuzzleState? = null,
    val countdownEndsAt: Long? = null,
    val raceStartedAt: Long? = null,
    val mySolvedMs: Long? = null,
    val result: RaceResult? = null,
    val lostReason: LossReason? = null,
    val notice: MatchNotice? = null,
)

/**
 * Host/guest state machine for one connection. All state changes happen on [scope]'s
 * dispatcher (Main in the app), so the host's "first Solved wins" check is race-free.
 */
class MatchController(
    val role: Role,
    private val connection: PeerConnection,
    private val myName: String,
    private val platform: String,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    /** Guest-side check that the host's photo can be used. */
    private val validateImage: (ByteArray) -> Boolean,
    private val random: Random = Random.Default,
    private val pingIntervalMs: Long = 2_000,
    private val silenceTimeoutMs: Long = 6_000,
    private val countdownMs: Long = MatchMessage.COUNTDOWN_MS,
) {
    private val _state = MutableStateFlow(MatchSnapshot())
    val state: StateFlow<MatchSnapshot> = _state.asStateFlow()

    private val outbox = Channel<Frame>(Channel.UNLIMITED)
    private val jobs = mutableListOf<Job>()
    private var lastHeardAt = now()
    private var pendingConfig: MatchConfig? = null
    private var leaving = false

    init {
        jobs += scope.launch { for (frame in outbox) connection.send(frame) }
        jobs += scope.launch {
            connection.incoming.collect { frame ->
                lastHeardAt = now()
                handle(frame)
            }
            onConnectionEnded()
        }
        jobs += scope.launch {
            while (isActive) {
                delay(pingIntervalMs)
                send(MatchMessage.Ping)
            }
        }
        jobs += scope.launch {
            while (isActive) {
                delay(silenceTimeoutMs / 12)
                if (now() - lastHeardAt > silenceTimeoutMs) {
                    lose(LossReason.DISCONNECTED)
                    connection.close()
                    break
                }
            }
        }
        send(MatchMessage.Hello(PROTOCOL_VERSION, myName, platform))
    }

    /** Race stopwatch for the UI. */
    fun elapsedNow(): Long = _state.value.let { s -> s.mySolvedMs ?: s.raceStartedAt?.let { now() - it } ?: 0L }

    fun millisUntilStart(): Long = (_state.value.countdownEndsAt?.minus(now()) ?: 0L).coerceAtLeast(0L)

    // ---- Host actions -------------------------------------------------------------------

    /** Sets up (or re-sets up) the match with a fresh seed and sends it to the guest. */
    fun configure(grid: Int, mode: Mode, imageBytes: ByteArray?) {
        check(role == Role.HOST) { "only the host configures" }
        val config = MatchConfig(grid, random.nextLong(), mode, customImage = imageBytes != null, imageBytes = imageBytes?.size ?: 0)
        _state.update {
            it.copy(
                phase = MatchPhase.CONFIGURED, config = config, imageBytes = imageBytes, peerReady = false,
                me = null, opponent = null, countdownEndsAt = null, raceStartedAt = null, mySolvedMs = null, result = null,
                notice = if (it.notice == MatchNotice.IMAGE_FALLBACK) it.notice else null,
            )
        }
        send(config)
        if (imageBytes != null) outbox.trySend(Frame.Image(imageBytes))
    }

    fun startRace() {
        val current = _state.value
        check(role == Role.HOST && current.phase == MatchPhase.CONFIGURED && current.peerReady) { "guest not ready" }
        send(MatchMessage.Start(countdownMs))
        beginCountdown(countdownMs)
    }

    // ---- Both ---------------------------------------------------------------------------

    fun tapTile(tileId: Int) {
        val current = _state.value
        if (current.phase != MatchPhase.RACING && current.phase != MatchPhase.SOLO) return
        if (current.mySolvedMs != null) return // solved: waiting for the host's Result
        val board = current.me ?: return
        val next = PuzzleEngine.slideTile(board, tileId)
        if (next === board) return
        _state.update { it.copy(me = next) }
        if (current.phase == MatchPhase.RACING) send(MatchMessage.Progress(next.tiles, next.moves, next.tilesInPlace))
        if (!next.isSolved) return
        val time = elapsedNow()
        when {
            current.phase == MatchPhase.SOLO -> _state.update { it.copy(mySolvedMs = time) }
            role == Role.HOST -> decide(hostWon = true, winnerTimeMs = time)
            else -> {
                _state.update { it.copy(mySolvedMs = time) }
                send(MatchMessage.Solved(time, next.moves))
            }
        }
    }

    /** Host: new board with the same settings. Guest: asks the host for one. */
    fun rematch() {
        val config = _state.value.config ?: return
        if (role == Role.HOST) configure(config.grid, config.mode, _state.value.imageBytes) else send(MatchMessage.Rematch)
    }

    /** After a lost connection mid-race: keep solving locally, unranked. */
    fun continueSolo() {
        val current = _state.value
        if (current.phase == MatchPhase.LOST && current.me != null && current.raceStartedAt != null) {
            _state.update { it.copy(phase = MatchPhase.SOLO) }
        }
    }

    /** Test hook: replace this player's board. */
    internal fun debugSetBoard(board: PuzzleState) = _state.update { it.copy(me = board) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun leave() {
        leaving = true
        scope.launch {
            runCatching { connection.send(Frame.Control(MatchMessage.Leave)) }
            connection.close()
            jobs.forEach { it.cancel() }
            outbox.close()
        }
    }

    // ---- Incoming -----------------------------------------------------------------------

    private fun handle(frame: Frame) {
        when (frame) {
            is Frame.Image -> onImage(frame.bytes)
            is Frame.Control -> onMessage(frame.message)
        }
    }

    private fun onMessage(message: MatchMessage) {
        when (message) {
            is MatchMessage.Hello -> onHello(message)
            is MatchConfig -> if (role == Role.GUEST) onConfig(message)
            MatchMessage.Ready -> if (role == Role.HOST) _state.update { it.copy(peerReady = true) }
            MatchMessage.ImageFailed -> if (role == Role.HOST) onImageFailed()
            is MatchMessage.Start -> if (role == Role.GUEST && _state.value.phase == MatchPhase.CONFIGURED) beginCountdown(message.countdownMs)
            is MatchMessage.Progress -> onProgress(message)
            is MatchMessage.Solved -> if (role == Role.HOST) decide(hostWon = false, winnerTimeMs = message.elapsedMs)
            is MatchMessage.Result -> if (role == Role.GUEST) finish(iWon = !message.winnerIsHost, winnerTimeMs = message.winnerTimeMs)
            MatchMessage.Rematch -> if (role == Role.HOST) _state.update { it.copy(notice = MatchNotice.REMATCH_REQUESTED) }
            MatchMessage.Leave -> lose(LossReason.PEER_LEFT)
            MatchMessage.Ping -> Unit
        }
    }

    private fun onHello(hello: MatchMessage.Hello) {
        if (hello.protocolVersion != PROTOCOL_VERSION) {
            lose(LossReason.VERSION_MISMATCH)
            leave()
            return
        }
        _state.update {
            it.copy(peerName = hello.playerName, phase = if (it.phase == MatchPhase.WAITING) MatchPhase.CONNECTED else it.phase)
        }
    }

    private fun onConfig(config: MatchConfig) {
        pendingConfig = config
        if (!config.customImage) markReady(config, null)
    }

    private fun onImage(bytes: ByteArray) {
        if (role != Role.GUEST) return
        val config = pendingConfig ?: return
        if (validateImage(bytes)) markReady(config, bytes) else send(MatchMessage.ImageFailed)
    }

    private fun markReady(config: MatchConfig, imageBytes: ByteArray?) {
        pendingConfig = null
        _state.update {
            it.copy(
                phase = MatchPhase.CONFIGURED, config = config, imageBytes = imageBytes, me = null, opponent = null,
                countdownEndsAt = null, raceStartedAt = null, mySolvedMs = null, result = null, notice = null,
            )
        }
        send(MatchMessage.Ready)
    }

    private fun onImageFailed() {
        val config = _state.value.config ?: return
        _state.update { it.copy(notice = MatchNotice.IMAGE_FALLBACK) }
        configure(config.grid, config.mode, imageBytes = null)
    }

    private fun onProgress(progress: MatchMessage.Progress) {
        val grid = _state.value.config?.grid ?: return
        val board = runCatching { PuzzleState(grid, progress.tiles, progress.moves) }.getOrNull() ?: return
        _state.update { it.copy(opponent = board) }
    }

    // ---- Transitions --------------------------------------------------------------------

    private fun beginCountdown(durationMs: Long) {
        _state.update { it.copy(phase = MatchPhase.COUNTDOWN, countdownEndsAt = now() + durationMs) }
        scope.launch {
            delay(durationMs)
            if (_state.value.phase == MatchPhase.COUNTDOWN) beginRace()
        }
    }

    private fun beginRace() {
        val config = _state.value.config ?: return
        val board = PuzzleEngine.shuffled(config.grid, config.seed)
        _state.update { it.copy(phase = MatchPhase.RACING, me = board, opponent = board, raceStartedAt = now()) }
    }

    /** Host only: the first solve it learns about wins, and only one Result is ever sent. */
    private fun decide(hostWon: Boolean, winnerTimeMs: Long) {
        val current = _state.value
        if (current.phase != MatchPhase.RACING || current.result != null) return
        val myTime = if (hostWon) winnerTimeMs else current.mySolvedMs
        _state.update { it.copy(phase = MatchPhase.FINISHED, mySolvedMs = myTime, result = RaceResult(hostWon, myTime, winnerTimeMs)) }
        send(MatchMessage.Result(winnerIsHost = hostWon, winnerTimeMs = winnerTimeMs))
    }

    private fun finish(iWon: Boolean, winnerTimeMs: Long) {
        _state.update {
            it.copy(phase = MatchPhase.FINISHED, result = RaceResult(iWon, if (iWon) it.mySolvedMs else null, winnerTimeMs))
        }
    }

    private fun lose(reason: LossReason) {
        if (leaving) return
        _state.update {
            if (it.phase == MatchPhase.LOST || it.phase == MatchPhase.SOLO) it else it.copy(phase = MatchPhase.LOST, lostReason = reason)
        }
    }

    private fun onConnectionEnded() = lose(LossReason.DISCONNECTED)

    private fun send(message: MatchMessage) {
        outbox.trySend(Frame.Control(message))
    }
}
