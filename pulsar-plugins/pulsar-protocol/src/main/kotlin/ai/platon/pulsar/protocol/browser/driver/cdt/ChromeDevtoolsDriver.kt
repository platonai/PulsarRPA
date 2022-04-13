package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProtocolException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.WebDriverException
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.hotfix.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdInitializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.types.network.Cookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ChromeDevtoolsDriver(
    private val browserSettings: WebDriverSettings,
    override val browserInstance: ChromeDevtoolsBrowserInstance,
) : AbstractWebDriver(browserInstance) {

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
    val chromeTabTimeout get() = browserSettings.fetchTaskTimeout.plusSeconds(10)
    val userAgent get() = BrowserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val isSPA get() = browserSettings.isSPA

//    private val preloadJs by lazy { generatePreloadJs() }
    private val preloadJs get() = generatePreloadJs()
    private val toolsConfig = DevToolsConfig()
    private val chromeTab: ChromeTab
    private val devTools: RemoteDevTools
    private val mouse: Mouse
    private val keyboard: Keyboard

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private val browser get() = devTools.browser
    private var navigateEntry: NavigateEntry? = null
    private var navigateUrl = ""
    private val page get() = devTools.page.takeIf { isActive }
    private val dom get() = devTools.dom.takeIf { isActive }
    private val input get() = devTools.input.takeIf { isActive }
    private val mainFrame get() = page?.frameTree?.frame
    private val network get() = devTools.network.takeIf { isActive }
    private val fetch get() = devTools.fetch.takeIf { isActive }
    private val runtime get() = devTools.runtime.takeIf { isActive }
    private val emulation get() = devTools.emulation.takeIf { isActive }

    private var mainRequestId = ""
    private var mainRequestHeaders: Map<String, Any> = mapOf()
    private var mainRequestCookies: List<Map<String, String>> = listOf()

    private val enableBlockingReport = false
    private val closed = AtomicBoolean()

    val sessionLosts = AtomicInteger()
    override var lastActiveTime = Instant.now()
    // TODO: collect application state from IO operations
    val isGone get() = closed.get() || !AppContext.isActive || !devTools.isOpen || sessionLosts.get() > 0
    val isActive get() = !isGone

    init {
        try {
            // In chrome every tab is a separate process
            chromeTab = browserInstance.createTab()
            navigateUrl = chromeTab.url ?: ""

            devTools = browserInstance.createDevTools(chromeTab, toolsConfig)
            mouse = Mouse(input!!)
            keyboard = Keyboard(input!!)

            if (userAgent.isNotEmpty()) {
                emulation?.setUserAgentOverride(userAgent)
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

        val driver = this
        withContext(Dispatchers.IO) {
            driver.takeIf { browserSettings.jsInvadingEnabled }?.getInvaded(url) ?: getNoInvaded(url)
        }
    }

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return mainRequestHeaders
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        return mainRequestCookies
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        if (!refreshState()) return listOf()

        return withContext(Dispatchers.IO) {
            getCookies0()
        }
    }

    private fun getCookies0(): List<Map<String, String>> {
        if (!refreshState()) return listOf()

        network?.enable()
        return network?.cookies?.map { serialize(it) }?: listOf()
    }

    private fun serialize(cookie: Cookie): Map<String, String> {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(cookie)
        val map: Map<String, String?> = mapper.readValue(json)
        return map.filterValues { it != null }.mapValues { it.toString() }
    }

    override suspend fun stop() {
        if (!isActive) {
            return
        }

        refreshState()
        try {
            navigateEntry?.stopped = true

            if (browserInstance.isGUI) {
                // in gui mode, just stop the loading, so we can make a diagnosis
                page?.stopLoading()
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

    /**
     * Evaluate a javascript expression in the browser.
     * The expression should be a single line.
     * */
    override suspend fun evaluate(expression: String): Any? {
        if (!refreshState()) return null

        try {
            val evaluate = withContext(Dispatchers.IO) {
                runtime?.evaluate(browserSettings.nameMangling(expression))
            }

            val exception = evaluate?.exceptionDetails?.exception
            if (exception != null) {
//                logger.warn(exception.value?.toString())
//                logger.warn(exception.unserializableValue)
                logger.info(exception.description + "\n>>>$expression<<<")
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
            if (!refreshState()) return null

            lastSessionId = try {
                if (!isActive) null else mainFrame?.id
            } catch (e: ChromeRPCException) {
                sessionLosts.incrementAndGet()
                logger.warn("Failed to retrieve session id, session might be closed, {}", e.message)
                null
            }
            return lastSessionId
        }

    override suspend fun currentUrl(): String {
        if (!refreshState()) return navigateUrl

        navigateUrl = try {
            return withContext(Dispatchers.IO) {
                mainFrame?.url ?: navigateUrl
            }
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to retrieve current url, session might be closed, {}", e.message)
            ""
        }
        return navigateUrl
    }

    override suspend fun exists(selector: String): Boolean {
        if (!refreshState()) return false

        val nodeId = querySelector(selector)
        return nodeId != null && nodeId > 0
    }

    /**
     * Wait until [selector] for [timeout] at most
     * */
    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        if (!refreshState()) return -1

        val timeoutMillis = timeout.toMillis()
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        var nodeId = querySelector(selector)
        while (elapsedTime < timeoutMillis && (nodeId == null || nodeId <= 0)) {
            gap()
            elapsedTime = System.currentTimeMillis() - startTime
            nodeId = querySelector(selector)
        }

        return timeoutMillis - elapsedTime
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        if (!refreshState()) return -1

        val oldUrl = currentUrl()
        var navigated = isNavigated(oldUrl)
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        val timeoutMillis = timeout.toMillis()
        while (elapsedTime < timeoutMillis && !navigated) {
            gap()
            elapsedTime = System.currentTimeMillis() - startTime
            navigated = isNavigated(oldUrl)
        }

        return timeoutMillis - elapsedTime
    }

    private suspend fun isNavigated(oldUrl: String): Boolean {
        if (oldUrl != currentUrl()) {
            return true
        }

        // TODO: other signals

        return false
    }

    override suspend fun click(selector: String, count: Int) {
        if (!refreshState()) return

        val nodeId = scrollIntoViewIfNeeded(selector) ?: return
        val offset = OffsetD(4.0, 4.0)

        val p = page
        val d = dom
        if (p != null && d != null) {
            val point = ClickableDOM(p, d, nodeId, offset).clickablePoint() ?: return
            mouse.click(point.x, point.y, count, delayPolicy("click"))
            gap()
        }
    }

    override suspend fun type(selector: String, text: String) {
        if (!refreshState()) return

        val nodeId = focus(selector)
        if (nodeId == 0) return
        keyboard.type(nodeId, text, delayPolicy("type"))
        gap()
    }

    override suspend fun scrollTo(selector: String) {
        if (!refreshState()) return

        val nodeId = focus(selector)
        if (nodeId == 0) return
        dom?.scrollIntoViewIfNeeded(nodeId, null, null, null)
    }

    private fun refreshState(): Boolean {
        navigateEntry?.refresh()
        return isActive
    }

    private suspend fun gap() = delay(delayPolicy("gap"))

    private fun focus(selector: String): Int {
        if (!refreshState()) return 0

        val rootId = dom?.document?.nodeId ?: return 0
        val nodeId = dom?.querySelector(rootId, selector)
        if (nodeId == 0) {
            logger.warn("No node found for selector: $selector")
            return 0
        }

        try {
            dom?.focus(nodeId, null, null)
        } catch (e: Exception) {
            logger.warn("Failed to focus #$nodeId | {}", e.message)
        }

        return nodeId ?: 0
    }

    private fun querySelector(selector: String): Int? {
        if (!refreshState()) return null

        val rootId = dom?.document?.nodeId ?: return null
        return kotlin.runCatching { dom?.querySelector(rootId, selector) }.onFailure {
            logger.warn("Failed to query selector {} | {}", selector, it.message)
        }.getOrNull()
    }

    private fun scrollIntoViewIfNeeded(selector: String): Int? {
        if (!refreshState()) return 0

        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        val node = dom?.describeNode(nodeId, null, null, null, false)
        // see org.w3c.dom.Node.ELEMENT_NODE
        val ELEMENT_NODE = 1
        if (node?.nodeType != ELEMENT_NODE) {
            logger.info("Node is not an element: $selector")
            return null
        }

        dom?.scrollIntoViewIfNeeded(nodeId, null, null, null)
        return nodeId
    }

    override suspend fun pageSource(): String? {
        if (!refreshState()) return null

        try {
            return withContext(Dispatchers.IO) {
                dom?.getOuterHTML(dom?.document?.nodeId, null, null)
            }
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to get page source | {}", e.message)
        }

        return null
    }

    override suspend fun bringToFront() {
        withContext(Dispatchers.IO) {
            page?.bringToFront()
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
                browserInstance.closeTab(chromeTab)
                devTools.close()
            } catch (e: ChromeProtocolException) {
                // ignored
            }
        }
    }

    private fun getInvaded(url: String) {
        if (!refreshState()) return

        page?.enable()
        dom?.enable()
        runtime?.enable()
        network?.enable()

        try {
            page?.addScriptToEvaluateOnNewDocument(preloadJs)

            if (enableUrlBlocking) {
                network?.enable()
                setupUrlBlocking(url)
            }

            network?.onRequestWillBeSent {
                if (mainRequestId.isBlank()) {
                    mainRequestId = it.requestId
                    mainRequestHeaders = it.request.headers
                }
            }

            network?.onResponseReceived {

            }

            page?.onDocumentOpened {
                mainRequestCookies = getCookies0()
            }

            navigateUrl = url
            page?.navigate(url)
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
                page?.let { JdInitializer().init(it) }
            } else {
            }
        }
    }

    @Throws(WebDriverException::class)
    private fun getNoInvaded(url: String) {
        if (!refreshState()) return

        try {
            page?.enable()
            navigateUrl = url
            page?.navigate(url)
        } catch (e: ChromeRPCException) {
            sessionLosts.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    private suspend fun handleRedirect() {
        val finalUrl = currentUrl() ?: return
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
        if (isSPA) {
            return
        }

        tabs.forEach { oldTab ->
            oldTab.url?.let { closeTabsIfTimeout(it, oldTab) }
        }
    }

    private fun closeTabsIfTimeout(tabUrl: String, oldTab: ChromeTab) {
        val now = Instant.now()
        val entries = browserInstance.navigateHistory.asSequence()
            .filter { it.url == tabUrl }
            .filter { it.stopped }
            .filter { it.activeTime + chromeTabTimeout < now }
            .toList()

        if (entries.isNotEmpty()) {
            browserInstance.navigateHistory.removeAll(entries)
            browserInstance.closeTab(oldTab)
        }
    }

    private fun closeIrrelevantTabs(tabs: Array<ChromeTab>) {
        val irrelevantTabs = tabs
            .filter { it.url?.matches("about:".toRegex()) == true }
            .filter { oldTab -> browserInstance.navigateHistory.none { it.url == oldTab.url } }
        if (irrelevantTabs.isNotEmpty()) {
            // TODO: might close a tab open just now
            // irrelevantTabs.forEach { browserInstance.closeTab(it) }
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
        network?.setBlockedURLs(blockRules.blockingUrls)

        network?.takeIf { enableBlockingReport }?.onRequestWillBeSent {
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

    private fun generatePreloadJs(): String {
        val js = browserSettings.generatePreloadJs(false)
        return browserSettings.nameMangling(js)
    }

    @Throws(WebDriverException::class)
    private fun isMainFrame(frameId: String): Boolean {
        if (!isActive) return false

        return mainFrame?.id == frameId
    }

    class ShutdownHookRegistry : ChromeLauncher.ShutdownHookRegistry {

        override fun register(thread: Thread) {
        }

        override fun remove(thread: Thread) {
        }
    }
}
