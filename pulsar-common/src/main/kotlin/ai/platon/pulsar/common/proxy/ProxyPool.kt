package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_POLLING_TIMEOUT
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manage all external proxies.
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread.
 */
open class ProxyPool(conf: ImmutableConfig): AutoCloseable {

    private val logger = LoggerFactory.getLogger(ProxyPool::class.java)

    protected val capacity: Int = conf.getInt(PROXY_POOL_CAPACITY, 100)
    protected val pollingTimeout: Duration = conf.getDuration(PROXY_POOL_POLLING_TIMEOUT, Duration.ofSeconds(20))
    protected val proxyEntries = mutableSetOf<ProxyEntry>()
    protected val freeProxies = LinkedBlockingDeque<ProxyEntry>(capacity)
    protected var numProxyBanned = 0
    protected val closed = AtomicBoolean()

    /**
     * The probability to choose a test ip if absent
     * */
    val isActive get() = !closed.get() && AppContext.isActive
    var lastActiveTime = Instant.now()

    operator fun contains(element: ProxyEntry): Boolean = freeProxies.contains(element)

    operator fun iterator(): MutableIterator<ProxyEntry> = freeProxies.iterator()

    val size get() = freeProxies.size

    fun clear() = freeProxies.clear()

    open fun offer(proxyEntry: ProxyEntry): Boolean {
        proxyEntries.add(proxyEntry)
        return freeProxies.offer(proxyEntry)
    }

    @Throws(ProxyException::class)
    open fun take(): ProxyEntry? {
        lastActiveTime = Instant.now()

        var retry = 3
        while (retry-- > 0) {
            try {
                return freeProxies.poll(pollingTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: ProxyRetryException) {
                sleepSeconds(3)
            }
        }

        return null
    }

    /**
     * The proxy may be recovered later
     */
    open fun retire(proxyEntry: ProxyEntry) {
        proxyEntry.retire()
    }

    open fun report(proxyEntry: ProxyEntry) {
        logger.info("Ban proxy <{}> after {} pages served in {} | total ban: {} | {}",
                proxyEntry.outIp, proxyEntry.numSuccessPages, proxyEntry.elapsedTime.readable(),
                numProxyBanned, proxyEntry)
    }

    open fun dump() {
        if (proxyEntries.isEmpty()) {
            logger.info("Used total 0 proxies, do not dump")
            return
        }

        synchronized(AppPaths.PROXY_ARCHIVE_DIR) {
            try {
                val ident = DateTimes.now("MMdd.HH")
                val currentArchiveDir = AppPaths.PROXY_ARCHIVE_DIR.resolve(ident)
                Files.createDirectories(currentArchiveDir)

                dump(currentArchiveDir.resolve("proxies.all.txt"), proxyEntries)
                logger.info("Proxy pool is dumped to file://{} | {}", currentArchiveDir, this)
            } catch (e: IOException) {
                logger.warn(e.toString())
            }
        }
    }

    override fun toString(): String = String.format("total %d, free: %d", proxyEntries.size, freeProxies.size)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (proxyEntries.isNotEmpty()) {
                dump()
            }
        }
    }

    protected fun dump(path: Path, proxyEntries: Collection<ProxyEntry>) {
        val content = proxyEntries.joinToString("\n") { it.serialize() }
        try {
            Files.deleteIfExists(path)
            Files.createDirectories(path.parent)
            Files.writeString(path, content, StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            logger.warn(e.toString())
        }
    }
}
