package com.lexterlabra.puzzle.p2p

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorPeerConnectionTest {
    @Test
    fun loopbackExchangesFrames() = runBlocking {
        withTimeout(10_000) {
            val server = PeerSockets.listen()
            val accepted = async { PeerSockets.accept(server) }
            val client = PeerSockets.connect("127.0.0.1", PeerSockets.port(server))
            val host = accepted.await()

            val hello = Frame.Control(MatchMessage.Hello(PROTOCOL_VERSION, "Mia", "android"))
            val photo = ByteArray(200_000) { (it % 13).toByte() }
            client.send(hello)
            client.send(Frame.Image(photo))
            host.send(Frame.Control(MatchMessage.Ready))

            val received = host.incoming
            assertEquals(hello, received.first())
            val image = received.first()
            assertTrue(image is Frame.Image && image.bytes.contentEquals(photo))
            assertEquals(Frame.Control(MatchMessage.Ready), client.incoming.first())

            client.close(); host.close(); server.close()
        }
    }

    @Test
    fun closeIsObserved() = runBlocking {
        withTimeout(10_000) {
            val server = PeerSockets.listen()
            val accepted = async { PeerSockets.accept(server) }
            val client = PeerSockets.connect("127.0.0.1", PeerSockets.port(server))
            val host = accepted.await()
            client.close()
            host.closed.first { it }
            server.close()
        }
    }
}
