package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.common.urls.Hyperlink
import java.time.Duration

interface MiniWebDriver {
    /**
     * @param url URL to navigate page to.
     */
    suspend fun navigateTo(url: String)

    /**
     * @return A string containing the URL of the document.
     */
    suspend fun url(): String

    /**
     * @return The cookies of the current page.
     */
    suspend fun getCookies(): List<Map<String, String>>

    /**
     * @param name Name of the cookies to remove.
     * @param url If specified, deletes all the cookies with the given name where domain and path match provided URL.
     * @param domain If specified, deletes only cookies with the exact domain.
     * @param path If specified, deletes only cookies with the exact path.
     */
    suspend fun deleteCookies(name: String, url: String? = null, domain: String? = null, path: String? = null)

    suspend fun clearBrowserCookies()

    /**
     * @param timeoutMillis The maximum time to wait for the element to become present.
     * @return The remaining time until timeout when the element becomes present.
     */
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long

    suspend fun waitForNavigation(oldUrl: String = ""): Duration

    /**
     * @param timeoutMillis The maximum time to wait for the url to change.
     */
    suspend fun waitForNavigation(oldUrl: String = "", timeoutMillis: Long): Long

    /**
     * @param selector - The selector of the element to check.
     * @return Whether the element exists.
     */
    suspend fun exists(selector: String): Boolean

    /**
     * @param selector - The selector of the element to check.
     * @return Whether the element is hidden.
     */
    suspend fun isHidden(selector: String): Boolean

    /**
     * @param selector - The selector of the element to check.
     * @return Whether the element is visible.
     */
    suspend fun isVisible(selector: String): Boolean

    /**
     * @param selector - The selector of the element to check.
     * @return Whether the element is visible.
     */
    suspend fun visible(selector: String): Boolean

    /**
     * @param selector - The selector of the element to check.
     * @return Whether the element is checked.
     */
    suspend fun isChecked(selector: String): Boolean

