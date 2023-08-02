package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.OffsetD
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.*
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.NetworkManager
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.RobustRPC
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceived
import com.github.kklisura.cdt.protocol.v2023.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.v2023.types.network.Cookie
import com.github.kklisura.cdt.protocol.v2023.types.network.ErrorReason
import com.github.kklisura.cdt.protocol.v2023.types.page.Viewport
import com.github.kklisura.cdt.protocol.v2023.types.runtime.Evaluate
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ChromeDevtoolsDriver(
    val chromeTab: ChromeTab,
    val devTools: RemoteDevTools,
    val browserSettings: BrowserSettings,
    override val browser: ChromeDevtoolsBrowser,
) : AbstractWebDriver(browser) {

    companion object {
        val LOCALHOST_PREFIX = "http://localfile.org"
    }

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    val openSequence = 1 + browser.drivers.size

    val resourceBlockProbability get() = browserSettings.resourceBlockProbability
    /**
     * Disable network after n response received
     * TODO: check if this is necessary
     * */
    private val numResponseReceivedToDisableNetwork: Int get() {
        return System.getProperty("WebDriver.numResponseReceivedToDisableNetwork")?.toIntOrNull() ?: -1
    }
    private val _blockedURLs = mutableListOf<String>()
    private val _probabilityBlockedURLs = mutableListOf<String>()
    val blockedURLs: List<String> get() = _blockedURLs
    val probabilityBlockedURLs: List<String> get() = _probabilityBlockedURLs

    private val page = PageHandler(devTools, browserSettings)
    private val screenshot = Screenshot(page, devTools)

    private var isFirstLaunch = openSequence == 1
    private var lastSessionId: String? = null
    private var navigateUrl = chromeTab.url ?: ""

    private val browserAPI get() = devTools.browser.takeIf { isActive }
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val targetAPI get() = devTools.target.takeIf { isActive }
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

    private var numResponseReceived = AtomicInteger()
    private val rpc = RobustRPC(this)
    private var credentials: Credentials? = null

    private val networkManager by lazy { NetworkManager(this, rpc) }

    private val enableStartupScript get() = browserSettings.isStartupScriptEnabled
    private val initScriptCache = mutableListOf<String>()
    private val closed = AtomicBoolean()

    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || isQuit || !AppContext.isActive || !devTools.isOpen
    val isActive get() = !isGone
    /**
     * Expose the underlying implementation, used for development purpose
     * */
    val implementation get() = devTools

    init {
        val userAgent = browser.userAgent
        if (!userAgent.isNullOrEmpty()) {
            emulationAPI?.setUserAgentOverride(userAgent)
        }
    }

    override suspend fun addInitScript(script: String) {
        initScriptCache.add(script)
//        try {
//            rpc.invokeDeferred("addInitScript") {
//                pageAPI?.enable()
//                val script0 = browserSettings.confuser.confuse(script)
//                pageAPI?.addScriptToEvaluateOnNewDocument(script0)
//            }
//        } catch (e: ChromeRPCException) {
//            rpc.handleRPCException(e, "addInitScript")
//        }
    }

    override suspend fun addBlockedURLs(urls: List<String>) {
        _blockedURLs.addAll(urls)
    }

    override suspend fun addProbabilityBlockedURLs(urls: List<String>) {
        _probabilityBlockedURLs.addAll(urls)
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) {
        navigateHistory.add(entry)
        this.navigateEntry = entry

        browser.emit(BrowserEvents.willNavigate, entry)

        try {
            enableAPIAgents()

            rpc.invokeDeferred("navigateTo") {
                if (enableStartupScript) navigateInvaded(entry) else navigateNonInvaded(entry)
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "navigateTo", entry.url)
        }
    }

    @Deprecated("Getter is available", replaceWith = ReplaceWith("mainRequestHeaders"))
    @Throws(WebDriverException::class)
    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return mainRequestHeaders
    }

    @Deprecated("Getter is available", replaceWith = ReplaceWith("mainRequestCookies"))
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
        enableAPIAgents()
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
            rpc.invokeDeferred("pause") { pageAPI?.stopLoading() }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "pause")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun stop() {
        navigateEntry.stopped = true
        try {
            handleRedirect()

            if (browser.isGUI) {
                // in gui mode, just stop the loading, so we can diagnose
                pageAPI?.stopLoading()
            } else {
                // go to about:blank, so the browser stops the previous page and releases all resources
                navigateTo(ChromeImpl.ABOUT_BLANK_PAGE)
            }
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

    @Throws(WebDriverException::class)
    override suspend fun evaluateDetail(expression: String): JsEvaluation? {
        try {
            return rpc.invokeDeferred("evaluateDetail") {
                createJsEvaluate(page.evaluateDetail(expression))
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "evaluateDetail")
        }

        return null
    }

    private fun createJsEvaluate(evaluate: Evaluate?): JsEvaluation? {
        evaluate ?: return null

        val result = evaluate.result
        val exception = evaluate.exceptionDetails
        return if (exception != null) {
            val jsException = JsException(
                text = exception.text,
                lineNumber = exception.lineNumber,
                columnNumber = exception.columnNumber,
                url = exception.url,
            )
            JsEvaluation(exception = jsException)
        } else {
            JsEvaluation(
                value = result.value,
                unserializableValue = result.unserializableValue,
                className = result.className,
                description = result.description
            )
        }
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
            return rpc.invokeDeferred("currentUrl") { mainFrameAPI?.url ?: navigateUrl } ?: ""
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
            return rpc.invokeDeferred("isVisible") { page.visible(selector) } ?: false
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "visible >$selector<")
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
    override suspend fun waitForPage(url: String, timeout: Duration): WebDriver? {
        var now = Instant.now()
        val endTime = now + timeout
        var driver = browser.findDriver(url)
        while (driver == null && now < endTime) {
            delay(1000)
            now = Instant.now()
            driver = browser.findDriver(url)
        }
        return driver
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

    /**
     * TODO: test is required
     * */
    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(x: Double, y: Double) {
        try {
            rpc.invokeDeferred("moveMouseTo") { mouse?.moveTo(x, y) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "moveMouseTo")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val nodeId = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                page.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)
            val p = pageAPI
            val d = domAPI
            if (p != null && d != null) {
                rpc.invokeDeferred("moveMouseTo") {
                    val point = ClickableDOM(p, d, nodeId, offset).clickablePoint().value
                    if (point != null) {
                        val point2 = PointD(point.x + deltaX, point.y + deltaY)
                        mouse?.moveTo(point2)
                    }
                    gap()
                }
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
    override suspend fun focus(selector: String) {
        focusOnSelector(selector)
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
            rpc.invokeDeferred("scrollTo") { page.scrollIntoViewIfNeeded(selector) }
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
            return rpc.invokeDeferred("querySelector") { page.querySelector(selector) }
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
        browser.destroyDriver(this)
        doClose()
    }

    fun doClose() {
        if (closed.compareAndSet(false, true)) {
            state.set(WebDriver.State.QUIT)

            try {
                devTools.close()
            } catch (e: WebDriverException) {
                // ignored
            }
        }
    }

    fun enableAPIAgents() {
        pageAPI?.enable()
        domAPI?.enable()
        runtimeAPI?.enable()
        networkAPI?.enable()
        cssAPI?.enable()

        if (resourceBlockProbability > 1e-6) {
            fetchAPI?.enable()
        }

        val proxyUsername = browser.id.fingerprint.proxyUsername
        if (!proxyUsername.isNullOrBlank()) {
            // allow all url patterns
            val patterns = listOf(RequestPattern())
            fetchAPI?.enable(patterns, true)
        }
    }

    override fun toString() = "Driver#$id"

    /**
     * Navigate to the page and inject scripts.
     * */
    private fun navigateInvaded(entry: NavigateEntry) {
        val url = entry.url

//        pageAPI?.addScriptToEvaluateOnNewDocument(buildInitScripts())
        addScriptToEvaluateOnNewDocument()

        if (blockedURLs.isNotEmpty()) {
            // Blocks URLs from loading.
            networkAPI?.setBlockedURLs(blockedURLs)
        }

        networkAPI?.onRequestWillBeSent { requestWillBeSent ->
            onRequestWillBeSent(entry, requestWillBeSent)
        }

        networkAPI?.onResponseReceived { response ->
            onResponseReceived(entry, response)
        }

        pageAPI?.onDocumentOpened {
            entry.mainRequestCookies = getCookies0()
        }

        val proxyUsername = browser.id.fingerprint.proxyUsername
        if (!proxyUsername.isNullOrBlank()) {
            credentials = Credentials(proxyUsername, browser.id.fingerprint.proxyPassword)
            credentials?.let { networkManager.authenticate(it) }
        }

        navigateUrl = url
        // TODO: This is a temporary solution to serve local file, for example, file:///tmp/example.html
        if (LOCALHOST_PREFIX in url) {
            val url0 = url.removePrefix(LOCALHOST_PREFIX)
            pageAPI?.navigate("file://$url0")
        } else {
            pageAPI?.navigate(url)
        }
    }

    /**
     * Navigate to a url without javascript injected, this is only for debugging
     * */
    private fun navigateNonInvaded(entry: NavigateEntry) {
        val url = entry.url

        navigateUrl = url
        pageAPI?.navigate(url)
    }

    private fun onRequestWillBeSent(entry: NavigateEntry, requestWillBeSent: RequestWillBeSent) {
        if (entry.mainRequestId.isBlank()) {
            // The first request, fetch the main HTML

            // amazon.com uses "referer" instead of "referrer" in the request header,
            // not clear if other sites uses the other one
            val headers: MutableMap<String, Any> = requestWillBeSent.request.headers
            entry.pageReferrer?.let {
                headers["referer"] = it
                headers["referrer"] = it
            }

            entry.mainRequestId = requestWillBeSent.requestId
            entry.mainRequestHeaders = requestWillBeSent.request.headers
        }

        if (resourceBlockProbability > 1e-6) {
            val requestUrl = requestWillBeSent.request.url
            if (probabilityBlockedURLs.any { requestUrl.matches(it.toRegex()) }) {
                // random drop requests
                val hit = Random.nextInt(100) / 100.0f < resourceBlockProbability
                if (hit) {
                    // ErrorReason.CONNECTION_FAILED
                    fetchAPI?.failRequest(requestWillBeSent.requestId, ErrorReason.ABORTED)
                }
            }
        }
    }

    private fun onResponseReceived(entry: NavigateEntry, response: ResponseReceived) {
        val maxResponses = numResponseReceivedToDisableNetwork
        if (maxResponses > 0 && entry.mainRequestId.isNotBlank()) {
            // split the `if` to make it clearer
            if (numResponseReceived.incrementAndGet() == maxResponses) {
                // Disables network tracking, prevents network events from being sent to the client.
                // TODO: does the resource blocking logic work if we disable the network API?
                // networkAPI?.disable()
                // logger.info("Network API for driver #{} is disabled", id)
            }
        }

        if (entry.mainResponseStatus < 0) {
            entry.mainResponseStatus = response.response.status
            entry.mainResponseStatusText = response.response.statusText
            entry.mainResponseHeaders = response.response.headers
        }
    }

    private suspend fun handleRedirect() {
        val finalUrl = currentUrl()
        // redirect
        if (finalUrl.isNotBlank() && finalUrl != navigateUrl) {
            // browser.addHistory(NavigateEntry(finalUrl))
        }
    }

    private fun addScriptToEvaluateOnNewDocument() {
        val js = browserSettings.scriptLoader.getPreloadJs(false)
        if (js !in initScriptCache) {
            // utils comes first
            initScriptCache.add(0, js)
        }

        val confuser = browserSettings.confuser
        initScriptCache.forEach {
            pageAPI?.addScriptToEvaluateOnNewDocument(confuser.confuse(it))
        }

        if (logger.isTraceEnabled) {
            reportInjectedJs()
        }

        // the cache is used for a single navigation, so we have to clear it
        initScriptCache.clear()
    }

    private fun reportInjectedJs() {
        val script = browserSettings.confuser.confuse(initScriptCache.joinToString("\n;\n\n\n;\n"))

        // TODO: write to driver specific directory
        val dir = AppPaths.REPORT_DIR.resolve("browser/js")
        Files.createDirectories(dir)
        val report = Files.writeString(dir.resolve("preload.all.js"), script)
        logger.trace("All injected js: file://$report")
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
