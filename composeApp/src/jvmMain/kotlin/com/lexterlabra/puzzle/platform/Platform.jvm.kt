package com.lexterlabra.puzzle.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Test-only target: pickers do nothing. */
@Composable
actual fun rememberImagePicker(onResult: (PickResult) -> Unit): ImagePicker = remember {
    object : ImagePicker {
        override val cameraAvailable = false
        override fun pickFromGallery() = onResult(PickResult.Cancelled)
        override fun takePhoto() = onResult(PickResult.Cancelled)
    }
}

actual object DeviceName {
    actual fun get(): String = "Desktop"
}

actual val platformName: String = "jvm"

actual fun openAppSettings() = Unit

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
