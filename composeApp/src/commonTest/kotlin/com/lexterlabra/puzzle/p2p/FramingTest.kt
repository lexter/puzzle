package com.lexterlabra.puzzle.p2p

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FramingTest {
    private fun frame(kind: Byte, size: Int) = RawFrame(kind, ByteArray(size) { (it % 251).toByte() })

    @Test
    fun decoderHandlesSplitFrames() {
        val bytes = Framing.encode(frame(RawFrame.KIND_CONTROL, 300))
        val decoder = Framing.Decoder()
        val out = bytes.flatMap { decoder.feed(byteArrayOf(it)) }
        assertEquals(1, out.size)
        assertTrue(out[0].payload.contentEquals(frame(RawFrame.KIND_CONTROL, 300).payload))
    }

    @Test
    fun decoderHandlesMergedFrames() {
        val decoder = Framing.Decoder()
        val out = decoder.feed(Framing.encode(frame(RawFrame.KIND_CONTROL, 10)) + Framing.encode(frame(RawFrame.KIND_IMAGE, 20)))
        assertEquals(listOf(RawFrame.KIND_CONTROL to 10, RawFrame.KIND_IMAGE to 20), out.map { it.kind to it.payload.size })
    }

    @Test
    fun imageFrameKeepsBytes() {
        val image = frame(RawFrame.KIND_IMAGE, 200_000)
        val out = Framing.Decoder().feed(Framing.encode(image)).single()
        assertTrue(out.payload.contentEquals(image.payload))
    }

    @Test
    fun rejectsOversizeFrame() {
        assertFailsWith<IllegalStateException> { Framing.encode(frame(RawFrame.KIND_IMAGE, Framing.MAX_FRAME_BYTES)) }
    }

    @Test
    fun decoderRejectsOversizeHeader() {
        assertFailsWith<IllegalStateException> { Framing.Decoder().feed(byteArrayOf(0x7F, 0, 0, 0, 0)) }
    }

    @Test
    fun emptyChunkYieldsNothing() = assertTrue(Framing.Decoder().feed(ByteArray(0)).isEmpty())
}
