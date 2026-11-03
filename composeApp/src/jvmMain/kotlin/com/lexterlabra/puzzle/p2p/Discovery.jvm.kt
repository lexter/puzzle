package com.lexterlabra.puzzle.p2p

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Test-only: an in-process registry standing in for mDNS. */
actual class Discovery actual constructor() {
    private var advertisedName: String? = null

    actual fun advertise(name: String, port: Int) {
        stopAdvertising()
        advertisedName = name
        registry.value = registry.value + FoundHost(name, name, "127.0.0.1", port)
    }

    actual fun stopAdvertising() {
        val name = advertisedName ?: return
        registry.value = registry.value.filterNot { it.id == name }
        advertisedName = null
    }

    actual fun browse(): Flow<List<FoundHost>> = registry

    companion object {
        val registry = MutableStateFlow<List<FoundHost>>(emptyList())
    }
}
