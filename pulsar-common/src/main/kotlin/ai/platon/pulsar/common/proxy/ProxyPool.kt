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
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manage all external proxies
 * Check all unavailable proxies, recover them if possible.
 * This might take a long time, so it should be run in a separate thread
*/
class ProxyPool(conf: ImmutableConfig): AbstractQueue<ProxyEntry>(), AutoCloseable {
    private val capacity: Int = conf.getInt(PROXY_POOL_CAPACITY, 1000)
    private val pollingInterval: Duration = conf.getDuration(PROXY_POOL_POLLING_INTERVAL, Duration.ofSeconds(10))
    private val lastModifiedTimes = mutableMapOf<Path, Instant>()
    private val proxyEntries = mutableSetOf<ProxyEntry>()
    private val freeProxies = LinkedBlockingDeque<ProxyEntry>(capacity)
    private val workingProxies = ConcurrentSkipListSet<ProxyEntry>()
    private val retiredProxies = ConcurrentSkipListSet<ProxyEntry>()
    private val bannedHosts = ConcurrentSkipListSet<String>()
    private val bannedSegments = ConcurrentSkipListSet<String>()
    private val lock = ReentrantLock()
    private val closed = AtomicBoolean()

    /**
     * The probability to choose a test ip if absent
     * */
    var testIpRate = 0.3
    var banIpSegment = true
    val isClosed get() = closed.get()
    var lastActiveTime = Instant.now()
    var idleTimeout = Duration.ofMinutes(5)
    val isIdle get() = numWorkingProxies == 0 && lastActiveTime.plus(idleTimeout) < Instant.now()
    val providers = mutableSetOf<String>()
    val numFreeProxies get() = freeProxies.size
    val numWorkingProxies get() = workingProxies.size
    val numRetiredProxies get() = retiredProxies.size

    init {
        loadBannedIps()
    }

    override operator fun contains(element: ProxyEntry): Boolean {
        return freeProxies.contains(element)
    }

    override operator fun iterator(): MutableIterator<ProxyEntry> {
        return freeProxies.iterator()
    }

    override val size get() = freeProxies.size

    override fun clear() {
        lock.withLock {
            freeProxies.clear()
            workingProxies.clear()
        }
    }

    override fun offer(proxyEntry: ProxyEntry): Boolean {
        lock.withLock {
            proxyEntry.refresh()
            proxyEntries.add(proxyEntry)
            workingProxies.remove(proxyEntry)
            return freeProxies.offer(proxyEntry)
        }
    }

    override fun poll(): ProxyEntry? {
        lastActiveTime = Instant.now()

        lock.withLock {
            if (numFreeProxies == 0) {
                updateProxies(asap = true)
            }
        }

        var proxy: ProxyEntry? = getTestProxyIfAbsent()
        if (proxy != null) {
            return proxy
        }

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
        lock.withLock {
            proxyEntry.retire()

            workingProxies.remove(proxyEntry)
            retiredProxies.add(proxyEntry)

            if (proxyEntry.isBanned) {
                bannedHosts.add(proxyEntry.host)
                bannedSegments.add(proxyEntry.segment)
            }
        }
    }

