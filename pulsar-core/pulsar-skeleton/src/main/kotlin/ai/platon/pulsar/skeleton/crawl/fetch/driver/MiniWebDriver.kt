package ai.platon.pulsar.skeleton.crawl.fetch.driver

import java.time.Duration

@Suppress("unused")
interface MiniWebDriver {

    /**
     * Opens the specified URL in the web driver.
     *
     * This function navigates the web driver to the provided URL and waits for the navigation to complete.
     * It is a suspend function, meaning it can be used within coroutines for asynchronous execution.
     *
     * Example usage:
     * ```kotlin
     * driver.open("https://www.example.com")
     * ```
     *
     * @param url The URL to which the web driver should navigate. Must be a valid URL string.
     * @throws WebDriverException If an error occurs during navigation or waiting for the navigation to complete.
     */
    @Throws(WebDriverException::class)
    suspend fun open(url: String)

    /**
     * Navigates current page to the given URL.
     *
     * ```kotlin
     * driver.navigateTo("https://www.example.com")
     * driver.waitForNavigation()
     * ```
     *
     * @param url URL to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(url: String)

    /**
     * Navigates current page to the given URL.
     *
     * ```kotlin
     * val entry = NavigateEntry("https://www.example.com?timestamp=11712067353", pageUrl = "https://www.example.com")
     * driver.navigateTo(entry)
     * driver.waitForNavigation()
     * ```
     *
     * @param entry NavigateEntry to navigate page to.
     */
    @Throws(WebDriverException::class)
    suspend fun navigateTo(entry: NavigateEntry)

    /**
     * Navigates the browser to the previous page in the navigation history.
     *
     * This method is expected to use the browser's navigation history to move back to the previous page.
     * It should handle any exceptions that may occur during the navigation process.
     *
     * @throws WebDriverException If an error occurs while navigating back.
     */
    @Throws(WebDriverException::class)
    suspend fun goBack()

    /**
     * Navigates the browser to the next page in the navigation history.
     *
     * This method is expected to use the browser's navigation history to move forward to the next page.
     * It should handle any exceptions that may occur during the navigation process.
     *
     * @throws WebDriverException If an error occurs while navigating forward.
     */
    @Throws(WebDriverException::class)
    suspend fun goForward()

    /**
     * Returns a string representing the current URL that the browser is looking at. The current url is always
     * the main frame's `document.documentURI` if the browser succeed to return it, and is displayed in the browser's
     * address bar.
     *
     * If the browser failed to return a proper url, returns the passed in url to navigate, just like a real user enter
     * a url in the address bar but the browser failed to load the page.
     *
     * NOTE: the url can be non-standard, for example:
     *
     * about:blank
     * chrome://newtab
     * chrome://settings
     *
     * TODO: see [com.github.kklisura.cdt.protocol.types.page.NavigationEntry], userTypeURL
     *
     * @return A string containing the URL of the document, or the passed in url to navigate.
     */
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
     * @return A string containing the URL of the document
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
     * PageSource and outerHTML:
     *
     * - pageSource: returns document HTML markup, will support non-HTML document
     * - outerHTML: returns document HTML markup, for HTML document only
     *
     * ```kotlin
     * val pageSource = driver.pageSource()
     * ```
     *
     * @return The source of the current page
     */
    @Throws(WebDriverException::class)
    suspend fun pageSource(): String?

    /**
     * Returns the cookies of the current page.
     *
     * ```kotlin
     * val cookies = driver.getCookies()
     * ```
     *
     * @return The cookies of the current page.
     */
    @Throws(WebDriverException::class)
    suspend fun getCookies(): List<Map<String, String>>

