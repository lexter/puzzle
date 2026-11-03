package com.lexterlabra.puzzle.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.image.ChosenPhoto
import com.lexterlabra.puzzle.image.ImageCodec
import com.lexterlabra.puzzle.image.ImageTooSmall
import com.lexterlabra.puzzle.image.PhotoSelection
import com.lexterlabra.puzzle.image.PuzzleImage
import com.lexterlabra.puzzle.platform.PickResult
import com.lexterlabra.puzzle.ui.game.UserMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoUiState(
    val photo: ChosenPhoto? = null,
    val grid: Int = PuzzleState.DEFAULT_SIZE,
    val loading: Boolean = false,
    val message: UserMessage? = null,
)

/** "Make your puzzle": pick a camera/gallery photo (or the scenic default) and a grid size. */
class PhotoViewModel(
    private val settings: SettingsStore,
    private val selection: PhotoSelection,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(PhotoUiState(photo = selection.chosen.value))
    val state: StateFlow<PhotoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val grid = settings.gridSize.first()
            _state.update { it.copy(grid = grid) }
        }
    }

    fun setPicture(result: PickResult) {
        when (result) {
            PickResult.Cancelled -> Unit
            PickResult.PermissionDenied -> _state.update { it.copy(message = UserMessage.CameraDenied) }
            is PickResult.Picked -> viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                val prepared = withContext(workDispatcher) {
                    PuzzleImage.prepare(result.bytes).map { ChosenPhoto(it, ImageCodec.encodeJpeg(it, PuzzleImage.P2P_JPEG_QUALITY)) }
                }
                prepared.fold(
                    onSuccess = { photo ->
                        selection.choose(photo)
                        _state.update { it.copy(photo = photo, loading = false) }
                    },
                    onFailure = { e ->
                        val message = if (e is ImageTooSmall) UserMessage.PhotoTooSmall else UserMessage.PhotoUnreadable
                        _state.update { it.copy(loading = false, message = message) }
                    },
                )
            }
        }
    }

    fun useScenic() {
        selection.choose(null)
        _state.update { it.copy(photo = null) }
    }

    fun setGrid(size: Int) {
        val clamped = size.coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE)
        _state.update { it.copy(grid = clamped) }
        viewModelScope.launch { settings.setGridSize(clamped) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
