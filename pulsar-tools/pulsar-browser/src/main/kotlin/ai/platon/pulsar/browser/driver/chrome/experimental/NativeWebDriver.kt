package ai.platon.pulsar.browser.driver.chrome.experimental

import ai.platon.pulsar.common.urls.Hyperlink

interface NativeWebDriver {

    /**
     * @param timeoutMillis The maximum time to wait for the element to become present.
     * @return The remaining time until timeout when the element becomes present.
     */
    suspend fun waitForNodeId(nodeId: Int, timeoutMillis: Long): Long

    /**
     * @param nodeId Identifier of the node.
     * @return Whether the element exists.
     */
    suspend fun exists(nodeId: Int): Boolean

    /**
     * @param nodeId Identifier of the node.
     * @return Whether the element is hidden.
     */
    suspend fun isHidden(nodeId: Int): Boolean

    /**
     * @param nodeId Identifier of the node.
     * @return Whether the element is visible.
     */
    suspend fun isVisible(nodeId: Int): Boolean

    /**
     * @param nodeId Identifier of the node.
     * @return Whether the element is visible.
     */
    suspend fun visible(nodeId: Int): Boolean

    /**
     * @param nodeId Identifier of the node.
     * @return Whether the element is checked.
     */
    suspend fun isChecked(nodeId: Int): Boolean

    suspend fun bringToFront()

    /**
     * @param nodeId Identifier of the node.
     */
    suspend fun focus(nodeId: Int)

    /**
     * @param nodeId Identifier of the node.
     * @param text The text to insert.
     */
    suspend fun type(nodeId: Int, text: String)

    /**
     * @param nodeId Identifier of the node.
     * @param text The text to fill.
     */
    suspend fun fill(nodeId: Int, text: String)

    /**
     * @param nodeId Identifier of the node.
     * @param key - A key to press. The key can be a single character, a key name, or a combination of both.
     */
    suspend fun press(nodeId: Int, key: String)

    /**
     * @param nodeId Identifier of the node.
     * @param count The number of times to click.
     */
    suspend fun click(nodeId: Int, count: Int = 1)

    /**
     * @param nodeId Identifier of the node.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     */
    suspend fun clickTextMatches(nodeId: Int, pattern: String, count: Int = 1)

    /**
     * @param nodeId Identifier of the node.
     * @param attrName The attribute name to match.
     * @param pattern The pattern to match the text content.
     * @param count The number of times to click.
     */
    suspend fun clickMatches(nodeId: Int, attrName: String, pattern: String, count: Int = 1)

    /**
     * @param n The index of the anchor element to click (0-based).
     * @param rootNodeId The CSS nodeId of the root element to search within (default is "body").
     * @return The href attribute of the clicked anchor element, or null if the element does not exist.
     */
    suspend fun clickNthAnchor(n: Int, rootNodeId: Int): String?

    /**
     * @param nodeId Identifier of the node.
     */
    suspend fun check(nodeId: Int)

    /**
     * @param nodeId Identifier of the node.
     */
    suspend fun uncheck(nodeId: Int)

    /**
     * @param nodeId Identifier of the node.
     */
    suspend fun scrollTo(nodeId: Int)

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
     * @param nodeId Identifier of the node.
     * @param deltaX The distance to the left of the element.
     * @param deltaY The distance to the top of the element.
     */
    suspend fun moveMouseTo(nodeId: Int, deltaX: Int, deltaY: Int = 0)

    /**
     * @param nodeId Identifier of the node.
     * @param deltaX The distance to drag horizontally.
     * @param deltaY The distance to drag vertically.
     */
    suspend fun dragAndDrop(nodeId: Int, deltaX: Int, deltaY: Int = 0)

    /**
     * @return The HTML markup of the document.
     */
    suspend fun outerHTML(): String?

    /**
     * @param nodeId Identifier of the node.
     * @return The HTML markup of the node.
     */
    suspend fun outerHTML(nodeId: Int): String?

    /**
     * @param nodeId Identifier of the node.
     * @return The text content of the node.
     */
    suspend fun selectFirstTextOrNull(nodeId: Int, selector: String): String?

    /**
     * @param nodeId Identifier of the node.
     * @return The text contents of the nodes.
     */
    suspend fun selectTextAll(nodeId: Int, selector: String): List<String>

    /**
     * @param nodeId Identifier of the node.
     * @param attrName The attribute name to retrieve.
     * @return The attribute value of the node.
     */
    suspend fun selectFirstAttributeOrNull(nodeId: Int, selector: String, attrName: String): String?

    /**
     * @param nodeId Identifier of the node.
     * @return The attribute pairs of the nodes.
     */
    suspend fun selectAttributes(nodeId: Int): Map<String, String>

    /**
     * @param nodeId Identifier of the node.
     * @param attrName The attribute name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The attribute values of the nodes.
     */
    suspend fun selectAttributeAll(nodeId: Int, attrName: String, start: Int = 0, limit: Int = 10000): List<String>

    /**
     * @param nodeId Identifier of the node.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    suspend fun setAttribute(nodeId: Int, attrName: String, attrValue: String)

    /**
     * @param nodeId Identifier of the node.
     * @param attrName The attribute name to set.
     * @param attrValue The attribute value to set.
     */
    suspend fun setAttributeAll(nodeId: Int, attrName: String, attrValue: String)

    /**
     * @param nodeId Identifier of the node.
     * @param propName The property name to retrieve.
     * @return The property value of the node.
     */
    suspend fun selectFirstPropertyValueOrNull(nodeId: Int, propName: String): String?

    /**
     * @param nodeId Identifier of the node.
     * @param propName The property name to retrieve.
     * @param start The offset of the first node to select.
     * @param limit The maximum number of nodes to select.
     * @return The property values of the nodes.
     */
    suspend fun selectPropertyValueAll(
        nodeId: Int,
        propName: String,
        start: Int = 0,
        limit: Int = 10000
    ): List<String>

    /**
     * @param nodeId Identifier of the node.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    suspend fun setProperty(nodeId: Int, propName: String, propValue: String)

    /**
     * @param nodeId Identifier of the node.
     * @param propName The property name to set.
     * @param propValue The property value to set.
     */
    suspend fun setPropertyAll(nodeId: Int, propName: String, propValue: String)

    /**
     * @param nodeId Identifier of the node.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The hyperlinks in the elements.
     */
    suspend fun selectHyperlinks(nodeId: Int, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink>

    /**
     * @param nodeId Identifier of the node.
     * @param offset The offset of the first element to select.
     * @param limit The maximum number of elements to select.
     * @return The image URLs.
     */
    suspend fun selectImages(nodeId: Int, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String>
}