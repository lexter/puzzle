package com.lexterlabra.puzzle.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lexterlabra.puzzle.platform.toByteArray
import com.lexterlabra.puzzle.platform.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max
import kotlin.math.min

actual object ImageCodec {
    @OptIn(ExperimentalForeignApi::class)
    actual fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap? {
        val source = UIImage.imageWithData(bytes.toNSData()) ?: return null
        val (width, height) = source.size.useContents { width to height }
        if (width <= 0.0 || height <= 0.0) return null
        // Redraw to apply EXIF orientation and downscale in one pass.
        val scale = min(1.0, maxSide / max(width, height))
        val targetWidth = width * scale
        val targetHeight = height * scale
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
        val upright = renderer.imageWithActions { _ ->
            source.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        }
        val jpeg = UIImageJPEGRepresentation(upright, 0.95) ?: return null
        return runCatching { Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap() }.getOrNull()
    }

    actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
        Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: ByteArray(0)
}
