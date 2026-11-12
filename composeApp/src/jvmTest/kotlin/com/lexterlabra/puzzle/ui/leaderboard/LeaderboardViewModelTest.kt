package com.lexterlabra.puzzle.ui.leaderboard

import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.fakes.FakeScoreStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun leaderboardSwitchesSize() = runTest {
        val store = FakeScoreStore()
        store.record(Score(4, 10_000, 40, 0))
        store.record(Score(5, 20_000, 80, 0))
        val vm = LeaderboardViewModel(store)
        assertEquals(listOf(4), vm.rows.value.map { it.gridSize })
        vm.select(5)
        assertEquals(listOf(5), vm.rows.value.map { it.gridSize })
    }

    @Test
    fun shortDateFormatsMonthAndDay() {
        // 2026-11-02T12:00:00Z is Nov 2 in every UTC−11…UTC+11 zone.
        assertEquals("Nov 2", shortDate(1_793_620_800_000))
    }
}
