package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.dom.nodes.GeoAnchor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.*
import com.microsoft.playwright.*
import org.jsoup.Connection
import java.time.Duration
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

/**
 * Playwright-based implementation of the WebDriver interface.
 */
class PlaywrightDriver(
    private val browserType: BrowserType,
    private val headless: Boolean = true
) : WebDriver {

    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private var context: BrowserContext? = null
    private var page: Page? = null
    private val id = System.identityHashCode(this)
    private val data = mutableMapOf<String, Any?>()
    private val delayPolicy = mapOf(
        "default" to 500..1000,
        "click" to 500..1000,
        "type" to 100..300,
        "scroll" to 200..500
    )
    private val timeoutPolicy = mapOf(
        "default" to Duration.ofSeconds(30),
        "navigation" to Duration.ofSeconds(30),
        "selector" to Duration.ofSeconds(10)
    )
    private var navigateEntry = NavigateEntry("")
    private val navigateHistory = NavigateHistory()
    private val frames = mutableListOf<WebDriver>()
    private var opener: WebDriver? = null
    private val outgoingPages = mutableSetOf<WebDriver>()

    init {
        launch()
    }

    private fun launch() {
        playwright = Playwright.create()
        browser = when (browserType) {
            BrowserType.PULSAR_CHROME -> playwright?.chromium()
            BrowserType.PULSAR_FIREFOX -> playwright?.firefox()
            BrowserType.PULSAR_SAFARI -> playwright?.webkit()
            else -> throw WebDriverException("Unsupported browser type: $browserType")
        }?.launch(BrowserType.LaunchOptions().setHeadless(headless))

        context = browser?.newContext()
        page = context?.newPage()

        // Set up event listeners
        page?.onFrameNavigated { frame ->
            if (frame == page?.mainFrame()) {
                navigateEntry = NavigateEntry(frame.url)
                navigateHistory.entries.add(navigateEntry)
            } else {
                val frameDriver = PlaywrightDriver(browserType, headless)
                frameDriver.page = frame
                frameDriver.context = context
                frameDriver.browser = browser
                frames.add(frameDriver)
            }
        }

        page?.onPopup { popup ->
            val popupDriver = PlaywrightDriver(browserType, headless)
            popupDriver.page = popup
            popupDriver.context = context
            popupDriver.browser = browser
            popupDriver.opener = this
            outgoingPages.add(popupDriver)
        }
    }

    override val id: Int
        get() = this.id

    override val browser: Browser
        get() = browser ?: throw WebDriverException("Browser not launched")

    override val frames: List<WebDriver>
        get() = this.frames

    override val opener: WebDriver?
        get() = this.opener

    override val outgoingPages: Set<WebDriver>
        get() = this.outgoingPages

    override val browserType: BrowserType
        get() = this.browserType

    override var navigateEntry: NavigateEntry
        get() = this.navigateEntry
        set(value) {
            this.navigateEntry = value
        }

    override val navigateHistory: NavigateHistory
        get() = this.navigateHistory

    override val data: MutableMap<String, Any?>
        get() = this.data

    override val delayPolicy: Map<String, IntRange>
        get() = this.delayPolicy

    override val timeoutPolicy: Map<String, Duration>
        get() = this.timeoutPolicy

    override fun jvm(): JvmWebDriver {
        return object : JvmWebDriver {
            override fun addInitScriptAsync(script: String) = CompletableFuture.completedFuture(Unit)
            override fun addBlockedURLsAsync(urls: List<String>) = CompletableFuture.completedFuture(Unit)
            override fun navigateToAsync(url: String) = CompletableFuture.completedFuture(Unit)
            override fun navigateToAsync(entry: NavigateEntry) = CompletableFuture.completedFuture(Unit)
            override fun setTimeoutsAsync(browserSettings: BrowserSettings) = CompletableFuture.completedFuture(Unit)
            override fun currentUrlAsync() = CompletableFuture.completedFuture("")
            override fun pageSourceAsync() = CompletableFuture.completedFuture("")
            override fun getCookiesAsync() = CompletableFuture.completedFuture(emptyList())
            override fun bringToFrontAsync() = CompletableFuture.completedFuture(Unit)
            override fun waitForSelectorAsync(selector: String) = CompletableFuture.completedFuture(Duration.ZERO)
            override fun waitForSelectorAsync(selector: String, timeoutMillis: Long) = CompletableFuture.completedFuture(0L)
            override fun waitForSelectorAsync(selector: String, timeout: Duration) = CompletableFuture.completedFuture(Duration.ZERO)
            override fun waitForNavigationAsync(oldUrl: String) = CompletableFuture.completedFuture(Duration.ZERO)
            override fun waitForNavigationAsync(oldUrl: String, timeoutMillis: Long) = CompletableFuture.completedFuture(0L)
            override fun waitForNavigationAsync(oldUrl: String, timeout: Duration) = CompletableFuture.completedFuture(Duration.ZERO)
            override fun existsAsync(selector: String) = CompletableFuture.completedFuture(false)
            override fun isVisibleAsync(selector: String) = CompletableFuture.completedFuture(false)
            override fun visibleAsync(selector: String) = CompletableFuture.completedFuture(false)
            override fun isHiddenAsync(selector: String) = CompletableFuture.completedFuture(true)
            override fun isCheckedAsync(selector: String) = CompletableFuture.completedFuture(false)
            override fun typeAsync(selector: String, text: String) = CompletableFuture.completedFuture(Unit)
            override fun clickAsync(selector: String, count: Int) = CompletableFuture.completedFuture(Unit)
            override fun clickMatchesAsync(selector: String, pattern: String, count: Int) = CompletableFuture.completedFuture(Unit)
            override fun clickMatchesAsync(selector: String, attrName: String, pattern: String, count: Int) = CompletableFuture.completedFuture(Unit)
            override fun mouseWheelDownAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) = CompletableFuture.completedFuture(Unit)
            override fun mouseWheelUpAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) = CompletableFuture.completedFuture(Unit)
            override fun moveMouseToAsync(x: Double, y: Double) = CompletableFuture.completedFuture(Unit)
            override fun moveMouseToAsync(selector: String, deltaX: Int, deltaY: Int) = CompletableFuture.completedFuture(Unit)
            override fun dragAndDropAsync(selector: String, deltaX: Int, deltaY: Int) = CompletableFuture.completedFuture(Unit)
            override fun clickablePointAsync(selector: String) = CompletableFuture.completedFuture(null)
            override fun boundingBoxAsync(selector: String) = CompletableFuture.completedFuture(null)
            override fun selectFirstTextOrNullAsync(selector: String) = CompletableFuture.completedFuture(null)
            override fun selectFirstAttributeOrNullAsync(selector: String, attrName: String) = CompletableFuture.completedFuture(null)
            override fun selectFirstAttributeOptionalAsync(selector: String, attrName: String) = CompletableFuture.completedFuture(Optional.empty())
            override fun selectAttributeAllAsync(selector: String, attrName: String) = CompletableFuture.completedFuture(emptyList())
            override fun evaluateAsync(expression: String) = CompletableFuture.completedFuture(null)
            override fun captureScreenshotAsync(selector: String) = CompletableFuture.completedFuture(null)
            override fun captureScreenshotAsync(rect: RectD) = CompletableFuture.completedFuture(null)
        }
    }

    override suspend fun addInitScript(script: String) {
        context?.addInitScript(script)
    }

    override suspend fun addBlockedURLs(urlPatterns: List<String>) {
        context?.route("**/*") { route ->
            if (urlPatterns.any { pattern -> route.request.url.contains(pattern) }) {
                route.abort()
            } else {
                route.continue_()
            }
        }
    }

    override suspend fun addProbabilityBlockedURLs(urlPatterns: List<String>) {
        context?.route("**/*") { route ->
            if (urlPatterns.any { pattern -> route.request.url.contains(pattern) && Random.nextBoolean() }) {
                route.abort()
            } else {
                route.continue_()
            }
        }
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
        if (browserSettings.navigationTimeout != null) {
            timeoutPolicy["navigation"] = Duration.ofMillis(browserSettings.navigationTimeout)
        }
        if (browserSettings.selectorTimeout != null) {
            timeoutPolicy["selector"] = Duration.ofMillis(browserSettings.selectorTimeout)
        }
    }

    override suspend fun navigateTo(url: String) {
        page?.goto(url)
        navigateEntry = NavigateEntry(url)
        navigateHistory.entries.add(navigateEntry)
    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        navigateTo(entry.url)
    }

    override suspend fun currentUrl(): String {
        return page?.url ?: throw WebDriverException("Page not initialized")
    }

    override suspend fun url(): String {
        return currentUrl()
    }

    override suspend fun documentURI(): String {
        return currentUrl()
    }

    override suspend fun baseURI(): String {
        return page?.evaluate("document.baseURI") as? String ?: currentUrl()
    }

    override suspend fun referrer(): String {
        return page?.evaluate("document.referrer") as? String ?: ""
    }

    override suspend fun pageSource(): String {
        return page?.content() ?: throw WebDriverException("Page not initialized")
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        return context?.cookies() ?: emptyList()
    }

    override suspend fun deleteCookies(name: String, url: String?, domain: String?, path: String?) {
        context?.clearCookies()
    }

    override suspend fun clearBrowserCookies() {
        context?.clearCookies()
    }

    override suspend fun exists(selector: String): Boolean {
        return try {
            page?.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(100)) != null
        } catch (e: TimeoutError) {
            false
        }
    }

    override suspend fun isVisible(selector: String): Boolean {
        val element = page?.querySelector(selector) ?: return false
        return element.isVisible()
    }

    override suspend fun isChecked(selector: String): Boolean {
        return page?.evaluate("""(selector) => {
            const element = document.querySelector(selector);
            return element ? element.checked : false;
        }""", selector) as? Boolean ?: false
    }

    override suspend fun bringToFront() {
        page?.bringToFront()
    }

    override suspend fun focus(selector: String) {
        page?.focus(selector)
    }

    override suspend fun type(selector: String, text: String) {
        page?.fill(selector, text)
        delay(randomDelayMillis("type"))
    }

    override suspend fun fill(selector: String, text: String) {
        page?.fill(selector, text)
        delay(randomDelayMillis("type"))
    }

    override suspend fun press(selector: String, key: String) {
        page?.press(selector, key)
        delay(randomDelayMillis("type"))
    }

    override suspend fun click(selector: String, count: Int) {
        page?.click(selector, Page.ClickOptions().setClickCount(count))
        delay(randomDelayMillis("click"))
    }

    override suspend fun clickTextMatches(selector: String, pattern: String, count: Int) {
        page?.evaluate("""(selector, pattern) => {
            const element = document.querySelector(selector);
            if (element && element.textContent.match(new RegExp(pattern))) {
                element.click();
            }
        }""", selector, pattern)
        delay(randomDelayMillis("click"))
    }

    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        page?.evaluate("""(selector, attrName, pattern) => {
            const element = document.querySelector(selector);
            if (element && element.getAttribute(attrName).match(new RegExp(pattern))) {
                element.click();
            }
        }""", selector, attrName, pattern)
        delay(randomDelayMillis("click"))
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        repeat(count) {
            page?.mouse?.wheel(deltaX, deltaY)
            delay(delayMillis)
        }
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        mouseWheelDown(count, -deltaX, -deltaY, delayMillis)
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        page?.mouse?.move(x, y)
    }

    override suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int) {
        val box = boundingBox(selector) ?: return
        page?.mouse?.move(box.x + deltaX, box.y + deltaY)
    }

    override suspend fun scrollTo(selector: String) {
        page?.evaluate("""(selector) => {
            const element = document.querySelector(selector);
            if (element) {
                element.scrollIntoView();
            }
        }""", selector)
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        val box = boundingBox(selector) ?: return
        page?.mouse?.down(box.x, box.y)
        page?.mouse?.move(box.x + deltaX, box.y + deltaY)
        page?.mouse?.up()
    }

    override suspend fun clickablePoint(selector: String): PointD? {
        val box = boundingBox(selector) ?: return null
        return PointD(box.x + box.width / 2, box.y + box.height / 2)
    }

    override suspend fun boundingBox(selector: String): RectD? {
        val box = page?.evaluate("""(selector) => {
            const element = document.querySelector(selector);
            if (!element) return null;
            const rect = element.getBoundingClientRect();
            return {
                x: rect.x,
                y: rect.y,
                width: rect.width,
                height: rect.height
            };
        }""", selector) as? Map<String, Double> ?: return null

        return RectD(
            box["x"] ?: 0.0,
            box["y"] ?: 0.0,
            box["width"] ?: 0.0,
            box["height"] ?: 0.0
        )
    }

    override suspend fun newJsoupSession(): Connection {
        TODO("Not implemented")
    }

    override suspend fun loadJsoupResource(url: String): Connection.Response {
        TODO("Not implemented")
    }

    override suspend fun loadResource(url: String): NetworkResourceResponse {
        TODO("Not implemented")
    }

    override suspend fun delay(millis: Long) {
        kotlinx.coroutines.delay(millis)
    }

    override suspend fun delay(duration: Duration) {
        kotlinx.coroutines.delay(duration.toMillis())
    }

    override fun close() {
        page?.close()
        context?.close()
        browser?.close()
        playwright?.close()
    }
}