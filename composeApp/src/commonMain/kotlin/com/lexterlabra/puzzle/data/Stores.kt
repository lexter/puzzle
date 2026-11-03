package com.lexterlabra.puzzle.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

enum class Appearance { LIGHT, DARK, SYSTEM }

@Serializable
data class Score(val gridSize: Int, val timeMs: Long, val moves: Int, val playedAt: Long)

interface ScoreStore {
    /** Top 10 for [gridSize], fastest first, fewer moves breaking ties. */
    fun topTen(gridSize: Int): Flow<List<Score>>

    /** Saves [score]; returns true when it made the top 10. */
    suspend fun record(score: Score): Boolean
}

interface SettingsStore {
    val appearance: Flow<Appearance>
    val gridSize: Flow<Int>
    val playerName: Flow<String?>
    suspend fun setAppearance(appearance: Appearance)
    suspend fun setGridSize(size: Int)
    suspend fun setPlayerName(name: String)
}
