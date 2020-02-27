package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.ProxyManager
import org.apache.commons.io.FileUtils
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.devtools.log.Log
import org.openqa.selenium.devtools.network.Network
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

private class WebDriverAllocator(private val driverPool: WebDriverPool) {
    private val log = LoggerFactory.getLogger(WebDriverAllocator::class.java)
    private val conf = driverPool.conf
    private val driverControl = driverPool.driverControl
    private val proxyManager = driverPool.proxyManager
    private val proxyPool = driverPool.proxyPool
    private val defaultWebDriverClass = conf.getClass(
            BROWSER_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)

    fun allocate(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        if (driverPool.aliveSize >= driverPool.capacity) {
            log.warn("Too many web drivers. Cpu cores: {}, capacity: {}, {}",
                    AppConstants.NCPU, driverPool.capacity, driverPool.formatStatus(verbose = false))
            return null
        }

        try {
            return createWebDriver(priority, conf)
        } catch (e: Throwable) {
            log.error("Unexpected exception, failed to create a web driver", e)
        }

        return null
    }

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(NoSuchMethodException::class,
            IllegalAccessException::class,
            InvocationTargetException::class,
            InstantiationException::class
    )
    fun createWebDriver(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        val capabilities = driverControl.createGeneralOptions()

        if (ProxyPool.isProxyEnabled()) {
            setProxy(capabilities)
        }

        // Choose the WebDriver
        val browserType = getBrowserType(conf)
        val driver: WebDriver = when {
            browserType == BrowserType.CHROME -> {
                val options = driverControl.createChromeDevtoolsOptions(capabilities)
                ChromeDevtoolsDriver(driverControl.randomUserAgent(), driverControl, options)
            }
            browserType == BrowserType.SELENIUM_CHROME -> {
                // System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
                ChromeDriver(driverControl.createChromeOptions(capabilities))
            }
            RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass) -> {
                defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
            }
            else -> defaultWebDriverClass.getConstructor().newInstance()
        }

        if (driver is ChromeDriver) {
            val fakeAgent = driverControl.randomUserAgent()
            val devTools = driver.devTools
            devTools.createSession()
            devTools.send(Log.enable())
            devTools.addListener(Log.entryAdded()) { e -> log.error(e.text) }
            devTools.send(Network.setUserAgentOverride(fakeAgent, Optional.empty(), Optional.empty()))

            // devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));
            // devTools.send(emulateNetworkConditions(false,100,200000,100000, Optional.of(ConnectionType.cellular4g)));
        }

