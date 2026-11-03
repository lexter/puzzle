package com.lexterlabra.puzzle.data

import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    private val store = createDataStore(File.createTempFile("puzzle", ".preferences_pb").also { it.delete() }.absolutePath)
    private val scores = ScoreRepository(store)
    private val settings = SettingsRepository(store)

    @Test
    fun topTenOrdersByTimeThenMoves() = runBlocking {
        scores.record(Score(4, 60_000, 90, 1))
        scores.record(Score(4, 50_000, 120, 2))
        scores.record(Score(4, 50_000, 100, 3))
        assertEquals(listOf(50_000L to 100, 50_000L to 120, 60_000L to 90), scores.topTen(4).first().map { it.timeMs to it.moves })
    }

    @Test
    fun topTenLimitsToTen() = runBlocking {
        for (i in 1..12) scores.record(Score(4, i * 1_000L, 50, i.toLong()))
        val top = scores.topTen(4).first()
        assertEquals(10, top.size)
        assertEquals(10_000L, top.last().timeMs)
    }

    @Test
    fun topTenFiltersBySize() = runBlocking {
        scores.record(Score(5, 1_000, 10, 1))
        assertTrue(scores.topTen(4).first().isEmpty())
        assertEquals(1, scores.topTen(5).first().size)
    }

    @Test
    fun recordReportsTopTenEntry() = runBlocking {
        for (i in 1..10) scores.record(Score(4, i * 1_000L, 50, i.toLong()))
        assertFalse(scores.record(Score(4, 99_000, 50, 11)))
        assertTrue(scores.record(Score(4, 500, 50, 12)))
    }

    @Test
    fun settingsDefaults() = runBlocking {
        assertEquals(Appearance.SYSTEM, settings.appearance.first())
        assertEquals(4, settings.gridSize.first())
        assertNull(settings.playerName.first())
    }

    @Test
    fun gridSizeClamped() = runBlocking {
        settings.setGridSize(12); assertEquals(10, settings.gridSize.first())
        settings.setGridSize(2); assertEquals(4, settings.gridSize.first())
    }

    @Test
    fun appearancePersists() = runBlocking {
        settings.setAppearance(Appearance.DARK)
        assertEquals(Appearance.DARK, settings.appearance.first())
    }
}
