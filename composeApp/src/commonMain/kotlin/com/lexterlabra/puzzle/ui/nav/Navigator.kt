package com.lexterlabra.puzzle.ui.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.lexterlabra.puzzle.game.Mode

sealed interface Screen {
    data object Landing : Screen
    data object Leaderboard : Screen
    data class Game(val mode: Mode) : Screen
    data class Lobby(val mode: Mode) : Screen
    data object Race : Screen
    data class Photo(val mode: Mode) : Screen
}

/** One back-stack entry; owns the ViewModels of its screen and clears them when popped. */
class NavEntry(val screen: Screen) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

/** Activity-scoped (survives rotation) back stack. */
class Navigator : ViewModel() {
    private val stack = mutableStateListOf(NavEntry(Screen.Landing))

    val current: NavEntry get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.add(NavEntry(screen))
    }

    /** Replaces the top entry (e.g. Lobby → Race). */
    fun replace(screen: Screen) {
        val top = stack.removeAt(stack.lastIndex)
        stack.add(NavEntry(screen))
        top.viewModelStore.clear()
    }

    fun pop(): Boolean {
        if (!canPop) return false
        stack.removeAt(stack.lastIndex).viewModelStore.clear()
        return true
    }

    fun popToRoot() {
        while (canPop) pop()
    }

    override fun onCleared() {
        stack.forEach { it.viewModelStore.clear() }
    }
}
