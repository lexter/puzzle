package com.lexterlabra.puzzle.ui.layout

import androidx.compose.runtime.Composable

enum class LayoutMode { COMPACT, SIDE_PANEL }

/** Fold/hinge state from the window (Android foldables). Null when the device has no fold. */
data class FoldInfo(val isSeparating: Boolean, val isVertical: Boolean)

@Composable
expect fun rememberFoldInfo(): FoldInfo?

private const val SIDE_PANEL_MIN_WIDTH_DP = 600

/**
 * Phone portrait → [LayoutMode.COMPACT]. Wide landscape windows (tablets, unfolded foldables,
 * iPad split view wide enough) and any window split by a vertical hinge → [LayoutMode.SIDE_PANEL],
 * where the board sits on the start side and never crosses the hinge.
 */
fun layoutModeFor(widthDp: Int, heightDp: Int, fold: FoldInfo?): LayoutMode = when {
    fold != null && fold.isSeparating && fold.isVertical -> LayoutMode.SIDE_PANEL
    widthDp >= SIDE_PANEL_MIN_WIDTH_DP && widthDp > heightDp -> LayoutMode.SIDE_PANEL
    else -> LayoutMode.COMPACT
}
