package ai.platon.pulsar.net.browser

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.BrowserControl.Companion.imagesEnabled
import ai.platon.pulsar.common.BrowserControl.Companion.pageLoadStrategy
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.InternalProxyServer
import com.gargoylesoftware.htmlunit.WebClient
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.CapabilityType
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
    UNKNOWN, FREE, WORKING, RETIRED, CRASHED
}

class ManagedWebDriver(
        val id: Int,
        val driver: WebDriver,
        val priority: Int = 1000
) {
    val stat = DriverStat()
    var status: DriverStatus = DriverStatus.UNKNOWN
    var lastActiveTime = Instant.EPOCH
    var proxy: ProxyEntry? = null

    override fun equals(other: Any?): Boolean {
        return other is ManagedWebDriver && other.id == this.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "#$id - $driver"
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverManager(
        private val browserControl: BrowserControl,
        private val proxyPool: ProxyPool,
        private val internalProxyServer: InternalProxyServer,
        private val conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverManager::class.java)

    companion object {
        private val instanceCounter = AtomicInteger()
        private val pollingTimeout = Duration.ofMillis(500)
        private val allDrivers = Collections.synchronizedMap(HashMap<Int, ManagedWebDriver>())
        // Every value collection is a first in, first out queue
        private val freeDrivers = mutableMapOf<Int, LinkedList<ManagedWebDriver>>()
        private val workingDrivers = HashMap<Int, ManagedWebDriver>()
        private val lock: Lock = ReentrantLock()
        private val notEmpty: Condition = lock.newCondition()

        private val numFreeDrivers = AtomicInteger()
        private val numWorkingDrivers = AtomicInteger()
        private val numCrashed = AtomicInteger()
        private val numRetired = AtomicInteger()

        private val numPageViews = AtomicInteger()
    }

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true)
    private val env = PulsarEnv.getOrCreate()
    private val closed = AtomicBoolean(false)
    private val isClosed = closed.get()
    val capacity: Int = conf.getInt(SELENIUM_MAX_WEB_DRIVERS, (1.5 * PulsarEnv.NCPU).toInt())

    val workingSize: Int get() = numWorkingDrivers.get()

    val freeSize: Int get() = numFreeDrivers.get()

    val aliveSize: Int get() = numFreeDrivers.get() + numWorkingDrivers.get()

    val totalSize get() = allDrivers.size

    fun offer(driver: ManagedWebDriver) {
        try {
            val handles = driver.driver.windowHandles.size
            if (handles > 1) {
                driver.driver.close()
            }
            driver.status = DriverStatus.FREE
            driver.lastActiveTime = Instant.now()
            driver.stat.pageViews++
            numPageViews.incrementAndGet()

            if (numPageViews.get() % 10 == 0) {
                // report
            }

            lock.withLock {
                freeDrivers.computeIfAbsent(driver.priority) { LinkedList() }.add(driver)
                numFreeDrivers.incrementAndGet()
                workingDrivers.remove(driver.id)
                numWorkingDrivers.decrementAndGet()
                notEmpty.signal()
            }
        } catch (e: Exception) {
            log.warn("Failed to recycle a WebDriver - {}", e)
        } finally {
        }
    }

    fun retire(driver: ManagedWebDriver, e: Exception?) {
        driver.status = DriverStatus.RETIRED

        lock.withLock {
            freeDrivers.computeIfAbsent(driver.priority) { LinkedList() }.remove(driver)
            numFreeDrivers.decrementAndGet()
            workingDrivers.remove(driver.id)
            numWorkingDrivers.decrementAndGet()
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
            log.info("Quit web driver {}", driver)
            // Quits this driver, closing every associated window.
            driver.driver.quit()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, e.message?.splitToSequence("\n")?.firstOrNull())
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, StringUtil.stringifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", StringUtil.stringifyException(e))
        } finally {
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        var driver: ManagedWebDriver?
        var nanos = pollingTimeout.toNanos()

        try {
            lock.lockInterruptibly()

            driver = dequeue(priority, conf)
            while (driver == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos)
                driver = dequeue(priority, conf)
            }

            if (driver != null) {
                driver.status = DriverStatus.FREE
                workingDrivers[driver.id] = driver
                numFreeDrivers.decrementAndGet()
                numWorkingDrivers.incrementAndGet()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.info("Interrupted, no web driver should return")
            driver = null
        } finally {
            lock.unlock()
        }

        return if (isClosed) null else driver
    }

    private fun dequeue(group: Int, conf: ImmutableConfig): ManagedWebDriver? {
        val queue = freeDrivers.computeIfAbsent(group) { LinkedList() }

        if (queue.isEmpty()) {
            val driver = allocateWebDriver(group, conf)
            if (driver != null) {
                queue.add(driver)
                numFreeDrivers.incrementAndGet()
            }
        }

        return if (queue.isEmpty()) null else queue.remove()
    }

    fun closeAll() {
        // wait for all drivers are recycled
        val maxWait = 60
        var i = 0
        // TODO: use a condition
        while (i++ < maxWait && workingSize > 0) {
            if (i > 30) {
                log.warn("Waited {} round to wait for idle ...", i)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        lock.withLock {
            closeAllUnlocked()
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
        if (env.useProxy) {
            val proxy = getProxy()
            if (proxy != null) {
                capabilities.setCapability(CapabilityType.PROXY, proxy)
                log.info("Use proxy {}", proxy)
            }
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

    private fun getProxy(): org.openqa.selenium.Proxy? {
        var hostPort: String? = null
        if (internalProxyServer.waitUntilRunning()) {
            // TODO: internal proxy server can be run at another host
            hostPort = "127.0.0.1:${internalProxyServer.port}"
        }

        if (hostPort == null) {
            // internal proxy server is not available, set proxy to the browser directly
            hostPort = proxyPool.poll()?.hostPort
        }

        if (hostPort == null) {
            return null
        }

        val proxy = org.openqa.selenium.Proxy()
        proxy.httpProxy = hostPort
        proxy.sslProxy = hostPort
        proxy.ftpProxy = hostPort

        return proxy
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
        log.info("Closing all web drivers ...")

        freeDrivers.clear()
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
                log.info("Quit {}", driver)
                driver.driver.quit()
            } catch (e: org.openqa.selenium.WebDriverException) {
                if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                    // already closed, nothing to do
                    log.warn("Web driver is already closed: {}", e.toString().splitToSequence("\n").firstOrNull())
                } else if (e is org.openqa.selenium.NoSuchSessionException) {
                    log.warn("Web driver is already closed: {}", e.toString().splitToSequence("\n").firstOrNull())
                } else {
                    log.error("Unexpected exception: {}", e)
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
