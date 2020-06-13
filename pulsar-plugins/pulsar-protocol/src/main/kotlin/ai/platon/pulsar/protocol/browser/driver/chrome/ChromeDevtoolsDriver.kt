package ai.platon.pulsar.protocol.browser.driver.chrome

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsInvocationException
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.conf.blockingResourceTypes
import ai.platon.pulsar.protocol.browser.conf.blockingUrlPatterns
import ai.platon.pulsar.protocol.browser.conf.blockingUrls
import ai.platon.pulsar.protocol.browser.conf.mustPassUrlPatterns
import ai.platon.pulsar.protocol.browser.driver.BrowserInstance
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import com.github.kklisura.cdt.protocol.types.page.CaptureScreenshotFormat
import com.github.kklisura.cdt.protocol.types.page.Viewport
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * TODO: more compatible methods with RemoteWebDriver, or disable them explicitly
 * */
class ChromeDevtoolsDriver(
        private val launchOptions: ChromeDevtoolsOptions,
        private val browserControl: WebDriverControl,
        private val browserInstanceManager: BrowserInstanceManager
): RemoteWebDriver() {
    private val log = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    val dataDir get() = launchOptions.userDataDir
    val proxyServer get() = launchOptions.proxyServer

    val userAgent get() = browserControl.randomUserAgent()
    val pageLoadTimeout get() = browserControl.pageLoadTimeout
    val scriptTimeout get() = browserControl.scriptTimeout
    val scrollDownCount get() = browserControl.scrollDownCount
    val scrollInterval get() = browserControl.scrollInterval
    // TODO: load blocking rules from config files
    val enableUrlBlocking get() = browserControl.enableUrlBlocking
    val clientLibJs = browserControl.parseLibJs(false)
    var devToolsConfig = DevToolsConfig()

    val browserInstance: BrowserInstance
    val tab: ChromeTab
    val devTools: RemoteDevTools

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
    private val isGone get() = closed.get() || !devTools.isOpen || numSessionLost.get() > 1

    val viewport = Viewport().apply {
        x = 0.0
        y = 0.0
        width = BrowserControl.viewPort.getWidth()
        height = BrowserControl.viewPort.getHeight()
        scale = 1.0
    }

    init {
        try {
            browserInstance = browserInstanceManager.launchIfAbsent(launchOptions)

            // In chrome every tab is a separate process
            tab = browserInstance.createTab()
            navigateUrl = tab.url ?: ""

            devTools = browserInstance.createDevTools(tab, devToolsConfig)
            browserInstance.devToolsList.add(devTools)

            if (userAgent.isNotEmpty()) {
                emulation.setUserAgentOverride(userAgent)
            }
        } catch (t: Throwable) {
            throw DriverLaunchException("Failed to create chrome devtools driver", t)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun get(url: String) {
        takeIf { browserControl.jsInvadingEnabled }?.getInvaded(url)?:getNoInvaded(url)
    }

    @Throws(NoSuchSessionException::class)
    fun stopLoading() {
        if (isGone) return

        try {
            page.stopLoading()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            log.warn("Failed to call stop loading, session is already closed, {}", Strings.simplifyException(e))
        }
    }

    @Throws(NoSuchSessionException::class)
    fun evaluate(expression: String): Any? {
        if (isGone) return null

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
            lastSessionId = if (isGone) null else SessionId(mainFrame.id)
            return lastSessionId
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getCurrentUrl(): String {
        try {
            navigateUrl = if (isGone) navigateUrl else mainFrame.url
            return navigateUrl
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getWindowHandles(): Set<String> {
        if (isGone) return setOf()
        return browserInstance.chrome.getTabs().mapTo(HashSet()) { it.id }
    }

    @Throws(NoSuchSessionException::class)
    override fun getPageSource(): String {
        try {
            val evaluate = runtime.evaluate("document.documentElement.outerHTML")
            return evaluate?.result?.value?.toString()?:""
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun getCookieNames(): List<String> {
        if (isGone) return listOf()

        try {
            return devTools.network.allCookies.map { it.name }
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun deleteAllCookies() {
        if (isGone) return

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
        if (isGone) return

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

    @Throws(NoSuchSessionException::class)
    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X {
        try {
            val result = page.captureScreenshot(CaptureScreenshotFormat.PNG, 100, viewport, true)
            return outputType.convertFromBase64Png(result)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    override fun toString() = "Chrome Devtools Driver ($lastSessionId)"

    /**
     * Quit the browser instance
     * */
    override fun quit() {
        close()
        browserInstanceManager.closeIfAbsent(launchOptions.userDataDir)
    }

    /**
     * Close the tab hold by this driver
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            devTools.close()
            browserInstance.closeTab()
        }
    }

    @Throws(NoSuchSessionException::class)
    private fun getInvaded(url: String) {
        if (isGone) return

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
        if (isGone) return

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
        if (isGone) return false

        return mainFrame.id == frameId
    }

    class ShutdownHookRegistry : ChromeLauncher.ShutdownHookRegistry {

        override fun register(thread: Thread) {
        }

        override fun remove(thread: Thread) {
        }
    }
}
