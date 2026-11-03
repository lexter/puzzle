package com.lexterlabra.puzzle.p2p

import com.lexterlabra.puzzle.game.Mode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