    /**
     * Wait until the element identified by the selector becomes present in the DOM or timeout.
     *
     * ```kotlin
     * val remainingTime = driver.waitForSelector("h2.title")
     * ```
     *
     * @param selector The selector of the element to wait for.
     * @return The remaining time until timeout when the element becomes present.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String): Duration

    /**
     * Wait until the element identified by the selector becomes present in the DOM or timeout.
     *
     * ```kotlin
     * val remainingTime = driver.waitForSelector("h2.title", 30000)
     * ```
     *
     * @param timeoutMillis The maximum time to wait for the element to become present.
     * @return The remaining time until timeout when the element becomes present.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long

    /**
     * Wait until the current url changes or timeout.
     *
     * ```kotlin
     * val url = "https://www.example.com"
     * driver.navigateTo(url)
     * var remainingTime = driver.waitForNavigation()
     * if (remainingTime > 0) {
     *   driver.click("a[href='/next']")
     *   remainingTime = driver.waitForNavigation(url)
     * }
     * ```
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(oldUrl: String = ""): Duration

    /**
     * Wait until the current url changes or timeout.
     *
     * ```kotlin
     * val url = "https://www.example.com"
     * driver.navigateTo(url)
     * var remainingTime = driver.waitForNavigation(1000)
     * if (remainingTime > 0) {
     *   driver.click("a[href='/next']")
     *   remainingTime = driver.waitForNavigation(url, 1000)
     * }
     * ```
     *
     * @param timeoutMillis The maximum time to wait for the url to change.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(oldUrl: String = "", timeoutMillis: Long): Long

    /**
     * Wait until the current url changes or timeout.
     *
     * ```kotlin
     * val timeout = Duration.ofSeconds(30)
     * val url = "https://www.example.com"
     * driver.navigateTo(url)
     * var remainingTime = driver.waitForNavigation(timeout)
     * if (remainingTime > 0) {
     *   driver.click("a[href='/next']")
     *   remainingTime = driver.waitForNavigation(url, timeout)
     * }
     * ```
     *
     * @param timeout The maximum time to wait for the url to change.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForNavigation(oldUrl: String = "", timeout: Duration): Duration

    /**
     * Await navigation to the specified URL page or timeout if necessary.
     *
     * ```kotlin
     * val newDriver = driver.waitForPage("https://www.example.com", Duration.ofSeconds(30))
     * ```
     *
     * @param url The URL to navigate to.
     * @return The remaining time until timeout when the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitForPage(url: String, timeout: Duration): WebDriver?

    /**
     * Wait until the predicate returns true.
     *
     * ```kotlin
     * val remainingTime = driver.waitUntil {
     *    driver.exists("h2.title")
     * }
     * ```
     *
     * @param predicate The predicate to check.
     * @return The remaining time until timeout when the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(predicate: suspend () -> Boolean): Duration

    /**
     * Wait until the predicate returns true.
     *
     * ```kotlin
     * val remainingTime = driver.waitUntil(10000) {
     *   driver.exists("h2.title")
     * }
     * ```
     *
     * @param timeoutMillis The maximum time to wait for the predicate to return true.
     * @param predicate The predicate to check.
     * @return The remaining time until timeout when the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(timeoutMillis: Long, predicate: suspend () -> Boolean): Long =
        waitUntil(Duration.ofMillis(timeoutMillis), predicate).toMillis()

    /**
     * Wait until the predicate returns true.
     *
     * ```kotlin
     * val remainingTime = driver.waitUntil(Duration.ofSeconds(10)) {
     *    driver.exists("h2.title")
     * }
     * ```
     *
     * @param timeout The maximum time to wait for the predicate to return true.
     * @param predicate The predicate to check.
     * @return The remaining time until timeout when the predicate returns true.
     * */
    @Throws(WebDriverException::class)
    suspend fun waitUntil(timeout: Duration, predicate: suspend () -> Boolean): Duration

    ///////////////////////////////////////////////////////////////////
    // Status checking
    //

    /**
     * Returns whether the element exists.
     *
     * ```kotlin
     * driver.exists("h2.title")
     * ```
     *
     * @param selector - The selector of the element to check.
     * @return Whether the element exists.
     * */
    @Throws(WebDriverException::class)
    suspend fun exists(selector: String): Boolean

    /**
     * Returns whether the element is hidden.
     *
     * ```kotlin
     * driver.isHidden("input[name='q']")
     * ```
     *
     * @param selector - The selector of the element to check.
     * @return Whether the element is hidden.
     * */
    @Throws(WebDriverException::class)
    suspend fun isHidden(selector: String): Boolean = !isVisible(selector)

    /**
     * Returns whether the element is visible.
     *
     * ```kotlin
     * driver.isVisible("input[name='q']")
     * ```
     *
     * @param selector - The selector of the element to check.
     * @return Whether the element is visible.
     * */
    @Throws(WebDriverException::class)
    suspend fun isVisible(selector: String): Boolean

