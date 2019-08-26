package ai.platon.pulsar.net.browser

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.BrowserControl.Companion.imagesEnabled
import ai.platon.pulsar.common.BrowserControl.Companion.pageLoadStrategy
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.InternalProxyServer
import com.gargoylesoftware.htmlunit.WebClient
import org.apache.commons.lang3.StringUtils
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
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
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

data class DriverStat(
        var pageViews: Int = 0
)

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, RETIRED, CRASHED, QUIT
}

class ManagedWebDriver(
        val id: Int,
        val driver: WebDriver,
        val priority: Int = 1000,
        var proxyEntry: ProxyEntry? = null
) {
    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)
    val stat = DriverStat()
    var status: DriverStatus = DriverStatus.UNKNOWN
        @Synchronized get
        @Synchronized set
    val isQuit: Boolean
        get() = status == DriverStatus.QUIT || sessionId == null
    var proxy: ProxyEntry? = null
    var incognito = true
    val sessionId get() = StringUtils.substringBetween(driver.toString(), "(", ")")

    fun quit() {
        if (isQuit) {
            return
        }

        if (incognito) {
            deleteAllCookiesSilently()
        }

        status = DriverStatus.QUIT
        driver.quit()
    }

    fun closeRedundantTabs() {
        val handles = driver.windowHandles.size
        if (handles > 1) {
            driver.close()
        }
    }

    fun deleteAllCookiesSilently() {
        try {
            driver.manage().deleteAllCookies()
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    fun deleteAllCookiesSilently(targetUrl: String) {
        try {
            driver.get(targetUrl)
            driver.manage().deleteAllCookies()
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is ManagedWebDriver && other.id == this.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return if (sessionId != null) "#$id-$sessionId" else "$id(closed)"
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverPool(
        private val browserControl: BrowserControl,
        private val proxyPool: ProxyPool,
        private val internalProxyServer: InternalProxyServer,
        private val conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverPool::class.java)

    companion object {
        private val instanceCounter = AtomicInteger()
        private val pollingTimeout = Duration.ofMillis(500)
        private val allDrivers = Collections.synchronizedMap(HashMap<Int, ManagedWebDriver>())
        // Every value collection is a first in, first out queue
        private val freeDrivers = mutableMapOf<Int, LinkedList<ManagedWebDriver>>()
        private val workingDrivers = HashMap<Int, ManagedWebDriver>()
        private val closingDrivers = AtomicBoolean()
        private val lock: Lock = ReentrantLock()
        private val notEmpty: Condition = lock.newCondition()
        private val noWorker: Condition = lock.newCondition()
        private val driversClosed = lock.newCondition()

        val numCrashed = AtomicInteger()
        val numRetired = AtomicInteger()
        val numQuit = AtomicInteger()

        val pageViews = AtomicInteger()
    }

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true)
    private val env = PulsarEnv.getOrCreate()
    private val closed = AtomicBoolean(false)
    private val isClosed = closed.get()
    val capacity: Int = conf.getInt(SELENIUM_MAX_WEB_DRIVERS, (1.5 * PulsarEnv.NCPU).toInt())
    var lastActiveTime = Instant.now()
    var idleTimeout = Duration.ofMinutes(5)
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = workingSize == 0 && idleTime > idleTimeout
    val isAllEmpty: Boolean
        get() {
            lock.withLock {
                return allDrivers.isEmpty() && freeDrivers.isEmpty() && workingDrivers.isEmpty()
            }
        }

    val workingSize: Int
        get() {
            lock.withLock { return workingDrivers.size }
        }
    val freeSize: Int
        get() {
            lock.withLock { return freeDrivers.values.sumBy { it.size } }
        }
    val aliveSize: Int
        get() {
            lock.withLock { return workingDrivers.size + freeDrivers.values.sumBy { it.size } }
        }
    val totalSize get() = allDrivers.size

    fun offer(driver: ManagedWebDriver) {
        if (closingDrivers.get()) {
            // closeAll is performing, the client can just throw away the driver
            return
        }

        try {
            lastActiveTime = Instant.now()

            // a driver is always hold by only one thread, so it's OK to use it without locks
            driver.closeRedundantTabs()
            driver.status = DriverStatus.FREE
            driver.stat.pageViews++
            pageViews.incrementAndGet()
        } catch (e: Exception) {
            log.warn("Failed to recycle a WebDriver, retire it - {}", StringUtil.simplifyException(e))
            driver.status = DriverStatus.UNKNOWN
            retire(driver, e)
            return
        }

        lock.withLock {
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
                noWorker.signalAll()
            }
        }
    }

    fun retire(driver: ManagedWebDriver, e: Exception?) {
        return retire(driver, e, true)
    }

    private fun retire(driver: ManagedWebDriver, e: Exception?, external: Boolean = true) {
        if (external && closingDrivers.get()) {
            // closeAll is performing, the client can just throw away the driver
            return
        }

        driver.status = DriverStatus.RETIRED

        lock.withLock {
            freeDrivers[driver.priority]?.remove(driver)
            workingDrivers.remove(driver.id)

            if (workingDrivers.isEmpty()) {
                noWorker.signalAll()
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
                log.info("Quit retired driver {} - {}", driver, StringUtil.simplifyException(e))
            } else {
                log.info("Quit retired driver {}", driver)
            }
            // Quits this driver, closing every associated window.
            driver.quit()
            numQuit.incrementAndGet()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, StringUtil.simplifyException(e))
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, StringUtil.stringifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", StringUtil.stringifyException(e))
        } finally {
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        var driver: ManagedWebDriver? = null
        var exception: Exception? = null
        var nanos = pollingTimeout.toNanos()

        try {
            lock.lockInterruptibly()

            driver = dequeue(priority, conf)
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

    fun closeAll(maxWait: Long = 60L) {
        lock.withLock {
            closingDrivers.set(true)
            if (workingSize > 0) {
                noWorker.await(maxWait, TimeUnit.SECONDS)
            }
            closeAllUnlocked()
            closingDrivers.set(false)
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        closeAll()
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
            log.warn("Too many web drivers ... cpu cores: {}, capacity: {}, free/working/total/crashed/retired: {}/{}/{}/{}/{}",
                    PulsarEnv.NCPU, capacity,
                    freeSize, workingSize, aliveSize, numCrashed.get(), numRetired.get())
            return null
        }

        try {
            val driver = createWebDriver(priority, conf)
            allDrivers[driver.id] = driver

            val level = setLogLevel(driver.driver)

            log.info("The {}th web driver is online, " +
                    "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {} level: {}",
                    totalSize, driver.driver.javaClass.simpleName,
                    imagesEnabled, pageLoadStrategy, capacity, level)

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
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    private fun createWebDriver(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        val capabilities = BrowserControl.createGeneralOptions()

        // Proxy is enabled by default
        var proxyEntry: ProxyEntry? = null
        if (env.useProxy) {
            proxyEntry = setProxy(capabilities)
        }

        // Choose the WebDriver
        val browserType = getBrowserType(conf)
        val driver: WebDriver = when {
            browserType == BrowserType.CHROME -> {
                ChromeDriver(BrowserControl.createChromeOptions(capabilities))
            }
            browserType == BrowserType.HTMLUNIT -> {
                PulsarHtmlUnitDriver(capabilities.also { it.setCapability("browserName", "htmlunit") })
            }
            RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass) -> {
                defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
            }
            else -> defaultWebDriverClass.getConstructor().newInstance()
        }

        driver.manage().window().maximize()

        return ManagedWebDriver(instanceCounter.incrementAndGet(), driver, priority, proxyEntry)
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
        if (internalProxyServer.waitUntilRunning()) {
            // TODO: internal proxy server can be run at another host
            proxyEntry = internalProxyServer.proxyEntry
            hostPort = "127.0.0.1:${internalProxyServer.port}"
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
        log.info("Use proxy {}", proxy)

        return proxyEntry
    }

    /**
     * TODO: choose a best browser automatically: which one is faster yet still have good result
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     */
    private fun getBrowserType(mutableConfig: ImmutableConfig?): BrowserType {
        return if (mutableConfig != null) {
            mutableConfig.getEnum(SELENIUM_BROWSER, BrowserType.CHROME)
        } else {
            conf.getEnum(SELENIUM_BROWSER, BrowserType.CHROME)
        }
    }

    private fun closeAllUnlocked() {
        if (allDrivers.isEmpty()) {
            if (aliveSize != 0) {
                log.info("Illegal statement - total: {}, {}free: {}, working: {}, alive: {}, " +
                        "crashed: {}, retired: {}, quit: {}, pageViews: {}",
                        totalSize, freeSize, workingSize, aliveSize,
                        numCrashed, numRetired, numQuit,
                        pageViews
                )
            }

            return
        }

        log.info("Closing all web drivers ...")

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

    internal inner class PulsarHtmlUnitDriver(capabilities: Capabilities) : HtmlUnitDriver() {
        private val throwExceptionOnScriptError: Boolean = capabilities.`is`("throwExceptionOnScriptError")

        override fun modifyWebClient(client: WebClient): WebClient {
            client.options.isThrowExceptionOnScriptError = throwExceptionOnScriptError
            return client
        }
    }
}
