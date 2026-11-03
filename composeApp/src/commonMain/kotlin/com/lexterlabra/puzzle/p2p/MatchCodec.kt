package com.lexterlabra.puzzle.p2p

import kotlinx.serialization.json.Json

object MatchCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    fun toRaw(frame: Frame): RawFrame = when (frame) {
        is Frame.Control -> RawFrame(RawFrame.KIND_CONTROL, json.encodeToString(MatchMessage.serializer(), frame.message).encodeToByteArray())
        is Frame.Image -> RawFrame(RawFrame.KIND_IMAGE, frame.bytes)
    }

    /** Null for unknown kinds or message types (e.g. from a newer app version). */
    fun fromRaw(raw: RawFrame): Frame? = when (raw.kind) {
        RawFrame.KIND_CONTROL -> runCatching { Frame.Control(json.decodeFromString(MatchMessage.serializer(), raw.payload.decodeToString())) }.getOrNull()
        RawFrame.KIND_IMAGE -> Frame.Image(raw.payload)
        else -> null
    }

    fun encode(frame: Frame): ByteArray = Framing.encode(toRaw(frame))
}
