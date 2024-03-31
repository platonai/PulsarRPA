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
 *
 * URLs and location properties in the browser:
 *
 * In the Document Object Model (DOM), the relationship between `document.URL`, `document.documentURI`,
 * `document.location`, and the URL displayed in the browser's address bar is as follows:
 * 1. `document.URL`:
 *    - This property returns the URL of the document as a string.
 *    - It is a read-only property and reflects the current URL of the document.
 *    - Changes to `document.location` will also update `document.URL`.
 * 2. `document.documentURI`:
 *    - This property returns the URI of the document.
 *    - It is also a read-only property and typically contains the same value as `document.URL`.
 *    - However, `document.documentURI` is defined to be the URI that was provided to the parser, which could
 *      potentially differ from `document.URL` in certain cases, although in practice, this is rare.
 * 3. `document.location`:
 *    - This property represents the location (URL) of the current page and allows you to manipulate the URL.
 *    - It is a read-write property, which means you can change it to navigate to a different page or to manipulate
 *      query strings, fragments, etc.
 *    - Changes to `document.location` will cause the browser to navigate to the new URL, updating both `document.URL`
 *      and the URL displayed in the address bar.
 * 4. URL displayed in the address bar:
 *    - The URL displayed in the browser's address bar is what users see and can edit directly.
 *    - It is typically synchronized with `document.URL` and `document.location.href` (a property of `document.location`).
 *    - When the page is loaded or when `document.location` is modified, the address bar is updated to reflect the new URL.
 * In summary, `document.URL` and `document.documentURI` are read-only properties that reflect the current URL of the
 * document, while `document.location` is a read-write property that not only reflects the current URL but also allows
 * you to navigate to a new one. The URL displayed in the address bar is a user-facing representation of the current
 * document's URL, which is usually in sync with `document.location`.
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
     * Web pages for the page open from the current page, via window.open(), link click, form submission,
     * etc.
     * */
    val frames: List<WebDriver>
    /**
     * The driver from whom opens the current page.
     * */
    val opener: WebDriver?
    /**
     * Web pages for the page open from the current page, via window.open(), link click, form submission,
     * etc.
     * */
    val outgoingPages: Set<WebDriver>
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
     * The associated data of the driver.
     * */
    val data: MutableMap<String, Any?>
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
     * TODO: NOT IMPLEMENTED
     * */
    @Throws(WebDriverException::class)
    suspend fun setTimeouts(browserSettings: BrowserSettings)
    /**
     * Navigates current page to the given URL.
     *
     * @param url URL to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(url: String)
    /**
     * Navigates current page to the given URL.
     *
     * @param entry NavigateEntry to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(entry: NavigateEntry)
    /**
     * Returns a string representing the current URL that the browser is looking at. The current url is always
     * the main frame's url if the browser succeed to return it, and is displayed in the browser's address bar.
     *
     * If the browser failed to return a proper url, return the passed in url to navigate.
     *
     * @return The document's URL without fragment, or the passed in url to navigate.
     */
    @Throws(WebDriverException::class)
    suspend fun currentUrl(): String
    /**
     * The URL read-only property of the Document interface returns the document location as a string.
     *
     * This property equals to javascript `document.URL`.
     * The `document.URL` property returns the same value. The `document.documentURI` property can be used on
     * any document types, while the `document.URL` property can only be used on HTML documents.
     *
     * @see [Document: URL property](https://developer.mozilla.org/en-US/docs/Web/API/Document/URL)
     *
     * @return A string containing the URL of the document.
     */
    @Throws(WebDriverException::class)
    suspend fun url(): String
    /**
     * Returns the document location as a string.
     *
     * This property equals to javascript `document.documentURI`.
     *
     * The `document.URL` property which returns the same value. The `document.documentURI` property can be used on
     * any document types, while the `document.URL` property can only be used on HTML documents.
     *
     * @see [Document: documentURI property](https://developer.mozilla.org/en-US/docs/Web/API/Document/documentURI)
     *
     * @return The document's documentURI.
     * */
    suspend fun documentURI(): String
    /**
     * Returns the document's baseURI.
     *
     * The baseURI is a property of Node, it's the absolute base URL of the
     * document containing the node. A baseURI is used to resolve relative URLs.
     *
     * The base URL is determined as follows:
     * 1. By default, the base URL is the location of the document
     *    (as determined by window.location).
     * 2. If the document has an `<base>` element, its href attribute is used.
     *
     * @return The document's baseURI.
     * */
    suspend fun baseURI(): String
    /**
     * The referrer property returns the URI of the page that linked to this page.
     *
     * The value is an empty string if the user navigated to the page directly (not through a link, but, for example,
     * by using a bookmark).
     *
     * Inside an <iframe>, the referrer will initially be set to the same value as the href of the parent
     * window's Window.location.
     *
     * @return The document's referrer.
     * */
    @Throws(WebDriverException::class)
    suspend fun referrer(): String
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
    /**
     * Returns the cookies of the current page.
     *
     * @return The cookies of the current page.
     */
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
     * Wait until the element identified by the selector becomes present in the DOM or timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String): Duration = waitForSelector(selector) {}
    /**
     * Wait until the element identified by the selector becomes present in the DOM or timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long = waitForSelector(selector, timeoutMillis) {}
    /**
     * Wait for the element identified by the selector to become present in the DOM, or until timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeout: Duration): Duration = waitForSelector(selector, timeout) {}
    /**
     * Wait for the element identified by the selector to become present in the DOM, or until timeout.
     * This method periodically checks for the existence of the element. If the element is not found during a check,
     * the action will be executed, such as scrolling the page down.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, action: suspend () -> Unit): Duration
    /**
     * Wait for the element identified by the selector to become present in the DOM, or until timeout.
     * This method periodically checks for the existence of the element. If the element is not found during a check, 
     * the action will be executed, such as scrolling the page down.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long, action: suspend () -> Unit): Long =
        waitForSelector(selector, Duration.ofMillis(timeoutMillis), action).toMillis()
    /**
     * Wait for the element identified by the selector to become present in the DOM, or until timeout.
     * This method periodically checks for the existence of the element. If the element is not found during a check,
     * the action will be executed, such as scrolling the page down.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeout: Duration, action: suspend () -> Unit): Duration
    /**
     * Wait until the current url changes or timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(): Duration
    /**
     * Wait until the current url changes or timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeoutMillis: Long): Long = waitForNavigation(Duration.ofMillis(timeoutMillis)).toMillis()
    /**
     * Wait until the current url changes or timeout.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(timeout: Duration): Duration
    /**
     * Await navigation to the specified URL page or timeout if necessary.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForPage(url: String, timeout: Duration): WebDriver?
    
    /**
     * Wait until the predication returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(predicate: suspend () -> Boolean): Duration
    /**
     * Wait until the predication returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(timeoutMillis: Long, predicate: suspend () -> Boolean): Long =
        waitUntil(Duration.ofMillis(timeoutMillis), predicate).toMillis()
    /**
     * Wait until the predication returns true.
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
    @Deprecated("Inappropriate name", ReplaceWith("selectTextAll(selector)"))
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
     * Returns the node's attribute values, the node is located by [selector].
     *
     * If the node do not exist, or the attribute does not exist, returns an empty list.
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
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The attribute values of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAttributeAll(selector: String, attrName: String, start: Int = 0, limit: Int = 1000): List<String>
    /**
     * Set the attribute of an element located by [selector].
     *
     * @param selector The CSS query to select an element.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     * */
    @Throws(WebDriverException::class)
    suspend fun setAttribute(selector: String, attrName: String, attrValue: String)
    /**
     * Set the attribute of all elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     * */
    @Throws(WebDriverException::class)
    suspend fun setAttributeAll(selector: String, attrName: String, attrValue: String)
    /**
     * Find hyperlinks in elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The hyperlinks in the elements.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectHyperlinks(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink>
    /**
     * Find anchor elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The anchors.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAnchors(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<GeoAnchor>
    /**
     * Find image elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The image URLs.
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
     * Create a new Jsoup session with the last page's context, which means, the same headers and cookies.
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
     * Load the url as a resource without browser rendering, with the last page's context, which means, the same headers
     * and cookies.
     * */
    @Throws(WebDriverException::class)
    suspend fun loadResource(url: String): NetworkResourceResponse
    /**
     * Force the page pauses all navigations and PENDING resource fetches.
     * If the page loading pauses, the user can still interact with the page,
     * and therefore resources can continue to load.
     * */
    @Throws(WebDriverException::class)
    suspend fun pause()
    /**
     * Force the page stop all navigations and RELEASES all resources. Interaction with the
     * stop page results in undefined behavior and the results should not be trusted.
     *
     * If a web driver stops, it can later be used to visit other pages.
     * */
    @Throws(WebDriverException::class)
    suspend fun stop()
}
