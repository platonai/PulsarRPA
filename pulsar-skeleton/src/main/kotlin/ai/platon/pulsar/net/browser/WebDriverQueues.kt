package ai.platon.pulsar.net.browser

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.BrowserControl.Companion.imagesEnabled
import ai.platon.pulsar.common.BrowserControl.Companion.pageLoadStrategy
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.InternalProxyServer
import com.gargoylesoftware.htmlunit.WebClient
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverQueues(
        private val browserControl: BrowserControl,
        private val internalProxyServer: InternalProxyServer,
        private val conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverQueues::class.java)

    companion object {
        private val freeDrivers = HashMap<Int, ArrayBlockingQueue<WebDriver>>()
        private val allDrivers = Collections.synchronizedSet(HashSet<WebDriver>())
        private val totalDriverCount = AtomicInteger(0)
        private val freeDriverCount = AtomicInteger(0)
        private val workingDriverCount = AtomicInteger(0)
    }

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val proxyPool = PulsarEnv.proxyPool
    private val isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true)
    private val pageLoadTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(60))
    private val closed = AtomicBoolean(false)
    private val isClosed = closed.get()
    val capacity = conf.getInt(SELENIUM_MAX_WEB_DRIVERS, (1.5 * PulsarEnv.NCPU).toInt())

    val freeSize get() = freeDriverCount.get()
    val totalSize get() = totalDriverCount.get()

    internal inner class PulsarHtmlUnitDriver(capabilities: Capabilities) : HtmlUnitDriver() {
        private val throwExceptionOnScriptError: Boolean = capabilities.`is`("throwExceptionOnScriptError")

        override fun modifyWebClient(client: WebClient): WebClient {
            client.options.isThrowExceptionOnScriptError = throwExceptionOnScriptError
            return client
        }
    }

    fun put(priority: Int, driver: WebDriver) {
        try {
            val queue = freeDrivers[priority]
            if (queue != null) {
                queue.put(driver)

                workingDriverCount.decrementAndGet()
                freeDriverCount.incrementAndGet()
            }
        } catch (e: InterruptedException) {
            log.warn("Failed to put a WebDriver into pool, $e")
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): WebDriver? {
        if (isClosed) {
            return null
        }

        val queue = getOrCreateWebDriverQueue(priority)
        val timeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)

        try {
            val driver = queue.poll(2 * timeout.seconds, TimeUnit.SECONDS)
            workingDriverCount.incrementAndGet()
            freeDriverCount.decrementAndGet()
            return driver
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.info("Interrupted, no web driver should return")
        }

        return null
    }

    private fun getOrCreateWebDriverQueue(priority: Int): ArrayBlockingQueue<WebDriver> {
        val queue = freeDrivers.computeIfAbsent(priority) { ArrayBlockingQueue(capacity) }
        if (queue.isEmpty()) {
            allocateWebDriver(queue, conf)
        }
        return queue
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun ssl() {
        val trustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private fun allocateWebDriver(queue: ArrayBlockingQueue<WebDriver>, conf: ImmutableConfig) {
        if (isClosed) {
            return
        }

        if (totalSize >= capacity) {
            log.warn("Too many web drivers ... cpu cores: {}, capacity: {}, free/total: {}/{}",
                    PulsarEnv.NCPU, capacity, freeSize, totalSize)
            return
        }

        try {
            val driver = doCreateWebDriver(conf)
            val level = setLogLevel(driver)

            synchronized(WebDriverQueues::class.java) {
                totalDriverCount.incrementAndGet()
                freeDriverCount.incrementAndGet()
                allDrivers.add(driver)
                queue.put(driver)

                log.info("The {}th web driver is online, " +
                        "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {} level: {}",
                        totalDriverCount, driver.javaClass.simpleName.toLowerCase(),
                        imagesEnabled, pageLoadStrategy, capacity, level)
            }
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
        }
    }

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    private fun doCreateWebDriver(conf: ImmutableConfig): WebDriver {
        val browser = getBrowser(conf)

        val capabilities = BrowserControl.createGeneralOptions()

        // Proxy is enabled by default
        val disableProxy = conf.getBoolean(PROXY_DISABLED, false)
        if (!disableProxy) {
            val proxy = getProxy(conf)
            if (proxy != null) {
                capabilities.setCapability(CapabilityType.PROXY, proxy)

                if (log.isDebugEnabled) {
                    log.debug("Use proxy $proxy")
                }
            }
        }

        // Choose the WebDriver
        val driver: WebDriver
        if (browser == BrowserType.CHROME) {
            val chromeOptions = BrowserControl.createChromeOptions(capabilities)
            driver = ChromeDriver(chromeOptions)
        } else if (browser == BrowserType.HTMLUNIT) {
            capabilities.setCapability("browserName", "htmlunit")
            driver = PulsarHtmlUnitDriver(capabilities)
        } else {
            if (RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass)) {
                driver = defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
            } else {
                driver = defaultWebDriverClass.getConstructor().newInstance()
            }
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

    /**
     * Get a proxy from the proxy pool
     * 1. Get a proxy from config, it is usually set in session scope
     * 2. Get a proxy from the proxy poll
     */
    private fun getProxy(conf: ImmutableConfig): org.openqa.selenium.Proxy? {
        var ipPort = if (isInternalProxyServerRunning()) {
            internalProxyServer.ipPort
        } else conf.get(PROXY_IP_PORT)

        if (ipPort == null) {
            // internal proxy server is not available, set proxy to the browser directly
            val proxyEntry = proxyPool.poll()
            if (proxyEntry != null) {
                ipPort = proxyEntry.ipPort()
            }
        }

        if (ipPort == null) {
            return null
        }

        val proxy = org.openqa.selenium.Proxy()
        proxy.httpProxy = ipPort
        proxy.sslProxy = ipPort
        proxy.ftpProxy = ipPort

        return proxy
    }

    private fun isInternalProxyServerRunning(): Boolean {
        if (internalProxyServer.disabled) {
            return false
        }

        if (isClosed) {
            return false
        }

        return internalProxyServer.waitUntilRunning()
    }

    /**
     * TODO: choose a best browser automatically: which one is faster yet still have good result
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     */
    private fun getBrowser(mutableConfig: ImmutableConfig?): BrowserType {
        val browser: BrowserType

        if (mutableConfig != null) {
            browser = mutableConfig.getEnum(SELENIUM_BROWSER, BrowserType.CHROME)
        } else {
            browser = conf.getEnum(SELENIUM_BROWSER, BrowserType.CHROME)
        }

        return browser
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        freeDrivers.values.forEach { it.clear() }
        freeDrivers.clear()

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        // wait for all drivers are recycled
        var maxWait = 5
        while (maxWait-- > 0 && workingDriverCount.get() > 0) {
            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {}
        }

        val it = allDrivers.iterator()
        while (it.hasNext()) {
            val driver = it.next()
            it.remove()

            try {
                log.info("Closing web driver {}", driver)
                driver.quit()
            } catch (e: org.openqa.selenium.WebDriverException) {
                if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                    // already closed, nothing to do
                    log.trace("Web driver is already closed: {}", e.message)
                } else {
                    log.error("Unexpected exception: {}", e)
                }
            } catch (e: Exception) {
                log.error("Unexpected exception: {}", e)
            }
        }
    }
}
