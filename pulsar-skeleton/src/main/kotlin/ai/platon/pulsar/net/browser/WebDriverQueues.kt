package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.GlobalExecutor.NCPU
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
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
class WebDriverQueues(val browserControl: BrowserControl, val conf: ImmutableConfig): Parameterized, AutoCloseable {
    val log = LoggerFactory.getLogger(WebDriverQueues::class.java)

    private val defaultWebDriverClass = conf.getClass(
            SELENIUM_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)

    private val freeDrivers = HashMap<Int, ArrayBlockingQueue<WebDriver>>()
    private val allDrivers = Collections.synchronizedSet(HashSet<WebDriver>())
    private val proxyPool: ProxyPool = ProxyPool.getInstance(conf)
    private val freeDriverCount = AtomicInteger(0)

    val isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true)
    val implicitlyWait = conf.getDuration(FETCH_DOM_WAIT_FOR_TIMEOUT, Duration.ofSeconds(20))
    val pageLoadTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(30))
    val scriptTimeout = conf.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(5))

    val freeSize get() = freeDriverCount.get().toLong()
    val totalSize get() = allDrivers.size.toLong()

    private val closed = AtomicBoolean(false)

    internal inner class PulsarHtmlUnitDriver(capabilities: Capabilities) : HtmlUnitDriver() {
        private val throwExceptionOnScriptError: Boolean = capabilities.`is`("throwExceptionOnScriptError")

        override fun modifyWebClient(client: WebClient): WebClient {
            client.options.isThrowExceptionOnScriptError = throwExceptionOnScriptError
            return client
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { this.close() }))
    }

    override fun getParams(): Params {
        return Params.of(
                "defaultWebDriverClass", defaultWebDriverClass,
                "isHeadless", isHeadless,
                "implicitlyWait", implicitlyWait,
                "pageLoadTimeout", pageLoadTimeout,
                "scriptTimeout", scriptTimeout
        )
    }

    fun put(priority: Int, driver: WebDriver) {
        try {
            val queue = freeDrivers[priority]
            if (queue != null) {
                queue.put(driver)
                freeDriverCount.getAndIncrement()
            }
        } catch (e: InterruptedException) {
            log.warn("Failed to put a WebDriver into pool, $e")
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): WebDriver? {
        Objects.requireNonNull(conf)

        try {
            var queue: ArrayBlockingQueue<WebDriver>? = freeDrivers[priority]
            if (queue == null) {
                queue = ArrayBlockingQueue(NCPU)
                freeDrivers[priority] = queue
            }

            if (queue.isEmpty()) {
                allocateWebDriver(queue, conf)
            }

            val timeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
            val driver = queue.poll(2 * timeout.seconds, TimeUnit.SECONDS)
            freeDriverCount.decrementAndGet()

            return driver
        } catch (e: InterruptedException) {
            log.warn("Failed to poll a WebDriver from pool, $e")
        }

        // TODO: throw exception
        return null
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun ssl() {
        val trustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private fun getRunningBrowserCount() {
    }

    private fun allocateWebDriver(queue: ArrayBlockingQueue<WebDriver>, conf: ImmutableConfig) {
        // TODO: configurable factor
        if (allDrivers.size >= 1.5 * NCPU) {
            log.warn("Too many WebDrivers ... cpu cores: {}, free/total: {}/{}", NCPU, freeSize, totalSize)
            return
        }

        try {
            val driver = doCreateWebDriver(conf)
            allDrivers.add(driver)
            queue.put(driver)
            freeDriverCount.incrementAndGet()
            log.info("The {}th WebDriver is online, browser: {}", allDrivers.size, driver.javaClass.simpleName)
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
            // throw new RuntimeException("Can not create WebDriver");
        }
    }

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    private fun doCreateWebDriver(conf: ImmutableConfig): WebDriver {
        val browser = getBrowser(conf)

        val capabilities = browserControl.generalOptions
        val chromeOptions = browserControl.chromeOptions

        // Reset proxy
        capabilities.setCapability(CapabilityType.PROXY, null as Any?)
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)

        // Proxy is enabled by default
        val disableProxy = conf.getBoolean(PROXY_DISABLED, false)
        if (!disableProxy) {
            val proxy = getProxy(conf)
            if (proxy != null) {
                capabilities.setCapability(CapabilityType.PROXY, proxy)
                chromeOptions.setCapability(CapabilityType.PROXY, proxy)

                if (log.isDebugEnabled) {
                    log.debug("Use proxy $proxy")
                }
            }
        }

        // Choose the WebDriver
        val driver: WebDriver
        if (browser == BrowserType.CHROME) {
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

        // Set timeouts
        val timeouts = driver.manage().timeouts()
        timeouts.pageLoadTimeout(pageLoadTimeout.seconds, TimeUnit.SECONDS)
        timeouts.setScriptTimeout(scriptTimeout.seconds, TimeUnit.SECONDS)
        timeouts.implicitlyWait(implicitlyWait.seconds, TimeUnit.SECONDS)

        // Set log level
        if (driver is RemoteWebDriver) {
            val webDriverLog = LoggerFactory.getLogger(WebDriver::class.java)
            var level = Level.FINE
            if (webDriverLog.isDebugEnabled) {
                level = Level.FINER
            } else if (webDriverLog.isTraceEnabled) {
                level = Level.ALL
            }

            log.info("WebDriver log level: $level")
            driver.setLogLevel(level)
        }

        return driver
    }

    /**
     * Get a proxy from the proxy pool
     * 1. Get a proxy from config, it is usually set in session scope
     * 2. Get a proxy from the proxy poll
     */
    private fun getProxy(conf: ImmutableConfig): org.openqa.selenium.Proxy? {
        var ipPort: String? = conf.get(PROXY_IP_PORT)
        if (ipPort == null) {
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
        proxy.ftpProxy = ipPort
        proxy.sslProxy = ipPort

        return proxy
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

        if (isHeadless) {
            freeDrivers.clear()
            val it = allDrivers.iterator()
            while (it.hasNext()) {
                val driver = it.next()
                it.remove()

                try {
                    log.info("Closing WebDriver $driver")
                    driver.quit()
                } catch (e: Exception) {
                    log.error(e.toString())
                }

            }
        }
    }
}
