package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.skeleton.crawl.fetch.driver.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.playwright.Page
import org.jsoup.Connection
import java.time.Duration
import java.util.*

data class Credentials(
    val username: String,
    val password: String?
)

/**
 * A Playwright-based implementation of the WebDriver interface.
 * This driver provides browser automation capabilities using the Playwright library.
 *
 * @property browser The PlaywrightBrowser instance
 * @property page The Playwright Page instance
 * @property settings Browser configuration settings
 */
class PlaywrightDriver(
    override val browser: PlaywrightBrowser,
    private val page: Page,
) : AbstractWebDriver(browser) {

    private val logger = getLogger(this)
    private val rpc = RobustRPC(this)

    override val browserType: BrowserType = BrowserType.PLAYWRIGHT_CHROME

    private var credentials: Credentials? = null

    private var navigateUrl = page.url() ?: ""

    override suspend fun addInitScript(script: String) {
        rpc.invokeDeferred("addInitScript") {
            page.addInitScript(script)
        }
    }

    override suspend fun addBlockedURLs(urlPatterns: List<String>) {
        rpc.invokeDeferred("addBlockedURLs") {
            page.route(urlPatterns.joinToString("|")) { route -> route.abort() }
        }
    }

    override suspend fun addProbabilityBlockedURLs(urlPatterns: List<String>) {
        // Implement probability-based blocking logic here
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
        // Implement timeout settings logic here
    }

    /**
     * Navigates to a URL without waiting for navigation to complete.
     * @param url The URL to navigate to
     * @throws RuntimeException if navigation fails
     */
    override suspend fun navigateTo(url: String) {
        rpc.invokeDeferred("navigateTo") {
            page.navigate(url)
        }
    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        navigateHistory.add(entry)
        this.navigateEntry = entry

        browser.emit(BrowserEvents.willNavigate, entry)

        try {
            rpc.invokeDeferred("navigateTo") {
                doNavigateTo(entry)
            }
        } catch (e: Exception) {
            rpc.handleException(e, "navigateTo", entry.url)
        }
    }

    /**
     * Navigate to the page and inject scripts.
     * */
    private fun doNavigateTo(entry: NavigateEntry) {
        val url = entry.url

        addScriptToEvaluateOnNewDocument()

        if (blockedURLs.isNotEmpty()) {
            // Blocks URLs from loading.
            // TODO: networkAPI?.setBlockedURLs(blockedURLs)
        }

        // TODO: add events
//        networkManager.on(NetworkEvents.RequestWillBeSent) { event: RequestWillBeSent ->
//            onRequestWillBeSent(entry, event)
//        }
//        networkManager.on(NetworkEvents.ResponseReceived) { event: ResponseReceived ->
//            onResponseReceived(entry, event)
//        }

        page.onResponse {
            // entry.mainRequestCookies = it.request()
        }

        // pageAPI?.onDocumentOpened { entry.mainRequestCookies = getCookies0() }
        // TODO: not working
        // pageAPI?.onWindowOpen { onWindowOpen(it) }
        // pageAPI?.onFrameAttached {  }
//        pageAPI?.onDomContentEventFired {  }

        val proxyEntry = browser.id.fingerprint.proxyEntry
        if (proxyEntry?.username != null) {
            credentials = Credentials(proxyEntry.username!!, proxyEntry.password)

            // credentials?.let { networkManager.authenticate(it) }
        }

        navigateUrl = url
        if (UrlUtils.isLocalFile(url)) {
            // serve local file, for example:
            // local file path:
            // C:\Users\pereg\AppData\Local\Temp\pulsar\test.txt
            // converted to:
            // http://localfile.org?path=QzpcVXNlcnNccGVyZWdcQXBwRGF0YVxMb2NhbFxUZW1wXHB1bHNhclx0ZXN0LnR4dA==
            openLocalFile(url)
        } else {
            page.navigate(url, Page.NavigateOptions().setReferer(navigateEntry.pageReferrer))
        }
    }

    private fun addScriptToEvaluateOnNewDocument() {
        val js = settings.scriptLoader.getPreloadJs(false)
        if (js !in initScriptCache) {
            // utils comes first
            initScriptCache.add(0, js)
        }

        val confuser = settings.confuser
        initScriptCache.forEach {
            page.addInitScript(confuser.confuse(it))
        }

        if (logger.isTraceEnabled) {
            reportInjectedJs()
        }

        // the cache is used for a single document, so we have to clear it
        initScriptCache.clear()
    }

    private fun openLocalFile(url: String) {
        val path = UrlUtils.localURLToPath(url)
        val uri = path.toUri()
        page.navigate(uri.toString())
    }

    override suspend fun currentUrl(): String {
        return rpc.invokeDeferred("currentUrl") {
            page.url()
        } ?: ""
    }

    override suspend fun url(): String {
        return rpc.invokeDeferred("url") {
            page.evaluate("document.URL") as String
        } ?: ""
    }

    override suspend fun documentURI(): String {
        return rpc.invokeDeferred("documentURI") {
            page.evaluate("document.documentURI") as String
        } ?: ""
    }

    override suspend fun baseURI(): String {
        return rpc.invokeDeferred("baseURI") {
            page.evaluate("document.baseURI") as String
        } ?: ""
    }

    override suspend fun referrer(): String {
        return rpc.invokeDeferred("referrer") {
            page.evaluate("document.referrer") as String
        } ?: ""
    }

    override suspend fun pageSource(): String? {
        return rpc.invokeDeferred("pageSource") {
            page.content()
        }
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        return rpc.invokeDeferred("getCookies") {
            val cookies = page.context().cookies()
            val mapper = jacksonObjectMapper()
            cookies.map { cookie ->
                val json = mapper.writeValueAsString(cookie)
                val map: Map<String, String?> = mapper.readValue(json)
                map.filterValues { it != null }.mapValues { it.toString() }
            }
        } ?: listOf()
    }

    override suspend fun deleteCookies(name: String) {
        rpc.invokeDeferred("deleteCookies") {
            page.context().clearCookies()
        }
    }

    override suspend fun deleteCookies(name: String, url: String?, domain: String?, path: String?) {
        rpc.invokeDeferred("deleteCookies") {
            page.context().clearCookies()
        }
    }

    override suspend fun clearBrowserCookies() {
        rpc.invokeDeferred("clearBrowserCookies") {
            page.context().clearCookies()
        }
    }

    override suspend fun waitForSelector(selector: String): Duration {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector)
        }
        return Duration.ZERO
    }

    override suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(timeoutMillis.toDouble()))
        }
        return timeoutMillis
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration): Duration {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(timeout.toMillis().toDouble()))
        }
        return timeout
    }

    override suspend fun waitForSelector(selector: String, action: suspend () -> Unit): Duration {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector)
            action()
        }
        return Duration.ZERO
    }

    override suspend fun waitForSelector(selector: String, timeoutMillis: Long, action: suspend () -> Unit): Long {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(timeoutMillis.toDouble()))
            action()
        }
        return timeoutMillis
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration, action: suspend () -> Unit): Duration {
        rpc.invokeDeferred("waitForSelector") {
            page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(timeout.toMillis().toDouble()))
            action()
        }
        return timeout
    }

    override suspend fun waitForPage(url: String, timeout: Duration): WebDriver? {
        rpc.invokeDeferred("waitForPage") {
            page.waitForURL(url, Page.WaitForURLOptions().setTimeout(timeout.toMillis().toDouble()))
        }
        return this
    }

    /**
     * Checks if an element exists in the DOM.
     * @param selector The CSS selector to check
     * @return true if the element exists, false otherwise
     */
    override suspend fun exists(selector: String): Boolean {
        return rpc.invokeDeferred("exists") {
            try {
                page.querySelector(selector) != null
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    override suspend fun isHidden(selector: String): Boolean {
        return rpc.invokeDeferred("isHidden") {
            !page.querySelector(selector).isVisible
        } ?: false
    }

    /**
     * Checks if an element is visible on the page.
     * @param selector The CSS selector to check
     * @return true if the element is visible, false otherwise
     */
    override suspend fun isVisible(selector: String): Boolean {
        return rpc.invokeDeferred("isVisible") {
            try {
                page.querySelector(selector).isVisible
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    override suspend fun isChecked(selector: String): Boolean {
        return rpc.invokeDeferred("isChecked") {
            page.querySelector(selector).isChecked
        } ?: false
    }

    override suspend fun bringToFront() {
        rpc.invokeDeferredSilently("bringToFront") {
            page.bringToFront()
        }
    }

    override suspend fun focus(selector: String) {
        rpc.invokeDeferred("focus") {
            page.querySelector(selector).focus()
        }
    }

    /**
     * Types text into an element.
     * @param selector The CSS selector of the element
     * @param text The text to type
     * @throws RuntimeException if typing fails
     */
    override suspend fun type(selector: String, text: String) {
        rpc.invokeDeferred("type") {
            page.querySelector(selector).type(text)
        }
    }

    /**
     * Fills an input element with text.
     * @param selector The CSS selector of the element
     * @param text The text to fill
     * @throws RuntimeException if filling fails
     */
    override suspend fun fill(selector: String, text: String) {
        rpc.invokeDeferred("fill") {
            page.querySelector(selector).fill(text)
        }
    }

    override suspend fun press(selector: String, key: String) {
        rpc.invokeDeferred("press") {
            page.querySelector(selector).press(key)
        }
    }

    /**
     * Clicks on an element matching the selector.
     * @param selector The CSS selector of the element to click
     * @param count The number of times to click
     * @throws RuntimeException if clicking fails
     */
    override suspend fun click(selector: String, count: Int) {
        rpc.invokeDeferred("click") {
            repeat(count) {
                page.querySelector(selector).click()
            }
        }
    }

    override suspend fun clickTextMatches(selector: String, pattern: String, count: Int) {
        rpc.invokeDeferred("clickTextMatches") {
            val elements = page.querySelectorAll(selector)
            elements.forEach { element ->
                if (element.textContent().contains(pattern)) {
                    repeat(count) {
                        element.click()
                    }
                }
            }
        }
    }

    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        rpc.invokeDeferred("clickMatches") {
            val elements = page.querySelectorAll(selector)
            elements.forEach { element ->
                if (element.getAttribute(attrName)?.contains(pattern) == true) {
                    repeat(count) {
                        element.click()
                    }
                }
            }
        }
    }

    override suspend fun clickNthAnchor(n: Int, rootSelector: String): String? {
        return rpc.invokeDeferred("clickNthAnchor") {
            val anchors = page.querySelectorAll("$rootSelector a")
            if (n < anchors.size) {
                anchors[n].click()
                anchors[n].getAttribute("href")
            } else {
                null
            }
        }
    }

    override suspend fun check(selector: String) {
        rpc.invokeDeferred("check") {
            page.querySelector(selector).check()
        }
    }

    override suspend fun uncheck(selector: String) {
        rpc.invokeDeferred("uncheck") {
            page.querySelector(selector).uncheck()
        }
    }

    override suspend fun scrollTo(selector: String) {
        rpc.invokeDeferredSilently("scrollTo") {
            page.querySelector(selector).scrollIntoViewIfNeeded()
        }
    }

    override suspend fun scrollDown(count: Int) {
        rpc.invokeDeferred("scrollDown") {
            repeat(count) {
                page.evaluate("window.scrollBy(0, window.innerHeight)")
            }
        }
    }

    override suspend fun scrollUp(count: Int) {
        rpc.invokeDeferred("scrollUp") {
            repeat(count) {
                page.evaluate("window.scrollBy(0, -window.innerHeight)")
            }
        }
    }

    override suspend fun scrollToTop() {
        rpc.invokeDeferred("scrollToTop") {
            page.evaluate("window.scrollTo(0, 0)")
        }
    }

    override suspend fun scrollToBottom() {
        rpc.invokeDeferred("scrollToBottom") {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        }
    }

    override suspend fun scrollToMiddle(ratio: Double) {
        rpc.invokeDeferred("scrollToMiddle") {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight * $ratio)")
        }
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        rpc.invokeDeferred("mouseWheelDown") {
            repeat(count) {
                page.mouse().wheel(deltaX, deltaY)
                delay(delayMillis)
            }
        }
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        rpc.invokeDeferred("mouseWheelUp") {
            repeat(count) {
                page.mouse().wheel(deltaX, deltaY)
                delay(delayMillis)
            }
        }
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        rpc.invokeDeferred("moveMouseTo") {
            page.mouse().move(x, y)
        }
    }

    override suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int) {
        rpc.invokeDeferred("moveMouseTo") {
            val element = page.querySelector(selector)
            val boundingBox = element.boundingBox()
            if (boundingBox != null) {
                page.mouse().move(boundingBox.x + deltaX, boundingBox.y + deltaY)
            }
        }
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        rpc.invokeDeferred("dragAndDrop") {
            val element = page.querySelector(selector)
            val boundingBox = element.boundingBox()
            if (boundingBox != null) {
                page.mouse().move(boundingBox.x, boundingBox.y)
                page.mouse().down()
                page.mouse().move(boundingBox.x + deltaX, boundingBox.y + deltaY)
                page.mouse().up()
            }
        }
    }

    override suspend fun outerHTML(): String? {
        return rpc.invokeDeferred("outerHTML") {
            page.content()
        }
    }

    override suspend fun outerHTML(selector: String): String? {
        return rpc.invokeDeferred("outerHTML") {
            page.querySelector(selector).innerHTML()
        }
    }

    override suspend fun selectFirstTextOrNull(selector: String): String? {
        return rpc.invokeDeferred("selectFirstTextOrNull") {
            page.querySelector(selector).textContent()
        }
    }

    override suspend fun selectTextAll(selector: String): List<String> {
        return rpc.invokeDeferred("selectTextAll") {
            page.querySelectorAll(selector).map { it.textContent() }
        } ?: listOf()
    }

    override suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String? {
        return rpc.invokeDeferred("selectFirstAttributeOrNull") {
            page.querySelector(selector).getAttribute(attrName)
        }
    }

    override suspend fun evaluate(expression: String): Any? {
        return rpc.invokeDeferred("evaluate") {
            page.evaluate(expression)
        }
    }

    override suspend fun <T> evaluate(expression: String, defaultValue: T): T {
        return rpc.invokeDeferred("evaluate") {
            (page.evaluate(expression) as? T) ?: defaultValue
        } ?: defaultValue
    }

    override suspend fun evaluateDetail(expression: String): JsEvaluation? {
        return rpc.invokeDeferred("evaluateDetail") {
            JsEvaluation(page.evaluate(expression))
        }
    }

    /**
     * Captures a screenshot of the current page.
     * @return The screenshot as a base64 encoded string
     * @throws RuntimeException if screenshot capture fails
     */
    override suspend fun captureScreenshot(): String? {
        return rpc.invokeDeferred("captureScreenshot") {
            Base64.getEncoder().encodeToString(page.screenshot())
        }
    }

    /**
     * Captures a screenshot of a specific element.
     * @param selector The CSS selector of the element
     * @return The screenshot as a base64 encoded string
     * @throws RuntimeException if screenshot capture fails
     */
    override suspend fun captureScreenshot(selector: String): String? {
        return rpc.invokeDeferred("captureScreenshot") {
            val sc = page.querySelector(selector).screenshot()
            Base64.getEncoder().encodeToString(sc)
        }
    }

    override suspend fun captureScreenshot(rect: RectD): String? {
        return rpc.invokeDeferred("captureScreenshot") {
            val sc = page.screenshot(Page.ScreenshotOptions().setClip(rect.x, rect.y, rect.width, rect.height))
            Base64.getEncoder().encodeToString(sc)
        }
    }

    override suspend fun clickablePoint(selector: String): PointD? {
        return rpc.invokeDeferred("clickablePoint") {
            val boundingBox = page.querySelector(selector).boundingBox()
            boundingBox?.let { PointD(it.x + it.width / 2, it.y + it.height / 2) }
        }
    }

    override suspend fun boundingBox(selector: String): RectD? {
        return rpc.invokeDeferred("boundingBox") {
            val box = page.querySelector(selector).boundingBox() ?: return@invokeDeferred null
            RectD(box.x, box.y, box.width, box.height)
        }
    }

    /**
     * Creates a new Jsoup session for making HTTP requests.
     * @return A new Jsoup Connection instance
     */
    override suspend fun newJsoupSession(): Connection {
        return rpc.invokeDeferred("newJsoupSession") {
            org.jsoup.Jsoup.newSession()
        } ?: throw WebDriverException("Failed to create Jsoup session")
    }

    /**
     * Loads a resource using Jsoup.
     * @param url The URL of the resource to load
     * @return The response from the resource
     * @throws RuntimeException if loading fails
     */
    override suspend fun loadJsoupResource(url: String): Connection.Response {
        return rpc.invokeDeferred("loadJsoupResource") {
            org.jsoup.Jsoup.connect(url).execute()
        } ?: throw WebDriverException("Failed to load Jsoup resource: $url")
    }

    /**
     * Loads a network resource.
     * @param url The URL of the resource to load
     * @return The network resource response
     * @throws RuntimeException if loading fails
     */
    override suspend fun loadResource(url: String): NetworkResourceResponse {
        return rpc.invokeDeferred("loadResource") {
            val response = page.context().request().get(url)
            NetworkResourceResponse(
                success = response.ok(),
                httpStatusCode = response.status(),
                stream = response.body()?.let { String(it) },
                headers = response.headers()
            )
        } ?: throw WebDriverException("Failed to load resource: $url")
    }

    override suspend fun pause() {
        rpc.invokeDeferredSilently("pause") {
            page.pause()
        }
    }

    override suspend fun stop() {
        rpc.invokeDeferredSilently("stop") {
            page.close()
        }
    }

    /**
     * Closes the browser page and releases associated resources.
     */
    override fun close() {
        try {
            page.close()
            browser.close()
        } catch (e: Exception) {
            logger.warn("Error during close: ${e.message}")
        }
    }
}
