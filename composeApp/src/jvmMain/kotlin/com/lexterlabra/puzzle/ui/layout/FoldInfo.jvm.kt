package com.lexterlabra.puzzle.ui.layout

import androidx.compose.runtime.Composable

/** No hinge API here; iPad split view is handled by window width alone. */
@Composable
actual fun rememberFoldInfo(): FoldInfo? = null
