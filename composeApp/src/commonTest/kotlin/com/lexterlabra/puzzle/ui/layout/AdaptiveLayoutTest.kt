package com.lexterlabra.puzzle.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLayoutTest {
    @Test fun layoutCompactOnPhonePortrait() = assertEquals(LayoutMode.COMPACT, layoutModeFor(390, 844, null))

    @Test fun layoutCompactOnPhoneLandscapeNarrow() = assertEquals(LayoutMode.COMPACT, layoutModeFor(560, 390, null))

    @Test fun layoutSidePanelOnTabletLandscape() = assertEquals(LayoutMode.SIDE_PANEL, layoutModeFor(1180, 820, null))

    @Test fun layoutCompactOnTabletPortrait() = assertEquals(LayoutMode.COMPACT, layoutModeFor(820, 1180, null))

    @Test fun layoutSidePanelOnUnfoldedFoldable() =
        assertEquals(LayoutMode.SIDE_PANEL, layoutModeFor(840, 900, FoldInfo(isSeparating = true, isVertical = true)))
}
