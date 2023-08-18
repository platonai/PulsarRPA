package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import com.google.common.annotations.Beta
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * [WebDriver] defines a concise interface to visit and interact with web pages,
 * all actions and behaviors are optimized to mimic real people as closely as possible,
 * such as scrolling, clicking, typing text, dragging and dropping, etc.
 *
 * The methods in this interface fall into three categories:
 *
 * * Control of the browser itself
 * * Selection of textContent and attributes of Elements
 * * Interact with the webpage
 *
 * Key methods:
 * * [navigateTo]: open a page.
 * * [scrollDown]: scroll down on a web page to fully load the page,
 * most modern webpages support lazy loading using ajax tech, where the page
 * content only starts to load when it is scrolled into view.
 * * [pageSource]: retrieve the source code of a webpage.
 */
interface WebDriver: Closeable {
    /**
     * Lifetime status
     * TODO: move lifetime status to AbstractWebDriver
     * */
    enum class State {
        INIT,
        READY,
        @Deprecated("Inappropriate name", ReplaceWith("READY"))
        FREE,
        WORKING,
        @Deprecated("Inappropriate lifetime status", ReplaceWith("WebDriver.canceled"))
        CANCELED,
        RETIRED,
        @Deprecated("Inappropriate lifetime status", ReplaceWith("WebDriver.crashed"))
        CRASHED,
        QUIT;

        val isInit get() = this == INIT
        @Deprecated("Inappropriate name", ReplaceWith("isReady"))
        val isFree get() = this == FREE
        val isReady get() = this == READY || isFree
        val isWorking get() = this == WORKING
        val isQuit get() = this == QUIT
        val isRetired get() = this == RETIRED
        @Deprecated("Inappropriate lifetime status", ReplaceWith("WebDriver.isCanceled"))
        val isCanceled get() = this == CANCELED
        @Deprecated("Inappropriate lifetime status", ReplaceWith("WebDriver.isCrashed"))
        val isCrashed get() = this == CRASHED
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
    val navigateHistory: NavigateHistory
    /**
     * The browser type.
     * */
    val browserType: BrowserType
    /**
     * Indicates whether the driver supports javascript. Web drivers such as MockDriver do not
     * support javascript.
     * */
    val supportJavascript: Boolean
    /**
     * Indicate whether the page source is mocked.
     * */
    val isMockedPageSource: Boolean
    /**
     * Whether the driver is recovered from the browser's tab list.
     * */
    var isRecovered: Boolean
    /**
     * Whether the driver is recovered and is reused to serve new tasks.
     * */
    var isReused: Boolean
    /**
     * The driver status.
     * */
    val state: AtomicReference<State>
    /**
     * The associated data.
     * */
    val data: MutableMap<String, Any?>
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
    var waitForElementTimeout: Duration

    val isInit: Boolean
    val isReady: Boolean
    @Deprecated("Inappropriate name", ReplaceWith("isReady()"))
    val isFree: Boolean
    val isWorking: Boolean
    val isRetired: Boolean
    val isQuit: Boolean

    val isCanceled: Boolean
    val isCrashed: Boolean

    @Deprecated("Not used any more")
    val sessionId: String?
    /**
     * Delay policy defines the delay time between actions, it is used to mimic real people
     * to interact with webpages.
     * */
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }

    val mainRequestHeaders: Map<String, Any>
    val mainRequestCookies: List<Map<String, String>>
    val mainResponseStatus: Int
    val mainResponseStatusText: String
    val mainResponseHeaders: Map<String, Any>

    /**
     * Returns a JvmWebDriver to support other JVM languages, such as java, clojure, scala, and so on,
     * the other JVM languages might have difficulty to handle kotlin suspend methods.
     * */
    fun jvm(): JvmWebDriver

    /**
     * Adds a script which would be evaluated in one of the following scenarios:
     *
     * * Whenever the page is navigated.
     *
     * The script is evaluated after the document was created but before any of
     * its scripts were run. This is useful to amend the JavaScript environment, e.g.
     * to seed [Math.random].
     *
     * This function should be invoked before a navigation, usually in an onWillNavigate
     * event handler.
     *
     * @param script Javascript source code to add.
     * */
    @Throws(WebDriverException::class)
    suspend fun addInitScript(script: String)
    /**
     * Blocks resource URLs from loading.
     *
     * @param urls URL patterns to block. Wildcards ('*') are allowed.
     */
    @Throws(WebDriverException::class)
    suspend fun addBlockedURLs(urls: List<String>)
    /**
     * Block resource URL loading with a certain probability.
     *
     * @param urls URL patterns to block.
     */
    suspend fun addProbabilityBlockedURLs(urls: List<String>)
    /**
     * Returns the main resource response. In case of multiple redirects, the navigation
     * will resolve with the first non-redirect response.
     *
     * @param url URL to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(url: String)
    /**
     * Returns the response of the main resource. In case of multiple redirects, the navigation will resolve
     * with the first non-redirect response.
     *
     * @param entry NavigateEntry to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(entry: NavigateEntry)

    @Throws(WebDriverException::class)
    suspend fun setTimeouts(browserSettings: BrowserSettings)

    /**
     * Returns a string representing the current URL that the browser is looking at.
     *
     * If the browser failed to return a proper url, return the url to navigate.
     *
     * @return The frame document's URL without fragment.
     */
    @Throws(WebDriverException::class)
    suspend fun currentUrl(): String
    /**
     * In javascript, the `window.location`, or `document.location`, is a read-only property
     * returns a Location object, which contains information about the URL of the
     * document and provides methods for changing that URL and loading another URL.
     *
     * To retrieve just the URL as a string, the read-only `document.URL` property can
     * also be used.
     * */
    suspend fun location(): String
    /**
     * In javascript, the baseURI is a property of Node, it's the absolute base URL of the
     * document containing the node. A baseURI is used to resolve relative URLs.
     *
     * The base URL is determined as follows:
     * 1. By default, the base URL is the location of the document
     *    (as determined by window.location).
     * 2. If the document has an `<base>` element, its href attribute is used.
     * */
    suspend fun baseURI(): String

