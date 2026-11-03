package com.lexterlabra.puzzle.game

import kotlin.time.TimeSource

/** Stopwatch driven by an injectable monotonic millisecond source. */
class GameClock(private val now: () -> Long = monotonicMillis()) {
    private var accumulatedMs = 0L
    private var runningSince: Long? = null
    private var paused = false

    val isRunning: Boolean get() = runningSince != null
    val elapsedMs: Long get() = accumulatedMs + (runningSince?.let { now() - it } ?: 0L)

    fun start() {
        accumulatedMs = 0L
        paused = false
        runningSince = now()
    }

    fun pause() {
        val since = runningSince ?: return
        accumulatedMs += now() - since
        runningSince = null
        paused = true
    }

    fun resume() {
        if (!paused) return
        paused = false
        runningSince = now()
    }

    fun reset() {
        accumulatedMs = 0L
        runningSince = null
        paused = false
    }
}

fun monotonicMillis(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}

/** `mm:ss.t`, minutes keep counting past 59. */
fun formatElapsed(ms: Long): String {
    val tenths = ms.coerceAtLeast(0) / 100
    val minutes = tenths / 600
    val seconds = (tenths / 10) % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${tenths % 10}"
}
