package com.lexterlabra.puzzle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Leaderboard kept as one small JSON list per grid size (at most 10 rows each). */
class ScoreRepository(private val store: DataStore<Preferences>) : ScoreStore {

    override fun topTen(gridSize: Int): Flow<List<Score>> = store.data.map { decode(it[key(gridSize)]) }

    override suspend fun record(score: Score): Boolean {
        var madeTopTen = false
        store.edit { prefs ->
            val ranked = rank(decode(prefs[key(score.gridSize)]) + score)
            madeTopTen = ranked.any { it === score }
            prefs[key(score.gridSize)] = json.encodeToString(serializer, ranked)
        }
        return madeTopTen
    }

    private fun decode(raw: String?): List<Score> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    private companion object {
        const val LIMIT = 10
        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(Score.serializer())
        fun key(gridSize: Int) = stringPreferencesKey("scores_$gridSize")
        fun rank(scores: List<Score>) = scores.sortedWith(compareBy({ it.timeMs }, { it.moves })).take(LIMIT)
    }
}
