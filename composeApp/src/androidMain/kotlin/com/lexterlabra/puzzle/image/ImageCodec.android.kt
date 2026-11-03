package com.lexterlabra.puzzle.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

actual object ImageCodec {
    actual fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val rotation = runCatching { ExifInterface(ByteArrayInputStream(bytes)).rotationDegrees }.getOrDefault(0)
        val upright = if (rotation == 0) bitmap else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        }
        return upright.asImageBitmap()
    }

    actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().also { image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
}