    /**
     * Returns whether the element is visible.
     *
     * ```kotlin
     * driver.visible("input[name='q']")
     * ```
     *
     * @param selector - The selector of the element to check.
     * @return Whether the element is visible.
     * */
    @Throws(WebDriverException::class)
    suspend fun visible(selector: String): Boolean = isVisible(selector)

    /**
     * Returns whether the element is checked.
     *
     * ```kotlin
     * driver.isChecked("input[name='agree']")
     * ```
     *
     * @param selector - The selector of the element to check.
     * @return Whether the element is checked.
     * */
    @Throws(WebDriverException::class)
    suspend fun isChecked(selector: String): Boolean

    /////////////////////////////////////////////////
    // Interacts with the Webpage

    /**
     * Brings the browser window to the front.
     *
     * ```kotlin
     * driver.bringToFront()
     * ```
     */
    @Throws(WebDriverException::class)
    suspend fun bringToFront()

    /**
     * This method fetches an element with `selector` and focuses it. If there's no
     * element matching `selector`, nothing to do.
     *
     * ```kotlin
     * driver.focus("input[name='q']")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     */
    @Throws(WebDriverException::class)
    suspend fun focus(selector: String)

    /**
     * This method emulates inserting text that doesn't come from a key press.
     *
     * ```kotlin
     * driver.type("input[name='q']", "Hello, World!")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
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
     * ```kotlin
     * driver.fill("input[name='q']", "Hello, World!")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus, and then fill text into it. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param text The text to fill.
     */
    @Throws(WebDriverException::class)
    suspend fun fill(selector: String, text: String)

    /**
     * Shortcut for keyboard down and keyboard up.
     *
     * The key is specified as a string, which can be a single character, a key name, or a combination of both.
     * For example, 'a', 'A', 'KeyA', 'Enter', 'Shift+A', and 'Control+Shift+Tab' are all valid keys.
     *
     * ```kotlin
     * driver.press("input[name='q']", "Enter")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus, and then press a key. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param key - A key to press. The key can be a single character, a key name, or a combination of both.
     *      See [Code values for keyboard events](https://developer.mozilla.org/en-US/docs/Web/API/UI_Events/Keyboard_event_code_values)
     */
    @Throws(WebDriverException::class)
    suspend fun press(selector: String, key: String)

    /**
     * This method focuses an element with [selector] and clicks it. If there's no
     * element matching `selector`, nothing to do.
     *
     * ```kotlin
     * driver.click("button[type='submit']")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param count The number of times to click.
     * */
    @Throws(WebDriverException::class)
    suspend fun click(selector: String, count: Int = 1)

    /**
     * focus on an element with [selector] and click it with modifier pressed
     * */
    @Throws(WebDriverException::class)
    suspend fun click(selector: String, modifier: String)

    /**
     * This method clicks an element with [selector] whose text content matches [pattern], and then focuses it.
     * If there's no element matching [selector], or the element's text content doesn't match [pattern], nothing to do.
     *
     * ```kotlin
     * driver.clickTextMatches("button", "submit")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     * */
    @Throws(WebDriverException::class)
    suspend fun clickTextMatches(selector: String, pattern: String, count: Int = 1)

