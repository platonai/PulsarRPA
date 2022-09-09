package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

open class WebDriverException(
    message: String? = null,
    val driver: WebDriver? = null,
    cause: Throwable? = null
): RuntimeException(message, cause) {

    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}

open class WebDriverCancellationException(
    message: String? = null,
    driver: WebDriver? = null,
    cause: Throwable? = null
): WebDriverException(message, driver, cause) {
    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
data class NavigateEntry(
    /**
     * The url to navigate to.
     * If page.href exists, the url is the href, otherwise, the url is page.url.
     * The href has the higher priority to locate a resource.
     * */
    val url: String,
    /**
     * The page id, 0 means there is no WebPage.
     * */
    val pageId: Int = 0,
    /**
     * The page url which can be used to retrieve the WebPage from database. An empty string means there is no WebPage.
     * */
    val pageUrl: String = "",
    /**
     * The referer claimed by the page.
     */
    var pageReferrer: String? = null,
    /**
     * The location of the page, it shows in the browser window, can differ from url.
     */
    var location: String = url,
    /**
     * Indicate if the driver be stopped.
     */
    var stopped: Boolean = false,
    /**
     * The last active time.
     */
    var lastActiveTime: Instant = Instant.now(),
    /**
     * The time when the object is created.
     */
    val createTime: Instant = Instant.now(),
) {
    /**
     * The time when the document is ready.
     */
    var documentReadyTime = Instant.MAX
    /**
     * Track the time of page actions.
     */
    val actionTimes = mutableMapOf<String, Instant>()
    /**
     * Refresh the entry with the given action.
     * */
    fun refresh(action: String) {
        val now = Instant.now()
        lastActiveTime = now
        if (action.isNotBlank()) {
            actionTimes[action] = now
        }
    }
}

/**
 * Similar to puppeteer's Page
 * */
interface WebDriver: Closeable {
    enum class Status {
        UNKNOWN, FREE, WORKING, CANCELED, RETIRED, CRASHED, QUIT;

        val isFree get() = this == FREE
        val isWorking get() = this == WORKING
        val isCanceled get() = this == CANCELED
        val isRetired get() = this == RETIRED
        val isCrashed get() = this == CRASHED
        val isQuit get() = this == QUIT
    }

    val id: Int
    val name: String
    val browser: Browser
    val browserId: BrowserId get() = browser.id

    var navigateEntry: NavigateEntry
    val navigateHistory: MutableList<NavigateEntry>

    val browserType: BrowserType
    val supportJavascript: Boolean
    val isMockedPageSource: Boolean
    val status: AtomicReference<Status>

    val lastActiveTime: Instant
    var idleTimeout: Duration
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    var waitForTimeout: Duration

    val isCanceled: Boolean
    val isWorking: Boolean
    val isQuit: Boolean
    val isRetired: Boolean
    val isFree: Boolean
    val isCrashed: Boolean

    val sessionId: String?
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }

    /**
     * Adds a script which would be evaluated in one of the following scenarios:
     * <ul>
     * <li> Whenever the page is navigated.</li>
     * </ul>
     *
     * <p> The script is evaluated after the document was created but before any of
     * its scripts were run. This is useful to amend the JavaScript environment, e.g.
     * to seed {@code Math.random}.
     * */
    @Throws(WebDriverException::class)
    suspend fun addInitScript(script: String)
    /**
     * Blocks URLs from loading.
     *
     * @param urls URL patterns to block. Wildcards ('*') are allowed.
     */
    @Throws(WebDriverException::class)
    suspend fun addBlockedURLs(urls: List<String>)
    /**
     * Returns the main resource response. In case of multiple redirects, the navigation will resolve with the first
     * non-redirect response.
     *
     * <p> The method will not throw an error when any valid HTTP status code is returned by the remote server,
     * including 404 "Not Found" and 500 "Internal Server Error".
     *
     * @param url URL to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(url: String)
    /**
     * Returns the main resource response. In case of multiple redirects, the navigation will resolve with the first
     * non-redirect response.
     *
     * <p> The method will not throw an error when any valid HTTP status code is returned by the remote server,
     * including 404 "Not Found" and 500 "Internal Server Error".
     *
     * @param entry NavigateEntry to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(entry: NavigateEntry)

    @Throws(WebDriverException::class)
    suspend fun setTimeouts(browserSettings: BrowserSettings)

    @Throws(WebDriverException::class)
    suspend fun currentUrl(): String
    @Throws(WebDriverException::class)
    suspend fun pageSource(): String?
    @Throws(WebDriverException::class)
    suspend fun mainRequestHeaders(): Map<String, Any>
    @Throws(WebDriverException::class)
    suspend fun mainRequestCookies(): List<Map<String, String>>
    @Throws(WebDriverException::class)
    suspend fun getCookies(): List<Map<String, String>>

    /**
     * Brings page to front (activates tab).
     */
    @Throws(WebDriverException::class)
    suspend fun bringToFront()
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String): Long
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeout: Duration): Long
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(): Long
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeoutMillis: Long): Long
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeout: Duration): Long

    @Throws(WebDriverException::class)
    suspend fun clickablePoint(selector: String): PointD?
    @Throws(WebDriverException::class)
    suspend fun boundingBox(selector: String): RectD?

    @Throws(WebDriverException::class)
    suspend fun exists(selector: String): Boolean
    @Throws(WebDriverException::class)
    suspend fun visible(selector: String): Boolean
    @Throws(WebDriverException::class)
    suspend fun type(selector: String, text: String)
    @Throws(WebDriverException::class)
    suspend fun click(selector: String, count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun clickMatches(selector: String, pattern: String, count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun clickNthAnchor(n: Int, rootSelector: String = "body"): String?
    @Throws(WebDriverException::class)
    suspend fun scrollTo(selector: String)
    @Throws(WebDriverException::class)
    suspend fun scrollDown(count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun scrollUp(count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun scrollToTop()
    @Throws(WebDriverException::class)
    suspend fun scrollToBottom()
    @Throws(WebDriverException::class)
    suspend fun scrollToMiddle(ratio: Float)
    @Throws(WebDriverException::class)
    suspend fun mouseWheelDown(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0)
    @Throws(WebDriverException::class)
    suspend fun mouseWheelUp(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0)
    @Throws(WebDriverException::class)
    suspend fun moveMouseTo(x: Double, y: Double)
    @Throws(WebDriverException::class)
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)

    @Throws(WebDriverException::class)
    suspend fun outerHTML(selector: String): String?
    @Throws(WebDriverException::class)
    suspend fun firstText(selector: String): String?
    @Throws(WebDriverException::class)
    suspend fun allTexts(selector: String): List<String>
    @Throws(WebDriverException::class)
    suspend fun firstAttr(selector: String, attrName: String): String?
    @Throws(WebDriverException::class)
    suspend fun allAttrs(selector: String, attrName: String): List<String>

    @Throws(WebDriverException::class)
    suspend fun evaluate(expression: String): Any?
    @Throws(WebDriverException::class)
    suspend fun evaluateSilently(expression: String): Any?

    @Throws(WebDriverException::class)
    suspend fun newSession(): Connection
    @Throws(WebDriverException::class)
    suspend fun loadResource(url: String): Connection.Response?

    /**
     * This method scrolls element into view if needed, and then ake a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(selector: String): String?
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(rect: RectD): String?
    /** Force the page stop all navigations and pending resource fetches. */
    @Throws(WebDriverException::class)
    suspend fun stopLoading()

    /** Force the page stop all navigations and releases all resources. */
    @Throws(Exception::class)
    suspend fun stop()
    /** Force the page stop all navigations and releases all resources. */
    @Throws(Exception::class)
    suspend fun terminate()
    /** Quit the tab, clicking the close button. */
    @Throws(Exception::class)
    fun quit()
    @Throws(Exception::class)
    fun awaitTermination()

    fun free()
    fun startWork()
    fun retire()
    fun cancel()
}
