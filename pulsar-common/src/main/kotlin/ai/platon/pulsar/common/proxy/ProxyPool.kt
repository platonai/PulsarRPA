package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.ibm.icu.impl.PluralRulesLoader.loader
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
 * Manager all external proxies
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread
*/
class ProxyPool(conf: ImmutableConfig) : AbstractQueue<ProxyEntry>(), AutoCloseable {
    enum class ResourceType { PROVIDER, PROXY }

    private val pollingWait: Duration = conf.getDuration(CapabilityTypes.PROXY_POOL_POLLING_INTERVAL, Duration.ofSeconds(10))
    private val maxPoolSize: Int = conf.getInt(CapabilityTypes.PROXY_POOL_SIZE, 10000)
    private val lastModifiedTimes = mutableMapOf<Path, Instant>()
    private val proxyEntries = mutableSetOf<ProxyEntry>()
    private val freeProxies = LinkedBlockingDeque<ProxyEntry>()
    private val workingProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val unavailableProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val closed = AtomicBoolean()

    val isClosed get() = closed.get()
    val providers = mutableSetOf<String>()

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

        while (!isClosed && proxy == null && !freeProxies.isEmpty()) {
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
    fun recover(limit: Int = 5): Int {
        var n = limit
        var recovered = 0

        synchronized(unavailableProxies) {
            val it = unavailableProxies.iterator()
            while (!isClosed && n-- > 0 && it.hasNext()) {
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

        if (isClosed || proxy == null) {
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

    fun updateProviders() {
        try {
            Files.list(PROVIDER_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                reloadIfModified(it, lastModified, RELOAD_PERIOD) { load(it, ResourceType.PROVIDER ) }
            }
        } catch (e: IOException) {
            log.info(toString())
        }
    }

    fun updateProxies() {
        try {
            // load enabled proxies
            Files.list(ENABLED_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                reloadIfModified(it, lastModified, RELOAD_PERIOD) { load(it, ResourceType.PROXY) }
            }
        } catch (e: IOException) {
            log.info(toString())
        }
    }

    private fun reloadIfModified(path: Path, lastModified: Instant, expires: Duration, loader: (Path) -> Unit) {
        val modified = Instant.ofEpochMilli(path.toFile().lastModified())
        val elapsed = Duration.between(lastModified, modified)

        if (elapsed > expires) {
            log.info("Reload from file, last modified: {}, elapsed: {}s", lastModified, elapsed)
            loader(path)
        }

        lastModifiedTimes[path] = modified
    }

    override fun toString(): String {
        return String.format("Total %d, free: %d, working: %d, gone: %d",
                proxyEntries.size, freeProxies.size, workingProxies.size, unavailableProxies.size)
    }

    private fun load(path: Path, type: ResourceType) {
        try {
            val lines = Files.readAllLines(path)
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .distinct().shuffled().toList()

            if (type == ResourceType.PROXY) {
                log.info("Loaded {} proxies from {}", lines.size, path)
                lines.mapNotNull { ProxyEntry.parse(it) }.forEach {
                    proxyEntries.add(it)
                    freeProxies.add(it)
                }
            } else if (type == ResourceType.PROVIDER) {
                lines.takeWhile { Urls.isValidUrl(it) }.toCollection(providers)
            }
        } catch (e: IOException) {
            log.warn(toString())
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
            val currentArchiveDir = Paths.get(ARCHIVE_DIR.toString(), now)
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
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)

        val RELOAD_PERIOD = Duration.ofMinutes(2)
        val BASE_DIR = PulsarPaths.get("proxy")
        val PROVIDER_DIR = PulsarPaths.get(BASE_DIR, "providers")
        val AVAILABLE_DIR = PulsarPaths.get(BASE_DIR, "available-proxies")
        val ENABLED_DIR  = PulsarPaths.get(BASE_DIR, "enabled-proxies")
        val ARCHIVE_DIR = PulsarPaths.get(BASE_DIR, "archived-proxies")

        init {
            Files.createDirectories(PROVIDER_DIR)
            Files.createDirectories(AVAILABLE_DIR)
            Files.createDirectories(ENABLED_DIR)
            Files.createDirectories(ARCHIVE_DIR)

            log.info(toString())
        }
    }
}
