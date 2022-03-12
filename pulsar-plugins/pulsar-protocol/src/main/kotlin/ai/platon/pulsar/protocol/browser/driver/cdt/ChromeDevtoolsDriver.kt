package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.BlockRules
import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsInvocationException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.WebDriverException
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.hotfix.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdInitializer
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ChromeDevtoolsDriver(
    private val browserSettings: WebDriverSettings,
    private val browserInstance: ChromeDevtoolsBrowserInstance,
) : AbstractWebDriver(browserInstance.id) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.CHROME

    override val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "gap" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    val waitForTimeout = Duration.ofMinutes(1).toMillis()

    val openSequence = 1 + browserInstance.devToolsCount
    val userAgent get() = browserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    var devToolsConfig = DevToolsConfig()
    val tab: ChromeTab
    val devTools: RemoteDevTools
    private var mouse: Mouse
    private var keyboard: Keyboard

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private val browser get() = devTools.browser
    private var navigateUrl = ""
    private val page get() = devTools.page
    private val dom get() = devTools.dom
    private val input get() = devTools.input
    private val mainFrame get() = page.frameTree.frame
    private val network get() = devTools.network
    private val fetch get() = devTools.fetch
    private val runtime get() = devTools.runtime
    private val emulation get() = devTools.emulation

    private val enableBlockingReport = false
    private val closed = AtomicBoolean()

    val sessionLosts = AtomicInteger()
    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || !devTools.isOpen || sessionLosts.get() > 0
    val isActive get() = !isGone

    init {
        try {
            // In chrome every tab is a separate process
            tab = browserInstance.createTab()
            navigateUrl = tab.url ?: ""

            devTools = browserInstance.createDevTools(tab, devToolsConfig)
            mouse = Mouse(input)
            keyboard = Keyboard(input)

            if (userAgent.isNotEmpty()) {
                emulation.setUserAgentOverride(userAgent)
            }
        } catch (e: ChromeProcessTimeoutException) {
            throw DriverLaunchException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            throw DriverLaunchException("Failed to create chrome devtools driver", e)
        }
    }

    override suspend fun setTimeouts(driverConfig: BrowserSettings) {
    }

    override suspend fun navigateTo(url: String) {
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

    override suspend fun stopLoading() {
        if (!isActive) return

        try {
            page.stopLoading()
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to call stop loading, session is already closed, {}", e.message)
        }
    }

    override suspend fun evaluate(expression: String): Any? {
        if (!isActive) return null

        try {
            val evaluate = runtime.evaluate(expression)
            val result = evaluate?.result
            // TODO: handle errors here
            return result?.value
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to evaluate, session might be closed, {}", e.message)
        }

        return null
    }

    override val sessionId: String?
        get() {
            lastSessionId = try {
                if (!isActive) null else mainFrame.id
            } catch (e: ChromeDevToolsInvocationException) {
                sessionLosts.incrementAndGet()
                logger.warn("Failed to retrieve session id, session might be closed, {}", e.message)
                null
            }
            return lastSessionId
        }

    override suspend fun currentUrl(): String {
        navigateUrl = try {
            if (isActive) navigateUrl else mainFrame.url
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to retrieve current url, session might be closed, {}", e.message)
            ""
        }
        return navigateUrl
    }

    override suspend fun exists(selector: String): Boolean {
        val nodeId = querySelector(selector)
        return nodeId != null && nodeId > 0
    }

    override suspend fun waitFor(selector: String): Long {
        val nodeId = querySelector(selector)
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        while (elapsedTime < waitForTimeout && (nodeId == null || nodeId <= 0)) {
            gap()
            elapsedTime = System.currentTimeMillis() - startTime
        }

        return waitForTimeout - elapsedTime
    }

    override suspend fun click(selector: String, count: Int) {
        val nodeId = scrollIntoViewIfNeeded(selector) ?: return
        val offset = OffsetD(4.0, 4.0)
        val point = ClickableDOM(page, dom, nodeId, offset).clickablePoint() ?: return

        mouse.click(point.x, point.y, count, delayPolicy("click"))
        gap()
    }

    override suspend fun type(selector: String, text: String) {
        val nodeId = focus(selector) ?: return
        keyboard.type(nodeId, text, delayPolicy("type"))
        gap()
    }

    private suspend fun gap() = delay(delayPolicy("gap"))

    private fun focus(selector: String): Int? {
        val rootId = dom.document.nodeId
        val nodeId = dom.querySelector(rootId, selector)
        if (nodeId == null) {
            logger.warn("No node found for selector: $selector")
            return null
        }

        try {
            dom.focus(nodeId, null, null)
        } catch (e: Exception) {
            logger.warn("Failed to focus | {}", e.message)
        }

        return nodeId
    }

    private fun querySelector(selector: String): Int? {
        val rootId = dom.document.nodeId
        return kotlin.runCatching { dom.querySelector(rootId, selector) }.onFailure {
            logger.warn("Failed to query selector {} | {}", selector, it.message)
        }.getOrNull()
    }

    private fun scrollIntoViewIfNeeded(selector: String): Int? {
        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        val node = dom.describeNode(nodeId, null, null, null, false)
        val ELEMENT_NODE = 1
        if (node.nodeType != ELEMENT_NODE) {
            logger.info("Node is not an element: $selector")
            return null
        }

        dom.scrollIntoViewIfNeeded(nodeId, null, null, null)
        return nodeId
    }

    override suspend fun pageSource(): String? {
        if (!isActive) return null

        try {
            return dom.getOuterHTML(dom.document.nodeId, null, null)
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to get page source | {}", e.message)
        }

        return null
    }

    override suspend fun bringToFront() {
        if (isActive) {
            page.bringToFront()
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

    private fun getInvaded(url: String) {
        if (!isActive) return

        page.enable()
        dom.enable()
        runtime.enable()

        try {
            val preloadJs = browserSettings.generatePreloadJs(false)
            page.addScriptToEvaluateOnNewDocument(preloadJs)

            if (enableUrlBlocking) {
                network.enable()
                setupUrlBlocking(url)
            }
//            fetch.enable()

            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    @Throws(WebDriverException::class)
    private fun getNoInvaded(url: String) {
        if (!isActive) return

        try {
            page.enable()
            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
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

    @Throws(WebDriverException::class)
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
