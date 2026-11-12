package com.lexterlabra.puzzle.fakes

import com.lexterlabra.puzzle.p2p.Frame
import com.lexterlabra.puzzle.p2p.PeerConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** In-memory connection; create both ends with [pair]. */
class FakeConnection : PeerConnection {
    private val inbox = Channel<Frame>(Channel.UNLIMITED)
    var peer: FakeConnection? = null
    val sent = mutableListOf<Frame>()

    /** When true, frames to the peer are silently dropped (simulates a dead link). */
    var dropOutgoing = false

    override val incoming: Flow<Frame> = inbox.receiveAsFlow()
    override val closed = MutableStateFlow(false)

    override suspend fun send(frame: Frame) {
        if (closed.value) return
        sent += frame
        if (!dropOutgoing) peer?.deliver(frame)
    }

    fun deliver(frame: Frame) {
        inbox.trySend(frame)
    }

    override fun close() {
        if (closed.value) return
        closed.value = true
        inbox.close()
        peer?.close()
    }

    companion object {
        fun pair(): Pair<FakeConnection, FakeConnection> {
            val a = FakeConnection(); val b = FakeConnection()
            a.peer = b; b.peer = a
            return a to b
        }
    }
}
