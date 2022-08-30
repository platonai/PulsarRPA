package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.persist.jackson.prettyPulsarObjectMapper
import ai.platon.pulsar.protocol.browser.hotfix.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdBlockRules
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.types.network.Cookie
import com.github.kklisura.cdt.protocol.types.page.Viewport
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class ChromeDevtoolsDriver(
    private val chromeTab: ChromeTab,
    private val devTools: RemoteDevTools,
    private val browserSettings: BrowserSettings,
    override val browserInstance: ChromeDevtoolsBrowser,
) : AbstractWebDriver(browserInstance) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    override val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "gap" -> 500L + Random.nextInt(500)
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "mouseWheel" -> 800L + Random.nextInt(500)
            "dragAndDrop" -> 800L + Random.nextInt(500)
            "waitForNavigation" -> 500L
            "waitForSelector" -> 500L
            else -> 100L + Random.nextInt(500)
        }
    }

    val openSequence = 1 + browserInstance.driverCount
    //    val chromeTabTimeout get() = browserSettings.fetchTaskTimeout.plusSeconds(20)
    val chromeTabTimeout get() = Duration.ofMinutes(2)
    val userAgent get() = BrowserSettings.randomUserAgent()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val isSPA get() = browserSettings.isSPA

    //    private val preloadJs by lazy { generatePreloadJs() }
    private val preloadJs get() = generatePreloadJs()

    private val pageHandler = PageHandler(devTools, browserSettings)
    private val screenshot = Screenshot(pageHandler, devTools)

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private var navigateUrl = chromeTab.url ?: ""

    private val browser get() = devTools.browser
    private val page get() = devTools.page.takeIf { isActive }
    private val target get() = devTools.target
    private val dom get() = devTools.dom.takeIf { isActive }
    private val css get() = devTools.css.takeIf { isActive }
    private val input get() = devTools.input.takeIf { isActive }
    private val mainFrame get() = page?.frameTree?.frame
    private val network get() = devTools.network.takeIf { isActive }
    private val fetch get() = devTools.fetch.takeIf { isActive }
    private val runtime get() = devTools.runtime.takeIf { isActive }
    private val emulation get() = devTools.emulation.takeIf { isActive }
    private val mouse get() = pageHandler.mouse.takeIf { isActive }
    private val keyboard get() = pageHandler.keyboard.takeIf { isActive }

    private var mainRequestId = ""
    private var mainRequestHeaders: Map<String, Any> = mapOf()
    private var mainRequestCookies: List<Map<String, String>> = listOf()

    private val rpc = RobustRPC(this)

    private val enableStartupScript get() = browserSettings.enableStartupScript
    private val enableBlockingReport = false

    private val closed = AtomicBoolean()

    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || !AppContext.isActive || !devTools.isOpen
    val isActive get() = !isGone

    val tabId get() = chromeTab.id

    /**
     * Expose the underlying implementation, used for development purpose
     * */
    val implementation get() = devTools

    init {
        if (userAgent.isNotEmpty()) {
            emulation?.setUserAgentOverride(userAgent)
        }
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        this.navigateEntry = entry
        browserInstance.navigateHistory.add(entry)

        try {
            rpc.invokeDeferred("navigateTo") {
                if (enableStartupScript) getInvaded(entry.url) else getNoInvaded(entry.url)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "navigateTo", entry.url)
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
            rpc.invokeDeferred("getCookies") { getCookies0() } ?: listOf()
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "getCookies")
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
            rpc.invokeDeferred("stopLoading") {
                page?.stopLoading()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "stopLoading")
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
            rpc.handleRPCException(e, "terminate")
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
            return rpc.invokeDeferred("evaluate") { pageHandler.evaluate(expression) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "evaluate")
        }

        return null
    }

    override val sessionId: String?
        get() {
            if (!refreshState()) return null

            lastSessionId = try {
                if (!isActive) null else mainFrame?.id
            } catch (e: ChromeRPCException) {
                rpc.handleRPCException(e, "sessionId")
                null
            }
            return lastSessionId
        }

    override suspend fun currentUrl(): String {
        if (!refreshState()) return navigateUrl

        navigateUrl = try {
            return rpc.invokeDeferred("currentUrl") {
                mainFrame?.url ?: navigateUrl
            } ?: ""
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "currentUrl")
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
            rpc.handleRPCException(e, "exists $selector")
        }

        return false
    }

    override suspend fun visible(selector: String): Boolean {
        if (!refreshState()) return false

        try {
            return pageHandler.visible(selector)
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "visible $selector")
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
                gap("waitForSelector")
                elapsedTime = System.currentTimeMillis() - startTime
                nodeId = querySelector(selector)
            }

            return timeoutMillis - elapsedTime
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "waitForSelector $selector")
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
                gap("waitForNavigation")
                elapsedTime = System.currentTimeMillis() - startTime
                navigated = isNavigated(oldUrl)
            }

            return timeoutMillis - elapsedTime
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "waitForNavigation $timeout")
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

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        rpc.invokeDeferred("mouseWheelDown") {
            repeat(count) { i ->
                if (i > 0) {
                    if (delayMillis > 0) gap(delayMillis) else gap("mouseWheel")
                }

                mouse?.wheel(deltaX, deltaY)
            }
        }
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        rpc.invokeDeferred("mouseWheelUp") {
            repeat(count) { i ->
                if (i > 0) {
                    if (delayMillis > 0) gap(delayMillis) else gap("mouseWheel")
                }

                mouse?.wheel(deltaX, deltaY)
            }
        }
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        rpc.invokeDeferred("moveMouseTo") {
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
        if (!refreshState()) return

        try {
            val nodeId = rpc.invokeDeferred("click") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)

            val p = page
            val d = dom
            if (p != null && d != null) {
                val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value ?: return

                rpc.invokeDeferred("click") {
                    mouse?.click(point.x, point.y, count, delayPolicy("click"))
                }

                gap("click")
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "click")
        }
    }

    private suspend fun click(nodeId: Int, count: Int) {
        val offset = OffsetD(4.0, 4.0)

        val p = page
        val d = dom
        if (p != null && d != null) {
            val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value ?: return
            mouse?.click(point.x, point.y, count, delayPolicy("click"))
        }
    }

    override suspend fun clickMatches(selector: String, pattern: String, count: Int) {
        if (!refreshState()) return

        try {
            rpc.invokeDeferred("clickMatches") {
                pageHandler.evaluate("__pulsar_utils__.clickMatches('$selector', '$pattern')")
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "click")
        }
    }

    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        if (!refreshState()) return

        try {
            rpc.invokeDeferred("clickMatches") {
                pageHandler.evaluate("__pulsar_utils__.clickMatches('$selector', '$attrName', '$pattern')")
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "click")
        }
    }

    private suspend fun clickMatches0(selector: String, attrName: String, pattern: String, count: Int) {
        if (!refreshState()) return

        rpc.invokeDeferred("clickMatches") {
            val bodyId = pageHandler.scrollIntoViewIfNeeded("body")
            val nodeIds = dom?.querySelectorAll(bodyId, selector) ?: return@invokeDeferred
            nodeIds.forEach { nodeId ->
                val count = dom?.getAttributes(nodeId)?.count { it.matches(pattern.toRegex()) } ?: 0
                if (count > 0) {
                    click(nodeId, count)
                    return@invokeDeferred
                }
            }
        }
    }

    override suspend fun type(selector: String, text: String) {
        if (!refreshState()) return

        try {
            rpc.invokeDeferred("type") {
                val nodeId = focusOnSelector(selector)
                if (nodeId != 0) {
                    keyboard?.type(nodeId, text, delayPolicy("type"))
                }
            }

            gap("type")
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "type")
        }
    }

    override suspend fun scrollTo(selector: String) {
        if (!refreshState()) return

        try {
            rpc.invokeDeferred("scrollTo") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "scrollTo")
        }
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val nodeId = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)
            val p = page
            val d = dom
            if (p != null && d != null) {
                rpc.invokeDeferred("dragAndDrop") {
                    val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value
                    if (point != null) {
                        val point2 = PointD(point.x + deltaX, point.y + deltaY)
                        mouse?.dragAndDrop(point, point2, delayPolicy("dragAndDrop"))
                    }
                    gap()
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "dragAndDrop")
        }
    }

    override suspend fun clickablePoint(selector: String): PointD? {
        try {
            return rpc.invokeDeferred("clickablePoint") {
                val nodeId = pageHandler.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(page, dom, nodeId)?.clickablePoint()?.value
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "clickablePoint")
        }

        return null
    }

    override suspend fun boundingBox(selector: String): RectD? {
        try {
            return rpc.invokeDeferred("boundingBox") {
                val nodeId = pageHandler.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(page, dom, nodeId)?.boundingBox()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "boundingBox")
        }

        return null
    }

    /**
     * This method scrolls element into view if needed, and then uses
     * {@link page.captureScreenshot} to take a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    override suspend fun captureScreenshot(selector: String): String? {
        return try {
            rpc.invokeDeferred("captureScreenshot") {
                // Force the page stop all navigations and pending resource fetches.
                rpc.invoke("stopLoading") { page?.stopLoading() }
                rpc.invoke("captureScreenshot") { screenshot.captureScreenshot(selector) }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }

    override suspend fun captureScreenshot(clip: RectD): String? {
        return try {
            rpc.invokeDeferred("captureScreenshot") {
                // Force the page stop all navigations and pending resource fetches.
                rpc.invoke("stopLoading") { page?.stopLoading() }
                rpc.invoke("captureScreenshot") { screenshot.captureScreenshot(clip) }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }

    internal fun refreshState(action: String = ""): Boolean {
        lastActiveTime = Instant.now()
        navigateEntry.refresh(action)
        return isActive
    }

    private suspend fun gap() {
        if (isActive) {
            delay(delayPolicy("gap"))
        }
    }

    private suspend fun gap(type: String) {
        if (isActive) {
            delay(delayPolicy(type))
        }
    }

    private suspend fun gap(millis: Long) {
        if (isActive) {
            delay(millis)
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
            return rpc.invokeDeferred("querySelector") { pageHandler?.querySelector(selector) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "querySelector")
        }

        return null
    }

    override suspend fun pageSource(): String? {
        if (!refreshState()) return null

        try {
            return rpc.invokeDeferred("pageSource") {
                dom?.getOuterHTML(dom?.document?.nodeId, null, null)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "pageSource")
        }

        return null
    }

    override suspend fun bringToFront() {
        try {
            rpc.invokeDeferred("bringToFront") {
                page?.bringToFront()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e)
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

    override fun awaitTermination() {
        devTools.awaitTermination()
    }

    /**
     * Close the tab hold by this driver
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                browserInstance.closeTab(chromeTab)
            } catch (e: WebDriverException) {
                // ignored
            }

            try {
                devTools.close()
            } catch (e: WebDriverException) {
                // ignored
            }
        }
    }

    private fun getInvaded(url: String) {
        page?.enable()
        dom?.enable()
        css?.enable()
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
        val tabs = browserInstance.listTabs()
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
        return rpc.invokeDeferred("isMainFrame") {
            mainFrame?.id == frameId
        } ?: false
    }

    private fun viewportToRectD(viewport: Viewport): RectD {
        return RectD(viewport.x, viewport.y, viewport.width, viewport.height)
    }
}