    /**
     * Returns the source of the last loaded page. If the page has been modified after loading (for
     * example, by Javascript) there is no guarantee that the returned text is that of the modified
     * page.
     *
     * TODO: distinguish pageSource and outerHTML
     *
     * @return The source of the current page
     */
    @Throws(WebDriverException::class)
    suspend fun pageSource(): String?

    @Deprecated("Getter is available", ReplaceWith("mainRequestHeaders"))
    @Throws(WebDriverException::class)
    suspend fun mainRequestHeaders(): Map<String, Any>
    @Deprecated("Getter is available", ReplaceWith("mainRequestCookies"))
    @Throws(WebDriverException::class)
    suspend fun mainRequestCookies(): List<Map<String, String>>
    
    @Throws(WebDriverException::class)
    suspend fun getCookies(): List<Map<String, String>>
    /**
     * Deletes browser cookies with matching name.
     *
     * @param name Name of the cookies to remove.
     */
    @Throws(WebDriverException::class)
    suspend fun deleteCookies(name: String)
    /**
     * Deletes browser cookies with matching name and url or domain/path pair.
     *
     * @param name Name of the cookies to remove.
     * @param url If specified, deletes all the cookies with the given name where domain and path
     * match provided URL.
     * @param domain If specified, deletes only cookies with the exact domain.
     * @param path If specified, deletes only cookies with the exact path.
     */
    @Throws(WebDriverException::class)
    suspend fun deleteCookies(name: String, url: String? = null, domain: String? = null, path: String? = null)
    /** Clears browser cookies. */
    @Throws(WebDriverException::class)
    suspend fun clearBrowserCookies()
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
    suspend fun waitForPage(url: String, timeout: Duration): WebDriver?

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
    suspend fun focus(selector: String)
    @Throws(WebDriverException::class)
    suspend fun type(selector: String, text: String)
    @Throws(WebDriverException::class)
    suspend fun click(selector: String, count: Int = 1)

    @Throws(WebDriverException::class)
    suspend fun clickTextMatches(selector: String, pattern: String, count: Int = 1)
    /**
     * Use clickTextMatches instead
     * */
    @Deprecated("Inappropriate name", ReplaceWith("clickTextMatches(selector, pattern, count"))
    @Throws(WebDriverException::class)
    suspend fun clickMatches(selector: String, pattern: String, count: Int = 1) = clickTextMatches(selector, pattern, count)
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
    suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int = 0)
    @Throws(WebDriverException::class)
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)
    /** Returns the document's HTML markup. */
    @Throws(WebDriverException::class)
    suspend fun outerHTML(): String?
    /** Returns the node's HTML markup. */
    @Throws(WebDriverException::class)
    suspend fun outerHTML(selector: String): String?
    /** Returns the node's text content. */
    @Throws(WebDriverException::class)
    suspend fun firstText(selector: String): String?
    /** Returns the nodes' text contents. */
    @Throws(WebDriverException::class)
    suspend fun allTexts(selector: String): List<String>
    /** Returns the node's attribute name. */
    @Throws(WebDriverException::class)
    suspend fun firstAttr(selector: String, attrName: String): String?
    /** Returns the nodes' attribute names. */
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
     * @param expression Javascript expression to evaluate
     * @return expression result
     * */
    @Beta
    @Throws(WebDriverException::class)
    suspend fun evaluateDetail(expression: String): JsEvaluation?
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
    @Deprecated("Inappropriate name", ReplaceWith("newJsoupSession()"))
    @Throws(WebDriverException::class)
    suspend fun newSession(): Connection
    /**
     * Create a new Jsoup session with the last page's context, which means, the same
     * headers and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun newJsoupSession(): Connection
    /**
     * Load the url as a resource with Jsoup rather than browser rendering, with the last page's context,
     * which means, the same headers and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun loadJsoupResource(url: String): Connection.Response
    /**
     * Load url as a resource without browser rendering, with the last page's context,
     * which means, the same headers and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun loadResource(url: String): NetworkResourceResponse
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
     * Force the page stop all navigations and RELEASES all resources.
     * If a web driver is terminated, it should not be used any more and should be quit
     * as soon as possible.
     * */
    @Throws(WebDriverException::class)
    suspend fun terminate()
    /**
     * Quits this driver, closing every associated window.
     * */
    @Deprecated("Inappropriate name", ReplaceWith("close()"))
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