    /**
     * Check n unavailable proxies, recover them if possible.
     * This might take a long time, so it should be run in a separate thread
     */
    fun recover(limit: Int = 5): Int {
        var n = limit
        var recovered = 0

        lock.withLock {
            val it = retiredProxies.iterator()
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

    fun isBanned(proxyEntry: ProxyEntry): Boolean {
        return proxyEntry.isBanned || proxyEntry.segment in bannedSegments || proxyEntry.host in bannedHosts
    }

    @Synchronized
    fun updateProxies(asap: Boolean = false) {
        log.trace("Update proxies ...")

        reloadProviders(Duration.ofSeconds(10))

        val minFreeProxies = 3

        val maxTry = 5
        var i = 0
        while (i++ < maxTry && numFreeProxies < minFreeProxies && !Thread.interrupted()) {
            providers.forEach {
                syncProxiesFromProvider(it)
            }

            val seconds = if (asap || i > 0) 0L else 5L
            reloadProxies(Duration.ofSeconds(seconds))

            try {
                TimeUnit.SECONDS.sleep(3)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * Get a test ip by a probability if exist in [TEST_PROXY_FILE]
     * */
    fun getTestProxyIfAbsent(): ProxyEntry? {
        if (testIpRate > 0 && ThreadLocalRandom.current().nextDouble() <= testIpRate) {
            // if testIpRate == 0.3, we hit the ( 30% ) case

            if (Files.exists(TEST_PROXY_FILE)) {
                return Files.readAllLines(TEST_PROXY_FILE).firstOrNull()
                        ?.let { ProxyEntry.parse(it) }
                        ?.also { it.isTestIp = true }
            }
        }

        return null
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

        if (isBanned(proxy)) {
            log.info("Proxy is banned | {}", proxy.display)
            retiredProxies.add(proxy)
            return null
        }

        if (proxy.isExpired) {
            if (proxy.test()) {
                proxy.refresh()
            }
        }

        if (proxy.isExpired) {
            retiredProxies.add(proxy)
            proxy = null
        } else {
            workingProxies.add(proxy)
        }

        return proxy
    }

    private fun syncProxiesFromProvider(providerUrl: String): Int {
        val space = StringUtils.SPACE
        val url = providerUrl.substringBefore(space)
        val metadata = providerUrl.substringAfter(space)
        var vendor = "none"
        var format = "txt"

        metadata.split(space).zipWithNext().forEach {
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
        archiveDestinations[retiredProxies] = "proxies.unavailable.txt"

        archiveDestinations.forEach { (proxies, destination) ->
            val path = AppPaths.get(currentArchiveDir, destination)
            dump(path, proxies)
        }

        try {
            Files.write(PROXY_BANNED_SEGMENTS_FILE, bannedSegments.joinToString("\n") { it }.toByteArray())
            Files.write(PROXY_BANNED_HOSTS_FILE, bannedHosts.joinToString("\n") { it }.toByteArray())
        } catch (e: IOException) {
            log.warn(e.toString())
        }

        log.info("Proxy pool is dumped to file://{} | {}", currentArchiveDir, this)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        String.format("total %d, free: %d, working: %d, gone: %d, banH: %d banS: %d",
                proxyEntries.size,
                freeProxies.size,
                workingProxies.size,
                retiredProxies.size,
                bannedHosts.size,
                bannedSegments.size
        ).also { sb.append(it) }

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
            proxies.asSequence().filterNot { it.willExpireAfter(minTTL) }
                    .filterNot { banIpSegment && it.host in bannedSegments }
                    .filterNot { it.host in bannedHosts }
                    .forEach {
                        offer(it)
                        ++count
                    }

            log.info("Loaded {} proxies from {}", proxies.size, path)
        } catch (e: IOException) {
            log.info(e.toString())
        }

        return count
    }

    private fun loadBannedIps() {
        try {
            if (Files.exists(PROXY_BANNED_HOSTS_FILE)) {
                Files.readAllLines(PROXY_BANNED_HOSTS_FILE).mapTo(bannedHosts) { it }
            }
            if (Files.exists(PROXY_BANNED_SEGMENTS_FILE)) {
                Files.readAllLines(PROXY_BANNED_SEGMENTS_FILE).mapTo(bannedSegments) { it }
            }

            if (bannedHosts.isNotEmpty()) {
                log.info("There are {} banned hosts: ", bannedHosts.take(50).joinToString(", ") { it })
            }
            if (bannedSegments.isNotEmpty()) {
                log.info("There are {} banned segments: ", bannedSegments.take(50).joinToString(", ") { it })
            }
        } catch (e: IOException) {
            log.warn(e.toString())
        }
    }

    private fun dump(path: Path, proxyEntries: Collection<ProxyEntry>) {
        val content = proxyEntries.joinToString("\n") { it.toString() }
        try {
            Files.deleteIfExists(path)
            Files.createDirectories(path.parent)
            Files.write(path, content.toByteArray(), StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            log.warn(e.toString())
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            dump()
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

        val TEST_PROXY_FILE = AppPaths.get(BASE_DIR, "test-ip")
        val INIT_PROXY_PROVIDER_FILES = arrayOf(AppConstants.TMP_DIR, AppConstants.HOME_DIR)
                .map { Paths.get(it, "proxy.providers.txt") }
        val PROXY_BANNED_HOSTS_FILE = AppPaths.get(BASE_DIR, "proxies-banned-hosts.txt")
        val PROXY_BANNED_SEGMENTS_FILE = AppPaths.get(BASE_DIR, "proxies-banned-segments.txt")

        private val ENABLED_PROVIDER_DIR_WATCH_INTERVAL = Duration.ofSeconds(45)
        private var enabledProviderDirLastWatchTime = Instant.EPOCH
        private var numEnabledProviderFiles = 0L

        init {
            arrayOf(ENABLED_PROVIDER_DIR, AVAILABLE_PROVIDER_DIR, ENABLED_PROXY_DIR, AVAILABLE_PROXY_DIR, ARCHIVE_DIR)
                    .forEach { Files.createDirectories(it) }
            arrayOf(LATEST_AVAILABLE_PROXY, PROXY_BANNED_HOSTS_FILE, PROXY_BANNED_SEGMENTS_FILE).forEach {
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
                val now = Instant.now()
                synchronized(ProxyPool::class.java) {
                    val elapsedTime = Duration.between(enabledProviderDirLastWatchTime, now)
                    if (elapsedTime > ENABLED_PROVIDER_DIR_WATCH_INTERVAL) {
                        numEnabledProviderFiles = Files.list(ENABLED_PROVIDER_DIR)
                                .filter { Files.isRegularFile(it) }.count()
                    }
                    enabledProviderDirLastWatchTime = now
                }
                return numEnabledProviderFiles > 0
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
