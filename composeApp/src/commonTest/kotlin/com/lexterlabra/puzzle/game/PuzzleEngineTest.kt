package com.lexterlabra.puzzle.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PuzzleEngineTest {
    @Test
    fun solvedBoardIsSolved() = assertTrue(PuzzleEngine.solved(4).isSolved)

    @Test
    fun solvedBoardHasGapInFirstCell() {
        for (n in 4..10) assertEquals(0, PuzzleEngine.solved(n).gapIndex)
    }

    @Test
    fun shuffleIsDeterministicPerSeed() =
        assertEquals(PuzzleEngine.shuffled(4, 42L).tiles, PuzzleEngine.shuffled(4, 42L).tiles)

    @Test
    fun shufflesAreSolvableAndNotSolved() {
        for (n in 4..10) for (seed in 0L until 200L) {
            val s = PuzzleEngine.shuffled(n, seed)
            assertTrue(PuzzleEngine.isSolvable(n, s.tiles), "n=$n seed=$seed")
            assertFalse(s.isSolved, "n=$n seed=$seed")
            assertEquals((0 until n * n).toSet(), s.tiles.toSet())
            assertEquals(0, s.gapIndex, "n=$n seed=$seed")
        }
    }

    @Test
    fun shuffleWalkIsLegalDeterministicAndScrambled() {
        for (n in 4..10) for (seed in 0L until 50L) {
            val frames = PuzzleEngine.shuffleWalk(n, seed, 50)
            assertTrue(frames.size >= 50, "n=$n seed=$seed")
            var previous = PuzzleEngine.solved(n)
            for (frame in frames) {
                // Every frame is exactly one slide away from the one before.
                val gap = previous.gapIndex
                val next = frame.gapIndex
                assertTrue(gap / n == next / n || gap % n == next % n, "n=$n seed=$seed")
                assertEquals(PuzzleEngine.slide(previous, next).tiles, frame.tiles)
                previous = frame
            }
            assertFalse(frames.last().isSolved, "n=$n seed=$seed")
            assertTrue(PuzzleEngine.isSolvable(n, frames.last().tiles))
            assertEquals(0, frames.last().gapIndex, "n=$n seed=$seed")
        }
        assertEquals(PuzzleEngine.shuffleWalk(4, 7L, 50), PuzzleEngine.shuffleWalk(4, 7L, 50))
    }

    @Test
    fun knownUnsolvableDetected() {
        val t = (0..13).toList() + listOf(15, 14)
        assertFalse(PuzzleEngine.isSolvable(4, t))
    }

    @Test
    fun oddSizeParity() {
        val t = (0..24).toList()
        val swapped = t.toMutableList().apply { this[1] = 2; this[2] = 1 }
        assertTrue(PuzzleEngine.isSolvable(5, t))
        assertFalse(PuzzleEngine.isSolvable(5, swapped))
    }

    @Test
    fun solvabilityMatchesReachability() {
        // Boards made by random legal slides from solved are always solvable.
        val r = kotlin.random.Random(7)
        for (n in listOf(4, 5, 6)) {
            var s = PuzzleEngine.solved(n)
            repeat(500) { s = PuzzleEngine.slide(s, r.nextInt(n * n)) }
            assertTrue(PuzzleEngine.isSolvable(n, s.tiles), "n=$n")
        }
    }

    @Test
    fun slideAdjacent() {
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 1)
        assertEquals(1, s.tiles[0]); assertEquals(0, s.tiles[1]); assertEquals(1, s.moves)
    }

    @Test
    fun slideWholeColumn() {
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 12)
        assertEquals(listOf(4, 8, 12, 0), listOf(s.tiles[0], s.tiles[4], s.tiles[8], s.tiles[12]))
        assertEquals(1, s.moves)
    }

    @Test
    fun slideWholeRow() {
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 3)
        assertEquals(listOf(1, 2, 3, 0), s.tiles.subList(0, 4))
    }

    @Test
    fun nonLineTapIgnored() {
        val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 5))
    }

    @Test
    fun gapTapIgnored() {
        val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 0))
    }

    @Test
    fun slideTileById() {
        val s = PuzzleEngine.slideTile(PuzzleEngine.solved(4), 1)
        assertEquals(1, s.tiles[0]); assertEquals(0, s.tiles[1])
    }

    @Test
    fun tilesInPlaceCounts() =
        assertEquals(14, PuzzleEngine.slide(PuzzleEngine.solved(4), 1).tilesInPlace)
}
