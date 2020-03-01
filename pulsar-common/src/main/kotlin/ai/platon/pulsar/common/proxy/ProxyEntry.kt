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
import kotlin.math.roundToInt

enum class ProxyType {
    HTTP, SOCKS4, SOCKS5
}

data class ProxyEntry(
        var host: String,
        var port: Int = 0,
        var id: Int = instanceSequence.incrementAndGet(),
        var lastTarget: String? = null,
        val declaredTTL: Instant? = null,
        var testUrls: List<URL> = TEST_URLS.toList(),
        var defaultTestUrl: URL = DEFAULT_TEST_URL,
        var isTestIp: Boolean = false,
        var proxyType: ProxyType = ProxyType.HTTP, // reserved
        var user: String? = null, // reserved
        var pwd: String? = null // reserved
): Comparable<ProxyEntry> {
    val hostPort get() = "$host:$port"
    val display get() = formatDisplay()
    var networkTester: (URL, Proxy) -> Boolean = NetUtil::testHttpNetwork
    val nTests = AtomicInteger()
    val nFailedTests = AtomicInteger()
    // accumulated test time
    val accumResponseMillis = AtomicLong()
    // failed connection count
    var availableTime: Instant = Instant.now()
    // number of failed pages
    val nFailedPages = AtomicInteger()
    // number of success pages
    val nSuccessPages = AtomicInteger()
    val servedDomains = ConcurrentHashMultiset.create<String>()
    val speed get() = (1000 * accumResponseMillis.get() / 1000 / (0.1 + nTests.get())).roundToInt() / 1000.0
    val ttl get() = declaredTTL ?: availableTime + PROXY_EXPIRED
    val ttlDuration get() = Duration.between(Instant.now(), ttl).takeIf { !it.isNegative }
    val isExpired get() = willExpireAt(Instant.now())
    val isGone get() = nFailedTests.get() >= 3

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
        val target = if (lastTarget != null) {
            URL(lastTarget)
        } else testUrls.random()

        var available = test(target)
        if (!available && target != defaultTestUrl) {
            available = test(defaultTestUrl)
            if (available) {
                log.warn("Target unreachable via {} | tests:{} | {}", display, nTests, target)
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

            nTests.incrementAndGet()
            accumResponseMillis.addAndGet(end - start)
        }

        if (!available) {
            nFailedTests.incrementAndGet()
        } else {
            // test if the proxy is expired
            nFailedTests.set(0)
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

    /**
     * The string representation, can be parsed using [parse]
     * */
    override fun toString(): String {
        val ttlStr = if (declaredTTL != null) ", ttl:$declaredTTL" else ""
        val nPages = nSuccessPages.get() + nFailedPages.get()
        return "$host:$port${META_DELIMITER}at:$availableTime$ttlStr, spd:$speed" +
                ", tt:$nTests, ftt:$nFailedTests, pg:$nPages, fpg:$nFailedPages"
    }

    override fun compareTo(other: ProxyEntry): Int {
        return hostPort.compareTo(other.hostPort)
    }

    private fun formatDisplay(): String {
        val ttlStr = ttlDuration?.let { DateTimeUtil.readableDuration(ttlDuration) }?:"0s"
        return "$host:$port($nFailedPages/$nSuccessPages/$ttlStr) spd:$speed"
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyEntry::class.java)

        private val instanceSequence = AtomicInteger()
        private const val META_DELIMITER = StringUtils.SPACE
        // Check if the proxy server is still available if it's not used for 30 seconds
        private val PROXY_EXPIRED = Duration.ofSeconds(30)
        // if a proxy server can not be connected in a hour, we announce it's dead and remove it from the file
        private val MISSING_PROXY_DEAD_TIME = Duration.ofHours(1)
        private const val DEFAULT_PROXY_SERVER_PORT = 80
        private const val PROXY_TEST_WEB_SITES_FILE = "proxy.test.web.sites.txt"
        val DEFAULT_TEST_URL = URL("https://www.baidu.com")
        val TEST_URLS = mutableSetOf<URL>()

        init {
            ResourceLoader.readAllLines(PROXY_TEST_WEB_SITES_FILE).mapNotNullTo(TEST_URLS) { Urls.getURLOrNull(it) }
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
                            item.startsWith("at:") -> availableTime = Instant.parse(item.substring("at:".length).trimEnd())
                            item.startsWith("ttl:") -> ttl = Instant.parse(item.substring("ttl:".length).trimEnd())
                        }
                    } catch (e: Throwable) {
                        log.warn("Ignore malformed proxy metadata <{}>", item)
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
