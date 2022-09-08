package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
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

class ChromeDevtoolsDriver(
    private val chromeTab: ChromeTab,
    private val devTools: RemoteDevTools,
    private val browserSettings: BrowserSettings,
    override val browser: ChromeDevtoolsBrowser,
) : AbstractWebDriver(browser) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    val openSequence = 1 + browser.drivers.size
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

    private val browserAPI get() = devTools.browser
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val targetAPI get() = devTools.target
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val inputAPI get() = devTools.input.takeIf { isActive }
    private val mainFrameAPI get() = pageAPI?.frameTree?.frame
    private val networkAPI get() = devTools.network.takeIf { isActive }
    private val fetchAPI get() = devTools.fetch.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }
    private val emulationAPI get() = devTools.emulation.takeIf { isActive }

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
            emulationAPI?.setUserAgentOverride(userAgent)
        }
    }

    override suspend fun addInitScript(script: String) {
        try {
            rpc.invokeDeferred("addInitScript") {
                pageAPI?.enable()
                pageAPI?.addScriptToEvaluateOnNewDocument(script)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "addInitScript")
        }
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) {
        this.navigateEntry = entry
        navigateHistory.add(entry)
        browser.onWillNavigate(entry)

        try {
            rpc.invokeDeferred("navigateTo") {
                if (enableStartupScript) getInvaded(entry.url) else getNoInvaded(entry.url)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "navigateTo", entry.url)
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return mainRequestHeaders
    }

    @Throws(WebDriverException::class)
    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        return mainRequestCookies
    }

    @Throws(WebDriverException::class)
    override suspend fun getCookies(): List<Map<String, String>> {
        return try {
            rpc.invokeDeferred("getCookies") { getCookies0() } ?: listOf()
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "getCookies")
            listOf()
        }
    }

    @Throws(WebDriverException::class)
    private fun getCookies0(): List<Map<String, String>> {
        networkAPI?.enable()
        return networkAPI?.cookies?.map { serialize(it) }?: listOf()
    }

    private fun serialize(cookie: Cookie): Map<String, String> {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(cookie)
        val map: Map<String, String?> = mapper.readValue(json)
        return map.filterValues { it != null }.mapValues { it.toString() }
    }

    /** Force the page stop all navigations and releases all resources. */
    @Throws(WebDriverException::class)
    override suspend fun stop() {
        terminate()
    }

    /** Force the page stop all navigations and pending resource fetches. */
    @Throws(WebDriverException::class)
    override suspend fun stopLoading() {
        try {
            rpc.invokeDeferred("stopLoading") {
                pageAPI?.stopLoading()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "stopLoading")
        }
    }

    /** Force the page stop all navigations and releases all resources. */
    @Throws(WebDriverException::class)
    override suspend fun terminate() {
        navigateEntry.stopped = true
        try {
            if (browser.isGUI) {
                // in gui mode, just stop the loading, so we can make a diagnosis
                pageAPI?.stopLoading()
            } else {
                // go to about:blank, so the browser stops the previous page and release all resources
                navigateTo(ChromeImpl.ABOUT_BLANK_PAGE)
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
    @Throws(WebDriverException::class)
    override suspend fun evaluate(expression: String): Any? {
        try {
            return rpc.invokeDeferred("evaluate") { pageHandler.evaluate(expression) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "evaluate")
        }

        return null
    }

    override val sessionId: String?
        @Throws(WebDriverException::class)
        get() {
            lastSessionId = try {
                if (!isActive) null else mainFrameAPI?.id
            } catch (e: ChromeRPCException) {
                rpc.handleRPCException(e, "sessionId")
                null
            }
            return lastSessionId
        }

    @Throws(WebDriverException::class)
    override suspend fun currentUrl(): String {
        navigateUrl = try {
            return rpc.invokeDeferred("currentUrl") {
                mainFrameAPI?.url ?: navigateUrl
            } ?: ""
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "currentUrl")
            ""
        }

        return navigateUrl
    }

    @Throws(WebDriverException::class)
    override suspend fun exists(selector: String): Boolean {
        try {
            val nodeId = querySelector(selector)
            return nodeId != null && nodeId > 0
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "exists $selector")
        }

        return false
    }

    @Throws(WebDriverException::class)
    override suspend fun visible(selector: String): Boolean {
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
    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        val timeoutMillis = timeout.toMillis()
        val startTime = System.currentTimeMillis()
        var elapsedTime = 0L

        try {
            var nodeId = querySelector(selector)
            while (elapsedTime < timeoutMillis && (nodeId == null || nodeId <= 0) && isActive) {
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

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(timeout: Duration): Long {
        try {
            val oldUrl = currentUrl()
            var navigated = isNavigated(oldUrl)
            val startTime = System.currentTimeMillis()
            var elapsedTime = 0L

            val timeoutMillis = timeout.toMillis()

            while (elapsedTime < timeoutMillis && !navigated && isActive) {
                gap("waitForNavigation")
                elapsedTime = System.currentTimeMillis() - startTime
                navigated = isNavigated(oldUrl)
            }

            return timeoutMillis - elapsedTime
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "waitForNavigation $timeout")
        }

        return -1
    }

    @Throws(WebDriverException::class)
    private suspend fun isNavigated(oldUrl: String): Boolean {
        if (oldUrl != currentUrl()) {
            return true
        }

        // TODO: other signals

        return false
    }

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        try {
            rpc.invokeDeferred("mouseWheelDown", 1) {
                repeat(count) { i ->
                    if (i > 0) {
                        if (delayMillis > 0) gap(delayMillis) else gap("mouseWheel")
                    }

                    mouse?.wheel(deltaX, deltaY)
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "mouseWheelDown")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        try {
            rpc.invokeDeferred("mouseWheelUp", 1) {
                repeat(count) { i ->
                    if (i > 0) {
                        if (delayMillis > 0) gap(delayMillis) else gap("mouseWheel")
                    }

                    mouse?.wheel(deltaX, deltaY)
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "mouseWheelUp")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(x: Double, y: Double) {
        try {
            rpc.invokeDeferred("moveMouseTo") {
                mouse?.move(x, y)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "moveMouseTo")
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
    @Throws(WebDriverException::class)
    override suspend fun click(selector: String, count: Int) {
        try {
            val nodeId = rpc.invokeDeferred("click") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)

            val p = pageAPI
            val d = domAPI
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

    @Throws(WebDriverException::class)
    private suspend fun click(nodeId: Int, count: Int) {
        val offset = OffsetD(4.0, 4.0)

        val p = pageAPI
        val d = domAPI
        if (p != null && d != null) {
            val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value ?: return
            mouse?.click(point.x, point.y, count, delayPolicy("click"))
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, pattern: String, count: Int) {
        try {
            rpc.invokeDeferred("clickMatches") {
                pageHandler.evaluate("__pulsar_utils__.clickMatches('$selector', '$pattern')")
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "click")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        try {
            rpc.invokeDeferred("clickMatches") {
                pageHandler.evaluate("__pulsar_utils__.clickMatches('$selector', '$attrName', '$pattern')")
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "click")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun type(selector: String, text: String) {
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

    @Throws(WebDriverException::class)
    override suspend fun scrollTo(selector: String) {
        try {
            rpc.invokeDeferred("scrollTo") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "scrollTo")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val nodeId = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                pageHandler.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)
            val p = pageAPI
            val d = domAPI
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

    @Throws(WebDriverException::class)
    override suspend fun clickablePoint(selector: String): PointD? {
        try {
            return rpc.invokeDeferred("clickablePoint") {
                val nodeId = pageHandler.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(pageAPI, domAPI, nodeId)?.clickablePoint()?.value
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "clickablePoint")
        }

        return null
    }

    @Throws(WebDriverException::class)
    override suspend fun boundingBox(selector: String): RectD? {
        try {
            return rpc.invokeDeferred("boundingBox") {
                val nodeId = pageHandler.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(pageAPI, domAPI, nodeId)?.boundingBox()
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
    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(selector: String): String? {
        return try {
            rpc.invokeDeferred("captureScreenshot") {
                // Force the page stop all navigations and pending resource fetches.
                rpc.invoke("stopLoading") { pageAPI?.stopLoading() }
                rpc.invoke("captureScreenshot") { screenshot.captureScreenshot(selector) }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(clip: RectD): String? {
        return try {
            rpc.invokeDeferred("captureScreenshot") {
                // Force the page stop all navigations and pending resource fetches.
                rpc.invoke("stopLoading") { pageAPI?.stopLoading() }
                rpc.invoke("captureScreenshot") { screenshot.captureScreenshot(clip) }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }

    @Throws(WebDriverCancellationException::class)
    internal fun checkState(action: String = ""): Boolean {
        if (!isActive) {
            return false
        }

        if (isCanceled) {
            // is it good to throw here?
            throw WebDriverCancellationException("WebDriver is canceled #$id | $navigateUrl", this)
        }

        if (action.isNotBlank()) {
            lastActiveTime = Instant.now()
            navigateEntry.refresh(action)
        }

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
        if (!checkState()) return 0

        val rootId = domAPI?.document?.nodeId ?: return 0
        val nodeId = domAPI?.querySelector(rootId, selector)
        if (nodeId == 0) {
            logger.warn("No node found for selector: $selector")
            return 0
        }

        try {
            domAPI?.focus(nodeId, rootId, null)
        } catch (e: Exception) {
            logger.warn("Failed to focus #$nodeId | {}", e.message)
        }

        return nodeId ?: 0
    }

    @Throws(WebDriverException::class)
    private suspend fun querySelector(selector: String): Int? {
        if (!checkState()) return null

        try {
            return rpc.invokeDeferred("querySelector") { pageHandler?.querySelector(selector) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "querySelector")
        }

        return null
    }

    @Throws(WebDriverException::class)
    override suspend fun pageSource(): String? {
        if (!checkState()) return null

        try {
            return rpc.invokeDeferred("pageSource") {
                domAPI?.getOuterHTML(domAPI?.document?.nodeId, null, null)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "pageSource")
        }

        return null
    }

    override suspend fun bringToFront() {
        if (!checkState()) return

        try {
            rpc.invokeDeferred("bringToFront") {
                pageAPI?.bringToFront()
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
                browser.closeTab(chromeTab)
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
        pageAPI?.enable()
        domAPI?.enable()
        runtimeAPI?.enable()
        networkAPI?.enable()

        pageAPI?.addScriptToEvaluateOnNewDocument(preloadJs)

        if (enableUrlBlocking) {
            setupUrlBlocking(url)
        }

        networkAPI?.onRequestWillBeSent {
            if (mainRequestId.isBlank()) {
                mainRequestId = it.requestId
                mainRequestHeaders = it.request.headers
            }
        }

        networkAPI?.onResponseReceived {

        }

        pageAPI?.onDocumentOpened {
            mainRequestCookies = getCookies0()
        }

        navigateUrl = url
        pageAPI?.navigate(url)
    }

    private fun getNoInvaded(url: String) {
        pageAPI?.enable()
        navigateUrl = url
        pageAPI?.navigate(url)
    }

    private suspend fun handleRedirect() {
        val finalUrl = currentUrl()
        // redirect
        if (finalUrl.isNotBlank() && finalUrl != navigateUrl) {
            browser.onWillNavigate(NavigateEntry(finalUrl))
        }
    }

    // close irrelevant tabs, which might be opened for humanization purpose
    @Throws(ChromeDriverException::class)
    private fun cleanTabs() {
        try {
            val tabs = browser.listTabs()
            closeTimeoutTabs(tabs)
            closeIrrelevantTabs(tabs)
        } catch (e: WebDriverException) {
            // ignored
        }
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
        val entries = browser.navigateHistory.asSequence()
            .filter { it.url == tabUrl }
            .filter { it.stopped }
            .filter { it.lastActiveTime + chromeTabTimeout < now }
            .toList()

        if (entries.isNotEmpty()) {
            // browser.navigateHistory.removeAll(entries)
            browser.closeTab(oldTab)
        }
    }

    private fun closeIrrelevantTabs(tabs: Array<ChromeTab>) {
        val now = Instant.now()
        val irrelevantTabs = tabs
            .filter { it.url?.matches("about:".toRegex()) == true }
            .filter { oldTab -> browser.navigateHistory.none { it.url == oldTab.url } }
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
        networkAPI?.setBlockedURLs(blockRules.blockingUrls)

        networkAPI?.takeIf { enableBlockingReport }?.onRequestWillBeSent {
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
            mainFrameAPI?.id == frameId
        } ?: false
    }

    private fun viewportToRectD(viewport: Viewport): RectD {
        return RectD(viewport.x, viewport.y, viewport.width, viewport.height)
    }
}
