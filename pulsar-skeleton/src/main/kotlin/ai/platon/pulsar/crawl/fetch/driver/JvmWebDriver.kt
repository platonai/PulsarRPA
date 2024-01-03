package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * [JvmWebDriver] defines a concise interface to visit and interact with web pages,
 * all actions and behaviors are optimized to mimic real people as closely as possible,
 * such as scrolling, clicking, typing text, dragging and dropping, etc.
 *
 * [JvmWebDriver] is programmatically generated from the [WebDriver] class and is designed to
 * help handle [WebDriver] tasks in other JVM languages such as java, clojure, scala, and so on,
 * which had difficulty handling kotlin suspend methods.
 *
 * @see [WebDriver]
 */
interface JvmWebDriver {

    /**
     * Adds a script which would be evaluated in one of the following scenarios:
     *
     * * Whenever the page is navigated.
     *
     * The script is evaluated after the document was created but before any of
     * its scripts were run. This is useful to amend the JavaScript environment, e.g.
     * to seed [Math.random].
     *
     * @param script Javascript source code to add.
     * */
    @Throws(WebDriverException::class)
    fun addInitScriptAsync(script: String): CompletableFuture<Unit>
    /**
     * Blocks URLs from loading.
     *
     * @param urls URL patterns to block. Wildcards ('*') are allowed.
     * @return the new CompletableFuture
     */
    @Throws(WebDriverException::class)
    fun addBlockedURLsAsync(urls: List<String>): CompletableFuture<Unit>
    /**
     * Returns the main resource response. In case of multiple redirects, the navigation
     * will resolve with the first non-redirect response.
     *
     * @param url URL to navigate page to.
     * @return the new CompletableFuture
     */
    @Throws(WebDriverException::class)
    fun navigateToAsync(url: String): CompletableFuture<Unit>

    /**
     * Returns the response of the main resource. In case of multiple redirects, the navigation will resolve
     * with the first non-redirect response.
     *
     * @param entry NavigateEntry to navigate page to.
     * @return the new CompletableFuture
     */
    @Throws(WebDriverException::class)
    fun navigateToAsync(entry: NavigateEntry): CompletableFuture<Unit>

    @Throws(WebDriverException::class)
    fun setTimeoutsAsync(browserSettings: BrowserSettings): CompletableFuture<Unit>

    /**
     * Returns a string representing the current URL that the browser is looking at.
     *
     * @return The URL of the page currently loaded in the browser
     */
    @Throws(WebDriverException::class)
    fun currentUrlAsync(): CompletableFuture<String>

    /**
     * Returns the source of the last loaded page. If the page has been modified after loading (for
     * example, by Javascript) there is no guarantee that the returned text is that of the modified
     * page.
     *
     * @return The source of the current page
     */
    @Throws(WebDriverException::class)
    fun pageSourceAsync(): CompletableFuture<String?>

    @Throws(WebDriverException::class)
    fun mainRequestHeadersAsync(): CompletableFuture<Map<String, Any>>
    @Throws(WebDriverException::class)
    fun mainRequestCookiesAsync(): CompletableFuture<List<Map<String, String>>>
    @Throws(WebDriverException::class)
    fun getCookiesAsync(): CompletableFuture<List<Map<String, String>>>

