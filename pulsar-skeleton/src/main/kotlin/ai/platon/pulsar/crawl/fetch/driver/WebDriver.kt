package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.dom.nodes.GeoAnchor
import com.google.common.annotations.Beta
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
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
     * The driver id.
     * */
    val id: Int
    /**
     * The browser of the driver.
     * The browser defines methods and events to manipulate a real browser.
     * */
    val browser: Browser
    /**
     * The browser type.
     * */
    val browserType: BrowserType
    /**
     * The current navigation entry.
     * */
    var navigateEntry: NavigateEntry
    /**
     * The navigation history.
     * */
    val navigateHistory: NavigateHistory
    /**
     * Whether the driver supports javascript. Web drivers such as MockDriver do not
     * support javascript.
     * */
    val supportJavascript: Boolean
    /**
     * Whether the page source is mocked.
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
     * The associated data of the driver.
     * */
    val data: MutableMap<String, Any?>

    /**
     * The default timeout to wait for any resources.
     * */
    var waitTimeout: Duration
    /**
     * The default timeout to wait for an element to appear.
     * */
    var waitForElementTimeout: Duration
    /**
     * The delay policy to wait for the next action, such as click, type, etc.
     * The delay policy is a function that returns a delay time in milliseconds.
     * It is used to mimic real people to interact with webpages.
     * */
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }
    /**
     * The main request's http headers.
     * */
    val mainRequestHeaders: Map<String, Any>
    /**
     * The main request's http cookies.
     * */
    val mainRequestCookies: List<Map<String, String>>
    /**
     * The main response's status.
     * */
    val mainResponseStatus: Int
    /**
     * The main response's status text.
     * */
    val mainResponseStatusText: String
    /**
     * The main response's http headers.
     * */
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
     * @param urlPatterns Regular expressions of URLs to block.
     */
    suspend fun addProbabilityBlockedURLs(urlPatterns: List<String>)
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
    
    /**
     * TODO: NOT IMPLEMENTED
     * */
    @Throws(WebDriverException::class)
    suspend fun setTimeouts(browserSettings: BrowserSettings)
    
    /**
     * Returns a string representing the current URL that the browser is looking at.
     *
     * If the browser failed to return a proper url, return the passed in url to navigate.
     *
     * @return The document's URL without fragment, or the passed in url to navigate.
     */
    @Throws(WebDriverException::class)
    suspend fun currentUrl(): String
    /**
     * Returns the document's location evaluated by javascript.
     *
     * In javascript, the `window.location`, or `document.location`, is a read-only property
     * returns a Location object, which contains information about the URL of the
     * document and provides methods for changing that URL and loading another URL.
     *
     * To retrieve just the URL as a string, the read-only `document.URL` property can
     * also be used.
     * */
    suspend fun location(): String
    /**
     * Returns the document's baseURI evaluated by javascript.
     *
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
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String): Duration = waitForSelector(selector) {}
    
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long = waitForSelector(selector, timeoutMillis) {}
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeout: Duration): Duration = waitForSelector(selector, timeout) {}
    
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, action: suspend () -> Unit): Duration =
        waitForSelector(selector, waitTimeout, action)
    /**
     * Waits for the element specified by selector to satisfy the state option.
     * Returns the time remaining until timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long, action: suspend () -> Unit): Long =
        waitForSelector(selector, Duration.ofMillis(timeoutMillis), action).toMillis()
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeout: Duration, action: suspend () -> Unit): Duration
    
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(): Duration = waitForNavigation(waitTimeout)
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeoutMillis: Long): Long = waitForNavigation(Duration.ofMillis(timeoutMillis)).toMillis()
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeout: Duration): Duration
    @Throws(WebDriverException::class)
    suspend fun waitForPage(url: String, timeout: Duration): WebDriver?
    
    /**
     * Waits until the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(predicate: suspend () -> Boolean): Duration = waitUntil(waitTimeout, predicate)
    /**
     * Waits until the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(timeoutMillis: Long, predicate: suspend () -> Boolean): Long =
        waitUntil(Duration.ofMillis(timeoutMillis), predicate).toMillis()
    /**
     * Waits until the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(timeout: Duration, predicate: suspend () -> Boolean): Duration
    
    ///////////////////////////////////////////////////////////////////
    // Status checking
    //

    /**
     * Returns whether the element exists.
     * */
    @Throws(WebDriverException::class)
    suspend fun exists(selector: String): Boolean
    /**
     * Returns whether the element is hidden.
     * */
    @Throws(WebDriverException::class)
    suspend fun isHidden(selector: String): Boolean = !isVisible(selector)
    /**
     * Returns whether the element is visible.
     * */
    @Throws(WebDriverException::class)
    suspend fun isVisible(selector: String): Boolean
    /**
     * Returns whether the element is visible.
     * */
    @Throws(WebDriverException::class)
    suspend fun visible(selector: String): Boolean = isVisible(selector)
    /**
     * Returns whether the element is checked.
     * */
    @Throws(WebDriverException::class)
    suspend fun isChecked(selector: String): Boolean
    
    /////////////////////////////////////////////////
    // Interacts with the Webpage
    
    /**
     * Brings the browser window to the front.
     */
    @Throws(WebDriverException::class)
    suspend fun bringToFront()
    
    /**
     * This method fetches an element with `selector` and focuses it. If there's no
     * element matching `selector`, nothing to do.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     */
    @Throws(WebDriverException::class)
    suspend fun focus(selector: String)
    /**
     * This method emulates inserting text that doesn't come from a key press.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param text The text to insert.
     */
    @Throws(WebDriverException::class)
    suspend fun type(selector: String, text: String)
    /**
     * This method emulates inserting text that doesn't come from a key press.
     *
     * Unlike [type], this method clears the existing value before typing.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param text The text to insert.
     */
    @Throws(WebDriverException::class)
    suspend fun fill(selector: String, text: String)
    /**
     * Shortcut for keyboard down and keyboard up.
     *
     * The key is specified as a string, which can be a single character, a key name, or a combination of both.
     * For example, 'a', 'A', 'KeyA', 'Enter', 'Shift+A', and 'Control+Shift+Tab' are all valid keys.
     *
     * TODO: find out can we press keys on pages which are not in the front.
     *
     * @param key - A key to press. The key can be a single character, a key name, or a combination of both.
     * See {@link KeyInput} for a list of all key names.
     *
     * see {@link https://source.chromium.org/chromium/chromium/src/+/main:third_party/blink/renderer/core/editing/commands/editor_command_names.h | Chromium Source Code} for valid command names.
     */
    @Throws(WebDriverException::class)
    suspend fun press(selector: String, key: String)
    /**
     * This method clicks an element with [selector] and focuses it. If there's no
     * element matching `selector`, nothing to do.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * */
    @Throws(WebDriverException::class)
    suspend fun click(selector: String, count: Int = 1)
    
    /**
     * This method clicks an element with [selector] whose text content matches [pattern], and then focuses it.
     * If there's no element matching [selector], or the element's text content doesn't match [pattern], nothing to do.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * */
    @Throws(WebDriverException::class)
    suspend fun clickTextMatches(selector: String, pattern: String, count: Int = 1)

    suspend fun clickMatches(selector: String, pattern: String, count: Int = 1) = clickTextMatches(selector, pattern, count)
    @Throws(WebDriverException::class)
    suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
    @Throws(WebDriverException::class)
    suspend fun clickNthAnchor(n: Int, rootSelector: String = "body"): String?
    /**
     * This method check an element with [selector]. If there's no element matching [selector], nothing to do.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to check. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * */
    @Throws(WebDriverException::class)
    suspend fun check(selector: String)
    /**
     * This method uncheck an element with [selector]. If there's no element matching [selector], nothing to do.
     *
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to uncheck. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * */
    @Throws(WebDriverException::class)
    suspend fun uncheck(selector: String)
    /**
     * This method fetches an element with [selector], scrolls it into view if needed. If there's no element matching
     * [selector], the method does nothing.
     *
     * @param selector - A selector to search for element to scroll to. If there are multiple elements satisfying
     * the [selector], the first will be selected.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollTo(selector: String)
    /**
     * The current page frame scrolls down for [count] times.
     *
     * @param count The times to scroll down.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollDown(count: Int = 1)
    /**
     * The current page frame scrolls up for [count] times.
     *
     * @param count The times to scroll up.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollUp(count: Int = 1)
    /**
     * The current page frame scrolls to the top.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToTop()
    /**
     * The current page frame scrolls to the bottom.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToBottom()
    /**
     * The current page frame scrolls to the middle.
     *
     * @param ratio The ratio of the page to scroll to, 0.0 means the top, 1.0 means the bottom.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToMiddle(ratio: Float)
    /**
     * The mouse wheels down for [count] times.
     *
     * @param count The times to wheel down.
     * @param deltaX The distance to wheel horizontally.
     * @param deltaY The distance to wheel vertically.
     * @param delayMillis The delay time in milliseconds.
     */
    @Throws(WebDriverException::class)
    suspend fun mouseWheelDown(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0)
    /**
     * The mouse wheels up for [count] times.
     *
     * @param count The times to wheel up.
     * @param deltaX The distance to wheel horizontally.
     * @param deltaY The distance to wheel vertically.
     * @param delayMillis The delay time in milliseconds.
     */
    @Throws(WebDriverException::class)
    suspend fun mouseWheelUp(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0)
    /**
     * The mouse moves to the position specified by [x] and [y].
     *
     * @param x The x coordinate to move to.
     * @param y The y coordinate to move to.
     */
    @Throws(WebDriverException::class)
    suspend fun moveMouseTo(x: Double, y: Double)
    /**
     * The mouse moves to the element with [selector].
     *
     * @param deltaX The distance to the left of the element.
     * @param deltaY The distance to the top of the element.
     */
    @Throws(WebDriverException::class)
    suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int = 0)
    /**
     * Performs a drag, dragenter, dragover, and drop in sequence.
     * @param selector - selector of the element to drag from.
     * @param deltaX The distance to drag horizontally.
     * @param deltaY The distance to drag vertically.
     */
    @Throws(WebDriverException::class)
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)

    /**
     * Returns the document's HTML markup.
     *
     * If the document does not exist, returns null.
     *
     * @return The HTML markup of the document.
     * */
    @Throws(WebDriverException::class)
    suspend fun outerHTML(): String?
    /**
     * Returns the node's HTML markup, the node is located by [selector].
     *
     * If the node does not exist, returns null.
     *
     * @param selector The selector to locate the node.
     * @return The HTML markup of the node.
     * */
    @Throws(WebDriverException::class)
    suspend fun outerHTML(selector: String): String?
    
    /** Returns the node's text content. */
    @Deprecated("Inappropriate name", ReplaceWith("selectFirstTextOrNull(selector)"))
    @Throws(WebDriverException::class)
    suspend fun firstText(selector: String): String? = selectFirstTextOrNull(selector)
    /** Returns the nodes' text contents. */
    @Deprecated("Inappropriate name", ReplaceWith("selectTextAll(selector)"))
    @Throws(WebDriverException::class)
    suspend fun allTexts(selector: String): List<String> = selectTextAll(selector)
    /**
     * Returns the node's text content, the node is located by [selector].
     *
     * If the node does not exist, returns null.
     *
     * @param selector The selector to locate the node.
     * @return The text content of the node.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectFirstTextOrNull(selector: String): String?
    
    /**
     * Returns a list of text contents of all the elements matching the specified selector within the page.
     *
     * If no elements match the selector, returns an empty list.
     *
     * @param selector The selector to locate the nodes.
     * @return The text contents of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectTexts(selector: String): List<String> = selectTextAll(selector)
    /**
     * Returns a list of text contents of all the elements matching the specified selector within the page.
     *
     * If no elements match the selector, returns an empty list.
     *
     * @param selector The selector to locate the nodes.
     * @return The text contents of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectTextAll(selector: String): List<String>
    /**
     * Returns the node's attribute value, the node is located by [selector], the attribute is [attrName].
     *
     * If the node does not exist, or the attribute does not exist, returns null.
     *
     * @param selector The selector to locate the node.
     * @param attrName The attribute name to retrieve.
     * @return The attribute value of the node.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String?
    /**
     * Returns the nodes' attribute values, the nodes are located by [selector].
     *
     * If the nodes do not exist, or the attribute does not exist, returns an empty list.
     *
     * @param selector The selector to locate the nodes.
     * @return The attribute pairs of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAttributes(selector: String): Map<String, String>
    /**
     * Returns the nodes' attribute values, the nodes are located by [selector], the attribute is [attrName].
     *
     * If the nodes do not exist, or the attribute does not exist, returns an empty list.
     *
     * @param selector The selector to locate the nodes.
     * @param attrName The attribute name to retrieve.
     * @return The attribute values of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAttributeAll(selector: String, attrName: String, start: Int = 0, limit: Int = 1000): List<String>
    
    @Throws(WebDriverException::class)
    suspend fun setAttribute(selector: String, attrName: String, attrValue: String)
    
    @Throws(WebDriverException::class)
    suspend fun setAttributeAll(selector: String, attrName: String, attrValue: String)
    
    /**
     * Find hyperlinks in elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectHyperlinks(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink>
    
    /**
     * Find anchor elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAnchors(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<GeoAnchor>
    
    /**
     * Find image elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectImages(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String>
    
    
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
    /**
     * This method scrolls element into view if needed, and then ake a screenshot of the element.
     */
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
    /**
     * Force the page stop all navigations and RELEASES all resources. Interaction with the
     * stop page results in undefined behavior and the results should not be trusted.
     *
     * If a web driver stops, it can later be used to visit new pages.
     * */
    @Throws(WebDriverException::class)
    suspend fun stop()
}
