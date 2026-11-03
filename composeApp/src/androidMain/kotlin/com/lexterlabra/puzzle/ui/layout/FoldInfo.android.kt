package com.lexterlabra.puzzle.ui.layout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

@Composable
actual fun rememberFoldInfo(): FoldInfo? {
    val activity = LocalContext.current.findActivity() ?: return null
    val flow = remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { info ->
            info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let {
                FoldInfo(isSeparating = it.isSeparating, isVertical = it.orientation == FoldingFeature.Orientation.VERTICAL)
            }
        }
    }
    val fold by flow.collectAsState(initial = null)
    return fold
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
