package com.lexterlabra.puzzle.p2p

/** One unit on the wire: a kind byte plus its payload. */
class RawFrame(val kind: Byte, val payload: ByteArray) {
    companion object {
        const val KIND_CONTROL: Byte = 0
        const val KIND_IMAGE: Byte = 1
    }
}

/**
 * Wire format: 4-byte big-endian length (kind + payload), 1 kind byte, payload.
 * Frames larger than [MAX_FRAME_BYTES] are refused on both ends.
 */
object Framing {
    const val MAX_FRAME_BYTES = 8 * 1024 * 1024

    fun encode(frame: RawFrame): ByteArray {
        val length = frame.payload.size + 1
        check(length <= MAX_FRAME_BYTES) { "frame too large" }
        val out = ByteArray(4 + length)
        out[0] = (length ushr 24).toByte()
        out[1] = (length ushr 16).toByte()
        out[2] = (length ushr 8).toByte()
        out[3] = length.toByte()
        out[4] = frame.kind
        frame.payload.copyInto(out, destinationOffset = 5)
        return out
    }

    /** Reassembles frames from arbitrary TCP chunks. Not thread-safe; use from one reader. */
    class Decoder {
        private var buffer = ByteArray(0)

        fun feed(chunk: ByteArray): List<RawFrame> {
            if (chunk.isNotEmpty()) buffer += chunk
            val frames = mutableListOf<RawFrame>()
            var offset = 0
            while (buffer.size - offset >= 4) {
                val length = readInt(buffer, offset)
                check(length in 1..MAX_FRAME_BYTES) { "frame too large" }
                if (buffer.size - offset - 4 < length) break
                val kind = buffer[offset + 4]
                frames += RawFrame(kind, buffer.copyOfRange(offset + 5, offset + 4 + length))
                offset += 4 + length
            }
            if (offset > 0) buffer = buffer.copyOfRange(offset, buffer.size)
            return frames
        }

        private fun readInt(bytes: ByteArray, at: Int): Int =
            ((bytes[at].toInt() and 0xFF) shl 24) or
                ((bytes[at + 1].toInt() and 0xFF) shl 16) or
                ((bytes[at + 2].toInt() and 0xFF) shl 8) or
                (bytes[at + 3].toInt() and 0xFF)
    }
}
