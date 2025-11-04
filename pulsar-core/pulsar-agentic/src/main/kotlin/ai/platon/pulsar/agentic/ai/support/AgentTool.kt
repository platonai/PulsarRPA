package ai.platon.pulsar.agentic.ai.support

object AgentTool {

    /**
     * The `TOOL_CALL_LIST` is written using kotlin syntax to express the tool's `domain`, `method`, `arguments`.
     * */
    const val TOOL_CALL_SPECIFICATION = """
// domain/object: driver
driver.navigateTo(url: String)
driver.waitForSelector(selector: String, timeoutMillis: Long = 3000)
driver.exists(selector: String): Boolean
driver.isVisible(selector: String): Boolean
driver.focus(selector: String)
driver.click(selector: String)                         // focus on an element with [selector] and click it
driver.click(selector: String, modifier: String)       // focus on an element with [selector] and click it with modifier pressed
driver.fill(selector: String, text: String)
driver.type(selector: String, text: String)
driver.press(selector: String, key: String)
driver.check(selector: String)
driver.uncheck(selector: String)
driver.scrollTo(selector: String)
driver.scrollToTop()
driver.scrollToBottom()
driver.scrollToMiddle(ratio: Double = 0.5)
driver.scrollBy(pixels: Double = 200.0): Double
driver.scrollToViewport(n: Double)                     // scroll to the [n]th viewport position, 1-based
driver.goBack()
driver.goForward()
driver.textContent(selector: String): String?          // Returns the node's text content, the node is located by [selector]. If the node does not exist, returns null.
driver.delay(millis: Long)

// domain/object: browser
browser.switchTab(tabId: String): Int

    """

    const val AGENT_TOOL_CALL_LIST = """
agent.observe(instruction: String): List<ObserveResult>
agent.observe(options: ObserveOptions): List<ObserveResult>
agent.act(action: String): ActResult
agent.act(action: ActionOptions): ActResult
agent.act(observe: ObserveResult): ActResult
agent.extract(instruction: String): ExtractResult
agent.extract(options: ExtractOptions): ExtractResult
    """

    val SUPPORTED_TOOL_CALLS = TOOL_CALL_SPECIFICATION
        .split("\n").asSequence()
        .map { it.trim() }
        .filterNot { it.startsWith("//") }
        .filter { it.contains("(") }
        .toList()

    @Suppress("unused")
    val SUPPORTED_ACTIONS = SUPPORTED_TOOL_CALLS.map { it.substringBefore("(") }

    val TOOL_ALIASES = mapOf(
        "driver.goto" to "driver.navigateTo",
        "driver.textContent" to "driver.selectFirstTextOrNull"
    )

    val SELECTOR_ACTIONS = setOf(
        "click", "fill", "press", "check", "uncheck", "exists", "isVisible", "visible", "focus",
        "scrollTo", "captureScreenshot", "outerHTML", "selectFirstTextOrNull", "selectTextAll",
        "selectFirstAttributeOrNull", "selectAttributes", "selectAttributeAll", "selectHyperlinks",
        "selectAnchors", "selectImages", "selectFirstPropertyValueOrNull", "selectPropertyValueAll",
        "setAttribute", "setAttributeAll", "setProperty", "setPropertyAll", "evaluate", "evaluateValue",
        "evaluateDetail", "evaluateValueDetail", "clickMatches", "clickTextMatches", "clickablePoint",
        "boundingBox", "moveMouseTo", "dragAndDrop"
    )

    @Suppress("unused")
    val NO_SELECTOR_ACTIONS = setOf(
        "navigateTo", "open", "waitForNavigation", "scrollDown", "scrollUp", "scrollBy", "scrollToTop", "scrollToBottom",
        "scrollToMiddle", "mouseWheelDown", "mouseWheelUp", "waitForPage", "bringToFront", "delay",
        "instruct", "getCookies", "deleteCookies", "clearBrowserCookies", "pause", "stop", "currentUrl",
        "url", "documentURI", "baseURI", "referrer", "pageSource", "newJsoupSession", "loadJsoupResource",
        "loadResource", "waitUntil"
    )

    val MAY_NAVIGATE_ACTIONS = setOf("navigateTo", "click", "goBack", "goForward")
}
