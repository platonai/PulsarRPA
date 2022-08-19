package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProtocolException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.NoSuchSessionException
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.hotfix.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdBlockRules
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.types.dom.Rect
import com.github.kklisura.cdt.protocol.types.network.Cookie
import com.github.kklisura.cdt.protocol.types.page.CaptureScreenshotFormat
import com.github.kklisura.cdt.protocol.types.page.Viewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.random.Random

class ChromeDevtoolsDriver(
    private val browserSettings: WebDriverSettings,
    override val browserInstance: ChromeDevtoolsBrowserInstance,
) : AbstractWebDriver(browserInstance) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    override val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "gap" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    val openSequence = 1 + browserInstance.devToolsCount
    val chromeTabTimeout get() = browserSettings.fetchTaskTimeout.plusSeconds(20)
    val userAgent get() = BrowserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val isSPA get() = browserSettings.isSPA

    //    private val preloadJs by lazy { generatePreloadJs() }
    private val preloadJs get() = generatePreloadJs()
    private val toolsConfig = DevToolsConfig()

    private val chromeTab: ChromeTab
    private val devTools: RemoteDevTools
    private val _mouse: Mouse
    private val _keyboard: Keyboard

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private var navigateUrl = ""

    private val browser get() = devTools.browser
    private val page get() = devTools.page.takeIf { isActive }
    private val dom get() = devTools.dom.takeIf { isActive }
    private val input get() = devTools.input.takeIf { isActive }
    private val mainFrame get() = page?.frameTree?.frame
    private val network get() = devTools.network.takeIf { isActive }
    private val fetch get() = devTools.fetch.takeIf { isActive }
    private val runtime get() = devTools.runtime.takeIf { isActive }
    private val emulation get() = devTools.emulation.takeIf { isActive }
    private val mouse get() = _mouse.takeIf { isActive }
    private val keyboard get() = _keyboard.takeIf { isActive }

    private var mainRequestId = ""
    private var mainRequestHeaders: Map<String, Any> = mapOf()
    private var mainRequestCookies: List<Map<String, String>> = listOf()

    private val enableStartupScript get() = browserSettings.enableStartupScript
    private val enableBlockingReport = false

    private val closed = AtomicBoolean()

    val rpcFailures = AtomicInteger()
    var maxRPCFailures = 5
    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || !AppContext.isActive || !devTools.isOpen
    val isActive get() = !isGone

    val tabId get() = chromeTab.id

    init {
        try {
            // In chrome every tab is a separate process
            chromeTab = browserInstance.createTab()
            navigateUrl = chromeTab.url ?: ""

            devTools = browserInstance.createDevTools(chromeTab, toolsConfig)
            _mouse = Mouse(input!!)
            _keyboard = Keyboard(input!!)

            if (userAgent.isNotEmpty()) {
                emulation?.setUserAgentOverride(userAgent)
            }
        } catch (e: ChromeDriverException) {
            throw DriverLaunchException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            throw DriverLaunchException("Failed to create chrome devtools driver", e)
        }
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        val driver = this
        this.navigateEntry = entry
        browserInstance.navigateHistory.add(entry)

        try {
            withIOContext("navigateTo") {
                driver.takeIf { enableStartupScript }?.getInvaded(entry.url) ?: getNoInvaded(entry.url)
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "navigateTo ${entry.url}")
        }
    }

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return mainRequestHeaders
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        return mainRequestCookies
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        try {
            return withIOContext("getCookies") {
                getCookies0()
            } ?: listOf()
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "getCookies")
        }

        return listOf()
    }

    private fun getCookies0(): List<Map<String, String>> {
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
            navigateEntry.stopped = true

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
            handleRPCException(e, "stop")
        }
    }

    /**
     * Evaluate a javascript expression in the browser.
     * The expression should be a single line.
     * */
    override suspend fun evaluate(expression: String): Any? {
        if (!refreshState()) return null

        try {
            val evaluate = withIOContext("evaluate") {
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
            handleRPCException(e, "evaluate")
        }

        return null
    }

    override val sessionId: String?
        get() {
            if (!refreshState()) return null

            lastSessionId = try {
                if (!isActive) null else mainFrame?.id
            } catch (e: ChromeRPCException) {
                handleRPCException(e, "sessionId")
                null
            }
            return lastSessionId
        }

    override suspend fun currentUrl(): String {
        if (!refreshState()) return navigateUrl

        navigateUrl = try {
            return withIOContext("currentUrl") {
                mainFrame?.url ?: navigateUrl
            } ?: ""
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "currentUrl")
            ""
        }
        return navigateUrl
    }

    override suspend fun exists(selector: String): Boolean {
        if (!refreshState()) return false

        try {
            val nodeId = querySelector(selector)
            return nodeId != null && nodeId > 0
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "exists $selector")
        }

        return false
    }

    /**
     * Wait until [selector] for [timeout] at most
     * */
    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        try {
            if (!refreshState()) return -1L

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
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "waitForSelector $selector")
        }

        return -1L
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        try {
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
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "waitForNavigation $timeout")
            return -1
        }
    }

    private suspend fun isNavigated(oldUrl: String): Boolean {
        if (oldUrl != currentUrl()) {
            return true
        }

        // TODO: other signals

        return false
    }

    /**
     * This method fetches an element with `selector`, scrolls it into view if
     * needed, and then uses {@link Mouse} to click in the center of the
     * element. If there's no element matching `selector`, the method do not click anything.
     * @remarks Bear in mind that if `click()` triggers a navigation event and
     * there's a separate `driver.waitForNavigation()` promise to be resolved, you
     * may end up with a race condition that yields unexpected results. The
     * correct pattern for click and wait for navigation is the following:
     * ```kotlin
     * driver.waitForNavigation()
     * driver.click(selector)
     * ```
     * @param selector - A `selector` to search for element to click. If there are
     * multiple elements satisfying the `selector`, the first will be clicked
     * @param count - Click count
     */
    override suspend fun click(selector: String, count: Int) {
        try {
            val nodeId = scrollIntoViewIfNeeded(selector) ?: return

            val offset = OffsetD(4.0, 4.0)

            val p = page
            val d = dom
            if (p != null && d != null) {
                val point = ClickableDOM(p, d, nodeId, offset).clickablePoint() ?: return

                withIOContext("click") {
                    mouse?.click(point.x, point.y, count, delayPolicy("click"))
                }

                gap()
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "click")
        }
    }

    override suspend fun type(selector: String, text: String) {
        if (!refreshState()) return

        try {
            withIOContext("type") {
                val nodeId = focusOnSelector(selector)
                if (nodeId != 0) {
                    keyboard?.type(nodeId, text, delayPolicy("type"))
                }
            }

            gap()
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "type")
        }
    }

    override suspend fun scrollTo(selector: String) {
        try {
            scrollIntoViewIfNeeded(selector)
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "scrollTo")
        }
    }

    /**
     * This method scrolls element into view if needed, and then uses
     * {@link page.captureScreenshot} to take a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    override suspend fun captureScreenshot(selector: String): String? {
        val nodeId = scrollIntoViewIfNeeded(selector)
        if (nodeId == null || nodeId <= 0) {
            logger.info("Can not find node for selector <{}>", selector)
            return null
        }

        val vi = firstAttr(selector, "vi") ?: return captureScreenshotWithoutVi(selector)

        val quad = vi.split(" ").map { it.toDoubleOrNull() ?: 0.0 }
        if (quad.size != 4) {
            logger.warn("Invalid node vi information for selector <{}>", selector)
            return null
        }

        val rect = RectD(quad[0], quad[1], quad[2], quad[3])

        return captureScreenshot(rect)
    }

    private suspend fun captureScreenshotWithoutVi(selector: String): String? {
        val nodeClip = calculateNodeClip(selector)

        if (nodeClip == null) {
            logger.info("Can not calculate node clip | {}", selector)
            return null
        }

        val rect = nodeClip.rect
        if (rect == null) {
            logger.info("Can not take clip | {}", selector)
            return null
        }

        // val clip = normalizeClip(rect)

        return captureScreenshot(rect)
    }

    override suspend fun captureScreenshot(clip: RectD): String? {
        val viewport = Viewport().apply {
            x = clip.x; y = clip.y
            width = clip.width; height = clip.height
            scale = 1.0
        }
        return captureScreenshot(viewport)
    }

    suspend fun captureScreenshot(viewport: Viewport): String? {
        val format = CaptureScreenshotFormat.JPEG
        val quality = BrowserSettings.screenshotQuality

        try {
            return withIOContext("captureScreenshot") {
                page?.captureScreenshot(format, quality, viewport, true, false)
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e)
        }

        return null
    }

    /**
     * TODO: This method is not implemented correctly yet.
     */
    private suspend fun calculateNodeClip(selector: String): NodeClip? {
        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId <= 0) {
            logger.info("Can not find node | {}", selector)
            return null
        }

        evaluate("__pulsar_utils__.scrollToTop()")
        val clientRect = evaluate("__pulsar_utils__.queryClientRect('$selector')")?.toString()
        if (clientRect == null) {
            logger.info("Can not query client rect for selector <{}>", selector)
            return null
        }

        val quad = clientRect.split(" ").map { it.toDoubleOrNull() ?: 0.0 }
        if (quad.size != 4) {
            return null
        }

        val rect = RectD(quad[0], quad[1], quad[2], quad[3])

        val p = page ?: return null
