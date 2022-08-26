package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.alwaysFalse
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.NoSuchSessionException
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

class ChromeDevtoolsDriverBackup(
    private val chromeTab: ChromeTab,
    private val devTools: RemoteDevTools,
    private val browserSettings: BrowserSettings,
    override val browserInstance: ChromeDevtoolsBrowserInstance,
) : AbstractWebDriver(browserInstance) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriverBackup::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    override val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "gap" -> 500L + Random.nextInt(500)
            "dragAndDrop" -> 800L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    val openSequence = 1 + browserInstance.devToolsCount
    val chromeTabTimeout get() = Duration.ofMinutes(2)
    //    val chromeTabTimeout get() = browserSettings.fetchTaskTimeout.plusSeconds(20)
    val userAgent get() = BrowserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val isSPA get() = browserSettings.isSPA

    //    private val preloadJs by lazy { generatePreloadJs() }
    private val preloadJs get() = generatePreloadJs()
    private val toolsConfig = DevToolsConfig()

    private val _mouse = Mouse(devTools)
    private val _keyboard = Keyboard(devTools)

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private var navigateUrl = ""

    private val page get() = devTools.page.takeIf { isActive }
    private val dom get() = devTools.dom.takeIf { isActive }
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
            navigateUrl = chromeTab.url ?: ""

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
        this.navigateEntry = entry
        browserInstance.navigateHistory.add(entry)

        try {
            withIOContext("navigateTo") {
                if (enableStartupScript) getInvaded(entry.url) else getNoInvaded(entry.url)
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
        return try {
            withIOContext("getCookies") { getCookies0() } ?: listOf()
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "getCookies")
            listOf()
        }
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

    /** Force the page stop all navigations and releases all resources. */
    override suspend fun stop() {
        terminate()
    }

    /** Force the page stop all navigations and pending resource fetches. */
    override suspend fun stopLoading() {
        if (!refreshState()) {
            return
        }

        try {
            withIOContext("stopLoading") {
                page?.stopLoading()
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "stopLoading")
        }
    }

    /** Force the page stop all navigations and releases all resources. */
    override suspend fun terminate() {
        if (!refreshState()) {
            return
        }

        try {
            navigateEntry.stopped = true

            if (browserInstance.isGUI) {
                // in gui mode, just stop the loading, so we can make a diagnosis
                withIOContext("terminate") {
                    page?.stopLoading()
                }
            } else {
                // go to about:blank, so the browser stops the previous page and release all resources
                navigateTo(Chrome.ABOUT_BLANK_PAGE)
            }

            handleRedirect()
            // dumpCookies()
            // TODO: it might be better to do this using a scheduled task
            cleanTabs()
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "terminate")
        } catch (e: ChromeDriverException) {
            logger.info("Terminate exception: {}", e.message)
        }
    }

    /**
     * Evaluate a javascript expression in the browser.
     * The expression should be a single line.
     * */
    override suspend fun evaluate(expression: String): Any? {
        if (!refreshState()) return null

        try {
            return evaluate0(expression)
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "evaluate")
        }

        return null
    }

    private fun evaluate0(expression: String): Any? {
        val evaluate = runtime?.evaluate(browserSettings.nameMangling(expression))

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
//                logger.warn(exception.value?.toString())
//                logger.warn(exception.unserializableValue)
            logger.info(exception.description + "\n>>>$expression<<<")
        }

        val result = evaluate?.result
        return result?.value
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
        if (!refreshState()) return -1L

        val timeoutMillis = timeout.toMillis()
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        try {
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

    override suspend fun moveMouseTo(x: Double, y: Double) {
        withIOContext("moveMouseTo") {
            mouse?.move(x, y)
        }
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
                val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value ?: return

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

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val nodeId = scrollIntoViewIfNeeded(selector) ?: return
            val offset = OffsetD(4.0, 4.0)
            val p = page
            val d = dom
            if (p != null && d != null) {
                val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value ?: return
                val point2 = PointD(point.x + deltaX, point.y + deltaY)
                withIOContext("dragAndDrop") {
                    mouse?.dragAndDrop(point, point2, delayPolicy("dragAndDrop"))
                }
                gap()
            }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "dragAndDrop")
        }
    }

    /**
     * This method scrolls element into view if needed, and then uses
     * {@link page.captureScreenshot} to take a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    override suspend fun captureScreenshot(selector: String): String? {
        return withIOContext("captureScreenshot") {
            captureScreenshot0(selector)
        }
    }

    override suspend fun captureScreenshot(clip: RectD) = withIOContext("captureScreenshot") {
        captureScreenshot0(0, clip)
    }

    suspend fun captureScreenshot(viewport: Viewport) = withIOContext("captureScreenshot") {
        captureScreenshot0(0, viewport)
    }

    private suspend fun captureScreenshot0(selector: String): String? {
        val nodeId = querySelector0(selector)
        if (nodeId == null || nodeId <= 0) {
            logger.info("No such element <{}>", selector)
            return null
        }
        scrollIntoViewIfNeeded0(nodeId, selector, null)

        val enableVi = alwaysFalse()
        val vi = if (enableVi) firstAttr(selector, "vi") else null

        return if (vi != null) {
            captureScreenshotWithVi(nodeId, selector, vi)
        } else {
            captureScreenshotWithoutVi(nodeId, selector)
        }
    }

    private fun captureScreenshotWithVi(nodeId: Int, selector: String, vi: String): String? {
        val quad = vi.split(" ").map { it.toDoubleOrNull() ?: 0.0 }
        if (quad.size != 4) {
            logger.warn("Invalid node vi information for selector <{}>", selector)
            return null
        }

        val rect = RectD(quad[0], quad[1], quad[2], quad[3])

        return captureScreenshot0(nodeId, rect)
    }

    private fun captureScreenshotWithoutVi(nodeId: Int, selector: String): String? {
        val nodeClip = calculateNodeClip(nodeId, selector)

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

        return captureScreenshot0(nodeId, rect)
    }

    private fun captureScreenshot0(nodeId: Int, clip: RectD): String? {
        val viewport = Viewport().apply {
            x = clip.x; y = clip.y
            width = clip.width; height = clip.height
            scale = 1.0
        }
        return captureScreenshot0(nodeId, viewport)
    }

    private fun captureScreenshot0(nodeId: Int, viewport: Viewport): String? {
        val format = CaptureScreenshotFormat.JPEG
        val quality = BrowserSettings.screenshotQuality

        try {
            if (nodeId > 0) {
                dom?.scrollIntoViewIfNeeded(nodeId, null, null, null)
            }
            println("viewport: ")
            println("" + viewport.x + " " + viewport.y + " " + viewport.width + " " + viewport.height)
            return page?.captureScreenshot(format, quality, viewport, true, false)
        } catch (e: ChromeRPCException) {
            handleRPCException(e)
        }

        return null
    }

    private fun calculateNodeClip(nodeId: Int, selector: String): NodeClip? {
        debugNodeClipDebug(nodeId, selector)

        val clientRect = evaluate0("__pulsar_utils__.queryClientRect('$selector')")?.toString()
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

    private fun debugNodeClipDebug(nodeId: Int, selector: String) {
        println("\n")
        println("===== $selector $nodeId")
        var result = evaluate0("__pulsar_utils__.queryClientRects('$selector')")
        println(result)
        result = dom?.getContentQuads(nodeId, null, null)
        println(result)

        var clientRect = evaluate0("__pulsar_utils__.queryClientRect('$selector')")?.toString()

        println("clientRect: ")
        println(clientRect)

        println("== scrollToTop ==")
        evaluate0("__pulsar_utils__.scrollToTop()")

        result = evaluate0("__pulsar_utils__.queryClientRects('$selector')")
        println(result)
        result = dom?.getContentQuads(nodeId, null, null)
        println(result)

        clientRect = evaluate0("__pulsar_utils__.queryClientRect('$selector')")?.toString()

        println("clientRect: ")
        println(clientRect)

        val p = page ?: return
//        val d = dom ?: return null

        val viewport = p.layoutMetrics.cssLayoutViewport
        val pageX = viewport.pageX
        val pageY = viewport.pageY

        println("pageX, pageY: ")
        println("$pageX, $pageY")
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
            return withIOContext("querySelector") { querySelector0(selector) }
        } catch (e: ChromeRPCException) {
            handleRPCException(e, "querySelector")
        }

        return null
    }

    private fun querySelector0(selector: String): Int? {
        val rootId = dom?.document?.nodeId
        return if (rootId != null && rootId != 0) {
            dom?.querySelector(rootId, selector)
        } else null
    }

    private suspend fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int? {
        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        return scrollIntoViewIfNeeded0(nodeId, selector, rect)
    }

    private fun scrollIntoViewIfNeeded0(nodeId: Int, selector: String, rect: Rect? = null): Int? {
        val node = dom?.describeNode(nodeId, null, null, null, false)
        // see org.w3c.dom.Node.ELEMENT_NODE
        val ELEMENT_NODE = 1
        if (node?.nodeType != ELEMENT_NODE) {
            logger.info("Node is not an element: $selector")
            return null
        }

        try {
            dom?.scrollIntoViewIfNeeded(nodeId, node.backendNodeId, null, rect)
        } catch (t: Throwable) {
            logger.info("Fallback to Element.scrollIntoView | {}", selector)
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
    @Throws(ChromeDriverException::class)
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

    private suspend fun <T> withIOContext(action: String, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T): T? {
        var i = maxRetry
        var result = kotlin.runCatching { withIOContext0(action, block) }
        while (result.isFailure && i-- > 0) {
            result = kotlin.runCatching { withIOContext0(action, block) }
        }

        return result.getOrElse { throw it }
    }

    private suspend fun <T> withIOContext0(action: String, block: suspend CoroutineScope.() -> T): T? {
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

    private fun viewportToRectD(viewport: Viewport): RectD {
        return RectD(viewport.x, viewport.y, viewport.width, viewport.height)
    }
}
