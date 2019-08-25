package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.*
import com.google.common.collect.ConcurrentHashMultiset
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
import kotlin.math.roundToInt

enum class ProxyType {
    HTTP, SOCKS4, SOCKS5
}

data class ProxyEntry(
        var host: String,
        var port: Int = 0,
        var proxyType: ProxyType = ProxyType.HTTP, // reserved
        var user: String? = null,
        var pwd: String? = null,
        val declaredTTL: Instant? = null,
        var targetHost: String? = null
) : Comparable<ProxyEntry> {
    val hostPort = "$host:$port"
    val display get() = "$host:$port ttl:$ttlDuration"
    var networkTester: (URL, Proxy) -> Boolean = NetUtil::testHttpNetwork
    val testCount = AtomicInteger()
    val testTime = AtomicLong()
    val servedDomains = ConcurrentHashMultiset.create<String>()
    var failedCount: Int = 0
    val speed: Double get() = (1000 * testTime.get() / 1000 / (0.1 + testCount.get())).roundToInt() / 1000.0
    var availableTime: Instant = Instant.now()
    val ttl: Instant get() = declaredTTL ?: availableTime + PROXY_EXPIRED
    val ttlDuration: Duration? get() = Duration.between(Instant.now(), ttl).takeIf { !it.isNegative }
    val isExpired get() = willExpireAt(Instant.now())
    val isGone get() = failedCount >= 3

    fun willExpireAt(instant: Instant): Boolean {
        return ttl < instant
    }

    fun willExpireAfter(duration: Duration): Boolean {
        return ttl < Instant.now() + duration
    }

    fun refresh() {
        availableTime = Instant.now()
    }

    fun test(): Boolean {
        val target = if (targetHost != null) {
            URL(targetHost)
        } else TEST_WEB_SITES.random()

        var available = test(target)
        if (!available && target != DEFAULT_TEST_WEB_SITE) {
            available = test(DEFAULT_TEST_WEB_SITE)
            if (available) {
                log.warn("Test web site is unreachable | <{}> - {}", this, target)
            }
        }

        return available
    }

    fun test(target: URL): Boolean {
        // first, check TCP network is reachable, this is fast
        var available = NetUtil.testTcpNetwork(host, port, Duration.ofSeconds(5))
        if (available) {
            // second, the destination web site is reachable through this proxy
            val addr = InetSocketAddress(host, port)
            val proxy = Proxy(Proxy.Type.HTTP, addr)

            val start = System.currentTimeMillis()
            available = networkTester(target, proxy)
            val end = System.currentTimeMillis()

            testCount.incrementAndGet()
            testTime.addAndGet(end - start)
        }

        if (!available) {
            ++failedCount
        } else {
            // test if the proxy is expired
            failedCount = 0
            refresh()
        }

        return available
    }

    override fun hashCode(): Int {
        return 31 * proxyType.hashCode() + hostPort.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ProxyEntry && other.proxyType == proxyType && other.host == host && other.port == port
    }

    override fun toString(): String {
        val ttlStr = if (declaredTTL != null) ", ttl:$declaredTTL" else ""
        return "$host:$port${META_DELIMITER}at:$availableTime$ttlStr, spd:$speed"
    }

    override fun compareTo(other: ProxyEntry): Int {
        return hostPort.compareTo(other.hostPort)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)

        private const val META_DELIMITER = StringUtils.SPACE
        private const val IP_PORT_REGEX = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)(:[0-9]{2,5})"
        private val IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX)
        // Check if the proxy server is still available if it's not used for 30 seconds
        private val PROXY_EXPIRED = Duration.ofSeconds(30)
        // if a proxy server can not be connected in a hour, we announce it's dead and remove it from the file
        private val MISSING_PROXY_DEAD_TIME = Duration.ofHours(1)
        private const val DEFAULT_PROXY_SERVER_PORT = 80
        private const val PROXY_TEST_WEB_SITES_FILE = "proxy.test.web.sites.txt"
        val DEFAULT_TEST_WEB_SITE = URL("https://www.baidu.com")
        val TEST_WEB_SITES = mutableSetOf<URL>()

        init {
            ResourceLoader.readAllLines(PROXY_TEST_WEB_SITES_FILE).mapNotNullTo(TEST_WEB_SITES) { Urls.getURLOrNull(it) }
        }

        fun parse(str: String): ProxyEntry? {
            val ipPort = str.substringBefore(META_DELIMITER)
            if (!StringUtil.isIpPortLike(ipPort)) {
                log.warn("Malformed ip port - {}", str)
                return null
            }

            val pos = ipPort.lastIndexOf(':')
            if (pos != -1) {
                val host = ipPort.substring(0, pos)
                val port = NumberUtils.toInt(ipPort.substring(pos + 1), DEFAULT_PROXY_SERVER_PORT)

                var availableTime: Instant? = null
                var ttl: Instant? = null
                val parts = str.substringAfter(META_DELIMITER).split(", ")
                parts.forEach { item ->
                    try {
                        when {
                            item.startsWith("at:") -> availableTime = Instant.parse(item.substring("at:".length))
                            item.startsWith("ttl:") -> ttl = Instant.parse(item.substring("ttl:".length))
                        }
                    } catch (e: Throwable) {
                        log.warn("Malformed proxy metadata {}", item)
                    }
                }

                val proxyEntry = ProxyEntry(host, port, declaredTTL = ttl)
                availableTime?.let { proxyEntry.availableTime = it }
                return proxyEntry
            }

            return null
        }
    }
}
