package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern

data class ProxyEntry(val host: String, val port: Int) : Comparable<ProxyEntry> {
    val targetHost: String = ""
    var availableTime: Instant = Instant.EPOCH
    var failedTimes: Int = 0

    val isExpired: Boolean
        get() = availableTime.plus(PROXY_EXPIRED).isBefore(Instant.now())

    val isGone: Boolean
        get() = failedTimes >= 3

    fun refresh() {
        availableTime = Instant.now()
    }

    fun ipPort(): String {
        return "$host:$port"
    }

    fun testNetwork(): Boolean {
        if (targetHost.isNotBlank()) {
            // TODO:
        }

        val available = NetUtil.testNetwork(host, port)
        if (!available) {
            ++failedTimes
        } else {
            failedTimes = 0
        }

        return available
    }

    override fun toString(): String {
        return "$host:$port${META_DELIMITER}at:$availableTime"
    }

    override fun equals(other: Any?): Boolean {
        return other is ProxyEntry && host == other.host && port == other.port
    }

    override fun compareTo(proxyEntry: ProxyEntry): Int {
        return ipPort().compareTo(proxyEntry.ipPort())
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)
        private const val META_DELIMITER = " - "
        private const val IP_PORT_REGEX = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)(:[0-9]{2,5})"
        private val IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX)
        // Check if the proxy server is still available if it's not used for 30 seconds
        private val PROXY_EXPIRED = Duration.ofSeconds(30)
        // if a proxy server can not be connected in a hour, we announce it's dead and remove it from the file
        private val MISSING_PROXY_DEAD_TIME = Duration.ofHours(1)
        private const val DEFAULT_PROXY_SERVER_PORT = 19080

        fun parse(str: String): ProxyEntry? {
            val ipPort = str.substringBefore(META_DELIMITER)
            val pos = ipPort.lastIndexOf(':')
            if (pos != -1) {
                val host = ipPort.substring(0, pos)
                val port = NumberUtils.toInt(ipPort.substring(pos + 1), DEFAULT_PROXY_SERVER_PORT)

                val proxyEntry = ProxyEntry(host, port)

                try {
                    var metadata = str.substringAfter(META_DELIMITER)
                    if (!metadata.endsWith(",")) {
                        metadata += ","
                    }
                    val instant = StringUtils.substringBetween(metadata, "at:", ",")
                    if (instant.startsWith("PT")) {
                        proxyEntry.availableTime = Instant.parse(instant)
                    }
                } catch (e: Throwable) {}

                return proxyEntry
            }

            return null
        }

        fun validateIpPort(ipPort: String): Boolean {
            return IP_PORT_PATTERN.matcher(ipPort).matches()
        }
    }
}
