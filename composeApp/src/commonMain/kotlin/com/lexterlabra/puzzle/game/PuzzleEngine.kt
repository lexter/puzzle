package com.lexterlabra.puzzle.game

import kotlin.random.Random

object PuzzleEngine {

    /** The solved board: the gap sits in the first (top-left) cell, tiles 1.. follow in order. */
    fun solved(size: Int): PuzzleState = PuzzleState(size, (0 until size * size).toList())

    /**
     * A solvable, not-yet-solved board with the gap in the first cell. The same [seed] always
     * gives the same board.
     */
    fun shuffled(size: Int, seed: Long): PuzzleState {
        val shuffledTiles = (0 until size * size).toMutableList()
        shuffledTiles.shuffle(Random(seed))
        // Sliding keeps solvability, so parking the gap first doesn't disturb the parity fix below.
        val tiles = parkGap(PuzzleState(size, shuffledTiles)).tiles.toMutableList()
        // Swapping two numbered tiles flips solvability parity.
        if (!isSolvable(size, tiles)) swapNumbered(tiles, first = true)
        if (PuzzleState(size, tiles).isSolved) {
            // Two swaps keep it solvable but no longer solved.
            swapNumbered(tiles, first = false)
            swapNumbered(tiles, first = true)
        }
        return PuzzleState(size, tiles.toList())
    }

    /**
     * A scramble made of real slides, for animating: each frame is one legal move from the
     * previous, starting from the solved board. The gap heads in a random direction each step
     * (never straight back the way it came) and drags 1..all tiles in that line along. Finally
     * the gap is slid back to the first cell. The last frame is never solved. The same [seed]
     * always gives the same frames.
     */
    fun shuffleWalk(size: Int, seed: Long, steps: Int): List<PuzzleState> {
        val rng = Random(seed)
        val frames = ArrayList<PuzzleState>(steps + 1)
        var board = solved(size)
        var lastDirection: Direction? = null
        var taken = 0
        while (true) {
            while (taken < steps || board.isSolved) {
                val gap = board.gapIndex
                val options = Direction.entries.filter { it != lastDirection?.opposite && reach(size, gap, it) > 0 }
                val direction = options[rng.nextInt(options.size)]
                val distance = 1 + rng.nextInt(reach(size, gap, direction))
                board = slide(board, gap + direction.step(size) * distance)
                lastDirection = direction
                frames += board
                taken++
            }
            for (parkMove in parkGapMoves(board)) {
                board = slide(board, parkMove)
                frames += board
            }
            if (!board.isSolved) break
            lastDirection = null
        }
        return frames.map { it.copy(moves = 0) }
    }

    /** Slides the gap to the first cell: along its row to column 0, then up column 0. */
    private fun parkGap(state: PuzzleState): PuzzleState =
        parkGapMoves(state).fold(state) { board, index -> slide(board, index) }

    /** The cell indices to tap, in order, to bring the gap of [state] to the first cell. */
    private fun parkGapMoves(state: PuzzleState): List<Int> {
        val n = state.size
        val gap = state.gapIndex
        return buildList {
            if (gap % n != 0) add(gap - gap % n)
            if (gap / n != 0) add(0)
        }
    }

    private enum class Direction(val dRow: Int, val dCol: Int) {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

        val opposite: Direction get() = entries.first { it.dRow == -dRow && it.dCol == -dCol }
        fun step(size: Int) = dRow * size + dCol
    }

    /** How many cells the gap can travel from [gap] in [direction]. */
    private fun reach(size: Int, gap: Int, direction: Direction): Int {
        val row = gap / size
        val col = gap % size
        return when (direction) {
            Direction.UP -> row
            Direction.DOWN -> size - 1 - row
            Direction.LEFT -> col
            Direction.RIGHT -> size - 1 - col
        }
    }

    fun isSolvable(size: Int, tiles: List<Int>): Boolean {
        val numbers = tiles.filter { it != 0 }
        var inversions = 0
        for (i in numbers.indices) for (j in i + 1 until numbers.size) if (numbers[i] > numbers[j]) inversions++
        if (size % 2 == 1) return inversions % 2 == 0
        // Each vertical slide flips inversion parity and moves the gap one row. The solved board
        // has no inversions and its gap in the top row (n rows from the bottom, an even number).
        val gapRowFromBottom = size - tiles.indexOf(0) / size
        return (inversions + gapRowFromBottom) % 2 == 0
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
