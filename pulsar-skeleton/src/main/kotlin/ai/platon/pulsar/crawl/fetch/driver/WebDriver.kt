package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * WebDriver defines a concise interface to visit and interact with web pages,
 * all actions and behaviors are optimized to mimic real people as closely as possible,
 * such as scrolling, clicking, typing text, dragging and dropping, etc.
 *
 * The methods in this interface fall into three categories:
 * <ul>
 * <li>Control of the browser itself</li>
 * <li>Selection of textContent and attributes of Elements</li>
 * <li>Interact with the webpage</li>
 * </ul>
 * <p>
 *
 * Key methods:
 * [WebDriver.navigateTo], load a new web page.
 * [WebDriver.scrollDown], scroll down on a web page to fully load the page,
 * most modern webpages support lazy loading using ajax tech, where the web
 * content only starts to load when it is scrolled into view.
 * [WebDriver.pageSource], retrieve the source code of a webpage.
 */
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

    /**
     * The unique driver id.
     * */
    val id: Int
    /**
     * The driver name.
     * */
    val name: String
    /**
     * The browser of the driver.
     * The browser defines methods and events to manipulate a real browser.
     * */
    val browser: Browser
    /**
     * The current navigation entry.
     * */
    var navigateEntry: NavigateEntry
    /**
     * The navigation history.
     * */
    val navigateHistory: MutableList<NavigateEntry>
    /**
     * The browser type.
     * */
    val browserType: BrowserType
    /**
     * Indicate the driver has javascript support or not. Web drivers such as MockDriver does not
     * support javascript.
     * */
    val supportJavascript: Boolean
    /**
     * Indicate the page source is mocked or not.
     * */
    val isMockedPageSource: Boolean
    /**
     * Indicate if the driver is recovered from the browser's tab list or not.
     * */
    var isRecovered: Boolean
    /**
     * The driver status.
     * */
    val status: AtomicReference<Status>
    /**
     * The time of the last action.
     * */
    val lastActiveTime: Instant
    /**
     * The idle timeout.
     * */
    var idleTimeout: Duration
    /**
     * The driver is idle if no action in [idleTimeout].
     * */
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    /**
     * The timeout to wait for some object.
     * */
    var waitForTimeout: Duration

    val isCanceled: Boolean
    val isWorking: Boolean
    val isQuit: Boolean
    val isRetired: Boolean
    val isFree: Boolean
    val isCrashed: Boolean

    @Deprecated("Not used any more")
    val sessionId: String?
    /**
     * Delay policy defines the delay time between actions, it is used to mimic real people
     * to interact with webpages.
     * */
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
     * Returns the main resource response. In case of multiple redirects, the navigation
     * will resolve with the first non-redirect response.
     *
     * @param url URL to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(url: String)
    /**
     * Returns the main resource response. In case of multiple redirects, the navigation will resolve with the first
     * non-redirect response.
     *
     * @param entry NavigateEntry to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(entry: NavigateEntry)

    @Throws(WebDriverException::class)
    suspend fun setTimeouts(browserSettings: BrowserSettings)

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @return The URL of the page currently loaded in the browser
     */
    @Throws(WebDriverException::class)
    suspend fun currentUrl(): String

    /**
     * Get the source of the last loaded page. If the page has been modified after loading (for
     * example, by Javascript) there is no guarantee that the returned text is that of the modified
     * page.
     *
     * @return The source of the current page
     */
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
     * Returns the time remaining until timeout.
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
    suspend fun exists(selector: String): Boolean
    @Throws(WebDriverException::class)
    suspend fun isHidden(selector: String): Boolean = !isVisible(selector)
    @Throws(WebDriverException::class)
    suspend fun isVisible(selector: String): Boolean
    @Throws(WebDriverException::class)
    suspend fun visible(selector: String): Boolean = isVisible(selector)
    @Throws(WebDriverException::class)
    suspend fun isChecked(selector: String): Boolean

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
    suspend fun check(selector: String)
    @Throws(WebDriverException::class)
    suspend fun uncheck(selector: String)

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
    /**
     * Executes JavaScript in the context of the currently selected frame or window. The script
     * fragment provided will be executed as the body of an anonymous function.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(WebDriverException::class)
    suspend fun evaluate(expression: String): Any?
    /**
     * Executes JavaScript in the context of the currently selected frame or window. The script
     * fragment provided will be executed as the body of an anonymous function.
     *
     * All possible exceptions are suppressed and do not throw.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    suspend fun evaluateSilently(expression: String): Any?

    /**
     * This method scrolls element into view if needed, and then ake a screenshot of the element.
     */
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(selector: String): String?
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(rect: RectD): String?

    /**
     * Calculate the clickable point of an element located by [selector].
     * If the element does not exist, or is not clickable, returns null.
     * */
    @Throws(WebDriverException::class)
    suspend fun clickablePoint(selector: String): PointD?
    /**
     * Return the bounding box of an element located by [selector].
     * If the element does not exist, returns null.
     * */
    @Throws(WebDriverException::class)
    suspend fun boundingBox(selector: String): RectD?
    /**
     * Create a new Jsoup session with the last page's context, which means, the same
     * headers and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun newSession(): Connection
    /**
     * Load url as a resource without browser rendering, with the last page's context,
     * which means, the same headers and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun loadResource(url: String): Connection.Response?
    /**
     * Force the page pauses all navigations and PENDING resource fetches.
     * If the page loading stops, the user can still interact with the page,
     * and therefore resources can continue to load.
     * */
    @Throws(WebDriverException::class)
    suspend fun pause()
    @Deprecated("Inappropriate name", ReplaceWith("pause"))
    @Throws(WebDriverException::class)
    suspend fun stopLoading() = pause()
    /**
     * Force the page stop all navigations and RELEASES all resources. Interaction with the
     * stop page results in undefined behavior and the results should not be trusted.
     *
     * If a web driver stops, it can later be used to visit new pages.
     * */
    @Throws(WebDriverException::class)
    suspend fun stop()
    /**
     * Force the page stop all navigations and releases all resources.
     * If a web driver is terminated, it should not be used any more and should be quit
     * as soon as possible.
     * */
    @Throws(WebDriverException::class)
    suspend fun terminate()
    /** Quits this driver, closing every associated window. */
    @Throws(Exception::class)
    fun quit()
    /** Wait until the tab is terminated and closed. */
    @Throws(Exception::class)
    fun awaitTermination()

    /**
     * Mark the driver as free, so it can be used to fetch a new page.
     * */
    fun free()
    /**
     * Mark the driver as working, so it can not be used to fetch another page.
     * */
    fun startWork()
    /**
     * Mark the driver as retired, so it can not be used to fetch any page,
     * and should be quit as soon as possible.
     * */
    fun retire()
    /**
     * Mark the driver as canceled, so the fetch process should return as soon as possible,
     * and the fetch result should be dropped.
     * */
    fun cancel()
}
