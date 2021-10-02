package ai.platon.pulsar.protocol.browser.driver.chrome

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.LauncherConfig
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.conf.blockingResourceTypes
import ai.platon.pulsar.protocol.browser.conf.blockingUrlPatterns
import ai.platon.pulsar.protocol.browser.conf.blockingUrls
import ai.platon.pulsar.protocol.browser.conf.mustPassUrlPatterns
import ai.platon.pulsar.protocol.browser.driver.BrowserInstance
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import com.github.kklisura.cdt.launch.ChromeLauncher
import com.github.kklisura.cdt.protocol.types.page.Viewport
import com.github.kklisura.cdt.services.ChromeDevToolsService
import com.github.kklisura.cdt.services.exceptions.ChromeDevToolsInvocationException
import com.github.kklisura.cdt.services.types.ChromeTab
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.ScreenshotException
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal data class DeviceMetrics(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double,
    val mobile: Boolean
)

class DevToolsConfig(
//    var workerGroup: EventLoopGroup = DefaultEventLoopGroup(),
    var readTimeout: Duration = Duration.ofMinutes(READ_TIMEOUT_MINUTES)
) {
    companion object {
        private const val READ_TIMEOUT_PROPERTY = "chrome.browser.services.config.readTimeout"
        private val READ_TIMEOUT_MINUTES = System.getProperty(READ_TIMEOUT_PROPERTY, "3").toLong()
    }
}

/**
 * TODO: inherit from ai.platon.pulsar.crawl.fetch.driver.WebDriver
 * */
class ChromeDevtoolsDriver(
    private val launcherConfig: LauncherConfig,
    private val launchOptions: ChromeDevtoolsOptions,
    private val browserControl: WebDriverSettings,
    private val browserInstanceManager: BrowserInstanceManager,
) : RemoteWebDriver() {
    private val log = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    val dataDir get() = launchOptions.userDataDir
    val proxyServer get() = launchOptions.proxyServer

    val userAgent get() = browserControl.randomUserAgent()

    // TODO: load blocking rules from config files
    val enableUrlBlocking get() = browserControl.enableUrlBlocking
    val clientLibJs = browserControl.parseLibJs(false)
    var devToolsConfig = DevToolsConfig()

    val browserInstance: BrowserInstance
    val tab: ChromeTab
    val devTools: ChromeDevToolsService

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
    private val numSessionLost = AtomicInteger()
    private val closed = AtomicBoolean()
    private val isGone get() = closed.get() || devTools.isClosed || numSessionLost.get() > 1
    private val isActive get() = !isGone

    val viewport = Viewport().apply {
        x = 0.0
        y = 0.0
        width = BrowserSettings.viewPort.getWidth()
        height = BrowserSettings.viewPort.getHeight()
        scale = 1.0
    }

    init {
        try {
            browserInstance = browserInstanceManager.launchIfAbsent(launcherConfig, launchOptions)

            // In chrome every tab is a separate process
            tab = browserInstance.createTab()
            navigateUrl = tab.url ?: ""

            devTools = browserInstance.createDevTools(tab)

            if (userAgent.isNotEmpty()) {
                emulation.setUserAgentOverride(userAgent)
            }
        } catch (t: Throwable) {
            throw DriverLaunchException("Failed to create chrome devtools driver", t)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun get(url: String) {
        takeIf { browserControl.jsInvadingEnabled }?.getInvaded(url) ?: getNoInvaded(url)
    }

    @Throws(NoSuchSessionException::class)
    fun stopLoading() {
        if (!isActive) return

        try {
            page.stopLoading()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            log.warn("Failed to call stop loading, session is already closed, {}", Strings.simplifyException(e))
        }
    }

    @Throws(NoSuchSessionException::class)
    fun evaluate(expression: String): Any? {
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

    @Throws(NoSuchSessionException::class)
    override fun executeScript(script: String, vararg args: Any): Any? {
        TODO("Use evaluate instead")
    }

    @Throws(NoSuchSessionException::class)
    override fun getSessionId(): SessionId? {
        try {
            lastSessionId = if (!isActive) null else SessionId(mainFrame.id)
            return lastSessionId
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getCurrentUrl(): String {
        try {
            navigateUrl = if (!isActive) navigateUrl else mainFrame.url
            return navigateUrl
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getWindowHandles(): Set<String> {
        if (!isActive) return setOf()
        return browserInstance.chrome.getTabs().mapTo(HashSet()) { it.id }
    }

    @Throws(NoSuchSessionException::class)
    override fun getPageSource(): String {
        try {
            val evaluate = runtime.evaluate("document.documentElement.outerHTML")
            return evaluate?.result?.value?.toString() ?: ""
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun bringToFront() {
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
    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X {
        try {
            val startTime = System.currentTimeMillis()
            var result: X? = null
            runBlocking {
                withContext(Dispatchers.IO) {
                    result = withTimeoutOrNull(30 * 1000) {
                        val deviceMetricsOverride = evaluate("__utils__.getFullPageMetrics()")?.toString()
                        val screenshot = if (deviceMetricsOverride != null) {
                            val m = GsonBuilder().create().fromJson(deviceMetricsOverride, DeviceMetrics::class.java)
                            emulation.setDeviceMetricsOverride(m.width, m.height, m.deviceScaleFactor, m.mobile)
                            page.captureScreenshot().also { emulation.clearDeviceMetricsOverride() }
                        } else {
                            // val screenshot = page.captureScreenshot(CaptureScreenshotFormat.PNG, 50, viewport, true)
                            page.captureScreenshot()
                        }

                        log.debug("It takes {} to take screenshot | {}",
                            DateTimes.elapsedTime(startTime).readable(),
                            navigateUrl)
                        outputType.convertFromBase64Png(screenshot)
                    }
                    log.takeIf { result == null }?.warn("Timeout to take screenshot | {}", navigateUrl)
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
        browserInstanceManager.closeIfPresent(launchOptions.userDataDir)
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
            page.addScriptToEvaluateOnNewDocument(clientLibJs)
//        page.onDomContentEventFired { event: DomContentEventFired ->
//            // The page's main html content is ready, but css/js are not ready, document.readyState === 'interactive'
//            // runtime.evaluate("__utils__.checkPulsarStatus()")
//        }
//
//        page.onLoadEventFired { event: LoadEventFired ->
//            simulate()
//        }

            // block urls by url pattern
//            if ("imagesEnabled" in launchOptions.additionalArguments.keys) {
//            }

            page.enable()

            // NOTE: There are too many network relative traffic, especially when the proxy is disabled
            // TODO: Find out the reason why there are too many network relative traffic, especially when the proxy is disabled
            if (enableUrlBlocking) {
                setupUrlBlocking()
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

    private fun setupUrlBlocking() {
        if (!enableUrlBlocking) return

        // TODO: case sensitive or not?
        network.setBlockedURLs(blockingUrls)
        network.takeIf { enableBlockingReport }?.onRequestWillBeSent {
            val requestUrl = it.request.url
            if (mustPassUrlPatterns.any { requestUrl.matches(it) }) {
                return@onRequestWillBeSent
            }

            if (it.type in blockingResourceTypes) {
                if (blockingUrlPatterns.none { requestUrl.matches(it) }) {
                    log.info("Resource ({}) might be blocked | {}", it.type, it.request.url)
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
