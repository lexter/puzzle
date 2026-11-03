package com.lexterlabra.puzzle.p2p

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A connected peer. [incoming] completes when the connection ends. */
interface PeerConnection {
    val incoming: Flow<Frame>
    val closed: StateFlow<Boolean>
    suspend fun send(frame: Frame)
    fun close()
}
