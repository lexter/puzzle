package com.lexterlabra.puzzle.fakes

import com.lexterlabra.puzzle.data.Appearance
import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.data.ScoreStore
import com.lexterlabra.puzzle.data.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScoreStore : ScoreStore {
    val recorded = mutableListOf<Score>()
    private val all = MutableStateFlow<List<Score>>(emptyList())

    override fun topTen(gridSize: Int): Flow<List<Score>> =
        all.map { list -> list.filter { it.gridSize == gridSize }.sortedWith(compareBy({ it.timeMs }, { it.moves })).take(10) }

    override suspend fun record(score: Score): Boolean {
        recorded += score
        all.value = all.value + score
        return true
    }
}

class FakeSettingsStore(grid: Int = 4) : SettingsStore {
    override val appearance = MutableStateFlow(Appearance.SYSTEM)
    override val gridSize = MutableStateFlow(grid)
    override val playerName = MutableStateFlow<String?>("Tester")
    override suspend fun setAppearance(appearance: Appearance) { this.appearance.value = appearance }
    override suspend fun setGridSize(size: Int) { gridSize.value = size.coerceIn(4, 10) }
    override suspend fun setPlayerName(name: String) { playerName.value = name }
}
