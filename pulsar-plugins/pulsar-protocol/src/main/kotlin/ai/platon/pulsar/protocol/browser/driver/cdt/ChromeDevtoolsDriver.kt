package ai.platon.pulsar.protocol.browser.driver.cdt

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
import ai.platon.pulsar.crawl.fetch.driver.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.types.network.Cookie
import com.github.kklisura.cdt.protocol.types.page.Viewport
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ChromeDevtoolsDriver(
    val chromeTab: ChromeTab,
    val devTools: RemoteDevTools,
    val browserSettings: BrowserSettings,
    override val browser: ChromeDevtoolsBrowser,
) : AbstractWebDriver(browser) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    val openSequence = 1 + browser.drivers.size

    val enableUrlBlocking get() = browserSettings.isUrlBlockingEnabled
    private val _blockedURLs = mutableListOf<String>()
    val blockedURLs: List<String> get() = _blockedURLs

    private val page = PageHandler(devTools, browserSettings)
    private val screenshot = Screenshot(page, devTools)

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

    private val mouse get() = page.mouse.takeIf { isActive }
    private val keyboard get() = page.keyboard.takeIf { isActive }

    private var mainRequestId = ""
    private var mainRequestHeaders: Map<String, Any> = mapOf()
    private var mainRequestCookies: List<Map<String, String>> = listOf()
    private var numResponseReceived = AtomicInteger()
    private val rpc = RobustRPC(this)

    private val enableStartupScript get() = browserSettings.isStartupScriptEnabled
    private val closed = AtomicBoolean()

    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || !AppContext.isActive || !devTools.isOpen
    val isActive get() = !isGone
    /**
     * Expose the underlying implementation, used for development purpose
     * */
    val implementation get() = devTools

    init {
        val userAgent = browser.userAgent
        if (userAgent != null && userAgent.isNotEmpty()) {
            emulationAPI?.setUserAgentOverride(userAgent)
        }
    }

    override suspend fun addInitScript(script: String) {
        try {
            rpc.invokeDeferred("addInitScript") {
                pageAPI?.enable()
                val script0 = browserSettings.confuser.confuse(script)
                pageAPI?.addScriptToEvaluateOnNewDocument(script0)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "addInitScript")
        }
    }

    override suspend fun addBlockedURLs(urls: List<String>) {
        _blockedURLs.addAll(urls)
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) {
        browser.emit(BrowserEvents.willNavigate, entry)
        navigateHistory.add(entry)

        this.navigateEntry = entry

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
        val cookies = networkAPI?.cookies?.map { serialize(it) }
        networkAPI?.disable()
        return cookies ?: listOf()
    }

    private fun serialize(cookie: Cookie): Map<String, String> {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(cookie)
        val map: Map<String, String?> = mapper.readValue(json)
        return map.filterValues { it != null }.mapValues { it.toString() }
    }

    @Throws(WebDriverException::class)
    override suspend fun pause() {
        try {
            rpc.invokeDeferred("pause") {
                pageAPI?.stopLoading()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "pause")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun stop() {
        navigateEntry.stopped = true
        try {
            if (browser.isGUI) {
                // in gui mode, just stop the loading, so we can diagnose
                pageAPI?.stopLoading()
            } else {
                // go to about:blank, so the browser stops the previous page and release all resources
                navigateTo(ChromeImpl.ABOUT_BLANK_PAGE)
            }

            handleRedirect()
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "terminate")
        } catch (e: ChromeDriverException) {
            logger.info("Terminate exception: {}", e.message)
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun terminate() {
        stop()
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluate(expression: String): Any? {
        try {
            return rpc.invokeDeferred("evaluate") { page.evaluate(expression) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "evaluate")
        }

        return null
    }

    @Deprecated("Not used any more")
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
    override suspend fun isVisible(selector: String): Boolean {
        try {
            return page.visible(selector)
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
                page.scrollIntoViewIfNeeded(selector)
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
                page.scrollIntoViewIfNeeded(selector)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "scrollTo")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val nodeId = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                page.scrollIntoViewIfNeeded(selector)
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
                val nodeId = page.scrollIntoViewIfNeeded(selector)
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
                val nodeId = page.scrollIntoViewIfNeeded(selector)
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
            return rpc.invokeDeferred("querySelector") { page?.querySelector(selector) }
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

    override fun toString() = "DevTools driver ($lastSessionId)"

    private fun getInvaded(url: String) {
        pageAPI?.enable()
        domAPI?.enable()
        runtimeAPI?.enable()
        networkAPI?.enable()

        pageAPI?.addScriptToEvaluateOnNewDocument(getInjectJs())

        if (enableUrlBlocking && blockedURLs.isNotEmpty()) {
            networkAPI?.setBlockedURLs(blockedURLs)
        }

        networkAPI?.onRequestWillBeSent {
            if (mainRequestId.isBlank()) {
                mainRequestId = it.requestId
                mainRequestHeaders = it.request.headers
            }
        }

        networkAPI?.onResponseReceived {
            if (mainRequestId != null) {
                // split the `if` to make it clearer
                if (numResponseReceived.incrementAndGet() == 100) {
                    // Disables network tracking, prevents network events from being sent to the client.
                    networkAPI?.disable()
                    // logger.info("Network tracking for driver #{} is disabled", id)
                }
            }
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
            // browser.addHistory(NavigateEntry(finalUrl))
        }
    }

    private fun getInjectJs(): String {
        val js = browserSettings.scriptLoader.getPreloadJs(false)
        return browserSettings.confuser.confuse(js)
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
