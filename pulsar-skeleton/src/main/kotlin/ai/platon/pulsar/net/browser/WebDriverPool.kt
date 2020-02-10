package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.InternalProxyServer
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
import java.nio.file.Files
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
import java.util.logging.Level
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverPool(
        private val driverControl: WebDriverControl,
        private val proxyPool: ProxyPool,
        private val ips: InternalProxyServer,
        private val conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverPool::class.java)

    companion object {
        private val instanceCounter = AtomicInteger()
        private val pollingTimeout = Duration.ofMillis(100)
        private val allDrivers = Collections.synchronizedMap(HashMap<Int, ManagedWebDriver>())
        // Every value collection is a first in, first out queue
        private val freeDrivers = mutableMapOf<Int, LinkedList<ManagedWebDriver>>()
        private val workingDrivers = HashMap<Int, ManagedWebDriver>()
        private var driversAreClosing = false
        private val lock = ReentrantLock() // lock for containers
        private val notEmpty = lock.newCondition()
        private val notBusy = lock.newCondition()
        private val notClosing = lock.newCondition()

        val numCrashed = AtomicInteger()
        val numRetired = AtomicInteger()
        val numQuit = AtomicInteger()

        val pageViews = AtomicInteger()
    }

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val isHeadless = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val isClosed = closed.get()
    val capacity: Int = conf.getInt(SELENIUM_MAX_WEB_DRIVERS, (1.5 * AppConstants.NCPU).toInt())
    var lastActiveTime = Instant.now()
    var idleTimeout = Duration.ofMinutes(5)
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = workingSize == 0 && idleTime > idleTimeout

    val isAllEmpty: Boolean = lock.withLock { allDrivers.isEmpty() && freeDrivers.isEmpty() && workingDrivers.isEmpty() }
    val workingSize: Int = lock.withLock { workingDrivers.size }
    val freeSize: Int = lock.withLock { freeDrivers.values.sumBy { it.size } }
    val aliveSize: Int = lock.withLock { workingDrivers.size + freeDrivers.values.sumBy { it.size } }

    val totalSize get() = allDrivers.size

    val proxyEntry get() = ips.proxyEntry

    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        val driver = poll(priority, volatileConfig)
                ?:throw WebDriverPoolExhaust("Web driver pool exhausted")

        try {
            driver.proxyEntry.set(proxyEntry)
            return action(driver)
        } finally {
            put(driver)
        }
    }

    fun allocate(priority: Int, numInit: Int, conf: ImmutableConfig) {
        ensureNotClosing()
        if (isClosed) {
            return
        }

        val drivers = mutableListOf<ManagedWebDriver>()
        var n = numInit
        while (n-- > 0) {
            poll(priority, conf)?.let { drivers.add(it) }
        }
        drivers.forEach { put(it) }
    }

    fun reset() {
        closeAll(incognito = true)
    }

    fun cancel(url: String): ManagedWebDriver? {
        ensureNotClosing()

        var driver: ManagedWebDriver? = null
        lock.withLock {
            if (isClosed) {
                return null
            }

            driver = workingDrivers.values.firstOrNull { it.currentUrl == url }
        }

        driver?.evaluateSilently(";window.stop();")
        return driver
    }

    fun report() {
        log.info(formatStatus(verbose = true))

        lock.withLock {
            val sb = StringBuilder()

            allDrivers.values.forEach { driver ->
                driver.driver.manage().cookies.joinTo(sb, "Cookies in driver #${driver.id}: ") { it.toString() }
            }

            if (sb.isNotBlank()) {
                log.info("Cookies: \n{}", sb)
            } else {
                log.info("All drivers have no cookie")
            }
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        ensureNotClosing()

        var driver: ManagedWebDriver? = null
        var exception: Exception? = null

        try {
            lock.lockInterruptibly()

            if (isClosed) {
                return null
            }

            driver = dequeue(priority, conf)
            var nanos = pollingTimeout.toNanos()
            while (driver == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos)
                driver = dequeue(priority, conf)
            }

            if (driver != null) {
                driver.status = DriverStatus.WORKING
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

    fun put(driver: ManagedWebDriver) {
        if (driver.isRetired) {
            retire(driver, null)
        } else {
            offer(driver)
        }
    }

    private fun offer(driver: ManagedWebDriver) {
        ensureNotClosing()
        if (isClosed) {
            return
        }

        try {
            lastActiveTime = Instant.now()

            // a driver is always hold by only one thread, so it's OK to use it without locks
            driver.closeIfNotOnly()
            driver.status = DriverStatus.FREE
            driver.stat.pageViews++
            pageViews.incrementAndGet()
        } catch (e: Exception) {
            // log.warn("Failed to recycle a WebDriver, retire it - {}", StringUtil.simplifyException(e))
            log.warn(StringUtil.stringifyException(e))
            driver.status = DriverStatus.UNKNOWN
            retire(driver, e)
            return
        }

        lock.withLock {
            if (isClosed) {
                return
            }

            // it can be retired by close all
            if (driver.status == DriverStatus.FREE) {
                val queue = freeDrivers[driver.priority]
                if (queue != null) {
                    queue.add(driver)
                    // TODO: every queue should have a signal
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
        ensureNotClosing()
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

        driver.status = DriverStatus.RETIRED

        lock.withLock {
            freeDrivers[driver.priority]?.remove(driver)
            workingDrivers.remove(driver.id)

            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }
        }

        when (e) {
            is org.openqa.selenium.NoSuchSessionException -> driver.status = DriverStatus.CRASHED
            is org.apache.http.conn.HttpHostConnectException -> driver.status = DriverStatus.CRASHED
        }

        when (driver.status) {
            DriverStatus.RETIRED -> numRetired.incrementAndGet()
            DriverStatus.CRASHED -> numCrashed.incrementAndGet()
            else -> {}
        }

        try {
            if (e != null) {
                log.info("Quit {} driver {} - {}", driver.status.name.toLowerCase(), driver, StringUtil.simplifyException(e))
            } else {
                log.info("Quit {} driver {}", driver.status.name.toLowerCase(), driver)
            }
            // Quits this driver, close every associated window.
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

    /**
     * TODO: conf is not really used if the queue is not empty
     * */
    private fun dequeue(group: Int, conf: ImmutableConfig): ManagedWebDriver? {
        val queue = freeDrivers.computeIfAbsent(group) { LinkedList() }

        if (queue.isEmpty()) {
            val driver = allocateWebDriver(group, conf)
            if (driver != null) {
                queue.add(driver)
                // log.debug("Allocate driver {}", driver)
            }
        }

        return if (queue.isEmpty()) null else queue.remove()
    }

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        ensureNotClosing()

        lock.withLock {
            driversAreClosing = true
            try {
                var i = 0
                while (workingSize > 0 && i++ < 120) {
                    notBusy.await(1, TimeUnit.SECONDS)
                }

                closeAllUnlocked(incognito, processExit)
            } finally {
                driversAreClosing = false
                notClosing.signalAll()
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(processExit = true)
        }
    }

    private fun ensureNotClosing() {
        var nanos = pollingTimeout.toNanos()
        lock.withLock {
            while (driversAreClosing && nanos > 0) {
                nanos = notClosing.awaitNanos(nanos)
            }
        }
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun ssl() {
        val trustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private fun allocateWebDriver(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        if (isClosed) {
            return null
        }

        if (aliveSize >= capacity) {
            log.warn("Too many web drivers. Cpu cores: {}, capacity: {}, {}",
                    AppConstants.NCPU, capacity, formatStatus(verbose = false))
            return null
        }

        try {
            val driver = createWebDriver(priority, conf)
            allDrivers[driver.id] = driver

            val level = setLogLevel(driver.driver)

            log.trace("The {}th web driver is online, " +
                    "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {} level: {}",
                    totalSize, driver.driver.javaClass.simpleName,
                    driverControl.imagesEnabled, driverControl.pageLoadStrategy, capacity, level)

            return driver
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
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
    private fun createWebDriver(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
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
            val fakeAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"
            val devTools = driver.devTools
            devTools.createSession()
            devTools.send(Log.enable())
            devTools.addListener(Log.entryAdded()) { e -> log.error(e.text) }
            devTools.send(Network.setUserAgentOverride(fakeAgent, Optional.empty(), Optional.empty()))

            // devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));
            // devTools.send(emulateNetworkConditions(false,100,200000,100000, Optional.of(ConnectionType.cellular4g)));

            // driver.manage().window().maximize()
        }

        return ManagedWebDriver(instanceCounter.incrementAndGet(), driver, priority)
    }

    private fun setLogLevel(driver: WebDriver): Level {
        // Set log level
        var level = Level.FINE
        if (driver is RemoteWebDriver) {
            val l = LoggerFactory.getLogger(WebDriver::class.java)
            level = when {
                l.isDebugEnabled -> Level.FINER
                l.isTraceEnabled -> Level.ALL
                else -> Level.FINE
            }

            driver.setLogLevel(level)
        }
        return level
    }

    private fun setProxy(capabilities: DesiredCapabilities): ProxyEntry? {
        var proxyEntry: ProxyEntry? = null
        var hostPort: String? = null
        val proxy = org.openqa.selenium.Proxy()
        if (ips.ensureAvailable()) {
            // TODO: internal proxy server can be run at another host
            proxyEntry = ips.proxyEntry
            hostPort = "127.0.0.1:${ips.port}"
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
        log.trace("Use proxy {}", proxy)

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

    private fun pauseAllWorkingDrivers() {
        workingDrivers.values.forEach {
            it.evaluateSilently("window.stop()")
            it.pause()
        }
    }

    private fun changeProxy() {
        proxyEntry?.let { ips.changeProxyIfRunning(it) }
    }

    private fun deleteBrowserCache() {
        val dataFiles = Files.list(AppPaths.SYS_TMP_DIR).filter { it.fileName.startsWith(".org.chromium.Chromium") }
        dataFiles.forEach {
            try {
                Files.deleteIfExists(it)
            } catch (t: Throwable) {
                log.warn("Unexpected exception. {}", t)
            }
        }
    }

    private fun resumeAllDrivers() {
        workingDrivers.values.forEach {
            it.status = DriverStatus.WORKING
        }
    }

    private fun deleteAllCookies() {
        allDrivers.values.forEach {
            it.deleteAllCookiesSilently()
        }
    }

    private fun closeAllUnlocked(incognito: Boolean = true, processExit: Boolean = false) {
        if (incognito) {
            deleteAllCookies()

            if (!processExit) {
                changeProxy()
            }

            // browser cache can be useful
            // deleteBrowserCache()
        }

        if (allDrivers.isEmpty()) {
            if (aliveSize != 0) {
                log.info("Illegal status - {}", formatStatus(verbose = true))
            }

            return
        }

        log.info("Closing all web drivers ... {}", formatStatus(verbose = true))

        freeDrivers.flatMap { it.value }.forEach { retire(it, null, external = false) }
        freeDrivers.forEach { it.value.clear() }
        freeDrivers.clear()

        workingDrivers.map { it.value }.forEach { retire(it, null, external = false) }
        workingDrivers.clear()

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        val it = allDrivers.iterator()
        while (it.hasNext()) {
            val driver = it.next().value
            it.remove()

            try {
                if (!driver.isQuit) {
                    log.info("Quiting {}", driver)
                    driver.quit()
                    numQuit.incrementAndGet()
                }
            } catch (e: org.openqa.selenium.WebDriverException) {
                when {
                    e.cause is org.apache.http.conn.HttpHostConnectException -> log.warn("Web driver is already closed: {}", StringUtil.simplifyException(e))
                    e is org.openqa.selenium.NoSuchSessionException -> log.warn("Web driver is already closed: {}", StringUtil.simplifyException(e))
                    else -> log.error("Unexpected exception: {}", e)
                }
            } catch (e: Exception) {
                log.error("Unexpected exception: {}", e)
            }
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        if (verbose) {
            return String.format("total: %d free: %d working: %d alive: %d" +
                    " crashed: %d retired: %d quit: %d pageViews: %d",
                    totalSize, freeSize, workingSize, aliveSize,
                    numCrashed.get(), numRetired.get(), numQuit.get(), pageViews.get()
            )
        } else {
            return String.format("%d/%d/%d/%d/%d (free/working/total/crashed/retired)",
                    freeSize, workingSize, aliveSize, numCrashed.get(), numRetired.get())
        }
    }
}
