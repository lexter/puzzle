package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.game.Mode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val PROTOCOL_VERSION = 1
const val SERVICE_TYPE = "_slidepuzzle._tcp"

@Serializable
sealed interface MatchMessage {
    @Serializable @SerialName("hello")
    data class Hello(val protocolVersion: Int, val playerName: String, val platform: String) : MatchMessage

    /** [imageBytes] > 0 means an [Frame.Image] with the host's photo follows. */
    @Serializable @SerialName("config")
    data class MatchConfig(val grid: Int, val seed: Long, val mode: Mode, val customImage: Boolean, val imageBytes: Int) : MatchMessage

    @Serializable @SerialName("ready")
    data object Ready : MatchMessage

    @Serializable @SerialName("imageFailed")
    data object ImageFailed : MatchMessage

    @Serializable @SerialName("start")
    data class Start(val countdownMs: Long = COUNTDOWN_MS) : MatchMessage

    @Serializable @SerialName("progress")
    data class Progress(val tiles: List<Int>, val moves: Int, val tilesInPlace: Int) : MatchMessage

    @Serializable @SerialName("solved")
    data class Solved(val elapsedMs: Long, val moves: Int) : MatchMessage

    @Serializable @SerialName("result")
    data class Result(val winnerIsHost: Boolean, val winnerTimeMs: Long) : MatchMessage

    @Serializable @SerialName("rematch")
    data object Rematch : MatchMessage

    @Serializable @SerialName("leave")
    data object Leave : MatchMessage

    @Serializable @SerialName("ping")
    data object Ping : MatchMessage

    companion object {
        const val COUNTDOWN_MS = 3_000L
    }
}

/** What [PeerConnection] sends and receives. */
sealed interface Frame {
    data class Control(val message: MatchMessage) : Frame
    class Image(val bytes: ByteArray) : Frame
}

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
