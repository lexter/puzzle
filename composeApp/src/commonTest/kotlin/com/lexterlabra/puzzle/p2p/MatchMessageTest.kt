package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.game.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchMessageTest {
    private val all = listOf(
        MatchMessage.Hello(PROTOCOL_VERSION, "Mia", "android"),
        MatchMessage.MatchConfig(5, -42L, Mode.FREE, customImage = true, imageBytes = 1234),
        MatchMessage.Ready,
        MatchMessage.ImageFailed,
        MatchMessage.Start(),
        MatchMessage.Progress(listOf(1, 0, 2, 3), moves = 7, tilesInPlace = 1),
        MatchMessage.Solved(61_000, 88),
        MatchMessage.Result(winnerIsHost = false, winnerTimeMs = 61_000),
        MatchMessage.Rematch,
        MatchMessage.Leave,
        MatchMessage.Ping,
    )

    @Test
    fun everyMessageRoundTrips() {
        for (message in all) {
            val decoded = Framing.Decoder().feed(MatchCodec.encode(Frame.Control(message))).single()
            assertEquals(Frame.Control(message), MatchCodec.fromRaw(decoded))
        }
    }

    @Test
    fun imageRoundTrips() {
        val bytes = ByteArray(1000) { it.toByte() }
        val decoded = MatchCodec.fromRaw(Framing.Decoder().feed(MatchCodec.encode(Frame.Image(bytes))).single())
        assertTrue(decoded is Frame.Image && decoded.bytes.contentEquals(bytes))
    }

    @Test
    fun unknownMessageTypeIsIgnored() {
        val raw = RawFrame(RawFrame.KIND_CONTROL, """{"type":"emoji","value":"wave"}""".encodeToByteArray())
        assertNull(MatchCodec.fromRaw(raw))
    }

    @Test
    fun unknownKindIsIgnored() = assertNull(MatchCodec.fromRaw(RawFrame(9, ByteArray(3))))
}
