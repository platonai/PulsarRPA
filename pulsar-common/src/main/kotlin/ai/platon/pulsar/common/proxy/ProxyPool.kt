package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_POLLING_INTERVAL
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.vendor.ProxyVendorFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manager all external proxies
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread
*/
class ProxyPool(conf: ImmutableConfig): AbstractQueue<ProxyEntry>(), AutoCloseable {
    private val pollingInterval: Duration = conf.getDuration(PROXY_POOL_POLLING_INTERVAL, Duration.ofSeconds(10))
    private val capacity: Int = conf.getInt(PROXY_POOL_CAPACITY, 1000)
    private val lastModifiedTimes = mutableMapOf<Path, Instant>()
    private val proxyEntries = mutableSetOf<ProxyEntry>()
    private val freeProxies = LinkedBlockingDeque<ProxyEntry>(capacity)
    private val workingProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val unavailableProxies = Collections.synchronizedSet(HashSet<ProxyEntry>())
    private val containerLock = ReentrantLock()
    private val closed = AtomicBoolean()

    val isClosed get() = closed.get()
    var lastActiveTime = Instant.now()
    var idleTimeout = Duration.ofMinutes(5)
    val isIdle get() = numWorkingProxies == 0 && lastActiveTime.plus(idleTimeout) < Instant.now()
    val providers = mutableSetOf<String>()
    val numFreeProxies get() = freeProxies.size
    val numWorkingProxies get() = workingProxies.size
    val numUnavailableProxies get() = unavailableProxies.size

    override operator fun contains(element: ProxyEntry): Boolean {
        return freeProxies.contains(element)
    }

    override operator fun iterator(): MutableIterator<ProxyEntry> {
        return freeProxies.iterator()
    }

    override val size get() = freeProxies.size

    override fun clear() {
        containerLock.withLock {
            freeProxies.clear()
            workingProxies.clear()
        }
    }

    override fun offer(proxyEntry: ProxyEntry): Boolean {
        containerLock.withLock {
            proxyEntry.refresh()
            proxyEntries.add(proxyEntry)
            workingProxies.remove(proxyEntry)
            return freeProxies.offer(proxyEntry)
        }
    }

