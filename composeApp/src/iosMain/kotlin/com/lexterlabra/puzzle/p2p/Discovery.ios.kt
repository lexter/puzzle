package com.lexterlabra.puzzle.p2p

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.lexterlabra.puzzle.platform.toByteArray
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject

private const val DOMAIN = "local."
private const val RESOLVE_TIMEOUT_S = 5.0

/** Bonjour via NSNetService; must be used from the main thread (Compose's Main dispatcher). */
actual class Discovery actual constructor() {
    private var published: NSNetService? = null

    actual fun advertise(name: String, port: Int) {
        stopAdvertising()
        published = NSNetService(domain = DOMAIN, type = "$SERVICE_TYPE.", name = name, port = port).also { it.publish() }
    }

    actual fun stopAdvertising() {
        published?.stop()
        published = null
    }

    actual fun browse(): Flow<List<FoundHost>> = callbackFlow {
        val browser = NSNetServiceBrowser()
        val pending = mutableListOf<NSNetService>()
        val found = linkedMapOf<String, FoundHost>()

        val resolver = ResolveDelegate { service ->
            val host = ipv4Of(service) ?: service.hostName ?: return@ResolveDelegate
            found[service.name] = FoundHost(service.name, service.name, host, service.port.toInt())
            trySend(found.values.toList())
        }
        val delegate = BrowserDelegate(
            onFound = { service ->
                pending += service
                service.delegate = resolver
                service.resolveWithTimeout(RESOLVE_TIMEOUT_S)
            },
            onRemoved = { service ->
                pending.remove(service)
                if (found.remove(service.name) != null) trySend(found.values.toList())
            },
        )
        browser.delegate = delegate
        trySend(emptyList())
        browser.searchForServicesOfType("$SERVICE_TYPE.", inDomain = DOMAIN)
        awaitClose {
            browser.stop()
            pending.forEach { it.stop() }
            // Keep delegates reachable until here (NSNetService holds them weakly).
            delegate.hashCode(); resolver.hashCode()
        }
    }
}

private const val AF_INET = 2

/**
 * Hosts listen on IPv4 only, but a `.local` name can resolve to IPv6 first, so dial the
 * IPv4 address from the resolved sockaddrs (Darwin sockaddr_in: len, family, port, addr).
 */
private fun ipv4Of(service: NSNetService): String? {
    service.addresses?.forEach { item ->
        val bytes = (item as? NSData)?.toByteArray() ?: return@forEach
        if (bytes.size >= 8 && bytes[1].toInt() == AF_INET) {
            return (4..7).joinToString(".") { (bytes[it].toInt() and 0xFF).toString() }
        }
    }
    return null
}

private class BrowserDelegate(
    private val onFound: (NSNetService) -> Unit,
    private val onRemoved: (NSNetService) -> Unit,
) : NSObject(), NSNetServiceBrowserDelegateProtocol {
    @ObjCSignatureOverride
    override fun netServiceBrowser(browser: NSNetServiceBrowser, didFindService: NSNetService, moreComing: Boolean) = onFound(didFindService)

    @ObjCSignatureOverride
    override fun netServiceBrowser(browser: NSNetServiceBrowser, didRemoveService: NSNetService, moreComing: Boolean) = onRemoved(didRemoveService)
}

private class ResolveDelegate(private val onResolved: (NSNetService) -> Unit) : NSObject(), NSNetServiceDelegateProtocol {
    override fun netServiceDidResolveAddress(sender: NSNetService) = onResolved(sender)
}