    /**
     * Brings page to front (activates tab).
     */
    @Throws(WebDriverException::class)
    fun bringToFrontAsync(): CompletableFuture<Unit>
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String): CompletableFuture<Long>
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout.
     * */
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String, timeoutMillis: Long): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String, timeout: Duration): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(timeoutMillis: Long): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(timeout: Duration): CompletableFuture<Long>

    @Throws(WebDriverException::class)
    fun existsAsync(selector: String): CompletableFuture<Boolean>
    @Throws(WebDriverException::class)
    fun isHiddenAsync(selector: String): CompletableFuture<Boolean>
    @Throws(WebDriverException::class)
    fun isVisibleAsync(selector: String): CompletableFuture<Boolean>
    @Throws(WebDriverException::class)
    fun visibleAsync(selector: String): CompletableFuture<Boolean>
    @Throws(WebDriverException::class)
    fun isCheckedAsync(selector: String): CompletableFuture<Boolean>

    @Throws(WebDriverException::class)
    fun typeAsync(selector: String, text: String): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun clickAsync(selector: String) = clickAsync(selector, 1)
    @Throws(WebDriverException::class)
    fun clickAsync(selector: String, count: Int): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun clickMatchesAsync(selector: String, pattern: String) = clickMatchesAsync(selector, pattern, 1)
    @Throws(WebDriverException::class)
    fun clickMatchesAsync(selector: String, pattern: String, count: Int): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun clickMatchesAsync(selector: String, attrName: String, pattern: String) = clickMatchesAsync(selector, attrName, pattern, 1)
    @Throws(WebDriverException::class)
    fun clickMatchesAsync(selector: String, attrName: String, pattern: String, count: Int): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun clickNthAnchorAsync(n: Int, rootSelector: String = "body"): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun checkAsync(selector: String): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun uncheckAsync(selector: String): CompletableFuture<Unit>

    @Throws(WebDriverException::class)
    fun scrollToAsync(selector: String): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun scrollDownAsync(count: Int = 1): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun scrollUpAsync(count: Int = 1): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun scrollToTopAsync(): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun scrollToBottomAsync(): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun scrollToMiddleAsync(ratio: Float): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun mouseWheelDownAsync(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun mouseWheelUpAsync(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun moveMouseToAsync(x: Double, y: Double): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun dragAndDropAsync(selector: String, deltaX: Int, deltaY: Int = 0): CompletableFuture<Unit>

    @Throws(WebDriverException::class)
    fun outerHTMLAsync(selector: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun firstTextAsync(selector: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstTextOrNullAsync(selector: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstTextOptionalAsync(selector: String): CompletableFuture<Optional<String>>
    @Throws(WebDriverException::class)
    fun allTextsAsync(selector: String): CompletableFuture<List<String>>
    @Throws(WebDriverException::class)
    fun selectTextsAsync(selector: String): CompletableFuture<List<String>>
    
    @Throws(WebDriverException::class)
    fun firstAttrAsync(selector: String, attrName: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstAttributeOrNullAsync(selector: String, attrName: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstAttributeOptionalAsync(selector: String, attrName: String): CompletableFuture<Optional<String>>
    @Throws(WebDriverException::class)
    fun allAttrsAsync(selector: String, attrName: String): CompletableFuture<List<String>>
    @Throws(WebDriverException::class)
    fun selectAttributesAsync(selector: String, attrName: String): CompletableFuture<List<String>>
    /**
     * Executes JavaScript in the context of the currently selected frame or window. The script
     * fragment provided will be executed as the body of an anonymous function.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(WebDriverException::class)
    fun evaluateAsync(expression: String): CompletableFuture<Any?>
    /**
     * Executes JavaScript in the context of the currently selected frame or window. The script
     * fragment provided will be executed as the body of an anonymous function.
     *
     * All possible exceptions are suppressed and do not throw.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    fun evaluateSilentlyAsync(expression: String): CompletableFuture<Any?>

    /**
     * This method scrolls element into view if needed, and then ake a screenshot of the element.
     */
    @Throws(WebDriverException::class)
    fun captureScreenshotAsync(selector: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun captureScreenshotAsync(rect: RectD): CompletableFuture<String?>

    /**
     * Calculate the clickable point of an element located by [selector].
     * If the element does not exist, or is not clickable, returns null.
     * */
    @Throws(WebDriverException::class)
    fun clickablePointAsync(selector: String): CompletableFuture<PointD?>
    /**
     * Return the bounding box of an element located by [selector].
     * If the element does not exist, returns null.
     * */
    @Throws(WebDriverException::class)
    fun boundingBoxAsync(selector: String): CompletableFuture<RectD?>
    /**
     * Create a new Jsoup session with the last page's context, which means, the same
     * headers and cookies.
     * */
    @Throws(WebDriverException::class)
    fun newJsoupSessionAsync(): CompletableFuture<Connection>
    /**
     * Load url as a resource without browser rendering, with the last page's context,
     * which means, the same headers and cookies.
     * */
    @Throws(WebDriverException::class)
    fun loadResourceAsync(url: String): CompletableFuture<NetworkResourceResponse>
    /**
     * Force the page pauses all navigations and PENDING resource fetches.
     * If the page loading stops, the user can still interact with the page,
     * and therefore resources can continue to load.
     * */
    @Throws(WebDriverException::class)
    fun pauseAsync(): CompletableFuture<Unit>
    /**
     * Force the page stop all navigations and RELEASES all resources. Interaction with the
     * stop page results in undefined behavior and the results should not be trusted.
     *
     * If a web driver stops, it can later be used to visit new pages.
     * */
    @Throws(WebDriverException::class)
    fun stopAsync(): CompletableFuture<Unit>
    /**
     * Force the page stop all navigations and RELEASES all resources.
     * If a web driver is terminated, it should not be used any more and should be quit
     * as soon as possible.
     * */
    @Throws(WebDriverException::class)
    fun terminateAsync(): CompletableFuture<Unit>
}
