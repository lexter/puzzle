package com.lexterlabra.puzzle.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleImageTest {
    @Test
    fun cropLandscapeCentersSquare() = assertEquals(IntRect(500, 0, 3500, 3000), PuzzleImage.cropRect(4000, 3000))

    @Test
    fun cropPortraitCentersSquare() = assertEquals(IntRect(0, 500, 3000, 3500), PuzzleImage.cropRect(3000, 4000))

    @Test
    fun rejectsTinyImage() {
        val tiny = ImageCodec.encodeJpeg(ImageBitmap(40, 40), 90)
        assertTrue(PuzzleImage.prepare(tiny).exceptionOrNull() is ImageTooSmall)
    }

    @Test
    fun rejectsGarbageBytes() {
        assertTrue(PuzzleImage.prepare(Random(1).nextBytes(512)).exceptionOrNull() is ImageDecodeFailed)
    }

    @Test
    fun preparesSquareCappedAtMaxSide() {
        val big = ImageCodec.encodeJpeg(ImageBitmap(2400, 1600), 80)
        val prepared = PuzzleImage.prepare(big).getOrThrow()
        assertEquals(PuzzleImage.MAX_SIDE, prepared.width)
        assertEquals(PuzzleImage.MAX_SIDE, prepared.height)
    }
}
