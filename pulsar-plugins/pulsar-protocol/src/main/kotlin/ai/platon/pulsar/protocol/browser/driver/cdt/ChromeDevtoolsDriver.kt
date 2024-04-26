package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.driver.*
import ai.platon.pulsar.protocol.browser.driver.cdt.detail.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceived
import com.github.kklisura.cdt.protocol.v2023.events.page.WindowOpen
import com.github.kklisura.cdt.protocol.v2023.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.v2023.types.network.Cookie
import com.github.kklisura.cdt.protocol.v2023.types.network.ErrorReason
import com.github.kklisura.cdt.protocol.v2023.types.network.LoadNetworkResourceOptions
import com.github.kklisura.cdt.protocol.v2023.types.network.ResourceType
import com.github.kklisura.cdt.protocol.v2023.types.runtime.Evaluate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.commons.lang3.SystemUtils
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import java.nio.file.Files
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class ChromeDevtoolsDriver(
    val chromeTab: ChromeTab,
    val devTools: RemoteDevTools,
    private val browserSettings: BrowserSettings,
    override val browser: ChromeDevtoolsBrowser,
) : AbstractWebDriver(browser) {
    
    companion object {
        val LOCALHOST_PREFIX = "http://localfile.org"
    }
    
    private val logger = getLogger(this)
    
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    
    override val browserType: BrowserType = BrowserType.PULSAR_CHROME
    
    /**
     * The probability to block a resource request if the request url is in probabilisticBlockedURLs.
     * The probability must be in [0, 1].
     * */
    val resourceBlockProbability get() = browserSettings.resourceBlockProbability
    
    private val _blockedURLs = mutableListOf<String>()
    private val _probabilityBlockedURLs = mutableListOf<String>()
    val blockedURLs: List<String> get() = _blockedURLs
    val probabilisticBlockedURLs: List<String> get() = _probabilityBlockedURLs
    
    /**
     * TODO: distinguish the navigateUrl, currentUrl, chromeTab.url, mainFrameAPI.url, dom.document.documentURL, dom.document.baseURL
     * */
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
    
    private val rpc = RobustRPC(this)
    private val page = PageHandler(devTools, browserSettings.confuser)
    private val mouse get() = page.mouse.takeIf { isActive }
    private val keyboard get() = page.keyboard.takeIf { isActive }
    private val screenshot = Screenshot(page, devTools)
    
    private var credentials: Credentials? = null
    
    private val networkManager by lazy { NetworkManager(this, rpc) }
    private val messageWriter = MiscMessageWriter()
    
    private val enableStartupScript get() = browserSettings.isStartupScriptEnabled
    private val initScriptCache = mutableListOf<String>()
    private val closed = AtomicBoolean()
    
    val isGone get() = closed.get() || isQuit || !AppContext.isActive || !devTools.isOpen
    val isActive get() = !isGone
    
    /**
     * Expose the underlying implementation, used for diagnosis purpose
     * */
    val implementation get() = devTools
    
    init {
        val userAgent = browser.userAgentOverride
        if (!userAgent.isNullOrEmpty()) {
            emulationAPI?.setUserAgentOverride(userAgent)
        }
    }
    
    override suspend fun addInitScript(script: String) {
        initScriptCache.add(script)
    }
    
    override suspend fun addBlockedURLs(urlPatterns: List<String>) {
        _blockedURLs.addAll(urlPatterns)
    }
    
    override suspend fun addProbabilityBlockedURLs(urlPatterns: List<String>) {
        _probabilityBlockedURLs.addAll(urlPatterns)
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
    
    @Throws(WebDriverException::class)
    override suspend fun getCookies(): List<Map<String, String>> {
        return invokeOnPage("getCookies") { getCookies0() } ?: listOf()
    }
    
    override suspend fun deleteCookies(name: String) {
        invokeOnPage("deleteCookies") { networkAPI?.deleteCookies(name) }
    }
    
    override suspend fun deleteCookies(name: String, url: String?, domain: String?, path: String?) {
        invokeOnPage("deleteCookies") { networkAPI?.deleteCookies(name, url, domain, path) }
    }
    
    override suspend fun clearBrowserCookies() {
        invokeOnPage("clearBrowserCookies") { networkAPI?.clearBrowserCookies() }
    }
    
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
    override suspend fun currentUrl(): String {
        navigateUrl = invokeOnPage("currentUrl") { mainFrameAPI?.url } ?: navigateUrl
        return navigateUrl
    }
    
    @Throws(WebDriverException::class)
    override suspend fun exists(selector: String) = predicateOnElement(selector, "exists") { it > 0 }
    
    /**
     * Wait until [selector] for [timeout] at most
     * */
    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeout: Duration, action: suspend () -> Unit): Duration {
        return waitUntil("waitForSelector", timeout) { exists(selector).apply { ifFalse { action() } } }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(oldUrl: String, timeout: Duration): Duration {
        // TODO: listen to the navigation event
        return waitUntil("waitForNavigation", timeout) { isNavigated(oldUrl) }
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
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "waitForNavigation $timeout")
        }
        
        return timeout - DateTimes.elapsedTime(startTime)
    }
    
    @Throws(WebDriverException::class)
    override suspend fun waitForPage(url: String, timeout: Duration): WebDriver? {
        return waitFor("waitForPage", timeout) { browser.findDriver(url) }
    }
    
    override suspend fun waitUntil(timeout: Duration, predicate: suspend () -> Boolean) =
        waitUntil("waitUtil", timeout, predicate)
    
    private suspend fun waitUntil(type: String, timeout: Duration, predicate: suspend () -> Boolean): Duration {
        val startTime = Instant.now()
        var elapsedTime = Duration.ZERO
        
        // it's OK to wait using a while loop, because all the operations are coroutines
        while (elapsedTime < timeout && !predicate()) {
            gap(type)
            elapsedTime = DateTimes.elapsedTime(startTime)
        }
        
        return timeout - elapsedTime
    }
    
    private suspend fun <T> waitFor(type: String, timeout: Duration, supplier: suspend () -> T): T? {
        val startTime = Instant.now()
        var elapsedTime = Duration.ZERO
        var result: T? = supplier()
        
        // it's OK to wait using a while loop, because all the operations are coroutines
        while (elapsedTime < timeout && result == null) {
            gap(type)
            result = supplier()
            elapsedTime = DateTimes.elapsedTime(startTime)
        }
        
        return result
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
        invokeOnPage("moveMouseTo") { mouse?.moveTo(x, y) }
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
        invokeOnElement(selector, "click", scrollIntoView = true) { nodeId ->
            click(nodeId, count)
        }
    }
    
    private suspend fun click(nodeId: Int, count: Int, position: String = "center") {
        val deltaX = 4.0 + Random.nextInt(4)
        val deltaY = 4.0
        val offset = OffsetD(deltaX, deltaY)
        val minDeltaX = 2.0
        
        val p = pageAPI
        val d = domAPI
        if (p == null || d == null) {
            return
        }
        
        val clickableDOM = ClickableDOM(p, d, nodeId, offset)
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
        rpc.invokeDeferredSilently("focus") { page.focusOnSelector(selector) }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun type(selector: String, text: String) {
        try {
            rpc.invokeDeferred("type") {
                val nodeId = page.focusOnSelector(selector)
                if (nodeId > 0) {
                    click(nodeId, 1)
                    keyboard?.type(text, randomDelayMillis("type"))
                    gap("type")
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "type")
        }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun fill(selector: String, text: String) {
        invokeOnElement(selector, "fill", focus = true) { nodeId ->
            // val value = evaluateDetail("document.querySelector('$selector').value")?.value?.toString() ?: ""
            val value = page.getAttribute(nodeId, "value")
            if (value != null) {
                // it's an input element, we should click on the right side of the element,
                // so the cursor appears at the tail of the text
                click(nodeId, 1, "right")
                keyboard?.delete(value.length, randomDelayMillis("delete"))
                // ensure the input is empty
                // page.setAttribute(nodeId, "value", "")
            }
            
            click(nodeId, 1)
            keyboard?.type(text, randomDelayMillis("type"))
        }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun press(selector: String, key: String) {
        invokeOnElement(selector, "press", focus = true) { nodeId ->
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
                        mouse?.dragAndDrop(point, point2, randomDelayMillis("dragAndDrop"))
                    }
                    gap()
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "dragAndDrop")
        }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun outerHTML(): String? {
        return invokeOnPage("outerHTML") { domAPI?.outerHTML }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun outerHTML(selector: String): String? {
        return invokeOnElement(selector, "outerHTML") { nodeId ->
            domAPI?.getOuterHTML(nodeId, null, null)
        }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun clickablePoint(selector: String): PointD? {
//        invokeOnElementOrNull(selector, "clickablePoint") { nodeId ->
//            ClickableDOM.create(pageAPI, domAPI, nodeId)?.clickablePoint()?.value
//        }
        
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
            val nodeId = page.scrollIntoViewIfNeeded(selector) ?: return null
            // Force the page stop all navigations and pending resource fetches.
            rpc.invokeDeferred("stopLoading") { pageAPI?.stopLoading() }
            rpc.invokeDeferred("captureScreenshot") { screenshot.captureScreenshot(selector) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }
    
    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(rect: RectD): String? {
        return try {
            // Force the page stop all navigations and pending resource fetches.
            rpc.invokeDeferred("stopLoading") { pageAPI?.stopLoading() }
            rpc.invokeDeferred("captureScreenshot") { screenshot.captureScreenshot(rect) }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, "captureScreenshot")
            null
        }
    }
    
    @Throws(IllegalWebDriverStateException::class)
    internal fun checkState(action: String = ""): Boolean {
        if (!isActive) {
            return false
            // throw IllegalWebDriverStateException("WebDriver is not active #$id | $navigateUrl", this)
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
    
    @Throws(WebDriverException::class)
    override suspend fun pageSource(): String? {
        return invokeOnPage("pageSource") { domAPI?.getOuterHTML(domAPI?.document?.nodeId, null, null) }
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
        val options = LoadNetworkResourceOptions().apply {
            disableCache = false
            includeCredentials = false
        }
        
        val frameId = pageAPI?.frameTree?.frame?.id
        val response = rpc.invokeDeferred("loadNetworkResource") {
            val resource = networkAPI?.loadNetworkResource(frameId, url, options)
            resource?.let {
                NetworkResourceResponse.from(it)
            }
        }
        
        return response ?: NetworkResourceResponse()
    }
    
    /**
     * Close the tab hold by this driver.
     * */
    override fun close() {
        // state should not be ready, working
//        if (state.get() == WebDriver.State.READY || state.get() == WebDriver.State.WORKING) {
//            logger.warn("Illegal driver state before close | {}", state.get())
//        }
        
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
    
    override fun toString() = "Driver#$id"
    
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
    
    /**
     * Navigate to the page and inject scripts.
     * */
    private fun navigateInvaded(entry: NavigateEntry) {
        val url = entry.url
        
        addScriptToEvaluateOnNewDocument()
        
        if (blockedURLs.isNotEmpty()) {
            // Blocks URLs from loading.
            networkAPI?.setBlockedURLs(blockedURLs)
        }
        
        networkManager.on(NetworkEvents.RequestWillBeSent) { event: RequestWillBeSent ->
            onRequestWillBeSent(entry, event)
        }
        networkManager.on(NetworkEvents.ResponseReceived) { event: ResponseReceived ->
            onResponseReceived(entry, event)
        }
        
        pageAPI?.onDocumentOpened { entry.mainRequestCookies = getCookies0() }
        // TODO: not working
        pageAPI?.onWindowOpen { onWindowOpen(it) }
        // pageAPI?.onFrameAttached {  }
//        pageAPI?.onDomContentEventFired {  }
        
        val proxyUsername = browser.id.fingerprint.proxyUsername
        if (!proxyUsername.isNullOrBlank()) {
            credentials = Credentials(proxyUsername, browser.id.fingerprint.proxyPassword)
            credentials?.let { networkManager.authenticate(it) }
        }

        navigateUrl = url
        // TODO: This is a temporary solution to serve local file, for example, file:///tmp/example.html
        if (LOCALHOST_PREFIX in url) {
            openLocalFile(url)
        } else {
            page.navigate(url, referrer = navigateEntry.pageReferrer)
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
    
    private fun openLocalFile(url: String) {
        if (url.contains("?path=")) {
            val queryParams = URIBuilder(url).queryParams
            val path = queryParams.firstOrNull { it.name == "path" }?.value
            if (path != null) {
                val path2 = Base64.getUrlDecoder().decode(path).toString(Charsets.UTF_8)
                page.navigate(path2)
            }
            return
        }

        val url0 = url.removePrefix(LOCALHOST_PREFIX)
        if (SystemUtils.IS_OS_WINDOWS) {
            page.navigate(url0)
        } else {
            page.navigate("file:///$url0")
        }
    }
    
    private fun onWindowOpen(event: WindowOpen) {
        val message = MessageFormat.format("Window opened | {0} | {1}", event.url, outgoingPages.size)
        println(" === =======  === === ")
        println(message)
//        logger.info("Window opened | {}", event.url)
        
        val driver = browser.runCatching { newDriver(event.url) }.onFailure { warnInterruptible(this, it) }.getOrNull()
        if (driver != null) {
            driver.opener = this
            this.outgoingPages.add(driver)
        }
    }
    
    private fun onRequestWillBeSent(entry: NavigateEntry, event: RequestWillBeSent) {
        if (!entry.url.startsWith("http")) {
            // This can happen for the following cases:
            // 1. non-http resources, for example, ftp, ws, etc.
            // 2. chrome's internal page, for example, about:blank, chrome://settings/, chrome://settings/system, etc.
            return
        }
        
        if (!UrlUtils.isStandard(entry.url)) {
            logger.warn("Not a valid url | {}", entry.url)
            return
        }
        
        tracer?.trace("onRequestWillBeSent | driver | {}", event.requestId)
        
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
    
    private fun onResponseReceived(entry: NavigateEntry, event: ResponseReceived) {
        val chromeNavigateEntry = ChromeNavigateEntry(entry)
        
        tracer?.trace("onResponseReceived | driver | {}", event.requestId)
        
        chromeNavigateEntry.updateStateAfterResponseReceived(event)
        
        if (logger.isDebugEnabled) {
            reportInterestingResources(entry, event)
        }
        
        // handle user-defined events
    }
    
    private fun reportInterestingResources(entry: NavigateEntry, event: ResponseReceived) {
        runCatching { traceInterestingResources0(entry, event) }.onFailure { warnInterruptible(this, it) }
    }
    
    private fun traceInterestingResources0(entry: NavigateEntry, event: ResponseReceived) {
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
        val host = URLUtil.getHost(pageUrl) ?: "unknown"
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
        val saveResourceBody = mimeType == "application/json"
            && event.response.encodedDataLength < 1_000_000
            && alwaysFalse()
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
        
        // the cache is used for a single document, so we have to clear it
        initScriptCache.clear()
    }
    
    @Throws(WebDriverException::class)
    private fun getCookies0(): List<Map<String, String>> {
        val cookies = networkAPI?.cookies?.map { serialize(it) }
        return cookies ?: listOf()
    }
    
    private fun serialize(cookie: Cookie): Map<String, String> {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(cookie)
        val map: Map<String, String?> = mapper.readValue(json)
        return map.filterValues { it != null }.mapValues { it.toString() }
    }
    
    private fun reportInjectedJs() {
        val script = browserSettings.confuser.confuse(initScriptCache.joinToString("\n;\n\n\n;\n"))
        
        val dir = browser.id.contextDir.resolve("driver.$id/js")
        Files.createDirectories(dir)
        val report = Files.writeString(dir.resolve("preload.all.js"), script)
        tracer?.trace("All injected js: file://{}", report)
    }
    
    /**
     * Delays the coroutine for a given time without blocking a thread and resumes it after a specified time.
     *
     * This suspending function is cancellable. If the Job of the current coroutine is cancelled or completed while
     * this suspending function is waiting, this function immediately resumes with CancellationException.
     * */
    @Throws(IllegalWebDriverStateException::class)
    private suspend fun gap() {
        if (!isActive) {
            // throw IllegalWebDriverStateException("WebDriver is not active #$id | $navigateUrl", this)
        }
        
        // Delays coroutine for a given time without blocking a thread and resumes it after a specified time.
        delay(randomDelayMillis("gap"))
    }
    
    /**
     * Delays the coroutine for a given time without blocking a thread and resumes it after a specified time.
     *
     * This suspending function is cancellable. If the Job of the current coroutine is cancelled or completed while
     * this suspending function is waiting, this function immediately resumes with CancellationException.
     * */
    @Throws(IllegalWebDriverStateException::class)
    private suspend fun gap(type: String) {
        if (!isActive) {
            // throw IllegalWebDriverStateException("WebDriver is not active #$id | $navigateUrl", this)
        }
        
        delay(randomDelayMillis(type))
    }
    
    /**
     * Delays the coroutine for a given time without blocking a thread and resumes it after a specified time.
     *
     * This suspending function is cancellable. If the Job of the current coroutine is cancelled or completed while
     * this suspending function is waiting, this function immediately resumes with CancellationException.
     * */
    @Throws(IllegalWebDriverStateException::class)
    private suspend fun gap(millis: Long) {
        if (!isActive) {
            // throw IllegalWebDriverStateException("WebDriver is not active #$id | $navigateUrl", this)
        }
        
        delay(millis)
    }
    
    private suspend fun <T> invokeOnPage(name: String, message: String? = null, action: suspend () -> T): T? {
        try {
            return rpc.invokeDeferred(name) {
                action()
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, name, message)
        }
        
        return null
    }
    
    private suspend fun <T> invokeOnElement(
        selector: String, name: String, focus: Boolean = false, scrollIntoView: Boolean = false,
        action: suspend (Int) -> T
    ): T? {
        try {
            return rpc.invokeDeferred(name) {
                val nodeId = if (focus) {
                    page.focusOnSelector(selector)
                } else if (scrollIntoView) {
                    page.scrollIntoViewIfNeeded(selector)
                } else {
                    page.querySelector(selector)
                }
                
                if (nodeId != null && nodeId > 0) {
                    action(nodeId)
                } else {
                    null
                }
            }
        } catch (e: ChromeRPCException) {
            rpc.handleRPCException(e, name, "selector: [$selector], focus: $focus, scrollIntoView: $scrollIntoView")
        }
        
        return null
    }
    
    private suspend fun predicateOnElement(
        selector: String, name: String, focus: Boolean = false, scrollIntoView: Boolean = false,
        predicate: suspend (Int) -> Boolean
    ): Boolean = invokeOnElement(selector, name, focus, scrollIntoView, predicate) == true
    
    @Throws(WebDriverException::class)
    private suspend fun isNavigated(oldUrl: String): Boolean {
        return oldUrl != currentUrl()
    }
    
    private fun isValidNodeId(nodeId: Int?): Boolean {
        return nodeId != null && nodeId > 0
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
