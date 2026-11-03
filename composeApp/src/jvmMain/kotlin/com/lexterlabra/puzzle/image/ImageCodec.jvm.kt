package com.lexterlabra.puzzle.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual object ImageCodec {
    actual fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap? =
        runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

    actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
        Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: ByteArray(0)
}