//        val d = dom ?: return null

        val viewport = p.layoutMetrics.cssLayoutViewport
        val pageX = viewport.pageX
        val pageY = viewport.pageY

        return NodeClip(nodeId, pageX, pageY, rect)
    }

    private fun normalizeClip(clip: RectD): RectD {
        val x = clip.x.roundToInt()
        val y = clip.y.roundToInt()
        val width = (clip.width + clip.x - x).roundToInt()
        val height = (clip.height + clip.y - y).roundToInt()
        return RectD(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    }

    private fun refreshState(action: String = ""): Boolean {
        lastActiveTime = Instant.now()
        navigateEntry.refresh(action)
        return isActive
    }

    private suspend fun gap() {
        if (isActive) {
            delay(delayPolicy("gap"))
        }
    }

    /**
     * This method fetches an element with `selector` and focuses it. If there's no
     * element matching `selector`, the method throws an error.
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @returns  NodeId which resolves when the element matching selector is
     * successfully focused. returns 0 if there is no element
     * matching selector.
     */
    private fun focusOnSelector(selector: String): Int {
        if (!refreshState()) return 0

        val rootId = dom?.document?.nodeId ?: return 0
        val nodeId = dom?.querySelector(rootId, selector)
        if (nodeId == 0) {
            logger.warn("No node found for selector: $selector")
            return 0
        }

        try {
            dom?.focus(nodeId, rootId, null)
        } catch (e: Exception) {
            logger.warn("Failed to focus #$nodeId | {}", e.message)
        }

        return nodeId ?: 0
    }

    private suspend fun querySelector(selector: String): Int? {
        if (!refreshState()) return null

        try {
            return withIOContext("querySelector") {
                val rootId = dom?.document?.nodeId
                if (rootId != null && rootId != 0) {
                    dom?.querySelector(rootId, selector)
                } else null
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "querySelector")
        }

        return null
    }

    private suspend fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int? {
        if (!refreshState()) return null

        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        val node = withIOContext("describeNode") {
            dom?.describeNode(nodeId, null, null, null, false)
        }
        // see org.w3c.dom.Node.ELEMENT_NODE
        val ELEMENT_NODE = 1
        if (node?.nodeType != ELEMENT_NODE) {
            logger.info("Node is not an element: $selector")
            return null
        }

        try {
            withIOContext("scrollIntoViewIfNeeded") {
                dom?.scrollIntoViewIfNeeded(nodeId, node.backendNodeId, null, rect)
            }
        } catch (t: Throwable) {
            logger.info("Fallback to Element.scrollIntoView | {}", selector)
            // Fallback to Element.scrollIntoView if DOM.scrollIntoViewIfNeeded is not supported
            evaluate("__pulsar_utils__.scrollIntoView('$selector')")
        }

        return nodeId
    }

    override suspend fun pageSource(): String? {
        if (!refreshState()) return null

        try {
            return withIOContext("pageSource") {
                dom?.getOuterHTML(dom?.document?.nodeId, null, null)
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "pageSource")
        }

        return null
    }

    override suspend fun bringToFront() {
        try {
            withIOContext("bringToFront") {
                page?.bringToFront()
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e)
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
            } catch (e: ChromeDriverException) {
                // ignored
            }
        }
    }

    private fun getInvaded(url: String) {
        page?.enable()
        dom?.enable()
        runtime?.enable()
        network?.enable()

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
    }

    private fun getNoInvaded(url: String) {
        page?.enable()
        navigateUrl = url
        page?.navigate(url)
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
        val now = Instant.now()
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

    private suspend fun isMainFrame(frameId: String): Boolean {
        return withIOContext("isMainFrame") {
            mainFrame?.id == frameId
        } ?: false
    }

    private fun handleRPCException(e: ChromeRPCException, message: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            throw NoSuchSessionException("Too many RPC failures")
        }
        logger.warn("Chrome RPC exception ({}/{}) | {}", rpcFailures, maxRPCFailures, message ?: e.message)
    }

    private suspend fun <T> withIOContext(action: String, block: suspend CoroutineScope.() -> T): T? {
        return withContext(Dispatchers.IO) {
            if (!refreshState(action)) {
                return@withContext null
            }

            try {
                block().also { decreaseRPCFailures() }
            } catch (e: ChromeRPCException) {
                increaseRPCFailures()
                throw e
            }
        }
    }

    private fun decreaseRPCFailures() {
        rpcFailures.decrementAndGet()
        if (rpcFailures.get() < 0) {
            rpcFailures.set(0)
        }
    }

    private fun increaseRPCFailures() {
        rpcFailures.incrementAndGet()
    }
}
