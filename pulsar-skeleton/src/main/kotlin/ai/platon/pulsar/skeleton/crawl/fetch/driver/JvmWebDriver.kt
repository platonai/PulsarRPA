package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.dom.nodes.GeoAnchor
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.InstructionResult
import org.jsoup.Connection
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

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
     * Block resource URLs from loading with a certain probability.
     *
     * @param urlPatterns Regular expressions of URLs to block.
     */
    @Throws(WebDriverException::class)
    fun addProbabilityBlockedURLsAsync(urlPatterns: List<String>): CompletableFuture<Unit>
    
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
     * The URL read-only property of the Document interface returns the document location as a string.
     *
     * This property equals to javascript `document.URL`.
     *
     * @return The URL of the document
     */
    @Throws(WebDriverException::class)
    fun urlAsync(): CompletableFuture<String>
    
    /**
     * Returns the document location as a string.
     *
     * This property equals to javascript `document.documentURI`.
     *
     * @return The document's documentURI
     */
    @Throws(WebDriverException::class)
    fun documentURIAsync(): CompletableFuture<String>
    
    /**
     * Returns the document's baseURI.
     *
     * @return The document's baseURI
     */
    @Throws(WebDriverException::class)
    fun baseURIAsync(): CompletableFuture<String>
    
    /**
     * The referrer property returns the URI of the page that linked to this page.
     *
     * @return The document's referrer
     */
    @Throws(WebDriverException::class)
    fun referrerAsync(): CompletableFuture<String>

    /**
     * Returns the source of the last loaded page. If the page has been modified after loading (for
     * example, by Javascript) there is no guarantee that the returned text is that of the modified
     * page.
     *
     * @return The source of the current page
     */
    @Throws(WebDriverException::class)
    fun pageSourceAsync(): CompletableFuture<String?>
    
    /**
     * Chat with the AI model about the specified element.
     *
     * @param prompt The prompt to chat with
     * @param selector The selector to find the element
     * @return The response from the model
     */
    @Throws(WebDriverException::class)
    fun chatAsync(prompt: String, selector: String): CompletableFuture<ModelResponse>
    
    /**
     * Instructs the webdriver to perform a series of actions based on the given prompt.
     *
     * @param prompt The textual prompt that describes the actions to be performed by the webdriver.
     * @return The instruction result
     */
    @Throws(WebDriverException::class)
    fun instructAsync(prompt: String): CompletableFuture<InstructionResult>

    @Throws(WebDriverException::class)
    fun getCookiesAsync(): CompletableFuture<List<Map<String, String>>>
    
    /**
     * Deletes browser cookies with matching name.
     *
     * @param name Name of the cookies to remove.
     */
    @Throws(WebDriverException::class)
    fun deleteCookiesAsync(name: String): CompletableFuture<Unit>
    
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
    fun deleteCookiesAsync(name: String, url: String?, domain: String?, path: String?): CompletableFuture<Unit>
    
    /**
     * Clears browser cookies.
     */
    @Throws(WebDriverException::class)
    fun clearBrowserCookiesAsync(): CompletableFuture<Unit>

    /**
     * Brings page to front (activates tab).
     */
    @Throws(WebDriverException::class)
    fun bringToFrontAsync(): CompletableFuture<Unit>
    
    /**
     * This method fetches an element with `selector` and focuses it.
     *
     * @param selector A selector of an element to focus.
     */
    @Throws(WebDriverException::class)
    fun focusAsync(selector: String): CompletableFuture<Unit>
    
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String): CompletableFuture<Duration>
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout.
     * */
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String, timeoutMillis: Long): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForSelectorAsync(selector: String, timeout: Duration): CompletableFuture<Duration>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(oldUrl: String): CompletableFuture<Duration>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(oldUrl: String, timeoutMillis: Long): CompletableFuture<Long>
    @Throws(WebDriverException::class)
    fun waitForNavigationAsync(oldUrl: String, timeout: Duration): CompletableFuture<Duration>
    
    /**
     * Await navigation to the specified URL page or timeout if necessary.
     *
     * @param url The URL to navigate to.
     * @param timeout The maximum time to wait.
     * @return The WebDriver for the new page, or null if timeout.
     */
    @Throws(WebDriverException::class)
    fun waitForPageAsync(url: String, timeout: Duration): CompletableFuture<WebDriver?>
    
    /**
     * Wait until the predicate returns true.
     *
     * @param timeoutMillis The maximum time to wait in milliseconds.
     * @param predicate The predicate function to evaluate.
     * @return The remaining time in milliseconds.
     */
    @Throws(WebDriverException::class)
    fun waitUntilAsync(timeoutMillis: Long, predicate: () -> Boolean): CompletableFuture<Long>
    
    /**
     * Wait until the predicate returns true.
     *
     * @param timeout The maximum time to wait.
     * @param predicate The predicate function to evaluate.
     * @return The remaining time.
     */
    @Throws(WebDriverException::class)
    fun waitUntilAsync(timeout: Duration, predicate: () -> Boolean): CompletableFuture<Duration>

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
    
    /**
     * This method emulates inserting text that doesn't come from a key press,
     * clearing the existing value first.
     *
     * @param selector A selector of an element to focus and fill.
     * @param text The text to fill.
     */
    @Throws(WebDriverException::class)
    fun fillAsync(selector: String, text: String): CompletableFuture<Unit>
    
    /**
     * Shortcut for keyboard down and keyboard up.
     *
     * @param selector A selector of an element to focus.
     * @param key A key to press.
     */
    @Throws(WebDriverException::class)
    fun pressAsync(selector: String, key: String): CompletableFuture<Unit>
    
    @Throws(WebDriverException::class)
    fun clickAsync(selector: String) = clickAsync(selector, 1)
    @Throws(WebDriverException::class)
    fun clickAsync(selector: String, count: Int): CompletableFuture<Unit>
    
    /**
     * This method clicks an element with selector whose text content matches pattern.
     *
     * @param selector A selector of an element to focus.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     */
    @Throws(WebDriverException::class)
    fun clickTextMatchesAsync(selector: String, pattern: String, count: Int = 1): CompletableFuture<Unit>
    
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
    
    /**
     * The current page frame scrolls to the middle.
     *
     * @param ratio The ratio of the page to scroll to, 0.0 means the top, 1.0 means the bottom.
     */
    @Throws(WebDriverException::class)
    fun scrollToMiddleAsync(ratio: Double): CompletableFuture<Unit>
    
    /**
     * The current page frame scrolls to the specified screen position.
     *
     * @param screenNumber The screen number to scroll to (0-based).
     */
    @Throws(WebDriverException::class)
    fun scrollToScreenAsync(screenNumber: Double): CompletableFuture<Unit>
    
    @Throws(WebDriverException::class)
    fun mouseWheelDownAsync(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun mouseWheelUpAsync(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0): CompletableFuture<Unit>
    @Throws(WebDriverException::class)
    fun moveMouseToAsync(x: Double, y: Double): CompletableFuture<Unit>
    
    /**
     * The mouse moves to the element with selector.
     *
     * @param selector A selector of an element to move to.
     * @param deltaX The distance to the left of the element.
     * @param deltaY The distance to the top of the element.
     */
    @Throws(WebDriverException::class)
    fun moveMouseToAsync(selector: String, deltaX: Int, deltaY: Int = 0): CompletableFuture<Unit>
    
    @Throws(WebDriverException::class)
    fun dragAndDropAsync(selector: String, deltaX: Int, deltaY: Int = 0): CompletableFuture<Unit>

    /**
     * Returns the document's HTML markup.
     *
     * @return The HTML markup of the document.
     */
    @Throws(WebDriverException::class)
    fun outerHTMLAsync(): CompletableFuture<String?>
    
    @Throws(WebDriverException::class)
    fun outerHTMLAsync(selector: String): CompletableFuture<String?>

    @Throws(WebDriverException::class)
    fun selectFirstTextOrNullAsync(selector: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstTextOptionalAsync(selector: String): CompletableFuture<Optional<String>>
    @Throws(WebDriverException::class)
    fun selectTextAllAsync(selector: String): CompletableFuture<List<String>>

    @Throws(WebDriverException::class)
    fun selectFirstAttributeOrNullAsync(selector: String, attrName: String): CompletableFuture<String?>
    @Throws(WebDriverException::class)
    fun selectFirstAttributeOptionalAsync(selector: String, attrName: String): CompletableFuture<Optional<String>>
    @Throws(WebDriverException::class)
    fun selectAttributeAllAsync(selector: String, attrName: String): CompletableFuture<List<String>>
    
    /**
     * Returns the node's attribute values.
     *
     * @param selector The selector to locate the nodes.
     * @return The attribute pairs of the nodes.
     */
    @Throws(WebDriverException::class)
    fun selectAttributesAsync(selector: String): CompletableFuture<Map<String, String>>
    
    /**
     * Set the attribute of an element located by selector.
     *
     * @param selector The CSS query to select an element.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    @Throws(WebDriverException::class)
    fun setAttributeAsync(selector: String, attrName: String, attrValue: String): CompletableFuture<Unit>
    
    /**
     * Set the attribute of all elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    @Throws(WebDriverException::class)
    fun setAttributeAllAsync(selector: String, attrName: String, attrValue: String): CompletableFuture<Unit>
    
    /**
     * Returns the node's property value.
     *
     * @param selector The selector to locate the node.
     * @param propName The property name to retrieve.
     * @return The property value of the node.
     */
    @Throws(WebDriverException::class)
    fun selectFirstPropertyValueOrNullAsync(selector: String, propName: String): CompletableFuture<String?>
    
    /**
     * Returns the nodes' property values.
     *
     * @param selector The selector to locate the nodes.
     * @param propName The property name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The property values of the nodes.
     */
    @Throws(WebDriverException::class)
    fun selectPropertyValueAllAsync(selector: String, propName: String, start: Int = 0, limit: Int = 10000): CompletableFuture<List<String>>
    
    /**
     * Set the property of an element located by selector.
     *
     * @param selector The CSS query to select an element.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    @Throws(WebDriverException::class)
    fun setPropertyAsync(selector: String, propName: String, propValue: String): CompletableFuture<Unit>
    
    /**
     * Set the property of all elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    @Throws(WebDriverException::class)
    fun setPropertyAllAsync(selector: String, propName: String, propValue: String): CompletableFuture<Unit>
    
    /**
     * Find hyperlinks in elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The hyperlinks in the elements.
     */
    @Throws(WebDriverException::class)
    fun selectHyperlinksAsync(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): CompletableFuture<List<Hyperlink>>
    
    /**
     * Find anchor elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The anchors.
     */
    @Throws(WebDriverException::class)
    fun selectAnchorsAsync(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): CompletableFuture<List<GeoAnchor>>
    
    /**
     * Find image elements matching the CSS query.
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The image URLs.
     */
    @Throws(WebDriverException::class)
    fun selectImagesAsync(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): CompletableFuture<List<String>>
    
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
     * Executes JavaScript in the context of the currently selected frame or window with a default value.
     *
     * @param expression Javascript expression to evaluate
     * @param defaultValue Default value to return if evaluation fails
     * @return Remote object value in case of primitive values or the default value
     */
    @Throws(WebDriverException::class)
    fun <T> evaluateAsync(expression: String, defaultValue: T): CompletableFuture<T>
    
    /**
     * Executes JavaScript in the context of the currently selected frame or window.
     *
     * @param expression Javascript expression to evaluate
     * @return Detailed evaluation result
     */
    @Throws(WebDriverException::class)
    fun evaluateDetailAsync(expression: String): CompletableFuture<Any?>
    
    /**
     * Executes JavaScript in the context of the currently selected frame or window.
     * Returns the result as a JSON object if applicable.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values
     */
    @Throws(WebDriverException::class)
    fun evaluateValueAsync(expression: String): CompletableFuture<Any?>
    
    /**
     * Executes JavaScript in the context of the currently selected frame or window with a default value.
     * Returns the result as a JSON object if applicable.
     *
     * @param expression Javascript expression to evaluate
     * @param defaultValue Default value to return if evaluation fails
     * @return Remote object value in case of primitive values or JSON values, or the default value
     */
    @Throws(WebDriverException::class)
    fun <T> evaluateValueAsync(expression: String, defaultValue: T): CompletableFuture<T>
    
    /**
     * Executes JavaScript in the context of the currently selected frame or window.
     * Returns detailed value evaluation results.
     *
     * @param expression Javascript expression to evaluate
     * @return Detailed evaluation result
     */
    @Throws(WebDriverException::class)
    fun evaluateValueDetailAsync(expression: String): CompletableFuture<Any?>

    /**
     * Captures a screenshot of the entire page.
     *
     * @return The screenshot in base64 format.
     */
    @Throws(WebDriverException::class)
    fun captureScreenshotAsync(): CompletableFuture<String?>
    
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
     * Load the url as a resource with Jsoup rather than browser rendering.
     *
     * @param url The URL to load.
     * @return The Jsoup response.
     */
    @Throws(WebDriverException::class)
    fun loadJsoupResourceAsync(url: String): CompletableFuture<Connection.Response>
    
    /**
     * Load url as a resource without browser rendering, with the last page's context,
     * which means, the same headers and cookies.
     * */
    @Throws(WebDriverException::class)
    fun loadResourceAsync(url: String): CompletableFuture<NetworkResourceResponse>
    
    /**
     * Delay for a given amount of time.
     *
     * @param millis The time to delay in milliseconds.
     */
    @Throws(WebDriverException::class)
    fun delayAsync(millis: Long): CompletableFuture<Unit>
    
    /**
     * Delay for a given amount of time.
     *
     * @param duration The time to delay.
     */
    @Throws(WebDriverException::class)
    fun delayAsync(duration: Duration): CompletableFuture<Unit>
    
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
}
