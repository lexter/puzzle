@file:Suppress("DEPRECATION") // resolveService / NsdServiceInfo.host: fine on API 29–36 for one-at-a-time resolves.

package com.lexterlabra.puzzle.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.lexterlabra.puzzle.platform.AndroidAppContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class Discovery actual constructor() {
    private val context = AndroidAppContext.get()
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registration: NsdManager.RegistrationListener? = null

    actual fun advertise(name: String, port: Int) {
        stopAdvertising()
        val info = NsdServiceInfo().apply {
            serviceName = name
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = listener
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    actual fun stopAdvertising() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
    }

    actual fun browse(): Flow<List<FoundHost>> = callbackFlow {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("slide-puzzle-discovery").apply { setReferenceCounted(false); acquire() }
        val found = linkedMapOf<String, FoundHost>()
        val toResolve = Channel<NsdServiceInfo>(Channel.UNLIMITED)

        launch {
            // NsdManager resolves one service at a time.
            for (service in toResolve) {
                resolve(service)?.let {
                    found[it.id] = it
                    trySend(found.values.toList())
                }
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(service: NsdServiceInfo) { toResolve.trySend(service) }
            override fun onServiceLost(service: NsdServiceInfo) {
                if (found.remove(service.serviceName) != null) trySend(found.values.toList())
            }
        }
        trySend(emptyList())
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            runCatching { nsd.stopServiceDiscovery(listener) }
            toResolve.close()
            lock.release()
        }
    }

    private suspend fun resolve(service: NsdServiceInfo): FoundHost? = suspendCancellableCoroutine { cont ->
        nsd.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val address = info.host?.hostAddress
                if (cont.isActive) cont.resume(address?.let { FoundHost(info.serviceName, info.serviceName, it, info.port) })
            }
        })
    }
}
