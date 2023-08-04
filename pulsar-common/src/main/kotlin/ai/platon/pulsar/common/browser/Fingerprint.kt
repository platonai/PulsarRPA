package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.proxy.ProxyEntry
import org.apache.commons.collections4.ComparatorUtils

/**
 * The browser fingerprint
 * */
data class Fingerprint(
    val browserType: BrowserType,
    var proxyServer: String? = null,
    var proxyUsername: String? = null,
    var proxyPassword: String? = null,
    var username: String? = null,
    var password: String? = null,
    var userAgent: String? = null,
): Comparable<Fingerprint> {
    private val comp = ComparatorUtils.nullLowComparator {
            o1: String, o2: String -> o1.compareTo(o2)
    }
    
    fun setProxy(protocol: String, hostPort: String, username: String?, password: String?) {
        proxyServer = if (protocol == "http") hostPort else "$protocol://$hostPort"
        if (!username.isNullOrBlank()) {
            proxyUsername = username
            proxyPassword = password
        }
    }
    
    fun setProxy(proxy: ProxyEntry) = setProxy(proxy.protocol, proxy.hostPort, proxy.username, proxy.password)
    
    override fun compareTo(other: Fingerprint): Int {
        var r = browserType.compareTo(other.browserType)
        if (r != 0) {
            return r
        }

        listOf(
            proxyServer to other.proxyServer,
            username to other.username,
            userAgent to other.userAgent,
        ).forEach {
            r = comp.compare(it.first, it.second)
            if (r != 0) {
                return r
            }
        }

        return 0
    }

    override fun hashCode() = toString().hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Fingerprint && listOf(
            browserType to other.browserType,
            proxyServer to other.proxyServer,
            username to other.username,
        ).all { it.first == it.second }
    }

    override fun toString(): String = listOfNotNull(
        browserType, proxyServer, username
    ).joinToString()
}
