package com.lexterlabra.puzzle.p2p

import kotlinx.coroutines.flow.Flow

/** A host advertising a match on the local network. */
data class FoundHost(val id: String, val name: String, val host: String, val port: Int)

/** mDNS / Bonjour advertising and browsing for [SERVICE_TYPE]. */
expect class Discovery() {
    fun advertise(name: String, port: Int)
    fun stopAdvertising()
    /** Current list of hosts; browsing stops when collection stops. */
    fun browse(): Flow<List<FoundHost>>
}
