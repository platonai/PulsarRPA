package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_POLLING_INTERVAL
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_SIZE
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
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

    private val pollingInterval: Duration = conf.getDuration(PROXY_POOL_POLLING_INTERVAL, Duration.ofSeconds(10))
    private val maxPoolSize: Int = conf.getInt(PROXY_POOL_SIZE, 1000)
    private val lastModifiedTimes = mutableMapOf<Path, Instant>()
    private val proxyEntries = mutableSetOf<ProxyEntry>()
    private val freeProxies = LinkedBlockingDeque<ProxyEntry>(maxPoolSize)
    private val workingProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val unavailableProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val closed = AtomicBoolean()

    val isClosed get() = closed.get()
    val providers = mutableSetOf<String>()
    val numFreeProxies get() = freeProxies.size
    val numWorkingProxies get() = workingProxies.size
    val numUnavailableProxies get() = unavailableProxies.size

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
        proxyEntries.add(proxyEntry)
        return freeProxies.offer(proxyEntry)
    }

    override fun poll(): ProxyEntry? {
        if (numFreeProxies == 0) {
            updateProxies()
        }

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
        workingProxies.remove(proxyEntry)
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
            proxy = freeProxies.poll(pollingInterval.seconds, TimeUnit.SECONDS)
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

    fun updateProxies() {
        reloadProviders(Duration.ofSeconds(30))

        if (numFreeProxies < 3) {
            var updatedCount = 0

            providers.forEach {
                updatedCount += syncProxyFromProvider(URL(it))
            }

            if (updatedCount > 0) {
                reloadProxies(Duration.ofSeconds(1))
            }
        }
    }

    fun syncProxyFromProvider(providerUrl: URL): Int {
        var updatedCount = 0

        try {
            val filename = "proxies." + PulsarPaths.fromUri(providerUrl.toString()) + ".txt"
            val target = Paths.get(ProxyPool.AVAILABLE_DIR.toString(), filename)
            val symbolLink = Paths.get(ProxyPool.ENABLED_DIR.toString(), filename)

            Files.deleteIfExists(target)
            Files.deleteIfExists(symbolLink)

            FileUtils.copyURLToFile(providerUrl, target.toFile())
            Files.createSymbolicLink(symbolLink, target)

            val proxies = Files.readAllLines(target)
            updatedCount = proxies.size

            log.info("Saved {} proxies to {} from provider {}", updatedCount, target.fileName, providerUrl.host)
        } catch (e: IOException) {
            log.error(e.toString())
        }

        return updatedCount
    }

    private fun reloadProviders(expires: Duration) {
        try {
            Files.list(PROVIDER_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                reloadIfModified(it, lastModified, expires) { load(it, ResourceType.PROVIDER ) }
            }
        } catch (e: IOException) {
            log.info(e.toString())
        }
    }

    private fun reloadProxies(expires: Duration) {
        try {
            // load enabled proxies
            Files.list(ENABLED_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                reloadIfModified(it, lastModified, expires) { load(it, ResourceType.PROXY) }
            }
        } catch (e: IOException) {
            log.info(e.toString())
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
                    offer(it)
                }
            } else if (type == ResourceType.PROVIDER) {
                lines.takeWhile { Urls.isValidUrl(it) }.toCollection(providers)
            }
        } catch (e: IOException) {
            log.info(e.toString())
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

        try {
            val now = DateTimeUtil.now("MMdd.HHmm")

            val currentArchiveDir = Paths.get(ARCHIVE_DIR.toString(), now)
            Files.createDirectories(currentArchiveDir)

            val archiveDestinations = HashMap<Collection<ProxyEntry>, String>()
            archiveDestinations[proxyEntries] = "proxies.all.txt"
            archiveDestinations[workingProxies] = "proxies.working.txt"
            archiveDestinations[freeProxies] = "proxies.free.txt"
            archiveDestinations[unavailableProxies] = "proxies.unavailable.txt"

            archiveDestinations.forEach { (proxies, destination) ->
                val path = Paths.get(currentArchiveDir.toString(), destination)
                archive(path, proxies)
            }
        } catch (e: Throwable) {
            log.warn(StringUtil.stringifyException(e))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)

        val RELOAD_PERIOD = Duration.ofSeconds(10)
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
