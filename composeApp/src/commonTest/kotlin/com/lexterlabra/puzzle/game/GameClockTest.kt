package com.lexterlabra.puzzle.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameClockTest {
    private var t = 0L
    private val clock = GameClock { t }

    @Test
    fun startsAtZero() {
        assertEquals(0L, clock.elapsedMs); assertFalse(clock.isRunning)
    }

    @Test
    fun countsWhileRunning() {
        clock.start(); t = 1500
        assertEquals(1500L, clock.elapsedMs); assertTrue(clock.isRunning)
    }

    @Test
    fun pausesOnBackground() {
        clock.start(); t = 1000; clock.pause(); t = 5000
        assertEquals(1000L, clock.elapsedMs)
        clock.resume(); t = 5500
        assertEquals(1500L, clock.elapsedMs)
    }

    @Test
    fun resumeWithoutPauseDoesNothing() {
        clock.resume(); t = 1000
        assertEquals(0L, clock.elapsedMs); assertFalse(clock.isRunning)
    }

    @Test
    fun resetStops() {
        clock.start(); t = 900; clock.reset(); t = 2000
        assertEquals(0L, clock.elapsedMs); assertFalse(clock.isRunning)
    }

    @Test
    fun formatsTenths() {
        assertEquals("01:47.3", formatElapsed(107_300))
        assertEquals("00:00.0", formatElapsed(0))
        assertEquals("61:00.0", formatElapsed(3_660_000))
    }
}
