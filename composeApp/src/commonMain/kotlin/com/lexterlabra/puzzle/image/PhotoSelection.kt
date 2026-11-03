package com.lexterlabra.puzzle.image

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A prepared custom picture: the square bitmap for the board and its JPEG for sending to a peer. */
class ChosenPhoto(val image: ImageBitmap, val jpeg: ByteArray)

/**
 * The photo picked on "Make your puzzle", shared with the solo game and the lobby so the
 * player's own picture follows them into whichever mode they start. Null = scenic default.
 */
class PhotoSelection {
    private val _chosen = MutableStateFlow<ChosenPhoto?>(null)
    val chosen: StateFlow<ChosenPhoto?> = _chosen.asStateFlow()

    fun choose(photo: ChosenPhoto?) {
        _chosen.value = photo
    }
}