        return ManagedWebDriver(driver, priority)
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun ssl() {
        val trustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private fun setProxy(capabilities: DesiredCapabilities): ProxyEntry? {
        var proxyEntry: ProxyEntry? = null
        var hostPort: String? = null
        val proxy = org.openqa.selenium.Proxy()
        if (proxyManager.ensureAvailable()) {
            // TODO: internal proxy server can be run at another host
            proxyEntry = proxyManager.proxyEntry
            hostPort = "127.0.0.1:${proxyManager.port}"
        }

        if (hostPort == null) {
            // internal proxy server is not available, set proxy to the browser directly
            proxyEntry = proxyPool.poll()
            hostPort = proxyEntry?.hostPort
        }

        proxy.httpProxy = hostPort
        proxy.sslProxy = hostPort
        proxy.ftpProxy = hostPort

        capabilities.setCapability(CapabilityType.PROXY, proxy)

        return proxyEntry
    }

    /**
     * TODO: choose a best browser automatically: which one is faster yet still have good result
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     */
    private fun getBrowserType(mutableConfig: ImmutableConfig?): BrowserType {
        return mutableConfig?.getEnum(BROWSER_TYPE, BrowserType.CHROME)
                ?: conf.getEnum(BROWSER_TYPE, BrowserType.CHROME)
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverPool(
        val driverControl: WebDriverControl,
        val proxyPool: ProxyPool,
        val proxyManager: ProxyManager,
        val conf: ImmutableConfig
): Parameterized, Freezable(), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverPool::class.java)

    companion object {
        private val pollingTimeout = Duration.ofMillis(100)
        private val aliveDrivers = Collections.synchronizedSet(HashSet<ManagedWebDriver>())
        // Every value collection is a first in, first out queue
        private val freeDrivers = mutableMapOf<Int, LinkedList<ManagedWebDriver>>()
        private val workingDrivers = HashMap<Int, ManagedWebDriver>()
        private val lock = ReentrantLock() // lock for containers
        private val notEmpty = lock.newCondition()
        private val notBusy = lock.newCondition()

        val numCrashed = AtomicInteger()
        val numRetired = AtomicInteger()
        val numQuit = AtomicInteger()

        val pageViews = AtomicInteger()
    }

    private val isHeadless = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val isClosed = closed.get()
    private val concurrency = conf.getInt(FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    private var lastActiveTime = Instant.now()
    private var idleTimeout = Duration.ofMinutes(5)

    val capacity = conf.getInt(BROWSER_MAX_DRIVERS, concurrency)
    val isIdle get() = workingSize == 0 && idleTime > idleTimeout
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isAllEmpty: Boolean = lock.withLock { aliveDrivers.isEmpty() && freeDrivers.isEmpty() && workingDrivers.isEmpty() }
    val workingSize: Int = lock.withLock { workingDrivers.size }
    val freeSize: Int = lock.withLock { freeDrivers.values.sumBy { it.size } }
    val aliveSize: Int = lock.withLock { workingDrivers.size + freeDrivers.values.sumBy { it.size } }
    val totalSize get() = aliveDrivers.size
    val proxyEntry get() = proxyManager.proxyEntry

    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenUnfrozen {
            val driver = poll(priority, volatileConfig)
                    ?: throw WebDriverPoolExhaust(formatStatus(verbose = true))

            try {
                driver.proxyEntry.set(proxyEntry)
                action(driver)
            } finally {
                put(driver)
            }
        }
    }

    fun allocate(priority: Int, numInit: Int, conf: ImmutableConfig) {
        whenUnfrozen {
            repeat(numInit) { poll(priority, conf)?.let { put(it) } }
        }

//        val drivers = mutableListOf<ManagedWebDriver>()
//        repeat(numInit) { poll(priority, conf)?.let { drivers.add(it) } }
//        drivers.forEach { put(it) }
    }

    fun reset() {
        freeze {
            cancelAllInternal()
            closeAllInternal(incognito = true)
        }
    }

    fun cancel(url: String): ManagedWebDriver? {
        return freeze {
            cancelInternal(url)
        }
    }

    fun cancelAll() {
        freeze {
            cancelAllInternal()
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        return whenUnfrozen {
            pollInternal(priority, conf)
        }
    }

    fun put(driver: ManagedWebDriver) {
        whenUnfrozen {
            if (driver.isRetired) {
                retire(driver, null)
            } else {
                offer(driver)
            }
        }
    }

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        freeze {
            closeAllInternal(incognito, processExit)
        }
    }

    fun report() {
        log.info(formatStatus(verbose = true))

        val sb = StringBuilder()

        lock.withLock {
            aliveDrivers.forEach { driver ->
                driver.driver.manage().cookies.joinTo(sb, "Cookies in driver #${driver.id}: ") { it.toString() }
            }
        }

        if (sb.isNotBlank()) {
            log.info("Cookies: \n{}", sb)
        } else {
            log.info("All drivers have no cookie")
        }
    }

    fun formatStatus(verbose: Boolean = false): String {
        return if (verbose) {
            String.format("total: %d free: %d working: %d alive: %d" +
                    " crashed: %d retired: %d quit: %d pageViews: %d",
                    totalSize, freeSize, workingSize, aliveSize,
                    numCrashed.get(), numRetired.get(), numQuit.get(), pageViews.get()
            )
        } else {
            String.format("%d/%d/%d/%d/%d (free/working/total/crashed/retired)",
                    freeSize, workingSize, aliveSize, numCrashed.get(), numRetired.get())
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAllInternal(incognito = true, processExit = true)
        }
    }

    private fun pollInternal(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        var driver: ManagedWebDriver? = null
        var exception: Exception? = null

        try {
            lock.lockInterruptibly()

            driver = getOrCreate(priority, conf)
            var nanos = pollingTimeout.toNanos()
            while (!isClosed && driver == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos)
                driver = getOrCreate(priority, conf)
            }

            if (driver != null) {
                driver.startWork()
                workingDrivers[driver.id] = driver
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.info("Interrupted, no web driver should return")
            exception = e
        } catch (e: Exception) {
            log.warn("Unexpected error - {}", StringUtil.simplifyException(e))
            exception = e
        } finally {
            lock.unlock()
        }

        if (exception != null && driver != null) {
            retire(driver, exception)
            driver = null
        }

        return if (isClosed) null else driver
    }

    private fun cancelInternal(url: String): ManagedWebDriver? {
        return workingDrivers.values.firstOrNull { it.currentUrl == url }?.also { it.cancel() }
    }

    private fun cancelAllInternal() {
        workingDrivers.values.forEach { it.cancel() }
    }

    /**
     * TODO: conf is not really used if the queue is not empty
     * */
    private fun getOrCreate(group: Int, conf: ImmutableConfig): ManagedWebDriver? {
        val queue = freeDrivers.computeIfAbsent(group) { LinkedList() }

        if (queue.isEmpty()) {
            allocateTo(group, queue, conf)
        }

        return if (queue.isEmpty()) null else queue.remove()
    }

    private fun allocateTo(group: Int, queue: LinkedList<ManagedWebDriver>, conf: ImmutableConfig): ManagedWebDriver? {
        val allocator = WebDriverAllocator(this)
        val driver = allocator.allocate(group, conf)
        if (driver != null) {
            queue.add(driver)
            aliveDrivers.add(driver)
            logDriverOnline(driver)
        }

        return driver
    }

    private fun logDriverOnline(driver: ManagedWebDriver) {
        if (log.isTraceEnabled) {
            log.trace("The {}th web driver is online, " +
                    "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {}",
                    totalSize, driver.name,
                    driverControl.imagesEnabled, driverControl.pageLoadStrategy, capacity)
        }
    }

    private fun offer(driver: ManagedWebDriver) {
        try {
            lastActiveTime = Instant.now()

            // a driver is always hold by only one thread, so it's OK to use it without locks
            driver.status.set(DriverStatus.FREE)
            driver.stat.pageViews++
            pageViews.incrementAndGet()
        } catch (e: Exception) {
            // log.warn("Failed to recycle a WebDriver, retire it - {}", StringUtil.simplifyException(e))
            log.warn(StringUtil.stringifyException(e))
            driver.status.set(DriverStatus.UNKNOWN)
            retire(driver, e)
            return
        }

        lock.withLock {
            // it can be retired by close all
            if (driver.isFree) {
                val queue = freeDrivers[driver.priority]
                if (queue != null) {
                    queue.add(driver)
                    // TODO: every queue should have a separate signal
                    notEmpty.signal()
                } else {
                    log.warn("Unexpected driver priority {}, no queue exist - {}", driver.priority, driver)
                }
            }

            workingDrivers.remove(driver.id)
            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }
        }
    }