    /**
     * This method clicks an element with [selector] whose attribute name is [attrName] and value matches [pattern],
     * and then focuses it. If there's no element matching [selector], or the element has no attribute [attrName],
     * or the element's attribute value doesn't match [pattern], nothing to do.
     *
     * ```kotlin
     * driver.clickAttributeMatches("button", "type", "submit")
     * ```
     *
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @param attrName The attribute name to match.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     * */
    @Throws(WebDriverException::class)
    suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)

    /**
     * Clicks the nth anchor element in the DOM.
     *
     * This function searches for all anchor (`<a>`) elements within the specified root element,
     * and clicks the nth anchor element (0-based index). If the anchor element exists, it returns
     * the `href` attribute of the clicked anchor element. If the element does not exist, it returns null.
     *
     * ```kotlin
     * driver.clickNthAnchor(100, "body")
     * ```
     *
     * @param n The index of the anchor element to click (0-based).
     * @param rootSelector The CSS selector of the root element to search within (default is "body").
     * @return The href attribute of the clicked anchor element, or null if the element does not exist.
     * @throws WebDriverException If an error occurs while interacting with the WebDriver.
     */
    @Throws(WebDriverException::class)
    suspend fun clickNthAnchor(n: Int, rootSelector: String = "body"): String?

    /**
     * This method check an element with [selector]. If there's no element matching [selector], nothing to do.
     *
     * @param selector - A
     * [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to check. If there are multiple elements satisfying the
     * selector, the first will be checked.
     * */
    @Throws(WebDriverException::class)
    suspend fun check(selector: String)

    /**
     * This method uncheck an element with [selector]. If there's no element matching [selector], nothing to do.
     *
     * @param selector - A
     * [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)
     * of an element to uncheck. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * */
    @Throws(WebDriverException::class)
    suspend fun uncheck(selector: String)

    /**
     * This method fetches an element with [selector], scrolls it into view if needed. If there's no element matching
     * [selector], the method does nothing.
     *
     * ```kotlin
     * driver.scrollTo("h2.title")
     * ```
     *
     * @param selector - A selector to search for element to scroll to. If there are multiple elements satisfying
     * the [selector], the first will be selected.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollTo(selector: String)

    /**
     * The current page frame scrolls down for [count] times.
     *
     * ```kotlin
     * driver.scrollDown(3)
     * ```
     *
     * @param count The times to scroll down.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollDown(count: Int = 1)

    /**
     * The current page frame scrolls up for [count] times.
     *
     * ```kotlin
     * driver.scrollUp(3)
     * ```
     *
     * @param count The times to scroll up.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollUp(count: Int = 1)

    /**
     * The current page frame scrolls to the top.
     *
     * ```kotlin
     * driver.scrollToTop()
     * ```
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToTop()

    /**
     * The current page frame scrolls to the bottom.
     *
     * ```kotlin
     * driver.scrollToBottom()
     * ```
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToBottom()

    /**
     * The current page frame scrolls to the middle.
     *
     * ```kotlin
     * driver.scrollToMiddle(0.2)
     * driver.scrollToMiddle(0.5)
     * driver.scrollToMiddle(0.8)
     * ```
     *
     * @param ratio The ratio of the page to scroll to, 0.0 means the top, 1.0 means the bottom.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToMiddle(ratio: Double)

    /**
     * Scroll to the 2.5th viewport position.
     *
     * ```kotlin
     * driver.scrollToViewport(1.0)
     * driver.scrollToViewport(1.5)
     * driver.scrollToViewport(2.5)
     * driver.scrollToViewport(3.0)
     * ```
     *
     * @param n The viewport number of the page to scroll to (1-based).
     * 1.00 means at the top of the first screen, 2.50 means halfway through the second screen.
     */
    @Throws(WebDriverException::class)
    suspend fun scrollToViewport(n: Double)

    /**
     * The mouse wheels down for [count] times.
     *
     * ```kotlin
     * driver.mouseWheelDown(3)
     * ```
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
     * ```kotlin
     * driver.mouseWheelUp(3)
     * ```
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
     * ```kotlin
     * driver.moveMouseTo(100.0, 200.0)
     * ```
     *
     * @param x The x coordinate to move to.
     * @param y The y coordinate to move to.
     */
    @Throws(WebDriverException::class)
    suspend fun moveMouseTo(x: Double, y: Double)

    /**
     * The mouse moves to the element with [selector].
     *
     * ```kotlin
     * driver.moveMouseTo("h2.title")
     * ```
     *
     * @param deltaX The distance to the left of the element.
     * @param deltaY The distance to the top of the element.
     */
    @Throws(WebDriverException::class)
    suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int = 0)

    /**
     * Performs a drag, dragenter, dragover, and drop in sequence.
     *
     * @param selector - selector of the element to drag from.
     * @param deltaX The distance to drag horizontally.
     * @param deltaY The distance to drag vertically.
     */
    @Throws(WebDriverException::class)
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)

    /**
     * Returns the document's HTML markup.
     *
     * ```kotlin
     * val html = driver.outerHTML()
     * ```
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
     * ```kotlin
     * val html = driver.outerHTML("h2.title")
     * ```
     *
     * @param selector The selector to locate the node.
     * @return The HTML markup of the node.
     * */
    @Throws(WebDriverException::class)
    suspend fun outerHTML(selector: String): String?

    /**
     * Returns the document's text content.
     *
     * If the document does not exist, returns null.
     *
     * ```kotlin
     * val text = driver.textContent()
     * ```
     *
     * @return The text content of the document.
     * */
    suspend fun textContent(): String?

    /**
     * Returns the node's text content, the node is located by [selector].
     *
     * If the node does not exist, returns null.
     *
     * ```kotlin
     * val text = driver.selectFirstTextOrNull("h2.title")
     * ```
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
     * ```kotlin
     * val texts = driver.selectTextAll("h2")
     * ```
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
     * ```kotlin
     * val classes = driver.selectFirstAttributeOrNull("h2.title", "class")
     * ```
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
     * ```kotlin
     * val classes = driver.selectAttributes("h2.title", "class")
     * ```
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
     * ```kotlin
     * val classes = driver.selectAttributeAll("h2.title", "class")
     * ```
     *
     * @param selector The selector to locate the nodes.
     * @param attrName The attribute name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The attribute values of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectAttributeAll(selector: String, attrName: String, start: Int = 0, limit: Int = 10000): List<String>

    /**
     * Returns the node's property value, the node is located by [selector], the property is [propName].
     *
     * If the node does not exist, or the property does not exist, returns null.
     *
     * ```kotlin
     * val classes = driver.selectFirstPropertyOrNull("input#input", "value")
     * ```
     *
     * @param selector The selector to locate the node.
     * @param propName The property name to retrieve.
     * @return The property value of the node.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectFirstPropertyValueOrNull(selector: String, propName: String): String?

    /**
     * Returns the nodes' property values, the nodes are located by [selector], the property is [propName].
     *
     * If the nodes do not exist, or the property does not exist, returns an empty list.
     *
     * ```kotlin
     * val classes = driver.selectPropertyAll("input#input", "value")
     * ```
     *
     * @param selector The selector to locate the nodes.
     * @param propName The property name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The property values of the nodes.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectPropertyValueAll(
        selector: String,
        propName: String,
        start: Int = 0,
        limit: Int = 10000
    ): List<String>

    /**
     * Find image elements matching the CSS query.
     *
     * ```kotlin
     * val images = driver.selectImages("img.product-image")
     * ```
     *
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The image URLs.
     * */
    @Throws(WebDriverException::class)
    suspend fun selectImages(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String>

    /**
     * Executes JavaScript in the context of the currently selected frame or window. If the result is not Javascript object,
     * it is not returned.
     *
     * If you want to execute a function, convert it to IIFE (Immediately Invoked Function Expression).
     *
     * ```kotlin
     * val title = driver.evaluate("document.title")
     * ```
     *
     * Multi-line JavaScript code:
     *
     * ```kotlin
     * val code = """
     * () => {
     *   const a = 10;
     *   const b = 20;
     *   return a * b;
     * }
     * """.trimIndent()
     *
     * val result = driver.evaluate(code)
     * ```
     *
     * ### 🔍 Notes:
     * * **Wrap the code in an IIFE (Immediately Invoked Function Expression)** to return a value.
     * * **Escape line breaks** with `\n`.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or null.
     * */
    @Throws(WebDriverException::class)
    suspend fun evaluate(expression: String): Any?

    /**
     * Capture a screenshot of the current viewport (or primary browsing context) after ensuring any pending layout.
     * If the backend supports element-centric capture this may represent the full page; implementation specific.
     *
     * The target element (if any) is scrolled into view before capture.
     *
     * ```kotlin
     * val base64 = driver.captureScreenshot()
     * val bytes = Base64.getDecoder().decode(base64)
     * ```
     */
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(): String?

    /**
     * Scroll the element matched by [selector] into view (if needed) then take a screenshot of that element's bounding box.
     * Returns a Base64 encoded image (implementation usually PNG/JPEG).
     */
    @Throws(WebDriverException::class)
    suspend fun captureScreenshot(selector: String): String?

    /**
     * Delay for a given amount of time.
     *
     * @param millis The amount of time to delay, in milliseconds.
     * */
    suspend fun delay(millis: Long = 1000) = kotlinx.coroutines.delay(millis)
}
