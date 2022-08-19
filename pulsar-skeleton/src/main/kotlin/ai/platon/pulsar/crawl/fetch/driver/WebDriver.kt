package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.RectD
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
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

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
data class NavigateEntry(
    /**
     * The url to navigate to.
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

interface WebDriver: Closeable {
    val id: Int
    val navigateEntry: NavigateEntry
//    val url: String

    val browserInstance: BrowserInstance
    val browserInstanceId: BrowserInstanceId get() = browserInstance.id

    val name: String
    val browserType: BrowserType
    val supportJavascript: Boolean
    val isMockedPageSource: Boolean

    val idleTimeout: Duration
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val isCanceled: Boolean
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
    suspend fun type(selector: String, text: String)
    suspend fun click(selector: String, count: Int = 1)
    suspend fun scrollTo(selector: String)
    suspend fun scrollDown(count: Int = 1)
    suspend fun scrollUp(count: Int = 1)

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
    suspend fun stop()

    fun free()
    fun startWork()
    fun retire()
    fun quit()
    fun cancel()
}
