package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProtocolException
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.NavigateEntry
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

    val openSequence = 1 + browserInstance.devToolsCount
    val tabTimeout = Duration.ofMinutes(3)
    val userAgent get() = browserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking

    private val config = DevToolsConfig()
    private val tab: ChromeTab
    private val devTools: RemoteDevTools
    private val mouse: Mouse
    private val keyboard: Keyboard

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private val browser get() = devTools.browser
    private var navigateEntry: NavigateEntry? = null
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
    // TODO: collect application status from IO operations
    val isGone get() = closed.get() || !devTools.isOpen || sessionLosts.get() > 0
    val isActive get() = !isGone

    init {
        try {
            // In chrome every tab is a separate process
            tab = browserInstance.createTab()
            navigateUrl = tab.url ?: ""

            devTools = browserInstance.createDevTools(tab, config)
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
        val entry = NavigateEntry(url)
        navigateEntry = entry
        browserInstance.navigateHistory.add(entry)
        lastActiveTime = Instant.now()
        takeIf { browserSettings.jsInvadingEnabled }?.getInvaded(url) ?: getNoInvaded(url)
    }

    override suspend fun cookies(): String {
        network.enable()
        val mapper = pulsarObjectMapper()
        return network.cookies.joinToString("\n") { mapper.writeValueAsString(it) }
    }

    override suspend fun stop() {
        if (!isActive) return

        try {
            navigateEntry?.stopped = true

            if (browserInstance.isGUI) {
                // in gui mode, just stop the loading, so we can make a diagnosis
                page.stopLoading()
            } else {
                // go to about:blank, so the browser stops the previous page and release all resources
                navigateTo(Chrome.ABOUT_BLANK_PAGE)
            }

            handleRedirect()
            // dumpCookies()
            // TODO: it might be better to do this using a scheduled task
            cleanTabs()
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to call stop loading, session is already closed, {}", e.message)
        }
    }

    override suspend fun evaluate(expression: String): Any? {
        if (!isActive) return null

        try {
            val evaluate = runtime.evaluate(expression)

            val exception = evaluate?.exceptionDetails?.exception
            if (exception != null) {
//                logger.warn(exception.value?.toString())
//                logger.warn(exception.unserializableValue)
                logger.warn(exception.description)
            }

            val result = evaluate?.result
            return result?.value
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to evaluate, session might be closed, {}", e.message)
        }

        return null
    }

    override val sessionId: String?
        get() {
            lastSessionId = try {
                if (!isActive) null else mainFrame.id
            } catch (e: ChromeRPCException) {
                sessionLosts.incrementAndGet()
                logger.warn("Failed to retrieve session id, session might be closed, {}", e.message)
                null
            }
            return lastSessionId
        }

    override suspend fun currentUrl(): String {
        navigateUrl = try {
            if (isActive) navigateUrl else mainFrame.url
        } catch (e: ChromeRPCException) {
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

    override suspend fun waitFor(selector: String, timeoutMillis: Long): Long {
        val nodeId = querySelector(selector)
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        while (elapsedTime < timeoutMillis && (nodeId == null || nodeId <= 0)) {
            gap()
            elapsedTime = System.currentTimeMillis() - startTime
        }

        return timeoutMillis - elapsedTime
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
        // see org.w3c.dom.Node.ELEMENT_NODE
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
        } catch (e: ChromeRPCException) {
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

    override fun toString() = "DevTools driver ($lastSessionId)"

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
            try {
                browserInstance.closeTab(tab)
                devTools.close()
            } catch (e: ChromeProtocolException) {
                // ignored
            }
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
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    /**
     * TODO: use an event handler to do this stuff
     * */
    private fun initSpecialSiteBeforeVisit(url: String) {
        if (isFirstLaunch) {
            // the first visit to jd.com
            val isFirstJdVisit = url.contains("jd.com")
                    && browserInstance.navigateHistory.none { it.url.contains("jd.com") }
            if (isFirstJdVisit) {
                JdInitializer().init(page)
            }
        }
    }

    @Throws(WebDriverException::class)
    private fun getNoInvaded(url: String) {
        if (!isActive) return

        try {
            page.enable()
            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    private suspend fun dumpCookies() {
        val cookies = cookies()
        println(cookies)
    }

    private suspend fun handleRedirect() {
        val finalUrl = currentUrl()
        // redirect
        if (finalUrl.isNotBlank() && finalUrl != navigateUrl) {
            browserInstance.navigateHistory.add(NavigateEntry(finalUrl))
        }
    }

    // close irrelevant tabs, which might be opened for humanization purpose
    private fun cleanTabs() {
        val tabs = browserInstance.listTab()
        closeTimeoutTabs(tabs)
        closeIrrelevantTabs(tabs)
    }

    // close timeout tabs
    private fun closeTimeoutTabs(tabs: Array<ChromeTab>) {
        tabs.forEach { tab ->
            val tabUrl = tab.url

            if (tabUrl != null && tabUrl.startsWith("http")) {
                val now = Instant.now()
                val entries = browserInstance.navigateHistory.asSequence()
                    .filter { it.url == tabUrl }
                    .filter { it.stopped }
                    .filter { it.createTime + tabTimeout < now }
                    .toList()
                if (entries.isNotEmpty()) {
                    browserInstance.navigateHistory.removeAll(entries)
                    browserInstance.closeTab(tab)
                }
            }
        }
    }

    private fun closeIrrelevantTabs(tabs: Array<ChromeTab>) {
        tabs.forEach { tab ->
            val tabUrl = tab.url

            if (tabUrl != null && tabUrl.startsWith("http")) {
                // close tabs open for humanization purpose
            }
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
