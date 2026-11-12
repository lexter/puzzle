package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.fakes.FakeConnection
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.p2p.MatchMessage.MatchConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Messages from a peer are untrusted: malformed values must be dropped or clamped, never throw. */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerInputTest {

    /** A guest controller plus its connection end, into which tests inject a (possibly hostile) host's messages. */
    private fun TestScope.guestWithRawHost(): Pair<MatchController, FakeConnection> {
        val (_, guestEnd) = FakeConnection.pair()
        val guest = MatchController(Role.GUEST, guestEnd, "Guest", "ios", backgroundScope, { testScheduler.currentTime }, { true })
        runCurrent()
        return guest to guestEnd
    }

    private fun FakeConnection.deliver(message: MatchMessage) = deliver(Frame.Control(message))

    private fun config(grid: Int) = MatchConfig(grid, seed = 1, mode = Mode.FREE, customImage = false, imageBytes = 0)

    @Test
    fun fromPeerRejectsNonPermutations() {
        assertNotNull(PuzzleState.fromPeer(4, (0 until 16).toList(), 0))
        assertNull(PuzzleState.fromPeer(4, List(16) { 0 }, 0))
        assertNull(PuzzleState.fromPeer(4, (1..16).toList(), 0))
        assertNull(PuzzleState.fromPeer(4, (0 until 15).toList(), 0))
        assertNull(PuzzleState.fromPeer(100_000, emptyList(), 0))
    }

    @Test
    fun guestIgnoresOutOfRangeGrid() = runTest {
        val (guest, host) = guestWithRawHost()
        listOf(-1, 0, 3, 11, 100_000, Int.MAX_VALUE).forEach { host.deliver(config(it)) }
        runCurrent()
        assertNull(guest.state.value.config)
        assertNotEquals(MatchPhase.CONFIGURED, guest.state.value.phase)
    }

    @Test
    fun guestIgnoresDuplicateTileProgress() = runTest {
        val (guest, host) = guestWithRawHost()
        host.deliver(config(4))
        host.deliver(MatchMessage.Progress(tiles = List(16) { 0 }, moves = 1, tilesInPlace = 0))
        runCurrent()
        assertEquals(4, guest.state.value.config?.grid)
        assertNull(guest.state.value.opponent)
    }

    @Test
    fun guestClampsHugeCountdown() = runTest {
        val (guest, host) = guestWithRawHost()
        host.deliver(config(4))
        runCurrent()
        val startedAt = testScheduler.currentTime
        host.deliver(MatchMessage.Start(countdownMs = Long.MAX_VALUE))
        runCurrent()
        assertEquals(MatchPhase.COUNTDOWN, guest.state.value.phase)
        assertEquals(startedAt + MatchMessage.MAX_COUNTDOWN_MS, guest.state.value.countdownEndsAt)
    }
}
