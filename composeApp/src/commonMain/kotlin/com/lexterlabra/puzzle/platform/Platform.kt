package com.lexterlabra.puzzle.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

sealed interface PickResult {
    class Picked(val bytes: ByteArray) : PickResult
    data object Cancelled : PickResult
    data object PermissionDenied : PickResult
}

interface ImagePicker {
    val cameraAvailable: Boolean
    fun pickFromGallery()
    fun takePhoto()
}

@Composable
expect fun rememberImagePicker(onResult: (PickResult) -> Unit): ImagePicker

expect object DeviceName {
    fun get(): String
}

/** "android" or "ios"; sent in the P2P handshake. */
expect val platformName: String

/** Opens this app's page in the system Settings (for denied permissions). */
expect fun openAppSettings()

/** System back gesture/button. No-op on iOS (screens show their own back button). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

class Haptics(private val feedback: HapticFeedback) {
    fun tap() = feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun success() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
