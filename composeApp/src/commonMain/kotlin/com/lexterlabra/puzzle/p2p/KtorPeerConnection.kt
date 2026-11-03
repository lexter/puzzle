package com.lexterlabra.puzzle.p2p

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [PeerConnection] over a Ktor TCP socket. Reads run on an IO scope it owns. */
class KtorPeerConnection(private val socket: Socket) : PeerConnection {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reader = socket.openReadChannel()
    private val writer = socket.openWriteChannel(autoFlush = true)
    private val writeLock = Mutex()
    private val inbox = Channel<Frame>(Channel.UNLIMITED)
    private val _closed = MutableStateFlow(false)

    override val incoming: Flow<Frame> = inbox.receiveAsFlow()
    override val closed: StateFlow<Boolean> = _closed.asStateFlow()

    init {
        scope.launch {
            val decoder = Framing.Decoder()
            val buffer = ByteArray(READ_CHUNK)
            try {
                while (true) {
                    val read = reader.readAvailable(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    decoder.feed(buffer.copyOf(read)).forEach { raw -> MatchCodec.fromRaw(raw)?.let { inbox.send(it) } }
                }
            } catch (_: Throwable) {
                // Socket reset or malformed stream: treated as a disconnect.
            } finally {
                close()
            }
        }
    }

    override suspend fun send(frame: Frame) {
        if (_closed.value) return
        try {
            writeLock.withLock { writer.writeFully(MatchCodec.encode(frame)) }
        } catch (_: Throwable) {
            close()
        }
    }

    override fun close() {
        if (_closed.value) return
        _closed.value = true
        inbox.close()
        runCatching { socket.close() }
        scope.cancel()
    }

    private companion object {
        const val READ_CHUNK = 16 * 1024
    }
}

/** Opening, accepting and dialing LAN TCP sockets. */
object PeerSockets {
    private val selector by lazy { SelectorManager(Dispatchers.IO) }

    suspend fun listen(): ServerSocket = aSocket(selector).tcp().bind("0.0.0.0", 0)

    fun port(server: ServerSocket): Int = (server.localAddress as InetSocketAddress).port

    suspend fun accept(server: ServerSocket): PeerConnection = KtorPeerConnection(server.accept())

    suspend fun connect(host: String, port: Int): PeerConnection = KtorPeerConnection(aSocket(selector).tcp().connect(host, port))
}
