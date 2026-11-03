package com.lexterlabra.puzzle.ui.lobby

import com.lexterlabra.puzzle.fakes.FakeSettingsStore
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.p2p.MatchHub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.lobby(): LobbyViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val hub = MatchHub(backgroundScope)
        return LobbyViewModel(Mode.TIME_BASED, FakeSettingsStore(grid = 5), hub, deviceName = { "Pixel" })
    }

    @Test
    fun usesSavedNameAndGrid() = runTest {
        val vm = lobby(); runCurrent()
        assertEquals("Tester", vm.state.value.myName)
        assertEquals(5, vm.state.value.grid)
        assertEquals(LobbyTab.HOST, vm.state.value.tab)
    }

    @Test
    fun noHostsHintAfter10s() = runTest {
        val vm = lobby(); runCurrent()
        vm.selectTab(LobbyTab.JOIN); runCurrent()
        advanceTimeBy(9_000); runCurrent()
        assertFalse(vm.state.value.noHostsHint)
        advanceTimeBy(1_001); runCurrent()
        assertTrue(vm.state.value.noHostsHint)
    }

    @Test
    fun gridIsClamped() = runTest {
        val vm = lobby(); runCurrent()
        vm.setGrid(42); runCurrent()
        assertEquals(10, vm.state.value.grid)
    }
}
