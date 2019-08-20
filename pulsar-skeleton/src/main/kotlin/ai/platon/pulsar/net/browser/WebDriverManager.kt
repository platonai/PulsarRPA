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
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.InternalProxyServer
import com.gargoylesoftware.htmlunit.WebClient
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.openqa.selenium.*
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
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

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
        data class DriverStatus(var pageViews: Int = 0)

        private val allDrivers = Collections.synchronizedSet(HashSet<WebDriver>())
        private val freeDrivers = HashMap<Int, ArrayBlockingQueue<WebDriver>>()
        private val workingDrivers = Collections.synchronizedSet(HashSet<WebDriver>())
        private val retiredDrivers = Collections.synchronizedSet(HashSet<String>())
        private val crashedDrivers = Collections.synchronizedSet(HashSet<String>())
        private val driversStatusTracker = Collections.synchronizedMap(HashMap<String, DriverStatus>())

        private val totalDriverCount get() = allDrivers.size
        private val freeDriverCount get() = freeDrivers.size
        private val workingDriverCount get() = workingDrivers.size
        private val crashedDriverCount get() = crashedDrivers.size
        private val retiredDriverCount get() = retiredDrivers.size
    }

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true)
    private val pageLoadTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(60))
    private val closed = AtomicBoolean(false)
    private val isClosed = closed.get()
    val capacity = conf.getInt(SELENIUM_MAX_WEB_DRIVERS, (1.5 * PulsarEnv.NCPU).toInt())

    val workingSize get() = workingDriverCount
    val freeSize get() = freeDriverCount
    val totalSize get() = totalDriverCount

    internal inner class PulsarHtmlUnitDriver(capabilities: Capabilities) : HtmlUnitDriver() {
        private val throwExceptionOnScriptError: Boolean = capabilities.`is`("throwExceptionOnScriptError")

        override fun modifyWebClient(client: WebClient): WebClient {
            client.options.isThrowExceptionOnScriptError = throwExceptionOnScriptError
            return client
        }
    }

    @Synchronized
    fun put(priority: Int, driver: WebDriver) {
        try {
            val handles = driver.windowHandles.size
            if (handles > 1) {
                driver.close()
            }

            freeDrivers.computeIfAbsent(priority) { ArrayBlockingQueue(capacity) }.put(driver)
            getStatus(driver).pageViews++
        } catch (e: Exception) {
            log.warn("Failed to recycle a WebDriver - {}", e)
        } finally {
            workingDrivers.remove(driver)
        }
    }

    @Synchronized
    fun retire(priority: Int, driver: WebDriver, e: Exception?) {
        freeDrivers.computeIfAbsent(priority) { ArrayBlockingQueue(capacity) }.remove(driver)
        workingDrivers.remove(driver)
        retiredDrivers.add(driver.toString())

        when (e) {
            is org.openqa.selenium.NoSuchSessionException -> crashedDrivers.add(driver.toString())
            is org.apache.http.conn.HttpHostConnectException -> crashedDrivers.add(driver.toString())
        }

        try {
            log.info("Quit web driver {}", driver)
            // Quits this driver, closing every associated window.
            driver.quit()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, e.message?.splitToSequence("\n")?.firstOrNull())
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, StringUtil.stringifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", StringUtil.stringifyException(e))
        } finally {
        }
    }

    @Synchronized
    fun poll(priority: Int, conf: ImmutableConfig): WebDriver? {
        if (isClosed) {
            return null
        }

        val queue = freeDrivers.computeIfAbsent(priority) { ArrayBlockingQueue(capacity) }
        if (queue.isEmpty()) {
            allocateWebDriver(priority, conf)
        }

        val driverPollingTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
        var driver: WebDriver?
        try {
            driver = queue.poll(2 * driverPollingTimeout.seconds, TimeUnit.SECONDS)
            driver?.also { workingDrivers.add(it) }
        } catch (e: InterruptedException) {
            log.info("Interrupted, no web driver should return")
            driver = null
        }

        return if (isClosed) null else driver
    }

    fun getStatus(driver: WebDriver): DriverStatus {
        return driversStatusTracker.computeIfAbsent(driver.toString()) { DriverStatus() }
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun ssl() {
        val trustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private fun allocateWebDriver(priority: Int, conf: ImmutableConfig) {
        if (isClosed) {
            return
        }

        if (freeSize + workingSize >= capacity) {
            log.warn("Too many web drivers ... cpu cores: {}, capacity: {}, free/working/total/crashed/retired: {}/{}/{}/{}/{}",
                    PulsarEnv.NCPU, capacity,
                    freeSize, workingSize, totalSize, crashedDriverCount, retiredDriverCount)
            return
        }

        try {
            val driver = createWebDriver(conf)
            val level = setLogLevel(driver)

            synchronized(WebDriverManager::class.java) {
                allDrivers.add(driver)
                freeDrivers.computeIfAbsent(priority) { ArrayBlockingQueue(capacity) }.put(driver)
            }

            log.info("The {}th web driver is online, " +
                    "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {} level: {}",
                    totalDriverCount, driver.javaClass.simpleName.toLowerCase(),
                    imagesEnabled, pageLoadStrategy, capacity, level)
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
        }
    }

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    private fun createWebDriver(conf: ImmutableConfig): WebDriver {
        val capabilities = BrowserControl.createGeneralOptions()

        // Proxy is enabled by default
        if (PulsarConstants.USE_PROXY) {
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

        return driver
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

    fun closeAll() {
        // wait for all drivers are recycled
        var maxWait = 60
        while (maxWait-- > 0 && workingDriverCount > 0) {
            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {}
        }

        closeAllUnchecked()
    }

    @Synchronized
    private fun closeAllUnchecked() {
        log.info("Closing all web drivers ...")

        freeDrivers.values.forEach { it.clear() }
        freeDrivers.clear()
        workingDrivers.clear()

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        val it = allDrivers.iterator()
        while (it.hasNext()) {
            val driver = it.next()
            it.remove()

            try {
                log.info("Quit {}", driver)
                driver.quit()
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

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        closeAll()
    }
}
