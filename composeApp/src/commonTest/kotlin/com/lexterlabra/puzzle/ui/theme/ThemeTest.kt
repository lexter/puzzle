package com.lexterlabra.puzzle.ui.theme

import com.lexterlabra.puzzle.data.Appearance
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTest {
    @Test
    fun resolvesAppearance() {
        assertEquals(false, resolveDark(Appearance.LIGHT, systemDark = true))
        assertEquals(true, resolveDark(Appearance.DARK, systemDark = false))
        assertEquals(true, resolveDark(Appearance.SYSTEM, systemDark = true))
        assertEquals(false, resolveDark(Appearance.SYSTEM, systemDark = false))
    }
}
