package com.lexterlabra.puzzle.image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageTooSmall : Exception("This photo is too small to make a puzzle.")
class ImageDecodeFailed : Exception("This photo couldn't be opened.")

/** Platform decoding (with EXIF orientation applied) and JPEG encoding. */
expect object ImageCodec {
    /** Decodes [bytes], downsampling so the longest side is about [maxSide] or less. Null if unreadable. */
    fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap?
    fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray
}

object PuzzleImage {
    const val MAX_SIDE = 1024
    const val MIN_SIDE = 64
    const val P2P_JPEG_QUALITY = 80

    /** Largest centered square inside a [width]×[height] picture. */
    fun cropRect(width: Int, height: Int): IntRect {
        val side = min(width, height)
        val left = (width - side) / 2
        val top = (height - side) / 2
        return IntRect(left, top, left + side, top + side)
    }

    /** Decode → center square crop → at most [MAX_SIDE] px. */
    fun prepare(bytes: ByteArray): Result<ImageBitmap> {
        val decoded = runCatching { ImageCodec.decode(bytes, 2 * MAX_SIDE) }.getOrNull()
            ?: return Result.failure(ImageDecodeFailed())
        if (min(decoded.width, decoded.height) < MIN_SIDE) return Result.failure(ImageTooSmall())
        val rect = cropRect(decoded.width, decoded.height)
        return Result.success(cropScaled(decoded, rect, min(rect.width, MAX_SIDE)))
    }

    /** Uniformly scales [image] so its longest side is [maxSide] (used for the blurred backdrop). */
    fun downscale(image: ImageBitmap, maxSide: Int): ImageBitmap {
        val scale = maxSide.toFloat() / max(image.width, image.height)
        if (scale >= 1f) return image
        val w = (image.width * scale).roundToInt().coerceAtLeast(1)
        val h = (image.height * scale).roundToInt().coerceAtLeast(1)
        return draw(image, IntRect(0, 0, image.width, image.height), IntSize(w, h))
    }

    fun cropScaled(image: ImageBitmap, source: IntRect, side: Int): ImageBitmap =
        draw(image, source, IntSize(side, side))

    private fun draw(image: ImageBitmap, source: IntRect, outSize: IntSize): ImageBitmap {
        val out = ImageBitmap(outSize.width, outSize.height)
        Canvas(out).drawImageRect(
            image = image,
            srcOffset = IntOffset(source.left, source.top),
            srcSize = IntSize(source.width, source.height),
            dstOffset = IntOffset.Zero,
            dstSize = outSize,
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
        return out
    }
}
