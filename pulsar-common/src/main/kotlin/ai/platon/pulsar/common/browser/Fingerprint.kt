package ai.platon.pulsar.common.browser

import org.apache.commons.collections4.ComparatorUtils
import org.apache.commons.lang3.compare.ComparableUtils

/**
 * The browser fingerprint
 * */
data class Fingerprint(
    val browserType: BrowserType,
    var proxyServer: String? = null,
    var username: String? = null,
    var password: String? = null,
    var userAgent: String? = null,
): Comparable<Fingerprint> {
    private val comp = ComparatorUtils.nullLowComparator {
            o1: String, o2: String -> o1.compareTo(o2)
    }

    override fun compareTo(other: Fingerprint): Int {
        var r = browserType.compareTo(other.browserType)
        if (r != 0) {
            return r
        }

        listOf(
            proxyServer to other.proxyServer,
            username to other.username,
            password to other.password,
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
            proxyServer to other.proxyServer,
            username to other.username,
            password to other.password,
            userAgent to other.userAgent,
        ).all { it.first == it.second }
    }

    override fun toString(): String = listOfNotNull(
        browserType, proxyServer, username, password, userAgent
    ).joinToString()
}