    override fun poll(): ProxyEntry? {
        lastActiveTime = Instant.now()

        containerLock.withLock {
            if (numFreeProxies == 0) {
                updateProxies(asap = true)
            }
        }

        var proxy: ProxyEntry? = null
        val maxRetry = 5
        var n = 0
        while (!isClosed && proxy == null && n++ < maxRetry && !Thread.currentThread().isInterrupted) {
            proxy = pollOne()
        }

        if (proxy != null) {
            Files.write(LATEST_AVAILABLE_PROXY, proxy.toString().toByteArray())
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
        containerLock.withLock {
            workingProxies.remove(proxyEntry)
            unavailableProxies.add(proxyEntry)
        }
    }

    /**
     * Check n unavailable proxies, recover them if possible.
     * This might take a long time, so it should be run in a separate thread
     */
    fun recover(limit: Int = 5): Int {
        var n = limit
        var recovered = 0

        containerLock.withLock {
            val it = unavailableProxies.iterator()
            while (!isClosed && n-- > 0 && it.hasNext()) {
                val proxy = it.next()

                if (proxy.isGone) {
                    it.remove()
                } else if (proxy.test()) {
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
        val proxyEntry: ProxyEntry?
        try {
            proxyEntry = freeProxies.poll(pollingInterval.seconds, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        }

        var proxy = proxyEntry
        if (isClosed || proxy == null) {
            return null
        }

        if (proxy.isExpired) {
            if (proxy.test()) {
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

    @Synchronized
    fun updateProxies(asap: Boolean = false) {
        log.trace("Update proxies ...")

        reloadProviders(Duration.ofSeconds(10))

        val minFreeProxies = 3

        val maxTry = 5
        var i = 0
        while (i++ < maxTry && numFreeProxies < minFreeProxies && !Thread.currentThread().isInterrupted) {
            providers.forEach {
                syncProxiesFromProvider(it)
            }

            val seconds = if (asap || i > 0) 0L else 5L
            reloadProxies(Duration.ofSeconds(seconds))

            try {
                TimeUnit.SECONDS.sleep(3)
            } catch (e: InterruptedException) {}
        }
    }

    private fun syncProxiesFromProvider(providerUrl: String): Int {
        val SPACE = StringUtils.SPACE
        val url = providerUrl.substringBefore(SPACE)
        val metadata = providerUrl.substringAfter(SPACE)
        var vendor = "none"
        var format = "txt"

        metadata.split(SPACE).zipWithNext().forEach {
            when (it.first) {
                "-vendor" -> vendor = it.second
                "-fmt" -> format = it.second
            }
        }

        return syncProxiesFromProvider(URL(url), vendor, format)
    }

    private fun syncProxiesFromProvider(providerUrl: URL, vendor: String = "none", format: String = "txt"): Int {
        var updatedCount = 0

        try {
            val filename = "proxies." + AppPaths.fromUri(providerUrl.toString()) + "." + vendor + "." + format
            val target = AppPaths.get(ARCHIVE_DIR, filename)

            Files.deleteIfExists(target)
            FileUtils.copyURLToFile(providerUrl, target.toFile())

            updatedCount = loadProxies(target, vendor, format)

            log.info("Added {} proxies from provider {}", updatedCount, providerUrl.host)
        } catch (e: IOException) {
            log.error(e.toString())
        }

        return updatedCount
    }

    fun dump() {
        val now = DateTimeUtil.now("MMdd.HHmm")

        val currentArchiveDir = AppPaths.get(ARCHIVE_DIR, now)
        Files.createDirectories(currentArchiveDir)

        val archiveDestinations = HashMap<Collection<ProxyEntry>, String>()
        archiveDestinations[proxyEntries] = "proxies.all.txt"
        archiveDestinations[workingProxies] = "proxies.working.txt"
        archiveDestinations[freeProxies] = "proxies.free.txt"
        archiveDestinations[unavailableProxies] = "proxies.unavailable.txt"

        archiveDestinations.forEach { (proxies, destination) ->
            val path = AppPaths.get(currentArchiveDir, destination)
            dump(path, proxies)
        }

        log.info("{} | Dumped to file://{}", this, currentArchiveDir)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        String.format("Total %d, free: %d, working: %d, gone: %d",
                proxyEntries.size,
                freeProxies.size,
                workingProxies.size,
                unavailableProxies.size
        ).also { sb.append(it) }

        if (workingProxies.isNotEmpty()) {
            sb.append(" | ").append('<').append(workingProxies.first()).append('>')
        }

        return sb.toString()
    }

    private fun reloadProviders(expires: Duration) {
        try {
            Files.list(ENABLED_PROVIDER_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                reloadIfModified(it, lastModified, expires) { loadProviders(it) }
            }
        } catch (e: IOException) {
            log.info(e.toString())
        }
    }

    private fun reloadProxies(expires: Duration) {
        try {
            // load enabled proxies
            Files.list(ENABLED_PROXY_DIR).filter { Files.isRegularFile(it) }.forEach {
                val lastModified = lastModifiedTimes.getOrDefault(it, Instant.EPOCH)
                val parts = it.toString().split(".")
                if (parts.size > 2) {
                    val vendor = parts[parts.size - 2]
                    val format = parts[parts.size - 1]
                    reloadIfModified(it, lastModified, expires) { loadProxies(it, vendor, format) }
                }
                reloadIfModified(it, lastModified, expires) { loadProxies(it) }
            }
        } catch (e: IOException) {
            log.info(e.toString())
        }
    }

    private fun reloadIfModified(path: Path, lastModified: Instant, expires: Duration, loader: (Path) -> Unit) {
        val modified = Instant.ofEpochMilli(path.toFile().lastModified())
        val elapsed = Duration.between(lastModified, modified)

        if (elapsed > expires) {
            log.info("Reload from file, last modified: {}, elapsed: {}", lastModified, elapsed)
            loader(path)
        }

        lastModifiedTimes[path] = modified
    }

    private fun loadProviders(path: Path): Int {
        var count = 0

        try {
            val lines = Files.readAllLines(path)
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .distinct().shuffled().toList()

            val urls = lines.takeWhile { Urls.isValidUrl(it) }
            providers.addAll(urls)
            count += urls.size
        } catch (e: IOException) {
            log.info(e.toString())
        }

        return count
    }

    private fun loadProxies(path: Path, vendor: String = "none", format: String = "txt"): Int {
        var count = 0

        try {
            val content = FileUtils.readFileToString(path.toFile())

            val parser = ProxyVendorFactory.getProxyParser(vendor)
            val proxies = parser.parse(content, format)

            val minTTL = Duration.ofMinutes(3)
            proxies.filterNot { it.willExpireAfter(minTTL) }.forEach {
                offer(it)
                ++count
            }

            log.info("Loaded {} proxies from {}", proxies.size, path)
        } catch (e: IOException) {
            log.info(e.toString())
        }

        return count
    }

    private fun dump(path: Path, proxyEntries: Collection<ProxyEntry>) {
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
            dump()
        } catch (e: Throwable) {
            log.warn(StringUtil.stringifyException(e))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyPool::class.java)

        val BASE_DIR = AppPaths.getTmp("proxy")
        val ENABLED_PROVIDER_DIR = AppPaths.get(BASE_DIR, "providers-enabled")
        val AVAILABLE_PROVIDER_DIR = AppPaths.get(BASE_DIR, "providers-available")
        val ENABLED_PROXY_DIR = AppPaths.get(BASE_DIR, "proxies-enabled")
        val AVAILABLE_PROXY_DIR = AppPaths.get(BASE_DIR, "proxies-available")
        val ARCHIVE_DIR = AppPaths.get(BASE_DIR, "proxies-archived")
        val LATEST_AVAILABLE_PROXY = AppPaths.get( "latest-available-proxy")
        val INIT_PROXY_PROVIDER_FILES = arrayOf(AppConstants.TMP_DIR, AppConstants.HOME_DIR)
                .map { Paths.get(it, "proxy.providers.txt") }

        init {
            arrayOf(ENABLED_PROVIDER_DIR, AVAILABLE_PROVIDER_DIR, ENABLED_PROXY_DIR, AVAILABLE_PROXY_DIR, ARCHIVE_DIR)
                    .forEach { Files.createDirectories(it) }
            arrayOf(LATEST_AVAILABLE_PROXY).forEach {
                if (!Files.exists(it)) Files.createFile(it)
            }

            INIT_PROXY_PROVIDER_FILES.forEach {
                if (Files.exists(it)) {
                    FileUtils.copyFileToDirectory(it.toFile(), AVAILABLE_PROVIDER_DIR.toFile())
                }
            }

            log.info("Proxy base dir: $BASE_DIR")
        }

        fun hasEnabledProvider(): Boolean {
            try {
                return Files.list(ENABLED_PROVIDER_DIR).filter { Files.isRegularFile(it) }.count() > 0
            } catch (e: IOException) {
                log.error("Failed to list files in $ENABLED_PROVIDER_DIR", e)
            }

            return false
        }

        /**
         * Proxy system can be enabled/disabled at runtime
         * */
        fun isProxyEnabled(): Boolean {
            if (RuntimeUtils.hasLocalFileCommand(AppConstants.CMD_ENABLE_PROXY)) {
                return true
            }

            // explicit set system environment property
            val useProxy = System.getProperty(CapabilityTypes.PROXY_USE_PROXY)
            if (useProxy != null) {
                when (useProxy) {
                    "yes" -> return true
                    "no" -> return false
                }
            }

            // if no one set the proxy availability explicitly, but we have providers, use it
            if (hasEnabledProvider()) {
                return true
            }

            return false
        }
    }
}
