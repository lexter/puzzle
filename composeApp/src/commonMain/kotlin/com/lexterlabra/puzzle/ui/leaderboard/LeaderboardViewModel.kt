package com.lexterlabra.puzzle.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexterlabra.puzzle.data.Score
import com.lexterlabra.puzzle.data.ScoreStore
import com.lexterlabra.puzzle.game.PuzzleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class LeaderboardViewModel(private val scores: ScoreStore) : ViewModel() {
    private val _selectedSize = MutableStateFlow(PuzzleState.DEFAULT_SIZE)
    val selectedSize: StateFlow<Int> = _selectedSize.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: StateFlow<List<Score>> = _selectedSize
        .flatMapLatest { scores.topTen(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun select(size: Int) {
        _selectedSize.value = size.coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE)
    }
}
