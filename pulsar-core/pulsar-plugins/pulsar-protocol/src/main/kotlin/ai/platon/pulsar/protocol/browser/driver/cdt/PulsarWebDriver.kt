package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.cdt.kt.protocol.events.network.RequestWillBeSent
import ai.platon.cdt.kt.protocol.events.network.ResponseReceived
import ai.platon.cdt.kt.protocol.events.page.FrameNavigated
import ai.platon.cdt.kt.protocol.events.page.WindowOpen
import ai.platon.cdt.kt.protocol.types.fetch.RequestPattern
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import ai.platon.cdt.kt.protocol.types.network.LoadNetworkResourceOptions
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import ai.platon.cdt.kt.protocol.types.runtime.Evaluate
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.dom.ChromeCdpDomService
import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeIOException
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.ChromeNavigateEntry
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.NetworkEvents
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.NetworkManager
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.RobustRPC
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil
import ai.platon.pulsar.skeleton.crawl.fetch.driver.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.SystemUtils
import org.apache.hc.core5.net.URIBuilder
import java.nio.file.Files
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class PulsarWebDriver(
    uniqueID: String,
    val chromeTab: ChromeTab,
    val devTools: RemoteDevTools,
    override val browser: PulsarBrowser
) : AbstractWebDriver(uniqueID, browser) {

    private val logger = getLogger(this)

    private val tracer get() = logger.takeIf { it.isTraceEnabled }

    override val browserType: BrowserType = BrowserType.PULSAR_CHROME

    private val browserAPI get() = devTools.browser.takeIf { isActive }
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val targetAPI get() = devTools.target.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val inputAPI get() = devTools.input.takeIf { isActive }
    private suspend fun mainFrameAPI() = pageAPI?.getFrameTree()?.frame
    private val networkAPI get() = devTools.network.takeIf { isActive }
    private val fetchAPI get() = devTools.fetch.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }
    private val emulationAPI get() = devTools.emulation.takeIf { isActive }

    private val rpc = RobustRPC(this)
    private val page = PageHandler(devTools, settings.confuser)
    private val mouse get() = page.mouse.takeIf { isActive }
    private val keyboard get() = page.keyboard.takeIf { isActive }
    private val screenshot = ScreenshotHandler(page, devTools)

    private val networkManager by lazy { NetworkManager(this, rpc) }
    private val messageWriter = MiscMessageWriter()

    private val closed = AtomicBoolean()

    private val isGone get() = closed.get() || isQuit || !AppContext.isActive || !devTools.isOpen

    private var navigateUrl = chromeTab.url ?: ""
    private var credentials: Credentials? = null

    var injectedScriptIdentifier: String? = null

    /**
     * Expose the underlying implementation, used for diagnosis purpose
     * */
    override val implementation: Any get() = devTools

    override val domService: DomService get() = ChromeCdpDomService(devTools)

    init {
        val userAgent = browser.userAgentOverride
        if (!userAgent.isNullOrEmpty()) {
            runBlocking { emulationAPI?.setUserAgentOverride(userAgent) }
        }
    }

    override suspend fun addBlockedURLs(urlPatterns: List<String>) {
        _blockedURLPatterns.addAll(urlPatterns)
    }

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) {
        navigateHistory.add(entry)
        this.navigateEntry = entry

        browser.emit(BrowserEvents.willNavigate, entry)

        invokeOnPage("enableAPIAgents") {
            enableAPIAgents()
        }

        invokeOnPage("navigateTo") {
            navigateInvaded(entry)
        }
    }

    override suspend fun goBack() {
        invokeOnPage("goBack") {
            val history = pageAPI?.getNavigationHistory() ?: return@invokeOnPage
            val currentIndex = history.currentIndex
            val entries = history.entries
            val targetIndex = currentIndex - 1
            if (targetIndex >= 0 && targetIndex < entries.size) {
                val entryId = entries[targetIndex].id
                pageAPI?.navigateToHistoryEntry(entryId)
            }
        }
    }

    override suspend fun goForward() {
        invokeOnPage("goForward") {
            val history = pageAPI?.getNavigationHistory() ?: return@invokeOnPage
            val currentIndex = history.currentIndex ?: return@invokeOnPage
            val entries = history.entries ?: return@invokeOnPage
            val targetIndex = currentIndex + 1
            if (targetIndex >= 0 && targetIndex < entries.size) {
                val entryId = entries[targetIndex].id
                pageAPI?.navigateToHistoryEntry(entryId)
            }
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun getCookies(): List<Map<String, String>> {
        return invokeOnPage("getCookies") { getCookies0() } ?: listOf()
    }

    @Deprecated(
        "Use deleteCookies(name, url, domain, path) instead." + "[deleteCookies] (3/5) | code: -32602, At least one of the url and domain needs to be specified",
        ReplaceWith("driver.deleteCookies(name, url, domain, path)")
    )
    override suspend fun deleteCookies(name: String) {
        invokeOnPage("deleteCookies") { cdpDeleteCookies(name) }
    }

    override suspend fun deleteCookies(name: String, url: String?, domain: String?, path: String?) {
        invokeOnPage("deleteCookies") { cdpDeleteCookies(name, url, domain, path) }
    }

    override suspend fun clearBrowserCookies() {
        invokeOnPage("clearBrowserCookies") { networkAPI?.clearBrowserCookies() }
    }

    // Use the JavaScript version in super class
    override suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String? {
        val name = "selectFirstAttributeOrNull"
        return invokeOnElement(selector, name) { page.getAttribute(it, attrName) }
    }

    // Unittest failed
//    override suspend fun selectAttributeAll(selector: String, attrName: String, start: Int, limit: Int): List<String> {
//        val name = "selectAttributeAll"
//        return invokeOnPage(name) { page.getAttributeAll(selector, attrName, start, limit) } ?: listOf()
//    }

    @Throws(WebDriverException::class)
    override suspend fun evaluate(expression: String): Any? {
        return invokeOnPage("evaluate") { page.evaluate(expression) }
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluateDetail(expression: String): JsEvaluation? {
        return invokeOnPage("evaluateDetail") { createJsEvaluate(page.evaluateDetail(expression)) }
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluateValue(expression: String): Any? {
        return invokeOnPage("evaluateValue") { page.evaluateValue(expression) }
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluateValueDetail(expression: String): JsEvaluation? {
        return invokeOnPage("evaluateValueDetail") { createJsEvaluate(page.evaluateValueDetail(expression)) }
    }

    override suspend fun currentUrl(): String {
        val mainFrameUrl = runCatching { invokeOnPage("currentUrl") { mainFrameAPI()?.url } }.onFailure {
                logger.warn("Failed to retrieve the mainFrameUrl", it)
            }.getOrNull()
        navigateUrl = mainFrameUrl ?: navigateUrl
        return navigateUrl
    }

    @Throws(WebDriverException::class)
    override suspend fun exists(selector: String) = predicateOnElement(selector, "exists") { it.nodeId != null || it.backendNodeId != null }

    /**
     * Wait until [selector] for [timeout] at most
     * */
    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeout: Duration, action: suspend () -> Unit): Duration {
        return waitUntil("waitForSelector", timeout) {
            val elementExists = exists(selector)
            if (!elementExists) {
                action()
            }
            elementExists
        }
    }

    @Throws(WebDriverException::class)
    private suspend fun waitForNavigationExperimental(oldUrl: String, timeout: Duration): Duration {
        val startTime = Instant.now()

        try {
            val channel = Channel<String>()

            pageAPI?.onDocumentOpened {
                val navigated = it.frame.url != oldUrl
                // emit(Navigation)
                channel.trySend("navigated")
            }

            channel.receive()
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "waitForNavigation $timeout")
        }

        return timeout - DateTimes.elapsedTime(startTime)
    }

    @Throws(WebDriverException::class)
    override suspend fun waitForPage(url: String, timeout: Duration): WebDriver? {
        return waitFor("waitForPage", timeout) { browser.findDriver(url) }
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
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "mouseWheelDown")
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
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "mouseWheelUp")
        }
    }

    /**
     * TODO: test is required
     * */
    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(x: Double, y: Double) {
        invokeOnPage("moveMouseTo") { mouse?.moveTo(x, y) }
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val node = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                page.scrollIntoViewIfNeeded(selector)
            } ?: return

            val offset = OffsetD(4.0, 4.0)
            val p = pageAPI
            val d = domAPI
            if (p != null && d != null) {
                rpc.invokeDeferred("moveMouseTo") {
                    val point = ClickableDOM(p, d, node, offset).clickablePoint().value
                    if (point != null) {
                        val point2 = PointD(point.x + deltaX, point.y + deltaY)
                        mouse?.moveTo(point2)
                    }
                    gap()
                }
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "moveMouseTo")
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
        invokeOnElement(selector, "click", scrollIntoView = true) { node ->
            click(node, count)
        }
    }

    private suspend fun click(node: NodeRef, count: Int, position: String = "center") {
        val deltaX = 4.0 + Random.nextInt(4)
        val deltaY = 4.0
        val offset = OffsetD(deltaX, deltaY)
        val minDeltaX = 2.0

        val p = pageAPI
        val d = domAPI
        if (p == null || d == null) {
            return
        }

        val clickableDOM = ClickableDOM(p, d, node, offset)
        val point = clickableDOM.clickablePoint().value ?: return
        val box = clickableDOM.boundingBox()
        val width = box?.width ?: 0.0
        // if it's an input element, we should click on the right side of the element,
        // so the cursor is at the tail of the text
        var offsetX = when (position) {
            "left" -> 0.0 + deltaX
            "right" -> width - deltaX
            else -> width / 2 + deltaX
        }
        offsetX = offsetX.coerceAtMost(width - minDeltaX).coerceAtLeast(minDeltaX)

        point.x += offsetX

        mouse?.click(point.x, point.y, count, randomDelayMillis("click"))
    }

    @Throws(WebDriverException::class)
    override suspend fun focus(selector: String) {
        // TODO: handle the minor exception: 0.	[focus] (3/5) | code: -32000, Element is not focusable
        // we can return false if the element is not focusable
        rpc.invokeDeferredSilently("focus") { page.focusOnSelector(selector) }
    }

    @Throws(WebDriverException::class)
    override suspend fun type(selector: String, text: String) {
        invokeOnElement(selector, "type") {
            val node = page.focusOnSelector(selector) ?: return@invokeOnElement
            click(node, 1)
            keyboard?.type(text, randomDelayMillis("type"))
            gap("type")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun fill(selector: String, text: String) {
        invokeOnElement(selector, "fill", focus = true) { node ->
            // val value = evaluateDetail("document.querySelector('$selector').value")?.value?.toString() ?: ""
            val value = page.getAttribute(node, "value")
            if (value != null) {
                // it's an input element, we should click on the right side of the element,
                // so the cursor appears at the tail of the text
                click(node, 1, "right")
                keyboard?.delete(value.length, randomDelayMillis("delete"))
                // ensure the input is empty
                // page.setAttribute(node, "value", "")
            }

            click(node, 1)
            // keyboard?.type(text, randomDelayMillis("fill"))
            // For fill, there is no delay between key presses
            keyboard?.type(text, 0)

            gap("fill")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun press(selector: String, key: String) {
        invokeOnElement(selector, "press", focus = true) { node ->
            keyboard?.press(key, randomDelayMillis("press"))
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollTo(selector: String) {
        rpc.invokeDeferredSilently("scrollTo") { page.scrollIntoViewIfNeeded(selector) }
    }

    @Throws(WebDriverException::class)
    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        try {
            val node = rpc.invokeDeferred("scrollIntoViewIfNeeded") {
                page.scrollIntoViewIfNeeded(selector)
            } ?: return

            // Use randomized offset like in click() for better anti-detection
            val deltaOffsetX = 4.0 + Random.nextInt(4)
            val deltaOffsetY = 4.0
            val offset = OffsetD(deltaOffsetX, deltaOffsetY)

            val p = pageAPI
            val d = domAPI
            if (p != null && d != null) {
                rpc.invokeDeferred("dragAndDrop") {
                    val clickableDOM = ClickableDOM(p, d, node, offset)
                    val startPoint = clickableDOM.clickablePoint().value
                    if (startPoint != null) {
                        // Calculate target point relative to start point
                        val targetPoint = PointD(startPoint.x + deltaX, startPoint.y + deltaY)
                        mouse?.dragAndDrop(startPoint, targetPoint, randomDelayMillis("dragAndDrop"))
                    }
                    gap()
                }
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "dragAndDrop")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun outerHTML(): String? {
        // https://github.com/platonai/browser4/issues/107
        return outerHTML(":root")
    }

    @Throws(WebDriverException::class)
    override suspend fun outerHTML(selector: String): String? {
        return invokeOnElement(selector, "outerHTML") { node ->
            domAPI?.getOuterHTML(node.nodeId, node.backendNodeId, node.objectId)
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun clickablePoint(selector: String): PointD? {
        try {
            return rpc.invokeDeferred("clickablePoint") {
                val node = page.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(pageAPI, domAPI, node)?.clickablePoint()?.value
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "clickablePoint")
        }

        return null
    }

    @Throws(WebDriverException::class)
    override suspend fun boundingBox(selector: String): RectD? {
        try {
            return rpc.invokeDeferred("boundingBox") {
                val node = page.scrollIntoViewIfNeeded(selector)
                ClickableDOM.create(pageAPI, domAPI, node)?.boundingBox()
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "boundingBox")
        }

        return null
    }

    /**
     * This method scrolls element into view if needed, and then uses
     * {@link screenshot.captureScreenshot} to take a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     *
     *
     */
    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(): String? {
        return try {
            rpc.invokeDeferred("captureScreenshot") { screenshot.captureScreenshot() }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "captureScreenshot")
            null
        }
    }

    /**
     * This method scrolls element into view if needed, and then uses
     * {@link page.captureScreenshot} to take a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(selector: String): String? {
        return try {
            val node = page.scrollIntoViewIfNeeded(selector) ?: return null
            // Force the page stop all navigations and pending resource fetches.
            rpc.invokeDeferred("captureScreenshot") { screenshot.captureScreenshot(selector) }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "captureScreenshot")
            null
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(rect: RectD): String? {
        return try {
            // Force the page stop all navigations and pending resource fetches.
            rpc.invokeDeferred("captureScreenshot") { screenshot.captureScreenshot(rect) }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, "captureScreenshot")
            null
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun pageSource(): String? {
        return invokeOnPage("pageSource") {
            // pageAPI?.getResourceContent(mainFrameAPI?.id, currentUrl())
            val document = domAPI?.getDocument() ?: return@invokeOnPage null
            domAPI?.getOuterHTML(document.nodeId, document.backendNodeId)
        }
    }

    override suspend fun bringToFront() {
        rpc.invokeDeferredSilently("bringToFront") {
            pageAPI?.bringToFront()
        }
    }

    override fun awaitTermination() {
        devTools.awaitTermination()
    }

    override suspend fun loadResource(url: String): NetworkResourceResponse {
        val options = LoadNetworkResourceOptions(
            disableCache = false, includeCredentials = false
        )

        val response = rpc.invokeDeferred("loadNetworkResource") {
            val frameId = pageAPI?.getFrameTree()?.frame?.id ?: return@invokeDeferred null
            val resource = networkAPI?.loadNetworkResource(frameId, url, options) ?: return@invokeDeferred null
            NetworkResourceResponse.from(resource)
        }

        return response ?: NetworkResourceResponse()
    }

    /**
     * Close the tab hold by this driver.
     * */
    override fun close() {
        browser.destroyDriver(this)
        doClose()
    }

    fun doClose() {
        super.close()

        if (closed.compareAndSet(false, true)) {
            devTools.runCatching { close() }.onFailure { warnForClose(this, it) }
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun pause() {
        invokeOnPage("pause") { pageAPI?.stopLoading() }
    }

    @Throws(WebDriverException::class)
    override suspend fun stop() {
        navigateEntry.stopped = true
        if (!isActive) {
            return
        }

        try {
            handleRedirect()

            if (browser.isGUI) {
                // in gui mode, just stop the loading, so we can diagnose
                pageAPI?.stopLoading()
            } else {
                // go to about:blank, so the browser stops the previous page and releases all resources
                navigateTo(ChromeImpl.ABOUT_BLANK_PAGE)
            }
        } catch (e: ChromeIOException) {
            if (!e.isOpen || !devTools.isOpen) {
                // ignored, since the chrome is closed
            }
        } catch (e: ChromeDriverException) {
            if (devTools.isOpen) {
                try {
                    rpc.handleChromeException(e, "terminate")
                } catch (e: Exception) {
                    logger.error("[Unexpected]", e)
                }
            }
        }
    }

    override fun toString() = "Driver#$id"

    /**
     *
     * */
    @Throws(ChromeIOException::class)
    suspend fun enableAPIAgents() {
        try {
            pageAPI?.enable()
            domAPI?.enable()
            runtimeAPI?.enable()
            networkAPI?.enable()
            cssAPI?.enable()

            if (resourceBlockProbability > 1e-6) {
                fetchAPI?.enable()
            }

            val proxyUsername = browser.id.fingerprint.proxyEntry?.username
            if (!proxyUsername.isNullOrBlank()) {
                // allow all url patterns
                val patterns = listOf(RequestPattern())
                fetchAPI?.enable(patterns, true)
            }
        } catch (e: Exception) {
            throw ChromeIOException("Failed to enable CDT agents", e)
        }
    }

    /**
     * Navigate to the page and inject scripts.
     * */
    private suspend fun navigateInvaded(entry: NavigateEntry) {
        val url = entry.url

        addScriptToEvaluateOnNewDocument()

        if (blockedURLs.isNotEmpty()) {
            // Blocks URLs from loading.
            networkAPI?.setBlockedURLs(blockedURLs)
        }

        networkManager.on1(NetworkEvents.RequestWillBeSent) { event: RequestWillBeSent ->
            onRequestWillBeSent(entry, event)
        }
        networkManager.on1(NetworkEvents.ResponseReceived) { event: ResponseReceived ->
            onResponseReceived(entry, event)
        }
        networkManager.on1(NetworkEvents.FrameNavigated) { event: FrameNavigated ->
            onFrameNavigated(entry, event)
        }

        pageAPI?.onDocumentOpened { entry.mainRequestCookies = getCookies0() }
        // TODO: seems not working
        pageAPI?.onWindowOpen { onWindowOpen(it) }
        // pageAPI?.onFrameAttached {  }
//        pageAPI?.onDomContentEventFired {  }

        val proxyEntry = browser.id.fingerprint.proxyEntry
        if (proxyEntry?.username != null) {
            credentials = Credentials(proxyEntry.username!!, proxyEntry.password)
            credentials?.let { networkManager.authenticate(it) }
        }

        navigateUrl = url
        if (URLUtils.isLocalFile(url)) {
            // serve local file, for example:
            // local file path:
            // C:\Users\pereg\AppData\Local\Temp\pulsar\test.txt
            // converted to:
            // http://localfile.org?path=QzpcVXNlcnNccGVyZWdcQXBwRGF0YVxMb2NhbFxUZW1wXHB1bHNhclx0ZXN0LnR4dA==
            //
            // DISCUSS: support URI format in the system, for example: file:///C:/Users/pereg/AppData/Local/Temp/pulsar/test.txt
            openLocalFile(url)
        } else {
            page.navigate(url, referrer = navigateEntry.pageReferrer)
        }
    }

    private suspend fun openLocalFile(url: String) {
        val path = URLUtils.localURLToPath(url)
        val uri = path.toUri()
        page.navigate(uri.toString())
    }

    @Deprecated("Use openLocalFile instead")
    private suspend fun openLocalFileDeprecated(url: String) {
        if (url.contains("?path=")) {
            val queryParams = URIBuilder(url).queryParams
            val path = queryParams.firstOrNull { it.name == "path" }?.value
            if (path != null) {
                val path2 = Base64.getUrlDecoder().decode(path).toString(Charsets.UTF_8)
                page.navigate(path2)
            }
            return
        }

        val url0 = url.removePrefix(AppConstants.LOCAL_FILE_BASE_URL)
        if (SystemUtils.IS_OS_WINDOWS) {
            page.navigate(url0)
        } else {
            page.navigate(url0)
        }
    }

    private fun onWindowOpen(event: WindowOpen) {
        val message = MessageFormat.format("Window opened | {0} | {1}", event.url, outgoingPages.size)
        println(" === =======  === === ")
        println(message)
//        logger.info("Window opened | {}", event.url)

        // TODO: handle BrowserUnavailableException
        val driver = browser.runCatching { newDriver(event.url) }.onFailure { warnInterruptible(this, it) }.getOrNull()
        if (driver != null) {
            driver.opener = this
            this.outgoingPages.add(driver)
        }
    }

    private suspend fun onRequestWillBeSent(entry: NavigateEntry, event: RequestWillBeSent) {
        if (!entry.url.startsWith("http")) {
            // This can happen for the following cases:
            // 1. non-http resources, for example, ftp, ws, etc.
            // 2. chrome's internal page, for example, about:blank, chrome://settings/, chrome://settings/system, etc.
            return
        }

        if (!URLUtils.isStandard(entry.url)) {
            logger.warn("Not a valid url | {}", entry.url)
            return
        }

        tracer?.trace("onRequestWillBeSent | driver | requestId: {}", event.requestId)

        val chromeNavigateEntry = ChromeNavigateEntry(navigateEntry)
        chromeNavigateEntry.updateStateBeforeRequestSent(event)

        // perform blocking logic
        val isMinor = chromeNavigateEntry.isMinorResource(event)
        if (isMinor && isBlocked(event.request.url)) {
            fetchAPI?.failRequest(event.requestId, ErrorReason.ABORTED)
        }

        // handle user-defined events
    }

    private fun isBlocked(url: String): Boolean {
        if (url in blockedURLs) {
            return true
        }

        if (resourceBlockProbability > 1e-6) {
            if (probabilisticBlockedURLs.any { url.matches(it.toRegex()) }) {
                return Random.nextInt(100) / 100.0f < resourceBlockProbability
            }
        }

        return false
    }

    private suspend fun onResponseReceived(entry: NavigateEntry, event: ResponseReceived) {
        val chromeNavigateEntry = ChromeNavigateEntry(entry)

        tracer?.trace("onResponseReceived | driver | {}", event.requestId)

        chromeNavigateEntry.updateStateAfterResponseReceived(event)

        if (logger.isDebugEnabled) {
            reportInterestingResources(entry, event)
        }

        // handle user-defined events
    }

    private suspend fun onFrameNavigated(entry: NavigateEntry, event: FrameNavigated) {
        val chromeNavigateEntry = ChromeNavigateEntry(entry)

        chromeNavigateEntry.updateStateAfterFrameNavigated(event)
    }

    private suspend fun reportInterestingResources(entry: NavigateEntry, event: ResponseReceived) {
        runCatching { traceInterestingResources0(entry, event) }.onFailure { warnInterruptible(this, it) }
    }

    private suspend fun traceInterestingResources0(entry: NavigateEntry, event: ResponseReceived) {
        val mimeType = event.response.mimeType
        val mimeTypes = listOf("application/json")
        if (mimeType !in mimeTypes) {
            return
        }

        val resourceTypes = listOf(
            ResourceType.FETCH,
            ResourceType.XHR,
            ResourceType.SCRIPT,
        )
        if (event.type !in resourceTypes) {
            // return
        }

        // page url is normalized
        val pageUrl = entry.pageUrl
        val resourceUrl = event.response.url
        val host = InternalURLUtil.getHost(pageUrl) ?: "unknown"
        val reportDir = messageWriter.reportDir.resolve("trace").resolve(host)

        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir)
        }

        val count = Files.list(reportDir).count()
        if (count > 2_000) {
            // TOO MANY tracing
            return
        }

        var suffix = "-" + event.type.name.lowercase() + "-urls.txt"
        var filename = AppPaths.fileId(pageUrl) + suffix
        var path = reportDir.resolve(filename)

        val message = String.format("%s\t%s", mimeType, event.response.url)
        messageWriter.writeTo(message, path)

        // configurable
        val saveResourceBody =
            mimeType == "application/json" && event.response.encodedDataLength < 1_000_000 && alwaysFalse()
        if (saveResourceBody) {
            val body = rpc.invokeSilently("getResponseBody") {
                fetchAPI?.enable()
                fetchAPI?.getResponseBody(event.requestId)?.body
            }
            if (!body.isNullOrBlank()) {
                suffix = "-" + event.type.name.lowercase() + "-body.txt"
                filename = AppPaths.fromUri(resourceUrl, suffix = suffix)
                path = reportDir.resolve(filename)
                messageWriter.writeTo(body, path)
            }
        }
    }

    private suspend fun handleRedirect() {
        val finalUrl = currentUrl()
        // redirect
        if (finalUrl.isNotBlank() && finalUrl != navigateUrl) {
            // browser.addHistory(NavigateEntry(finalUrl))
        }
    }

    private suspend fun addScriptToEvaluateOnNewDocument() {
        val js = settings.scriptLoader.getPreloadJs(false)
        if (js !in initScriptCache) {
            // utils comes first
            initScriptCache.add(0, js)
        }

        if (initScriptCache.isEmpty()) {
            logger.warn("No initScriptCache found")
            return
        }

        val scripts = initScriptCache.joinToString("\n;\n\n\n;\n")
        injectedScriptIdentifier = pageAPI?.addScriptToEvaluateOnNewDocument("\n;;\n$scripts\n;;\n")

        if (logger.isTraceEnabled) {
            reportInjectedJs(scripts)
        }

        // the cache is used for a single document, so we have to clear it
        initScriptCache.clear()
    }

    @Throws(WebDriverException::class)
    private suspend fun getCookies0(): List<Map<String, String>> {
        val cookies = networkAPI?.getCookies()?.map { serialize(it) }
        return cookies ?: listOf()
    }

    private fun serialize(cookie: ai.platon.cdt.kt.protocol.types.network.Cookie): Map<String, String> {
        val mapper = jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return mapper.readValue(mapper.writeValueAsString(cookie))
    }

    private suspend fun <T> invokeOnPage(name: String, message: String? = null, action: suspend () -> T): T? {
        try {
            return rpc.invokeDeferred(name) {
                action()
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, name, message)
        }

        return null
    }

    private suspend fun <T> invokeOnElement(
        selector: String,
        name: String,
        focus: Boolean = false,
        scrollIntoView: Boolean = false,
        action: suspend (NodeRef) -> T
    ): T? {
        try {
            return rpc.invokeDeferred(name) {
                val node = if (focus) {
                    page.focusOnSelector(selector)
                } else if (scrollIntoView) {
                    page.scrollIntoViewIfNeeded(selector)
                } else {
                    page.querySelector(selector)
                }

                if (node != null) {
                    action(node)
                } else {
                    null
                }
            }
        } catch (e: ChromeDriverException) {
            rpc.handleChromeException(e, name, "selector: [$selector], focus: $focus, scrollIntoView: $scrollIntoView")
        }

        return null
    }

    private suspend fun predicateOnElement(
        selector: String,
        name: String,
        focus: Boolean = false,
        scrollIntoView: Boolean = false,
        predicate: suspend (NodeRef) -> Boolean
    ): Boolean = invokeOnElement(selector, name, focus, scrollIntoView, predicate) == true

    private fun isValidNodeId(node: Int?): Boolean {
        return node != null && node > 0
    }

    private suspend fun cdpDeleteCookies(
        name: String, url: String? = null, domain: String? = null, path: String? = null
    ) {
        networkAPI?.deleteCookies(name, url, domain, path)
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
}