    suspend fun bringToFront()

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     */
    suspend fun focus(selector: String)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     * @param text The text to insert.
     */
    suspend fun type(selector: String, text: String)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus, and then fill text into it. If there are multiple elements satisfying the selector, the first will be focused.
     * @param text The text to fill.
     */
    suspend fun fill(selector: String, text: String)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus, and then press a key. If there are multiple elements satisfying the selector, the first will be focused.
     * @param key - A key to press. The key can be a single character, a key name, or a combination of both.
     */
    suspend fun press(selector: String, key: String)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     * @param count The number of times to click.
     */
    suspend fun click(selector: String, count: Int = 1)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     */
    suspend fun clickTextMatches(selector: String, pattern: String, count: Int = 1)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to focus. If there are multiple elements satisfying the selector, the first will be focused.
     * @param attrName The attribute name to match.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     */
    suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)

    /**
     * @param n The index of the anchor element to click (0-based).
     * @param rootSelector The CSS selector of the root element to search within (default is "body").
     * @return The href attribute of the clicked anchor element, or null if the element does not exist.
     */
    suspend fun clickNthAnchor(n: Int, rootSelector: String = "body"): String?

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to check. If there are multiple elements satisfying the selector, the first will be checked.
     */
    suspend fun check(selector: String)

    /**
     * @param selector - A [selector](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors) of an element to uncheck. If there are multiple elements satisfying the selector, the first will be focused.
     */
    suspend fun uncheck(selector: String)

    /**
     * @param selector - A selector to search for element to scroll to. If there are multiple elements satisfying the [selector], the first will be selected.
     */
    suspend fun scrollTo(selector: String)

    /**
     * @param count The times to scroll down.
     */
    suspend fun scrollDown(count: Int = 1)

    /**
     * @param count The times to scroll up.
     */
    suspend fun scrollUp(count: Int = 1)

    suspend fun scrollToTop()

    suspend fun scrollToBottom()

    /**
     * @param ratio The ratio of the page to scroll to, 0.0 means the top, 1.0 means the bottom.
     */
    suspend fun scrollToMiddle(ratio: Double)

    /**
     * @param screenNumber The screen number of the page to scroll to (0-based).
     * 0.00 means at the top of the first screen, 1.50 means halfway through the second screen.
     */
    suspend fun scrollToScreen(screenNumber: Double)

    /**
     * @param count The times to wheel down.
     * @param deltaX The distance to wheel horizontally.
     * @param deltaY The distance to wheel vertically.
     * @param delayMillis The delay time in milliseconds.
     */
    suspend fun mouseWheelDown(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = 150.0, delayMillis: Long = 0)

    /**
     * @param count The times to wheel up.
     * @param deltaX The distance to wheel horizontally.
     * @param deltaY The distance to wheel vertically.
     * @param delayMillis The delay time in milliseconds.
     */
    suspend fun mouseWheelUp(count: Int = 1, deltaX: Double = 0.0, deltaY: Double = -150.0, delayMillis: Long = 0)

    /**
     * @param x The x coordinate to move to.
     * @param y The y coordinate to move to.
     */
    suspend fun moveMouseTo(x: Double, y: Double)

    /**
     * @param deltaX The distance to the left of the element.
     * @param deltaY The distance to the top of the element.
     */
    suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int = 0)

    /**
     * @param selector - selector of the element to drag from.
     * @param deltaX The distance to drag horizontally.
     * @param deltaY The distance to drag vertically.
     */
    suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int = 0)

    /**
     * @return The HTML markup of the document.
     */
    suspend fun outerHTML(): String?

    /**
     * @param selector The selector to locate the node.
     * @return The HTML markup of the node.
     */
    suspend fun outerHTML(selector: String): String?

    /**
     * @param selector The selector to locate the node.
     * @return The text content of the node.
     */
    suspend fun selectFirstTextOrNull(selector: String): String?

    /**
     * @param selector The selector to locate the nodes.
     * @return The text contents of the nodes.
     */
    suspend fun selectTextAll(selector: String): List<String>

    /**
     * @param selector The selector to locate the node.
     * @param attrName The attribute name to retrieve.
     * @return The attribute value of the node.
     */
    suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String?

    /**
     * @param selector The selector to locate the nodes.
     * @return The attribute pairs of the nodes.
     */
    suspend fun selectAttributes(selector: String): Map<String, String>

    /**
     * @param selector The selector to locate the nodes.
     * @param attrName The attribute name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The attribute values of the nodes.
     */
    suspend fun selectAttributeAll(selector: String, attrName: String, start: Int = 0, limit: Int = 10000): List<String>

    /**
     * @param selector The CSS query to select an element.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    suspend fun setAttribute(selector: String, attrName: String, attrValue: String)

    /**
     * @param selector The CSS query to select elements.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    suspend fun setAttributeAll(selector: String, attrName: String, attrValue: String)

    /**
     * @param selector The selector to locate the node.
     * @param propName The property name to retrieve.
     * @return The property value of the node.
     */
    suspend fun selectFirstPropertyValueOrNull(selector: String, propName: String): String?

    /**
     * @param selector The selector to locate the nodes.
     * @param propName The property name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The property values of the nodes.
     */
    suspend fun selectPropertyValueAll(
        selector: String,
        propName: String,
        start: Int = 0,
        limit: Int = 10000
    ): List<String>

    /**
     * @param selector The CSS query to select an element.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    suspend fun setProperty(selector: String, propName: String, propValue: String)

    /**
     * @param selector The CSS query to select elements.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    suspend fun setPropertyAll(selector: String, propName: String, propValue: String)

    /**
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The hyperlinks in the elements.
     */
    suspend fun selectHyperlinks(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink>

    /**
     * @param selector The CSS query to select elements.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The image URLs.
     */
    suspend fun selectImages(selector: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String>

    /**
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or null.
     */
    suspend fun evaluate(expression: String): Any?

    /**
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     */
    suspend fun evaluateValue(expression: String): Any?

    /**
     * @return The screenshot of the element in base64 format.
     */
    suspend fun captureScreenshot(): String?

    /**
     * @param selector The selector of the element to capture.
     * @return The screenshot of the element in base64 format.
     */
    suspend fun captureScreenshot(selector: String): String?

    /**
     * @param millis The amount of time to delay, in milliseconds.
     */
    suspend fun delay(millis: Long)
}