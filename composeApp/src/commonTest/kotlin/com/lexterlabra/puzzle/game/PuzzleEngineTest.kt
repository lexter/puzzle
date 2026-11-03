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
    fun shuffleIsDeterministicPerSeed() =
        assertEquals(PuzzleEngine.shuffled(4, 42L).tiles, PuzzleEngine.shuffled(4, 42L).tiles)

    @Test
    fun shufflesAreSolvableAndNotSolved() {
        for (n in 4..10) for (seed in 0L until 200L) {
            val s = PuzzleEngine.shuffled(n, seed)
            assertTrue(PuzzleEngine.isSolvable(n, s.tiles), "n=$n seed=$seed")
            assertFalse(s.isSolved, "n=$n seed=$seed")
            assertEquals((0 until n * n).toSet(), s.tiles.toSet())
        }
    }

    @Test
    fun knownUnsolvableDetected() {
        val t = (1..13).toList() + listOf(15, 14, 0)
        assertFalse(PuzzleEngine.isSolvable(4, t))
    }

    @Test
    fun oddSizeParity() {
        val t = (1..24).toList() + listOf(0)
        val swapped = t.toMutableList().apply { this[0] = 2; this[1] = 1 }
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
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 14)
        assertEquals(0, s.tiles[14]); assertEquals(15, s.tiles[15]); assertEquals(1, s.moves)
    }

    @Test
    fun slideWholeColumn() {
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 3)
        assertEquals(listOf(0, 4, 8, 12), listOf(s.tiles[3], s.tiles[7], s.tiles[11], s.tiles[15]))
        assertEquals(1, s.moves)
    }

    @Test
    fun slideWholeRow() {
        val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 12)
        assertEquals(listOf(0, 13, 14, 15), s.tiles.subList(12, 16))
    }

    @Test
    fun nonLineTapIgnored() {
        val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 0))
    }

    @Test
    fun gapTapIgnored() {
        val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 15))
    }

    @Test
    fun slideTileById() {
        val s = PuzzleEngine.slideTile(PuzzleEngine.solved(4), 15)
        assertEquals(0, s.tiles[14]); assertEquals(15, s.tiles[15])
    }

    @Test
    fun tilesInPlaceCounts() =
        assertEquals(14, PuzzleEngine.slide(PuzzleEngine.solved(4), 14).tilesInPlace)
}
