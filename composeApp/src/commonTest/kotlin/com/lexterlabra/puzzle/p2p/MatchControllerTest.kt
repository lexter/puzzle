package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.fakes.FakeConnection
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MatchControllerTest {

    private fun TestScope.controllers(
        guestImageOk: Boolean = true,
        hostVersionOverride: Boolean = false,
    ): Pair<MatchController, MatchController> {
        val (hostEnd, guestEnd) = FakeConnection.pair()
        val now = { testScheduler.currentTime }
        val host = MatchController(Role.HOST, hostEnd, "Host", "android", backgroundScope, now, { true }, Random(1))
        val guest = MatchController(Role.GUEST, guestEnd, "Guest", "ios", backgroundScope, now, { guestImageOk }, Random(2))
        runCurrent()
        return host to guest
    }

    /** Puts [controller] one move from solved, then makes that move. */
    private fun solve(controller: MatchController) {
        controller.debugSetBoard(PuzzleEngine.slide(PuzzleEngine.solved(4), 1))
        controller.tapTile(1)
    }

    private fun TestScope.raceStarted(): Pair<MatchController, MatchController> {
        val (host, guest) = controllers()
        host.configure(4, Mode.TIME_BASED, null)
        runCurrent()
        host.startRace()
        runCurrent()
        advanceTimeBy(MatchMessage.COUNTDOWN_MS + 1)
        runCurrent()
        return host to guest
    }

    @Test
    fun handshakeConnectsBothSides() = runTest {
        val (host, guest) = controllers()
        assertEquals(MatchPhase.CONNECTED, host.state.value.phase)
        assertEquals("Guest", host.state.value.peerName)
        assertEquals("Host", guest.state.value.peerName)
    }

    @Test
    fun versionMismatchFails() = runTest {
        val (hostEnd, guestEnd) = FakeConnection.pair()
        val host = MatchController(Role.HOST, hostEnd, "Host", "android", backgroundScope, { testScheduler.currentTime }, { true })
        guestEnd.send(Frame.Control(MatchMessage.Hello(PROTOCOL_VERSION + 1, "Future", "ios")))
        runCurrent()
        assertEquals(MatchPhase.LOST, host.state.value.phase)
        assertEquals(LossReason.VERSION_MISMATCH, host.state.value.lostReason)
    }

    @Test
    fun guestBecomesReadyAfterConfig() = runTest {
        val (host, guest) = controllers()
        host.configure(5, Mode.FREE, null)
        runCurrent()
        assertTrue(host.state.value.peerReady)
        assertEquals(host.state.value.config, guest.state.value.config)
    }

    @Test
    fun bothBoardsIdentical() = runTest {
        val (host, guest) = raceStarted()
        assertEquals(MatchPhase.RACING, host.state.value.phase)
        assertEquals(MatchPhase.RACING, guest.state.value.phase)
        assertEquals(host.state.value.me, guest.state.value.me)
        assertFalse(host.state.value.me!!.isSolved)
    }

    @Test
    fun countdownLasts3s() = runTest {
        val (host, guest) = controllers()
        host.configure(4, Mode.TIME_BASED, null); runCurrent()
        host.startRace(); runCurrent()
        assertEquals(MatchPhase.COUNTDOWN, guest.state.value.phase)
        advanceTimeBy(2_999); runCurrent()
        assertEquals(MatchPhase.COUNTDOWN, host.state.value.phase)
        advanceTimeBy(2); runCurrent()
        assertEquals(MatchPhase.RACING, host.state.value.phase)
    }

    @Test
    fun progressUpdatesOpponentMirror() = runTest {
        val (host, guest) = raceStarted()
        val board = guest.state.value.me!!
        val movable = board.tiles[if (board.gapIndex % 4 == 0) board.gapIndex + 1 else board.gapIndex - 1]
        guest.tapTile(movable); runCurrent()
        assertEquals(guest.state.value.me, host.state.value.opponent)
    }

    @Test
    fun firstSolverWins() = runTest {
        val (host, guest) = raceStarted()
        advanceTimeBy(5_000)
        solve(guest); runCurrent()
        assertEquals(MatchPhase.FINISHED, guest.state.value.phase)
        assertTrue(guest.state.value.result!!.iWon)
        assertFalse(host.state.value.result!!.iWon)
        assertEquals(host.state.value.result!!.winnerTimeMs, guest.state.value.result!!.winnerTimeMs)
    }

    @Test
    fun nearSimultaneousSolveHasOneWinner() = runTest {
        val (host, guest) = raceStarted()
        solve(guest) // guest's Solved is queued, not yet delivered
        solve(host) // host solves in the same virtual millisecond
        runCurrent()
        val results = (host.state.value.result to guest.state.value.result)
        assertNotNull(results.first); assertNotNull(results.second)
        assertTrue(results.first!!.iWon != results.second!!.iWon)
    }

    @Test
    fun guestCannotMoveAfterSolving() = runTest {
        val (_, guest) = raceStarted()
        guest.debugSetBoard(PuzzleEngine.slide(PuzzleEngine.solved(4), 1))
        guest.tapTile(1)
        val solved = guest.state.value.me
        guest.tapTile(12)
        assertEquals(solved, guest.state.value.me)
    }

    @Test
    fun silenceMeansDisconnected() = runTest {
        val (hostEnd, guestEnd) = FakeConnection.pair()
        val host = MatchController(Role.HOST, hostEnd, "Host", "android", backgroundScope, { testScheduler.currentTime }, { true })
        guestEnd.dropOutgoing = true
        advanceTimeBy(6_600); runCurrent()
        assertEquals(MatchPhase.LOST, host.state.value.phase)
        assertEquals(LossReason.DISCONNECTED, host.state.value.lostReason)
    }

    @Test
    fun peerLeavingIsReported() = runTest {
        val (host, guest) = raceStarted()
        guest.leave(); runCurrent()
        assertEquals(MatchPhase.LOST, host.state.value.phase)
        host.continueSolo()
        assertEquals(MatchPhase.SOLO, host.state.value.phase)
    }

    @Test
    fun imageFailureFallsBackToScenic() = runTest {
        val (host, guest) = controllers(guestImageOk = false)
        host.configure(4, Mode.TIME_BASED, ByteArray(100) { 1 })
        runCurrent()
        assertEquals(MatchNotice.IMAGE_FALLBACK, host.state.value.notice)
        assertFalse(host.state.value.config!!.customImage)
        assertTrue(host.state.value.peerReady)
        assertEquals(host.state.value.config, guest.state.value.config)
    }

    @Test
    fun customImageReachesGuest() = runTest {
        val (host, guest) = controllers()
        val bytes = ByteArray(5_000) { (it % 7).toByte() }
        host.configure(4, Mode.TIME_BASED, bytes); runCurrent()
        assertTrue(guest.state.value.imageBytes!!.contentEquals(bytes))
    }
}
