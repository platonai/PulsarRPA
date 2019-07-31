package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager all proxy servers, for every request, we choose a proxy server from a proxy server list
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread
*/
class ProxyPool(private val conf: ImmutableConfig) : AbstractQueue<ProxyEntry>(), AutoCloseable {
    private val pollingWait: Duration = conf.getDuration(CapabilityTypes.PROXY_POOL_POLLING_WAIT, Duration.ofSeconds(1))
    private val maxPoolSize: Int = conf.getInt(CapabilityTypes.PROXY_POOL_SIZE, 10000)
    private val lastModifiedTimes = mutableMapOf<Path, Instant>()
    private val proxyEntries = mutableSetOf<ProxyEntry>()
    private val freeProxies = LinkedBlockingDeque<ProxyEntry>()
    private val workingProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val unavailableProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val closed = AtomicBoolean(false)

    val availableDir: Path = PulsarPaths.get("proxy", "available-proxies")
    val enabledDir: Path = PulsarPaths.get("proxy", "enabled-proxies")
    val archiveDir: Path = PulsarPaths.get("proxy", "archived-proxies")

    init {
        Files.createDirectories(availableDir)
        Files.createDirectories(enabledDir)
        Files.createDirectories(archiveDir)
        loadAll()
    }

    override operator fun contains(proxy: ProxyEntry): Boolean {
        return freeProxies.contains(proxy)
    }

    override operator fun iterator(): MutableIterator<ProxyEntry> {
        return freeProxies.iterator()
    }

    override val size: Int get() {
        return freeProxies.size
    }

    override fun offer(proxyEntry: ProxyEntry): Boolean {
        proxyEntry.refresh()
        return freeProxies.offer(proxyEntry)
    }

    override fun poll(): ProxyEntry? {
        var proxy: ProxyEntry? = null

        while (proxy == null && !freeProxies.isEmpty()) {
            proxy = pollOne()
        }

        return proxy
    }

    override fun peek(): ProxyEntry {
        return freeProxies.peek()
    }

    /**
     * The proxy may be recovered later
     */
    fun retire(proxyEntry: ProxyEntry) {
        unavailableProxies.add(proxyEntry)
    }

    /**
     * Check n unavailable proxies, recover them if possible.
     * This might take a long time, so it should be run in a separate thread
     */
    @JvmOverloads
    fun recover(limit: Int = Integer.MAX_VALUE): Int {
        var n = limit
        var recovered = 0

        val it = unavailableProxies.iterator()
        while (n-- > 0 && it.hasNext()) {
            val proxy = it.next()

            if (proxy.isGone) {
                it.remove()
            } else if (proxy.testNetwork()) {
                it.remove()
                proxy.refresh()
                offer(proxy)
                ++recovered
            }
        }

        return recovered
    }

    // Block until timeout or an available proxy entry returns
    private fun pollOne(): ProxyEntry? {
        var proxy: ProxyEntry? = null
        try {
            proxy = freeProxies.poll(pollingWait.seconds, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (proxy == null) {
            return null
        }

        if (proxy.isExpired) {
            if (proxy.testNetwork()) {
                proxy.refresh()
            }
        }

        if (proxy.isExpired) {
            unavailableProxies.add(proxy)
            proxy = null
        } else {
            workingProxies.add(proxy)
        }

        return proxy
    }

    fun reloadIfModified() {
        try {
            Files.list(enabledDir).filter { Files.isRegularFile(it) }
                    .forEach { reloadIfModified(it, lastModifiedTimes.getOrDefault(it, Instant.EPOCH)) }
        } catch (e: IOException) {
            log.info(toString())
        }
    }

    private fun reloadIfModified(path: Path, lastModified: Instant) {
        val modified = Instant.ofEpochMilli(path.toFile().lastModified())
        val elapsed = Duration.between(lastModified, modified)

        if (elapsed > RELOAD_PERIOD) {
            log.debug("Reload from file, last modified: {}, elapsed: {}s", lastModified, elapsed)
            load(path)
        }

        lastModifiedTimes[path] = modified
    }

    override fun toString(): String {
        return String.format("Total %d, free: %d, working: %d, gone: %d",
                proxyEntries.size, freeProxies.size, workingProxies.size, unavailableProxies.size)
    }

    private fun loadAll() {
        try {
            if (!Files.exists(enabledDir)) {
                Files.createDirectories(enabledDir)
            }

            Files.list(enabledDir).filter { Files.isRegularFile(it) }.forEach { load(it) }

            log.info(toString())
        } catch (e: IOException) {
            log.warn(e.toString())
        }
    }

    private fun load(path: Path) {
        try {
            val lines = Files.readAllLines(path)
                    .map { it.trim() }
                    .filter { !it.startsWith("#") }
                    .distinct().shuffled().toList()

            lines.mapNotNull { ProxyEntry.parse(it) }
                    .forEach { proxyEntry ->
                        proxyEntries.add(proxyEntry)
                        freeProxies.add(proxyEntry)
                    }
        } catch (e: IOException) {
            log.info(toString())
        }
    }

    private fun archive(path: Path, proxyEntries: Collection<ProxyEntry>) {
        val content = proxyEntries.joinToString("\n") { it.toString() }
        try {
            Files.deleteIfExists(path)
            Files.write(path, content.toByteArray(), StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            log.warn(e.toString())
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        val now = DateTimeUtil.now("MMdd.HHmm")

        try {
            val currentArchiveDir = Paths.get(archiveDir.toString(), now)
            if (!Files.exists(currentArchiveDir)) {
                Files.createDirectories(currentArchiveDir)
            }

            val archiveDestinations = HashMap<Collection<ProxyEntry>, String>()
            archiveDestinations[proxyEntries] = "proxies.all.txt"
            archiveDestinations[workingProxies] = "proxies.working.txt"
            archiveDestinations[freeProxies] = "proxies.free.txt"
            archiveDestinations[unavailableProxies] = "proxies.unavailable.txt"

            archiveDestinations.forEach { (proxies, destination) ->
                val path = Paths.get(currentArchiveDir.toString(), destination)
                archive(path, proxies)
            }
        } catch (e: IOException) {
            log.warn(e.toString())
        }
    }

    companion object {
        val RELOAD_PERIOD = Duration.ofMinutes(2)
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)
    }
}
