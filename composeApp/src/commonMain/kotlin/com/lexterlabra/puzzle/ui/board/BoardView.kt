package com.lexterlabra.puzzle.ui.board

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lexterlabra.puzzle.game.PuzzleState
import kotlin.math.min
import kotlin.math.roundToInt

const val SLIDE_MILLIS = 160
private val SMALL_BOARD = 200.dp
private val TILE_CORNER = 2.dp

/**
 * Square board. Each tile shows its home slice of [image] (which must be square) and
 * animates to its current cell. Taps report the tile number, not its index.
 */
@Composable
fun BoardView(
    state: PuzzleState,
    image: ImageBitmap,
    onTileTap: (tileId: Int) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    slideMillis: Int = SLIDE_MILLIS,
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val n = state.size
        val boardDp: Dp = min(maxWidth.value, maxHeight.value).dp
        val small = boardDp < SMALL_BOARD
        val gapDp = if (small) 2.dp else 4.dp
        val corner = RoundedCornerShape(TILE_CORNER)
        val density = LocalDensity.current
        val boardPx = with(density) { boardDp.toPx() }
        val gapPx = with(density) { gapDp.toPx() }
        val tilePx = (boardPx - gapPx * (n - 1)) / n
        val tileDp = with(density) { tilePx.toDp() }
        val cell = image.width.toFloat() / n

        state.tiles.forEachIndexed { index, id ->
            if (id == 0) return@forEachIndexed
            key(id) {
                val target = IntOffset(
                    ((index % n) * (tilePx + gapPx)).roundToInt(),
                    ((index / n) * (tilePx + gapPx)).roundToInt(),
                )
                val offset by animateIntOffsetAsState(target, tween(slideMillis), label = "tile-$id")
                val home = id
                val srcLeft = ((home % n) * cell).toInt()
                val srcTop = ((home / n) * cell).toInt()
                val srcRight = (((home % n) + 1) * cell).toInt().coerceAtMost(image.width)
                val srcBottom = (((home / n) + 1) * cell).toInt().coerceAtMost(image.height)
                Canvas(
                    Modifier
                        .offset { offset }
                        .size(tileDp)
                        .clip(corner)
                        .border(0.5.dp, Color.White.copy(alpha = 0.55f), corner)
                        .testTag("tile-$id")
                        .semantics { contentDescription = "Tile $id" }
                        .then(if (interactive) Modifier.clickable { onTileTap(id) } else Modifier),
                ) {
                    drawImage(
                        image = image,
                        srcOffset = IntOffset(srcLeft, srcTop),
                        srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }
        }
    }
}
