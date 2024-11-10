package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.concurrent.ConcurrentPassiveExpiringSet
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Load proxies from proxy vendors
 */
abstract class ProxyLoader(conf: ImmutableConfig): AutoCloseable {
    companion object {
        val TEST_PROXY_FILE = AppPaths.PROXY_BASE_DIR.resolve("test-ip")
    }

    private val logger = LoggerFactory.getLogger(ProxyLoader::class.java)
    protected val startTime = Instant.now()
    protected val closed = AtomicBoolean()

    /**
     * The minimum time to live for proxy IPs, the implementation might drop short-live proxy IPs.
     * */
    var minimumProxyTTL = Duration.ofMinutes(5)
    var testProxyBeforeUse = false
    // TODO: configurable
    var testUrl = "https://www.amazon.com/"
    val fileWatchInterval = Duration.ofSeconds(30)
    val lastModifiedTimes = mutableMapOf<Path, Instant>()

    var banStrategy = conf.get("proxy.ban.strategy", "segment")
    var ipTimeToBan = conf.getDuration("proxy.ip.time.to.ban", Duration.ofHours(1))
    var segmentTimeToBan = conf.getDuration("proxy.segment.time.to.ban", Duration.ofHours(2))

    val bannedIps = ConcurrentPassiveExpiringSet<String>(ipTimeToBan)
    val bannedSegments = ConcurrentPassiveExpiringSet<String>(segmentTimeToBan)
    /**
     * The probability to choose a test ip if absent
     * */
    var testIpRate = 0.3

    val isActive get() = !closed.get()
    
    @Throws(ProxyException::class)
    fun updateProxies() = updateProxies(Duration.ZERO)

    @Throws(ProxyException::class)
    abstract fun updateProxies(reloadInterval: Duration): List<ProxyEntry>

    @Synchronized
    fun updateBanStrategy() {
        val path = AppPaths.PROXY_BAN_STRATEGY
        loadIfModified(path, fileWatchInterval) { Files.readAllLines(it) }
                .firstOrNull()?.let { banStrategy = it }?.also { logger.info("Proxy ban strategy: $it") }
    }

    /**
     * Get a test ip by a probability if exist in file [TEST_PROXY_FILE]
     * */
    fun loadTestProxyIfAbsent(): ProxyEntry? {
        return TEST_PROXY_FILE.takeIf { isActive && testIpRate > 0 && ThreadLocalRandom.current().nextDouble() <= testIpRate }
                ?.takeIf { Files.exists(it) }
                ?.let { Files.readString(it).trim() }
                ?.let { ProxyEntry.parse(it) }
                ?.also { it.isTestIp = true }
    }

    protected fun <O> loadIfModified(path: Path, expires: Duration, loader: (Path) -> List<O>): List<O> {
        if (!Files.exists(path)) {
            return listOf()
        }
        val lastModified = lastModifiedTimes.getOrDefault(path, Instant.EPOCH)

        try {
            val modified = Files.getLastModifiedTime(path).toInstant()
            val elapsed = Duration.between(lastModified, modified)

            if (elapsed > expires) {
                logger.info("Reload from file, last modified: {}, elapsed: {} | {}", lastModified, elapsed.readable(), path)
                return loader(path).also { lastModifiedTimes[path] = modified }
            }
        } catch (e: IOException) {
            logger.warn("Failed to load - {}", e.message)
        }

        return listOf()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
