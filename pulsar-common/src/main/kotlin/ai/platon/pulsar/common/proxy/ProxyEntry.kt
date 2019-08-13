package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

data class ProxyEntry(val host: String, val port: Int, val targetHost: String = "") : Comparable<ProxyEntry> {
    var availableTime: Instant = Instant.EPOCH
    var failedCount: Int = 0
    var networkTester: (URL, Proxy) -> Boolean = NetUtil::testHttpNetwork
    val testCount = AtomicInteger()
    val testkTime = AtomicLong()

    val isExpired: Boolean
        get() = availableTime.plus(PROXY_EXPIRED).isBefore(Instant.now())

    val isGone: Boolean
        get() = failedCount >= 3

    val speed: Long
        get() = testkTime.get() / 1000 / testCount.get()

    fun refresh() {
        availableTime = Instant.now()
    }

    fun ipPort(): String {
        return "$host:$port"
    }

    fun testNetwork(): Boolean {
        val target = if (targetHost.isNotBlank()) {
            URL(targetHost)
        } else testWebsites.random()

        var available = false
        if (NetUtil.testTcpNetwork(host, port)) {
            val addr = InetSocketAddress(host, port)
            val proxy = Proxy(Proxy.Type.HTTP, addr)

            val start = System.currentTimeMillis()

            available = networkTester(target, proxy)

            val end = System.currentTimeMillis()
            testCount.incrementAndGet()
            testkTime.addAndGet(end - start)
        }

        if (!available) {
            ++failedCount
        } else {
            // test if the proxy is expired
            failedCount = 0
        }

        return available
    }

    override fun toString(): String {
        return "$host:$port${META_DELIMITER}at:$availableTime${META_DELIMITER}speed:$speed"
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

        val testWebsites = mutableSetOf(URL("https://www.baidu.com"))
        init {
//            ResourceLoader.readAllLines("proxy.test.web.sites.txt")
//                    .mapTo(testWebsites) { URL(it) }
        }

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
    }
}
