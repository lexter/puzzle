package com.lexterlabra.puzzle.game

/**
 * Immutable board. [tiles] is row-major with 0 for the gap; tile `k` belongs at index `k - 1`,
 * so the solved board is `1, 2, …, n² - 1, 0`.
 */
data class PuzzleState(val size: Int, val tiles: List<Int>, val moves: Int = 0) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size must be in $MIN_SIZE..$MAX_SIZE" }
        require(tiles.size == size * size) { "expected ${size * size} tiles" }
    }

    val gapIndex: Int get() = tiles.indexOf(0)
    val tileCount: Int get() = size * size - 1
    val tilesInPlace: Int get() = tiles.withIndex().count { (index, tile) -> tile != 0 && tile == index + 1 }
    val isSolved: Boolean get() = tilesInPlace == tileCount

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 10
        const val DEFAULT_SIZE = 4
    }
}
