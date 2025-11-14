package ai.platon.pulsar.agentic.tools

object ToolSpecification {

    const val AGENT_TOOL_CALL_LIST_BAK = """
// domain: agent
agent.observe(instruction: String): List<ObserveResult>
agent.observe(options: ObserveOptions): List<ObserveResult>
agent.act(action: String): ActResult
agent.act(action: ActionOptions): ActResult
agent.act(observe: ObserveResult): ActResult
agent.extract(instruction: String): ExtractResult
agent.extract(options: ExtractOptions): ExtractResult
agent.done()
    """

    const val AGENT_TOOL_CALL_LIST = """
// domain: agent
agent.done()
    """

    const val FILE_TOOL_CALL_LIST = """
// domain: fs
fs.writeString(filename: String, content: String)
fs.readString(filename: String)
fs.replaceContent(filename: String, oldStr: String, newStr: String)
    """

    /**
     * The `TOOL_CALL_LIST` is written using kotlin syntax to express the tool's `domain`, `method`, `arguments`.
     * */
    const val TOOL_CALL_SPECIFICATION = """
// domain: driver
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
driver.reload()
driver.goBack()
driver.goForward()
driver.textContent(): String?                            // Returns the document's text content.
driver.selectFirstTextOrNull(selector: String): String?  // Returns the node's text content including it's descendants, the node is located by [selector]. If the node does not exist, returns null.
driver.delay(millis: Long)

// domain: browser
browser.switchTab(tabId: String): Int
browser.closeTab(tabId: String)

// domain: fs
fs.writeString(filename: String, content: String)
fs.readString(filename: String): String
fs.replaceContent(filename: String, oldStr: String, newStr: String): String

// domain: agent
agent.extract(instruction: String, schema: String): String // Extract data with given JSON schema

// domain: system
system.help(domain: String, method: String): String        // get help for a tool call

    """

    val SUPPORTED_TOOL_CALLS = TOOL_CALL_SPECIFICATION
        .split("\n").asSequence()
        .map { it.trim() }
        .filterNot { it.startsWith("//") }
        .filter { it.contains("(") }
        .toList()

    val SUPPORTED_ACTIONS = SUPPORTED_TOOL_CALLS.map { it.substringBefore("(") }

    val MAY_NAVIGATE_ACTIONS = setOf("navigateTo", "click", "reload", "goBack", "goForward")
}
