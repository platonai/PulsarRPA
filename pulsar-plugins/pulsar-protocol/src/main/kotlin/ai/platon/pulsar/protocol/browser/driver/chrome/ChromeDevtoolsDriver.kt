package ai.platon.pulsar.protocol.browser.driver.chrome

import ai.platon.pulsar.browser.driver.BlockRules
import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsInvocationException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.browser.driver.chrome.util.ScreenshotException
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.conf.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.conf.sites.jd.JdBlockRules
import ai.platon.pulsar.protocol.browser.driver.BrowserInstance
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.driver.chrome.hotfix.JdInitializer
import com.github.kklisura.cdt.protocol.types.page.Viewport
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal data class DeviceMetrics(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double,
    val mobile: Boolean,
)

class ChromeDevtoolsDriver(
    private val browserSettings: WebDriverSettings,
    private val browserInstance: BrowserInstance,
) : AbstractWebDriver(browserInstance.id) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

//    val dataDir get() = launchOptions.userDataDir
//    val proxyServer get() = launchOptions.proxyServer

    override val browserType: BrowserType = BrowserType.CHROME

    val openSequence = 1 + browserInstance.devToolsCount
    val userAgent get() = browserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val clientLibJs = browserSettings.generatePreloadJs(false)
    var devToolsConfig = DevToolsConfig()

    val tab: ChromeTab
    val devTools: RemoteDevTools

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: SessionId? = null
    private val browser get() = devTools.browser
    private var navigateUrl = ""
    private val page get() = devTools.page
    private val mainFrame get() = page.frameTree.frame
    private val network get() = devTools.network
    private val fetch get() = devTools.fetch
    private val runtime get() = devTools.runtime
    private val emulation get() = devTools.emulation

    private val enableBlockingReport = false
    private val closed = AtomicBoolean()

    val numSessionLost = AtomicInteger()
    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || !devTools.isOpen || numSessionLost.get() > 1
    val isActive get() = !isGone

    val viewport = Viewport().apply {
        x = 0.0
        y = 0.0
        width = BrowserSettings.viewPort.getWidth()
        height = BrowserSettings.viewPort.getHeight()
        scale = 1.0
    }

    init {
        try {
            // In chrome every tab is a separate process
            tab = browserInstance.createTab()
            navigateUrl = tab.url ?: ""

            devTools = browserInstance.createDevTools(tab, devToolsConfig)

            if (userAgent.isNotEmpty()) {
                emulation.setUserAgentOverride(userAgent)
            }
        } catch (e: ChromeProcessTimeoutException) {
            throw DriverLaunchException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            throw DriverLaunchException("Failed to create chrome devtools driver", e)
        }
    }

    override fun setTimeouts(driverConfig: BrowserSettings) {
    }

    @Throws(NoSuchSessionException::class)
    override fun navigateTo(url: String) {
        initSpecialSiteBeforeVisit(url)

        browserInstance.navigateHistory.add(url)
        lastActiveTime = Instant.now()
        takeIf { browserSettings.jsInvadingEnabled }?.getInvaded(url) ?: getNoInvaded(url)
    }

    /**
     * TODO: use an event handler to do this stuff
     * */
    private fun initSpecialSiteBeforeVisit(url: String) {
        if (isFirstLaunch) {
            // the first visit to jd.com
            val isFirstJdVisit = url.contains("jd.com")
                    && browserInstance.navigateHistory.none { it.contains("jd.com") }
            if (isFirstJdVisit) {
                JdInitializer().init(page)
            }
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun stopLoading() {
        if (!isActive) return

        try {
            page.stopLoading()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            logger.warn("Failed to call stop loading, session is already closed, {}", Strings.simplifyException(e))
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun evaluate(expression: String): Any? {
        if (!isActive) return null

        try {
            val evaluate = runtime.evaluate(expression)
            val result = evaluate?.result
            // TODO: handle errors here
            return result?.value
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    override val sessionId: String
        @Throws(NoSuchSessionException::class)
        get() {
            try {
                lastSessionId = if (!isActive) null else SessionId(mainFrame.id)
                return lastSessionId.toString()
            } catch (e: ChromeDevToolsInvocationException) {
                numSessionLost.incrementAndGet()
                throw NoSuchSessionException(e.message)
            }
        }

    override val currentUrl: String
        @Throws(NoSuchSessionException::class)
        get() {
            try {
                navigateUrl = if (!isActive) navigateUrl else mainFrame.url
                return navigateUrl
            } catch (e: ChromeDevToolsInvocationException) {
                numSessionLost.incrementAndGet()
                throw NoSuchSessionException(e.message)
            }
        }

    @Throws(NoSuchSessionException::class)
    fun getWindowHandles(): Set<String> {
        if (!isActive) return setOf()
        return browserInstance.chrome.getTabs().mapTo(HashSet()) { it.id }
    }

    override val pageSource: String
        @Throws(NoSuchSessionException::class)
        get() {
            try {
                val evaluate = runtime.evaluate("document.documentElement.outerHTML")
                return evaluate?.result?.value?.toString() ?: ""
            } catch (e: ChromeDevToolsInvocationException) {
                numSessionLost.incrementAndGet()
                throw NoSuchSessionException(e.message)
            }
        }

    @Throws(NoSuchSessionException::class)
    override fun bringToFront() {
        if (isActive) {
            page.bringToFront()
        }
    }

    @Throws(NoSuchSessionException::class)
    fun getCookieNames(): List<String> {
        if (!isActive) return listOf()

        try {
            return devTools.network.allCookies.map { it.name }
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun deleteAllCookies() {
        if (!isActive) return

        try {
            devTools.network.clearBrowserCookies()
            devTools.network.clearBrowserCache()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun deleteCookieNamed(name: String) {
        if (!isActive) return

        try {
            devTools.network.deleteCookies(name)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

//    fun deleteLocalStorage() {
//        // devTools.cacheStorage.deleteCache()
//    }

    @Throws(ScreenshotException::class, NoSuchSessionException::class)
    fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X {
        try {
            val startTime = System.currentTimeMillis()
            var result: X? = null
            runBlocking {
                withContext(Dispatchers.IO) {
                    result = withTimeoutOrNull(30 * 1000L) {
                        val deviceMetricsOverride = evaluate("__utils__.getFullPageMetrics()")?.toString()
                        val screenshot = if (deviceMetricsOverride != null) {
                            val m = GsonBuilder().create().fromJson(deviceMetricsOverride, DeviceMetrics::class.java)
                            emulation.setDeviceMetricsOverride(m.width, m.height, m.deviceScaleFactor, m.mobile)
                            page.captureScreenshot().also { emulation.clearDeviceMetricsOverride() }
                        } else {
                            // val screenshot = page.captureScreenshot(CaptureScreenshotFormat.PNG, 50, viewport, true)
                            page.captureScreenshot()
                        }

                        logger.debug("It takes {} to take screenshot | {}",
                            DateTimes.elapsedTime(startTime).readable(),
                            navigateUrl)
                        outputType.convertFromBase64Png(screenshot)
                    }
                    logger.takeIf { result == null }?.warn("Timeout to take screenshot | {}", navigateUrl)
                }
            }
            return result ?: throw ScreenshotException("Failed to take screenshot | $navigateUrl")
        } catch (e: ScreenshotException) {
            throw e
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            // TODO: it seems not a proper Exception to throw
            throw NoSuchSessionException(e.message)
        }
    }

    override fun toString() = "Devtools driver ($lastSessionId)"

    /**
     * Quit the browser instance
     * */
    override fun quit() {
        close()
        // browserInstanceManager.closeIfPresent(launchOptions.userDataDir)
    }

    /**
     * Close the tab hold by this driver
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            devTools.close()
            browserInstance.closeTab(tab)
        }
    }

    @Throws(NoSuchSessionException::class)
    private fun getInvaded(url: String) {
        if (!isActive) return

        try {
            if (clientLibJs.isNotBlank()) {
                page.addScriptToEvaluateOnNewDocument(clientLibJs)
            }

            page.enable()

            if (enableUrlBlocking) {
                setupUrlBlocking(url)
                network.enable()
            }
//            fetch.enable()

            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    private fun getNoInvaded(url: String) {
        if (!isActive) return

        try {
            page.enable()
            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    /**
     * TODO: load blocking rules from config files
     * */
    private fun setupUrlBlocking(url: String) {
        val blockRules = when {
            "amazon.com" in url -> AmazonBlockRules()
            "jd.com" in url -> JdBlockRules()
            else -> BlockRules()
        }

        // TODO: case sensitive or not?
        network.setBlockedURLs(blockRules.blockingUrls)

        network.takeIf { enableBlockingReport }?.onRequestWillBeSent {
            val requestUrl = it.request.url
            if (blockRules.mustPassUrlPatterns.any { requestUrl.matches(it) }) {
                return@onRequestWillBeSent
            }

            if (it.type in blockRules.blockingResourceTypes) {
                if (blockRules.blockingUrlPatterns.none { requestUrl.matches(it) }) {
                    logger.info("Resource ({}) might be blocked | {}", it.type, it.request.url)
                }

                // TODO: when fetch is enabled, no resources is return, find out the reason
                // fetch.failRequest(it.requestId, ErrorReason.BLOCKED_BY_RESPONSE)
                // fetch.fulfillRequest(it.requestId, 200, listOf())
            }
        }
    }

    @Throws(NoSuchSessionException::class)
    private fun isMainFrame(frameId: String): Boolean {
        if (!isActive) return false

        return mainFrame.id == frameId
    }

    class ShutdownHookRegistry : ChromeLauncher.ShutdownHookRegistry {

        override fun register(thread: Thread) {
        }

        override fun remove(thread: Thread) {
        }
    }
}
