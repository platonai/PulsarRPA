package ai.platon.pulsar.skeleton.crawl.fetch.driver

data class ToolCall(val domain: String, val name: String, val args: Map<String, Any?>)

/**
 * Executes WebDriver commands provided as string expressions.
 *
 * This class serves as a bridge between text-based automation commands and WebDriver actions.
 * It parses string commands and executes the corresponding WebDriver methods, enabling
 * script-based control of browser automation.
 *
 * ## Key Features:
 * - Supports a wide range of WebDriver commands, such as navigation, interaction, and evaluation.
 * - Provides error handling to ensure robust execution of commands.
 * - Includes a companion object for parsing function calls from string inputs.
 *
 * ## Example Usage:
 *
 * ```kotlin
 * val dispatcher = SimpleCommandDispatcher()
 * val result = dispatcher.execute("driver.open('https://example.com')", driver)
 * ```
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class ToolCallExecutor {

    /**
     * Executes a WebDriver command provided as a string expression.
     *
     * Parses the command string to extract the function name and arguments, then invokes
     * the corresponding WebDriver method. For example, the string "driver.open('https://example.com')"
     * would be parsed and the driver.open() method would be called with the URL argument.
     *
     * @param command The command in text format (e.g., "driver.method(arg1, arg2)").
     * @param driver The WebDriver instance to execute the command on.
     * @return The result of the command execution, or null if the command could not be executed.
     */
    suspend fun execute(command: String, driver: WebDriver): Any? {
        return try {
            execute0(command, driver)
        } catch (e: Exception) {
            println("Error executing command: $command - ${e.message}")
            null
        }
    }

    suspend fun execute(toolCall: ToolCall, driver: WebDriver): Any? {
        // require(toolCall.domain == "driver")

        return try {
            val args = toolCall.args?.values?.toList()?.map { it.toString() } ?: emptyList()
            execute1(toolCall.domain, toolCall.name, args, driver)
        } catch (e: Exception) {
            println("Error executing command: ${toolCall.name} - ${e.message}")
            null
        }
    }

    private suspend fun execute0(command: String, driver: WebDriver): Any? {
        // Extract function name and arguments from the command string
        val (objectName, functionName, args) = parseSimpleFunctionCall(command) ?: return null

        return execute1(objectName, functionName, args, driver)
    }

    /**
     * Extract function name and arguments from the command string
     * */
    private suspend fun execute1(
        objectName: String,
        functionName: String,
        args: List<String>,
        driver: WebDriver
    ): Any? {
        // Execute the appropriate WebDriver method based on the function name
        return when (functionName) {
            "open" -> {
                // Navigate to URL and wait for page to load
                if (args.isNotEmpty()) driver.open(args[0]) else null
            }

            "navigateTo" -> {
                // Navigate to URL without waiting for page to load
                when (args.size) {
                    1 -> driver.navigateTo(args[0])
                    2 -> driver.navigateTo(NavigateEntry(args[0], pageUrl = args[1]))
                    else -> null
                }
            }

            "click" -> {
                // Click on an element with optional repeat count
                when (args.size) {
                    1 -> driver.click(args[0])
                    2 -> driver.click(args[0], args[1].toIntOrNull() ?: 1)
                    else -> if (args.isNotEmpty()) driver.click(args[0]) else null
                }
            }

            "type" -> {
                // Type text into an element
                if (args.size >= 2) driver.type(args[0], args[1]) else null
            }

            "waitForNavigation" -> {
                // Wait for navigation to complete with optional timeout
                when (args.size) {
                    0 -> driver.waitForNavigation()
                    1 -> driver.waitForNavigation(args[0])
                    else -> driver.waitForNavigation(args[0], args[1].toLongOrNull() ?: 30000L)
                }
            }

            "captureScreenshot" -> {
                // Capture screenshot of the page or a specific element
                if (args.isEmpty()) {
                    driver.captureScreenshot()
                } else {
                    driver.captureScreenshot(args[0])
                }
            }

            "scrollDown" -> {
                // Scroll down the page with optional repeat count
                if (args.isEmpty()) {
                    driver.scrollDown()
                } else {
                    driver.scrollDown(args[0].toIntOrNull() ?: 1)
                }
            }

            "scrollUp" -> {
                // Scroll up the page with optional repeat count
                if (args.isEmpty()) {
                    driver.scrollUp()
                } else {
                    driver.scrollUp(args[0].toIntOrNull() ?: 1)
                }
            }

            "scrollToTop" -> {
                // Scroll to the top of the page
                driver.scrollToTop()
            }

            "scrollToBottom" -> {
                // Scroll to the bottom of the page
                driver.scrollToBottom()
            }

            "scrollToMiddle" -> {
                // Scroll to a relative position on the page
                if (args.isNotEmpty()) {
                    driver.scrollToMiddle(args[0].toDoubleOrNull() ?: 0.5)
                } else {
                    driver.scrollToMiddle(0.5)
                }
            }

            "mouseWheelDown" -> {
                // Simulate mouse wheel down with various parameter options
                when (args.size) {
                    0 -> driver.mouseWheelDown()
                    1 -> driver.mouseWheelDown(args[0].toIntOrNull() ?: 1)
                    2 -> driver.mouseWheelDown(args[0].toIntOrNull() ?: 1, args[1].toDoubleOrNull() ?: 0.0)
                    3 -> driver.mouseWheelDown(
                        args[0].toIntOrNull() ?: 1,
                        args[1].toDoubleOrNull() ?: 0.0,
                        args[2].toDoubleOrNull() ?: 0.0
                    )

                    else -> driver.mouseWheelDown(
                        args[0].toIntOrNull() ?: 1,
                        args[1].toDoubleOrNull() ?: 0.0,
                        args[2].toDoubleOrNull() ?: 0.0,
                        args[3].toLongOrNull() ?: 0L
                    )
                }
            }

            "mouseWheelUp" -> {
                // Simulate mouse wheel up with various parameter options
                when (args.size) {
                    0 -> driver.mouseWheelUp()
                    1 -> driver.mouseWheelUp(args[0].toIntOrNull() ?: 1)
                    2 -> driver.mouseWheelUp(args[0].toIntOrNull() ?: 1, args[1].toDoubleOrNull() ?: 0.0)
                    3 -> driver.mouseWheelUp(
                        args[0].toIntOrNull() ?: 1,
                        args[1].toDoubleOrNull() ?: 0.0,
                        args[2].toDoubleOrNull() ?: 0.0
                    )

                    else -> driver.mouseWheelUp(
                        args[0].toIntOrNull() ?: 1,
                        args[1].toDoubleOrNull() ?: 0.0,
                        args[2].toDoubleOrNull() ?: 0.0,
                        args[3].toLongOrNull() ?: 0L
                    )
                }
            }

            "moveMouseTo" -> {
                // Move mouse to coordinates or element with offset
                when (args.size) {
                    2 -> driver.moveMouseTo(args[0].toDoubleOrNull() ?: 0.0, args[1].toDoubleOrNull() ?: 0.0)
                    3 -> driver.moveMouseTo(args[0], args[1].toIntOrNull() ?: 0, args[2].toIntOrNull() ?: 0)
                    else -> null
                }
            }

            "dragAndDrop" -> {
                // Perform drag and drop from an element with x,y offset
                if (args.size >= 3) {
                    driver.dragAndDrop(args[0], args[1].toIntOrNull() ?: 0, args[2].toIntOrNull() ?: 0)
                } else {
                    null
                }
            }

            "outerHTML" -> {
                // Get HTML markup of the page or a specific element
                if (args.isEmpty()) {
                    driver.outerHTML()
                } else {
                    driver.outerHTML(args[0])
                }
            }

            "selectFirstTextOrNull" -> {
                // Get text content of the first element matching the selector
                if (args.isNotEmpty()) driver.selectFirstTextOrNull(args[0]) else null
            }

            "selectTextAll" -> {
                // Get text content of all elements matching the selector
                if (args.isNotEmpty()) driver.selectTextAll(args[0]) else null
            }

            "selectFirstAttributeOrNull" -> {
                // Get attribute value of the first element matching the selector
                if (args.size >= 2) {
                    driver.selectFirstAttributeOrNull(args[0], args[1])
                } else {
                    null
                }
            }

            "selectAttributes" -> {
                // Get all attributes of the first element matching the selector
                if (args.isNotEmpty()) driver.selectAttributes(args[0]) else null
            }

            "selectAttributeAll" -> {
                // Get specified attribute of all elements matching the selector
                when (args.size) {
                    2 -> driver.selectAttributeAll(args[0], args[1])
                    4 -> driver.selectAttributeAll(
                        args[0],
                        args[1],
                        args[2].toIntOrNull() ?: 0,
                        args[3].toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.size >= 2) driver.selectAttributeAll(args[0], args[1]) else null
                }
            }

            "setAttribute" -> {
                // Set attribute on the first element matching the selector
                if (args.size >= 3) {
                    driver.setAttribute(args[0], args[1], args[2])
                } else {
                    null
                }
            }

            "setAttributeAll" -> {
                // Set attribute on all elements matching the selector
                if (args.size >= 3) {
                    driver.setAttributeAll(args[0], args[1], args[2])
                } else {
                    null
                }
            }

            "selectHyperlinks" -> {
                // Extract hyperlinks from elements matching the selector
                when (args.size) {
                    1 -> driver.selectHyperlinks(args[0])
                    3 -> driver.selectHyperlinks(
                        args[0],
                        args[1].toIntOrNull() ?: 0,
                        args[2].toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectHyperlinks(args[0]) else null
                }
            }

            "selectAnchors" -> {
                // Extract anchor elements matching the selector
                when (args.size) {
                    1 -> driver.selectAnchors(args[0])
                    3 -> driver.selectAnchors(
                        args[0],
                        args[1].toIntOrNull() ?: 0,
                        args[2].toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectAnchors(args[0]) else null
                }
            }

            "selectImages" -> {
                // Extract image sources from elements matching the selector
                when (args.size) {
                    1 -> driver.selectImages(args[0])
                    3 -> driver.selectImages(
                        args[0],
                        args[1].toIntOrNull() ?: 0,
                        args[2].toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectImages(args[0]) else null
                }
            }

            "evaluate" -> {
                // Execute JavaScript and return the result
                when (args.size) {
                    1 -> driver.evaluate(args[0])
                    2 -> driver.evaluate(args[0], args[1])
                    else -> if (args.isNotEmpty()) driver.evaluate(args[0]) else null
                }
            }

            "evaluateDetail" -> {
                // Execute JavaScript and return detailed evaluation result
                if (args.isNotEmpty()) driver.evaluateDetail(args[0]) else null
            }

            "clickablePoint" -> {
                // Get the clickable point of an element
                if (args.isNotEmpty()) driver.clickablePoint(args[0]) else null
            }

            "boundingBox" -> {
                // Get the bounding box of an element
                if (args.isNotEmpty()) driver.boundingBox(args[0]) else null
            }

            "newJsoupSession" -> {
                // Create a new Jsoup session with current page context
                driver.newJsoupSession()
            }

            "loadJsoupResource" -> {
                // Load a resource using Jsoup with current page context
                if (args.isNotEmpty()) driver.loadJsoupResource(args[0]) else null
            }

            "loadResource" -> {
                // Load a resource without browser rendering
                if (args.isNotEmpty()) driver.loadResource(args[0]) else null
            }

            "pause" -> {
                // Pause page navigation and pending resource fetches
                driver.pause()
            }

            "stop" -> {
                // Stop all navigations and release resources
                driver.stop()
            }
            // Additional WebDriver methods
            "currentUrl" -> {
                // Get the URL currently displayed in the address bar
                driver.currentUrl()
            }

            "url" -> {
                // Get the document URL
                driver.url()
            }

            "documentURI" -> {
                // Get the document URI
                driver.documentURI()
            }

            "baseURI" -> {
                // Get the base URI used to resolve relative URLs
                driver.baseURI()
            }

            "referrer" -> {
                // Get the referrer of the current page
                driver.referrer()
            }

            "pageSource" -> {
                // Get the source code of the current page
                driver.pageSource()
            }

            "chat" -> {
                // Interact with AI model about a specific element
                if (args.size >= 2) {
                    driver.chat(args[0], args[1])
                } else {
                    null
                }
            }

            "instruct" -> {
                // Execute AI-assisted browser automation
                if (args.isNotEmpty()) driver.instruct(args[0]) else null
            }

            "getCookies" -> {
                // Get all cookies from the current page
                driver.getCookies()
            }

            "deleteCookies" -> {
                // Delete cookies with various parameter options
                when (args.size) {
                    1 -> driver.deleteCookies(args[0])
                    2 -> driver.deleteCookies(args[0], args[1])
                    4 -> driver.deleteCookies(args[0], args[1], args[2], args[3])
                    else -> if (args.isNotEmpty()) driver.deleteCookies(args[0]) else null
                }
            }

            "clearBrowserCookies" -> {
                // Clear all browser cookies
                driver.clearBrowserCookies()
            }

            "waitForSelector" -> {
                // Wait for element to appear in DOM with optional timeout
                when (args.size) {
                    1 -> driver.waitForSelector(args[0])
                    2 -> driver.waitForSelector(args[0], args[1].toLongOrNull() ?: 30000L)
                    else -> if (args.isNotEmpty()) driver.waitForSelector(args[0]) else null
                }
            }

            "waitForPage" -> {
                // Wait for navigation to a specific URL
                if (args.size >= 2) {
                    driver.waitForPage(args[0], java.time.Duration.ofMillis(args[1].toLongOrNull() ?: 30000L))
                } else if (args.isNotEmpty()) {
                    driver.waitForPage(args[0], java.time.Duration.ofMillis(30000L))
                } else {
                    null
                }
            }

            "waitUntil" -> {
                // Cannot be implemented via string commands as it requires a lambda
                null
            }

            "exists" -> {
                // Check if element exists in DOM
                if (args.isNotEmpty()) driver.exists(args[0]) else null
            }

            "isVisible" -> {
                // Check if element is visible
                if (args.isNotEmpty()) driver.isVisible(args[0]) else null
            }

            "visible" -> {
                // Alias for isVisible
                if (args.isNotEmpty()) driver.visible(args[0]) else null
            }

            "isHidden" -> {
                // Check if element is hidden
                if (args.isNotEmpty()) driver.isHidden(args[0]) else null
            }

            "isChecked" -> {
                // Check if element is checked
                if (args.isNotEmpty()) driver.isChecked(args[0]) else null
            }

            "bringToFront" -> {
                // Bring browser window to front
                driver.bringToFront()
            }

            "focus" -> {
                // Focus on an element
                if (args.isNotEmpty()) driver.focus(args[0]) else null
            }

            "fill" -> {
                // Clear and fill text into an element
                if (args.size >= 2) {
                    driver.fill(args[0], args[1])
                } else {
                    null
                }
            }

            "press" -> {
                // Press a keyboard key on an element
                if (args.size >= 2) {
                    driver.press(args[0], args[1])
                } else {
                    null
                }
            }

            "clickTextMatches" -> {
                // Click element whose text content matches a pattern
                when (args.size) {
                    2 -> driver.clickTextMatches(args[0], args[1])
                    3 -> driver.clickTextMatches(args[0], args[1], args[2].toIntOrNull() ?: 1)
                    else -> if (args.size >= 2) driver.clickTextMatches(args[0], args[1]) else null
                }
            }

            "clickMatches" -> {
                // Click element whose attribute matches a pattern
                when (args.size) {
                    3 -> driver.clickMatches(args[0], args[1], args[2])
                    4 -> driver.clickMatches(args[0], args[1], args[2], args[3].toIntOrNull() ?: 1)
                    else -> if (args.size >= 3) driver.clickMatches(args[0], args[1], args[2]) else null
                }
            }

            "clickNthAnchor" -> {
                // Click the nth anchor element in the DOM
                when (args.size) {
                    1 -> driver.clickNthAnchor(args[0].toIntOrNull() ?: 0)
                    2 -> driver.clickNthAnchor(args[0].toIntOrNull() ?: 0, args[1])
                    else -> if (args.isNotEmpty()) driver.clickNthAnchor(args[0].toIntOrNull() ?: 0) else null
                }
            }

            "check" -> {
                // Check a checkbox or radio button
                if (args.isNotEmpty()) driver.check(args[0]) else null
            }

            "uncheck" -> {
                // Uncheck a checkbox
                if (args.isNotEmpty()) driver.uncheck(args[0]) else null
            }

            "scrollTo" -> {
                // Scroll to bring element into view
                if (args.isNotEmpty()) driver.scrollTo(args[0]) else null
            }

            "scrollToScreen" -> {
                // Scroll to a specific screen position by number
                if (args.isNotEmpty()) {
                    driver.scrollToScreen(args[0].toDoubleOrNull() ?: 0.5)
                } else {
                    driver.scrollToScreen(0.5)
                }
            }

            "selectFirstPropertyValueOrNull" -> {
                // Get property value of first element matching selector
                if (args.size >= 2) {
                    driver.selectFirstPropertyValueOrNull(args[0], args[1])
                } else {
                    null
                }
            }

            "selectPropertyValueAll" -> {
                // Get property values from all elements matching selector
                when (args.size) {
                    2 -> driver.selectPropertyValueAll(args[0], args[1])
                    4 -> driver.selectPropertyValueAll(
                        args[0],
                        args[1],
                        args[2].toIntOrNull() ?: 0,
                        args[3].toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.size >= 2) driver.selectPropertyValueAll(args[0], args[1]) else null
                }
            }

            "setProperty" -> {
                // Set property on first element matching selector
                if (args.size >= 3) {
                    driver.setProperty(args[0], args[1], args[2])
                } else {
                    null
                }
            }

            "setPropertyAll" -> {
                // Set property on all elements matching the selector
                if (args.size >= 3) {
                    driver.setPropertyAll(args[0], args[1], args[2])
                } else {
                    null
                }
            }

            "evaluateValue" -> {
                // Execute JavaScript and return JSON value
                when (args.size) {
                    1 -> driver.evaluateValue(args[0])
                    2 -> driver.evaluateValue(args[0], args[1])
                    else -> if (args.isNotEmpty()) driver.evaluateValue(args[0]) else null
                }
            }

            "evaluateValueDetail" -> {
                // Execute JavaScript and return detailed JSON value
                if (args.isNotEmpty()) driver.evaluateValueDetail(args[0]) else null
            }

            "delay" -> {
                // Pause execution for specified milliseconds
                if (args.isNotEmpty()) {
                    driver.delay(args[0].toLongOrNull() ?: 1000L)
                } else {
                    driver.delay(1000L)
                }
            }

            else -> {
                // Command not supported or implemented
                null
            }
        }
    }

    companion object {

        const val TOOL_CALL_LIST = """
- navigateTo(url: String)
- waitForSelector(selector: String, timeoutMillis: Long = 5000)
- exists(selector: String): Boolean
- isVisible(selector: String): Boolean
- focus(selector: String)
- click(selector: String)
- fill(selector: String, text: String)
- press(selector: String, key: String)
- check(selector: String)
- uncheck(selector: String)
- scrollDown(count: Int = 1)
- scrollUp(count: Int = 1)
- scrollTo(selector: String)
- scrollToTop()
- scrollToBottom()
- scrollToMiddle(ratio: Double = 0.5)
- scrollToScreen(screenNumber: Double)
- clickTextMatches(selector: String, pattern: String, count: Int = 1)
- clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
- clickNthAnchor(n: Int, rootSelector: String = "body")
- waitForNavigation(oldUrl: String = "", timeoutMillis: Long = 5000): Long
- goBack()
- goForward()
- captureScreenshot()
- captureScreenshot(selector: String)
- delay(millis: Long)
- stop()
    """

        val SELECTOR_ACTIONS = setOf(
            "click", "fill", "press", "check", "uncheck", "exists", "isVisible", "visible", "focus",
            "scrollTo", "captureScreenshot", "outerHTML", "selectFirstTextOrNull", "selectTextAll",
            "selectFirstAttributeOrNull", "selectAttributes", "selectAttributeAll", "selectHyperlinks",
            "selectAnchors", "selectImages", "selectFirstPropertyValueOrNull", "selectPropertyValueAll",
            "setAttribute", "setAttributeAll", "setProperty", "setPropertyAll", "evaluate", "evaluateValue",
            "evaluateDetail", "evaluateValueDetail", "clickMatches", "clickTextMatches", "clickablePoint",
            "boundingBox", "moveMouseTo", "dragAndDrop"
        )

        val NO_SELECTOR_ACTIONS = setOf(
            "navigateTo", "open", "waitForNavigation", "scrollDown", "scrollUp", "scrollToTop", "scrollToBottom",
            "scrollToMiddle", "mouseWheelDown", "mouseWheelUp", "waitForPage", "bringToFront", "delay",
            "instruct", "getCookies", "deleteCookies", "clearBrowserCookies", "pause", "stop", "currentUrl",
            "url", "documentURI", "baseURI", "referrer", "pageSource", "newJsoupSession", "loadJsoupResource",
            "loadResource", "waitUntil"
        )

        /**
         * Parses a function call from a text string into its components.
         *
         * Extracts the object name, function name, and argument list from a function call string.
         * Handles various formats of function calls, including with and without quotes.
         *
         * Supported formats:
         * - Standard: driver.method("arg1", "arg2")
         * - Single quotes: driver.method('arg1', 'arg2')
         * - No quotes: driver.method(arg1, arg2)
         * - Trailing comma: driver.method(arg1, arg2, )
         *
         * Examples:
         * ```
         * driver.open("https://example.com")
         * driver.navigateTo("https://example.com")
         * driver.scrollToMiddle(0.4)
         * driver.mouseWheelUp(2, 200, 200)
         * ```
         *
         * @see <a href="https://github.com/Kotlin/grammar-tools">Kotlin Grammar Tools</a>
         *
         * @param input The function call string to parse.
         * @return A triple containing (objectName, functionName, argumentList), or null if the input is not a valid function call.
         */
        fun parseSimpleFunctionCall(input: String): Triple<String, String, List<String>>? {
            val regex = """(\w+)\.(\w+)\((.*?)\)""".toRegex()
            val match = regex.find(input) ?: return null

            val (objectName, functionName, argsString) = match.destructured
            if (argsString.isBlank()) {
                return Triple(objectName, functionName, emptyList())
            }

            val args = argsString.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }
                .map { it.removeSurrounding("\'") }
                .map { it.removeSurrounding("\"") }

            return Triple(objectName, functionName, args)
        }

        fun toolCallToDriverLine(tc: ToolCall): String? = when (tc.name) {
            // Navigation
            "navigateTo" -> tc.args["url"]?.let { "driver.navigateTo(\"$it\")" }
            // Backward compatibility for older prompts
            "goto" -> tc.args["url"]?.let { "driver.navigateTo(\"$it\")" }
            // Wait
            "waitForSelector" -> tc.args["selector"]?.let { sel -> "driver.waitForSelector(\"$sel\", ${(tc.args["timeoutMillis"] ?: 5000)}L)" }
            // Status checking (first batch of new tools)
            "exists" -> tc.args["selector"]?.let { "driver.exists(\"$it\")" }
            "isVisible" -> tc.args["selector"]?.let { "driver.isVisible(\"$it\")" }
            "focus" -> tc.args["selector"]?.let { "driver.focus(\"$it\")" }
            // Basic interactions
            "click" -> tc.args["selector"]?.let { "driver.click(\"$it\")" }
            "fill" -> tc.args["selector"]?.let { s -> "driver.fill(\"$s\", \"${tc.args["text"] ?: ""}\")" }
            "press" -> tc.args["selector"]?.let { s -> tc.args["key"]?.let { k -> "driver.press(\"$s\", \"$k\")" } }
            "check" -> tc.args["selector"]?.let { "driver.check(\"$it\")" }
            "uncheck" -> tc.args["selector"]?.let { "driver.uncheck(\"$it\")" }
            // Scrolling
            "scrollDown" -> "driver.scrollDown(${tc.args["count"] ?: 1})"
            "scrollUp" -> "driver.scrollUp(${tc.args["count"] ?: 1})"
            "scrollTo" -> tc.args["selector"]?.let { "driver.scrollTo(\"$it\")" }
            "scrollToTop" -> "driver.scrollToTop()"
            "scrollToBottom" -> "driver.scrollToBottom()"
            "scrollToMiddle" -> "driver.scrollToMiddle(${tc.args["ratio"] ?: 0.5})"
            "scrollToScreen" -> tc.args["screenNumber"]?.let { n -> "driver.scrollToScreen(${n})" }
            // Advanced clicks
            "clickTextMatches" -> tc.args["selector"]?.let { s ->
                val pattern = tc.args["pattern"] ?: return@let null
                val count = tc.args["count"] ?: 1
                "driver.clickTextMatches(\"$s\", \"$pattern\", $count)"
            }

            "clickMatches" -> tc.args["selector"]?.let { s ->
                val attr = tc.args["attrName"] ?: return@let null
                val pattern = tc.args["pattern"] ?: return@let null
                val count = tc.args["count"] ?: 1
                "driver.clickMatches(\"$s\", \"$attr\", \"$pattern\", $count)"
            }

            "clickNthAnchor" -> tc.args["n"]?.let { n ->
                val root = tc.args["rootSelector"]?.toString() ?: "body"
                "driver.clickNthAnchor(${n}, \"$root\")"
            }
            // Enhanced navigation
            "waitForNavigation" -> {
                val oldUrl = tc.args["oldUrl"]?.toString() ?: ""
                val timeout = tc.args["timeoutMillis"] ?: 5000L
                "driver.waitForNavigation(\"$oldUrl\", ${timeout}L)"
            }

            "goBack" -> "driver.goBack()"
            "goForward" -> "driver.goForward()"
            // Screenshots
            "captureScreenshot" -> {
                val sel = tc.args["selector"]?.toString()
                if (sel.isNullOrBlank()) "driver.captureScreenshot()" else "driver.captureScreenshot(\"$sel\")"
            }
            // Timing
            "delay" -> "driver.delay(${tc.args["millis"] ?: 1000}L)"
            else -> null
        }

    }
}
