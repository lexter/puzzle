package com.lexterlabra.puzzle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lexterlabra.puzzle.game.PuzzleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val store: DataStore<Preferences>) : SettingsStore {

    override val appearance: Flow<Appearance> = store.data.map { prefs ->
        prefs[APPEARANCE]?.let { name -> Appearance.entries.firstOrNull { it.name == name } } ?: Appearance.SYSTEM
    }

    override val gridSize: Flow<Int> = store.data.map { prefs ->
        (prefs[GRID_SIZE] ?: PuzzleState.DEFAULT_SIZE).coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE)
    }

    override val playerName: Flow<String?> = store.data.map { it[PLAYER_NAME] }

    override suspend fun setAppearance(appearance: Appearance) {
        store.edit { it[APPEARANCE] = appearance.name }
    }

    override suspend fun setGridSize(size: Int) {
        store.edit { it[GRID_SIZE] = size.coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE) }
    }

    override suspend fun setPlayerName(name: String) {
        store.edit { it[PLAYER_NAME] = name.trim().take(MAX_NAME_LENGTH) }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 24
        val APPEARANCE = stringPreferencesKey("appearance")
        val GRID_SIZE = intPreferencesKey("gridSize")
        val PLAYER_NAME = stringPreferencesKey("playerName")
    }
}
