package com.lexterlabra.puzzle.ui.lobby

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexterlabra.puzzle.data.SettingsStore
import com.lexterlabra.puzzle.game.Mode
import com.lexterlabra.puzzle.game.PuzzleState
import com.lexterlabra.puzzle.image.ImageCodec
import com.lexterlabra.puzzle.image.ImageTooSmall
import com.lexterlabra.puzzle.image.PuzzleImage
import com.lexterlabra.puzzle.p2p.FoundHost
import com.lexterlabra.puzzle.p2p.MatchHub
import com.lexterlabra.puzzle.platform.DeviceName
import com.lexterlabra.puzzle.platform.PickResult
import com.lexterlabra.puzzle.ui.game.UserMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LobbyTab { HOST, JOIN }

data class LobbyUiState(
    val tab: LobbyTab = LobbyTab.HOST,
    val myName: String = "",
    val grid: Int = PuzzleState.DEFAULT_SIZE,
    val mode: Mode = Mode.TIME_BASED,
    val customImage: ImageBitmap? = null,
    val customJpeg: ByteArray? = null,
    val hosts: List<FoundHost> = emptyList(),
    val noHostsHint: Boolean = false,
    val joining: String? = null,
    val error: String? = null,
    val message: UserMessage? = null,
)

const val NO_HOSTS_HINT_MS = 10_000L

class LobbyViewModel(
    mode: Mode,
    private val settings: SettingsStore,
    private val hub: MatchHub,
    private val deviceName: () -> String = DeviceName::get,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(LobbyUiState(mode = mode))
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    val controller get() = hub.controller

    private var browseJob: Job? = null
    private var handedOff = false

    init {
        viewModelScope.launch {
            val name = settings.playerName.first()?.takeIf { it.isNotBlank() } ?: deviceName()
            val grid = settings.gridSize.first()
            _state.update { it.copy(myName = name, grid = grid) }
            selectTab(LobbyTab.HOST)
        }
    }

    fun selectTab(tab: LobbyTab) {
        _state.update { it.copy(tab = tab, error = null, hosts = emptyList(), noHostsHint = false, joining = null) }
        browseJob?.cancel()
        when (tab) {
            LobbyTab.HOST -> hub.host(_state.value.myName) { error ->
                _state.update { it.copy(error = "Couldn't start hosting (${error.message ?: "network error"}). Check Wi-Fi.") }
            }
            LobbyTab.JOIN -> {
                hub.end()
                browseJob = viewModelScope.launch {
                    launch {
                        delay(NO_HOSTS_HINT_MS)
                        if (_state.value.hosts.isEmpty()) _state.update { it.copy(noHostsHint = true) }
                    }
                    hub.discovery.browse().collect { hosts ->
                        _state.update { it.copy(hosts = hosts, noHostsHint = it.noHostsHint && hosts.isEmpty()) }
                    }
                }
            }
        }
    }

    fun retry() = selectTab(_state.value.tab)

    /** The other player left or the link dropped while still in the lobby: start over. */
    fun onMatchLost() {
        selectTab(_state.value.tab)
        _state.update { it.copy(error = "The other player left. You can host or join again.") }
    }

    fun join(host: FoundHost) {
        viewModelScope.launch {
            _state.update { it.copy(joining = host.name, error = null) }
            try {
                browseJob?.cancel()
                hub.join(host, _state.value.myName)
            } catch (e: Throwable) {
                _state.update { it.copy(joining = null, error = "Couldn't connect to ${host.name}. Are you on the same Wi-Fi?") }
            }
        }
    }

    /** Host accepts the waiting player: sends grid, mode and picture. */
    fun accept() {
        val state = _state.value
        hub.controller.value?.configure(state.grid, state.mode, state.customJpeg)
    }

    /** Host turns the joining player away and keeps hosting. */
    fun decline() = selectTab(LobbyTab.HOST)

    fun startRace() {
        hub.controller.value?.startRace()
    }

    fun setGrid(size: Int) {
        val clamped = size.coerceIn(PuzzleState.MIN_SIZE, PuzzleState.MAX_SIZE)
        _state.update { it.copy(grid = clamped) }
        viewModelScope.launch { settings.setGridSize(clamped) }
    }

    fun setPicture(result: PickResult) {
        when (result) {
            PickResult.Cancelled -> Unit
            PickResult.PermissionDenied -> _state.update { it.copy(message = UserMessage.CameraDenied) }
            is PickResult.Picked -> viewModelScope.launch {
                val prepared = withContext(workDispatcher) {
                    PuzzleImage.prepare(result.bytes).map { it to ImageCodec.encodeJpeg(it, PuzzleImage.P2P_JPEG_QUALITY) }
                }
                prepared.fold(
                    onSuccess = { (image, jpeg) -> _state.update { it.copy(customImage = image, customJpeg = jpeg) } },
                    onFailure = { e ->
                        val message = if (e is ImageTooSmall) UserMessage.PhotoTooSmall else UserMessage.PhotoUnreadable
                        _state.update { it.copy(message = message) }
                    },
                )
            }
        }
    }

    fun useScenic() = _state.update { it.copy(customImage = null, customJpeg = null) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Called when navigating to the race so the match isn't torn down with this screen. */
    fun handOff() {
        handedOff = true
    }

    override fun onCleared() {
        if (!handedOff) hub.end()
    }
}
