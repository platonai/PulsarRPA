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

open class WebDriverException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String?) : super(message) {
    }

    constructor(message: String?, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable?) : super(cause) {
    }
}

open class WebDriverCancellationException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String?) : super(message) {
    }

    constructor(message: String?, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable?) : super(cause) {
    }
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
    var activeTime: Instant = Instant.now(),
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
        activeTime = now
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
    val navigateEntry: NavigateEntry
//    val url: String

    val browser: Browser
    val browserId: BrowserId get() = browser.id

    val name: String
    val browserType: BrowserType
    val supportJavascript: Boolean
    val isMockedPageSource: Boolean
    val status: AtomicReference<Status>

    val idleTimeout: Duration
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val isCanceled: Boolean
    val isWorking: Boolean
    val isQuit: Boolean
    val isRetired: Boolean

    val sessionId: String?
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }

    val lastActiveTime: Instant

    suspend fun currentUrl(): String
    suspend fun pageSource(): String?
    suspend fun mainRequestHeaders(): Map<String, Any>
    suspend fun mainRequestCookies(): List<Map<String, String>>
    suspend fun getCookies(): List<Map<String, String>>

    suspend fun navigateTo(url: String)
    suspend fun navigateTo(entry: NavigateEntry)
    suspend fun setTimeouts(browserSettings: BrowserSettings)

    /**
     * Brings page to front (activates tab).
     */
    suspend fun bringToFront()
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    suspend fun waitForSelector(selector: String): Long
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout
     * */
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long
    suspend fun waitForSelector(selector: String, timeout: Duration): Long
    suspend fun waitForNavigation(): Long
    suspend fun waitForNavigation(timeoutMillis: Long): Long
    suspend fun waitForNavigation(timeout: Duration): Long

    suspend fun exists(selector: String): Boolean
    suspend fun visible(selector: String): Boolean
    suspend fun type(selector: String, text: String)
    suspend fun click(selector: String, count: Int = 1)
    suspend fun clickMatches(selector: String, pattern: String, count: Int = 1)
    suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
    suspend fun scrollTo(selector: String)
    suspend fun scrollDown(count: Int = 1)
    suspend fun scrollUp(count: Int = 1)
    suspend fun scrollToTop()
    suspend fun scrollToBottom()
    suspend fun scrollToMiddle(ratio: Float)
    suspend fun mouseWheelDown(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0)
    suspend fun mouseWheelUp(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0)
    suspend fun moveMouseTo(x: Double, y: Double)
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)

    suspend fun clickablePoint(selector: String): PointD?
    suspend fun boundingBox(selector: String): RectD?

    suspend fun outerHTML(selector: String): String?
    suspend fun firstText(selector: String): String?
    suspend fun allTexts(selector: String): List<String>
    suspend fun firstAttr(selector: String, attrName: String): String?
    suspend fun allAttrs(selector: String, attrName: String): List<String>

    suspend fun evaluate(expression: String): Any?
    suspend fun evaluateSilently(expression: String): Any?

    suspend fun newSession(): Connection
    suspend fun loadResource(url: String): Connection.Response?

    /**
     * This method scrolls element into view if needed, and then ake a screenshot of the element.
     * If the element is detached from DOM, the method throws an error.
     */
    suspend fun captureScreenshot(selector: String): String?
    suspend fun captureScreenshot(rect: RectD): String?
    /** Force the page stop all navigations and pending resource fetches. */
    suspend fun stopLoading()

    /** Force the page stop all navigations and releases all resources. */
    suspend fun stop()
    /** Force the page stop all navigations and releases all resources. */
    suspend fun terminate()
    /** Quit the tab, clicking the close button. */
    fun quit()
    fun awaitTermination()

    fun free()
    fun startWork()
    fun retire()
    fun cancel()
}
