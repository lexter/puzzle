package com.lexterlabra.puzzle.game

import kotlin.random.Random

object PuzzleEngine {

    fun solved(size: Int): PuzzleState = PuzzleState(size, (1 until size * size).toList() + 0)

    /** A solvable, not-yet-solved board. The same [seed] always gives the same board. */
    fun shuffled(size: Int, seed: Long): PuzzleState {
        val tiles = (0 until size * size).toMutableList()
        tiles.shuffle(Random(seed))
        // Swapping two numbered tiles flips solvability parity.
        if (!isSolvable(size, tiles)) swapNumbered(tiles, first = true)
        if (PuzzleState(size, tiles).isSolved) {
            // Two swaps keep it solvable but no longer solved.
            swapNumbered(tiles, first = false)
            swapNumbered(tiles, first = true)
        }
        return PuzzleState(size, tiles.toList())
    }

    fun isSolvable(size: Int, tiles: List<Int>): Boolean {
        val numbers = tiles.filter { it != 0 }
        var inversions = 0
        for (i in numbers.indices) for (j in i + 1 until numbers.size) if (numbers[i] > numbers[j]) inversions++
        if (size % 2 == 1) return inversions % 2 == 0
        val gapRowFromBottom = size - tiles.indexOf(0) / size
        return (inversions + gapRowFromBottom) % 2 == 1
    }

    /**
     * Taps the cell at [index]. If it shares a row or column with the gap, every tile between
     * it and the gap moves one step toward the gap (one move). Otherwise returns [state] itself.
     */
    fun slide(state: PuzzleState, index: Int): PuzzleState {
        val n = state.size
        val gap = state.gapIndex
        if (index !in state.tiles.indices || index == gap) return state
        val sameRow = index / n == gap / n
        val sameColumn = index % n == gap % n
        if (!sameRow && !sameColumn) return state
        val step = when {
            sameRow -> if (index < gap) -1 else 1
            else -> if (index < gap) -n else n
        }
        val tiles = state.tiles.toMutableList()
        var at = gap
        while (at != index) {
            tiles[at] = tiles[at + step]
            at += step
        }
        tiles[index] = 0
        return state.copy(tiles = tiles, moves = state.moves + 1)
    }

    /** Same as [slide] but addressed by tile number, so stale indices can't misapply a tap. */
    fun slideTile(state: PuzzleState, tileId: Int): PuzzleState {
        if (tileId == 0) return state
        val index = state.tiles.indexOf(tileId)
        return if (index < 0) state else slide(state, index)
    }

    private fun swapNumbered(tiles: MutableList<Int>, first: Boolean) {
        val numbered = tiles.indices.filter { tiles[it] != 0 }
        val (a, b) = if (first) numbered[0] to numbered[1] else numbered[numbered.size - 2] to numbered.last()
        val t = tiles[a]; tiles[a] = tiles[b]; tiles[b] = t
    }
}
