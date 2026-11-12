package com.lexterlabra.puzzle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator or device: the real app starts and its landing screen leads into a game. */
@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun landingScreenStartsASoloGame() {
        compose.onNodeWithText("Play solo").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodesWithTag("tile-1").fetchSemanticsNodes().isNotEmpty() }
    }
}