    private fun retire(driver: ManagedWebDriver, e: Exception?) {
        return retire(driver, e, true)
    }

    private fun retire(driver: ManagedWebDriver, e: Exception?, external: Boolean = true) {
        if (external && isClosed) {
            return
        }

        if (driver.isQuit) {
            if (freeDrivers[driver.priority]?.contains(driver) == true) {
                log.warn("Driver is quit, should not be in free driver list | {}", driver)
            }
            if (workingDrivers.containsKey(driver.id)) {
                log.warn("Driver is quit, should not be in working driver list | {}", driver)
            }
            return
        }

        driver.retire()

        lock.withLock {
            freeDrivers[driver.priority]?.remove(driver)
            workingDrivers.remove(driver.id)

            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }
        }

        when (e) {
            is org.openqa.selenium.NoSuchSessionException -> driver.status.set(DriverStatus.CRASHED)
            is org.apache.http.conn.HttpHostConnectException -> driver.status.set(DriverStatus.CRASHED)
        }

        when {
            driver.isRetired -> numRetired.incrementAndGet()
            driver.isCrashed -> numCrashed.incrementAndGet()
            else -> {}
        }

        try {
            if (e != null) {
                log.info("Quiting {} driver {} - {}", driver.status.get().name.toLowerCase(), driver, StringUtil.simplifyException(e))
            } else {
                log.trace("Quiting {} driver {}", driver.status.get().name.toLowerCase(), driver)
            }
            // Quits this driver, close every associated window
            driver.quit()
            numQuit.incrementAndGet()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, StringUtil.simplifyException(e))
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, StringUtil.simplifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", StringUtil.stringifyException(e))
        } finally {
        }
    }

    private fun closeAllInternal(incognito: Boolean = true, processExit: Boolean = false) {
        var i = 0
        while (workingSize > 0 && i++ < 120) {
            notBusy.await(1, TimeUnit.SECONDS)
        }

        doCloseAll(incognito, processExit)

        if (incognito) {
            // Force delete all browser data
            FileUtils.deleteDirectory(AppPaths.BROWSER_TMP_DIR.toFile())
        }
    }

    private fun doCloseAll(incognito: Boolean = true, processExit: Boolean = false) {
        if (aliveDrivers.isEmpty()) {
            if (aliveSize != 0) {
                log.info("Illegal status - {}", formatStatus(verbose = true))
            }

            return
        }

        log.info("Closing all web drivers ... {}", formatStatus(verbose = true))

        freeDrivers.flatMap { it.value }.forEach { retire(it, null, external = false) }
        freeDrivers.forEach { it.value.clear() }
        freeDrivers.clear()

        workingDrivers.asSequence().map { it.value }.forEach { retire(it, null, external = false) }
        workingDrivers.clear()

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        var count = 0
        val drivers = aliveDrivers.toList()
        aliveDrivers.clear()

        drivers.forEach { driver ->
            try {
                if (!driver.isQuit) {
                    // log.info("Quiting {}", driver)
                    driver.quit()
                    ++count
                    numQuit.incrementAndGet()
                }
            } catch (e: org.openqa.selenium.WebDriverException) {
                when {
                    e.cause is org.apache.http.conn.HttpHostConnectException ->
                        log.warn("Web driver is already closed: {}", StringUtil.simplifyException(e))
                    e is org.openqa.selenium.NoSuchSessionException ->
                        log.warn("Web driver is already closed: {}", StringUtil.simplifyException(e))
                    else -> log.error("Unexpected exception: {}", e)
                }
            } catch (e: Throwable) {
                log.error("Unexpected exception", e)
            }
        }

        log.info("Total $count/$numQuit drivers are quit")
    }
}
