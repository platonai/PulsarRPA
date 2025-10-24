package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.support.ToolCall
import javax.script.ScriptEngineManager

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
 * val executor = ToolCallExecutor()
 * val result = executor.execute("driver.open('https://example.com')", driver)
 * ```
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class ToolCallExecutor {
    private val logger = getLogger(this)
    private val engine = ScriptEngineManager().getEngineByExtension("kts")

    /**
     * Evaluate [expression].
     *
     * Slow, unsafe.
     *
     * ```kotlin
     * eval("""driver.click("#submit")""", driver)
     * ```
     * */
    fun eval(expression: String, driver: WebDriver): Any? {
        return try {
            engine.put("driver", driver)
            engine.eval(expression)
        } catch (e: Exception) {
            logger.warn("Error eval expression: {} - {}", expression, e.brief())
            null
        }
    }

    /**
     * Executes a WebDriver command provided as a string expression.
     *
     * Parses the command string to extract the function name and arguments, then invokes
     * the corresponding WebDriver method. For example, the string "driver.open('https://example.com')"
     * would be parsed and the driver.open() method would be called with the URL argument.
     *
     * @param expression The expression(e.g., "driver.method(arg1, arg2)").
     * @param driver The WebDriver instance to execute the command on.
     * @return The result of the command execution, or null if the command could not be executed.
     */
    suspend fun execute(expression: String, driver: WebDriver): Any? {
        return try {
            val r = execute0(expression, driver)
            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            null
        }
    }

    suspend fun execute(toolCall: ToolCall, driver: WebDriver): Any? {
        // require(toolCall.domain == "driver")
        val expression = toolCallToExpression(toolCall) ?: return null

        return try {
            execute(expression, driver)
        } catch (e: Exception) {
            logger.warn("Error executing TOOL CALL: {} - {}", toolCall, e.brief())
            null
        }
    }

    private suspend fun execute0(command: String, driver: WebDriver): Any? {
        // Extract function name and arguments from the command string
        val (objectName, functionName, args) = parseKotlinFunctionExpression(command) ?: return null

        return execute1(objectName, functionName, args, driver)
    }

    /**
     * Extract function name and arguments from the command string
     * */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun execute1(
        objectName: String,
        functionName: String,
        args: Map<String, Any?>,
        driver: WebDriver
    ): Any? {
        // Execute the appropriate WebDriver method based on the function name
        val arg0 = args["0"]?.toString()
        val arg1 = args["1"]?.toString()
        val arg2 = args["2"]?.toString()
        val arg3 = args["3"]?.toString()
        return when (functionName) {
            "open" -> {
                // Navigate to URL and wait for page to load
                if (args.isNotEmpty()) driver.open(arg0!!) else null
            }

            "navigateTo" -> {
                // Navigate to URL without waiting for page to load
                when (args.size) {
                    1 -> driver.navigateTo(arg0!!)
                    2 -> driver.navigateTo(NavigateEntry(arg0!!, pageUrl = arg1!!))
                    else -> null
                }
            }

            "click" -> {
                // Click on an element with optional repeat count
                when (args.size) {
                    1 -> driver.click(arg0!!)
                    2 -> driver.click(arg0!!, arg1!!.toIntOrNull() ?: 1)
                    else -> if (args.isNotEmpty()) driver.click(arg0!!) else null
                }
            }

            "type" -> {
                // Type text into an element
                if (args.size >= 2) driver.type(arg0!!, arg1!!) else null
            }

            "waitForNavigation" -> {
                // Wait for navigation to complete with optional timeout
                when (args.size) {
                    0 -> driver.waitForNavigation()
                    1 -> driver.waitForNavigation(arg0!!)
                    else -> driver.waitForNavigation(arg0!!, arg1!!.toLongOrNull() ?: 30000L)
                }
            }

            "captureScreenshot" -> {
                // Capture screenshot of the page or a specific element
                if (args.isEmpty()) {
                    driver.captureScreenshot()
                } else {
                    driver.captureScreenshot(arg0!!)
                }
            }

            "scrollDown" -> {
                // Scroll down the page with optional repeat count
                if (args.isEmpty()) {
                    driver.scrollDown()
                } else {
                    driver.scrollDown(arg0!!.toIntOrNull() ?: 1)
                }
            }

            "scrollUp" -> {
                // Scroll up the page with optional repeat count
                if (args.isEmpty()) {
                    driver.scrollUp()
                } else {
                    driver.scrollUp(arg0!!.toIntOrNull() ?: 1)
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
                    driver.scrollToMiddle(arg0!!.toDoubleOrNull() ?: 0.5)
                } else {
                    driver.scrollToMiddle(0.5)
                }
            }

            "mouseWheelDown" -> {
                // Simulate mouse wheel down with various parameter options
                when (args.size) {
                    0 -> driver.mouseWheelDown(1, 0.0, 0.0, 0)
                    1 -> driver.mouseWheelDown(arg0!!.toIntOrNull() ?: 1)
                    2 -> driver.mouseWheelDown(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        0.0,
                        0
                    )
                    3 -> driver.mouseWheelDown(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        arg2!!.toDoubleOrNull() ?: 0.0
                    )

                    else -> driver.mouseWheelDown(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        arg2!!.toDoubleOrNull() ?: 0.0,
                        arg3!!.toLongOrNull() ?: 0L
                    )
                }
            }

            "mouseWheelUp" -> {
                // Simulate mouse wheel up with various parameter options
                when (args.size) {
                    0 -> driver.mouseWheelUp(1, 0.0, 0.0, 0)
                    1 -> driver.mouseWheelUp(arg0!!.toIntOrNull() ?: 1)
                    2 -> driver.mouseWheelUp(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        0.0,
                        0
                    )
                    3 -> driver.mouseWheelUp(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        arg2!!.toDoubleOrNull() ?: 0.0
                    )

                    else -> driver.mouseWheelUp(
                        arg0!!.toIntOrNull() ?: 1,
                        arg1!!.toDoubleOrNull() ?: 0.0,
                        arg2!!.toDoubleOrNull() ?: 0.0,
                        arg3!!.toLongOrNull() ?: 0L
                    )
                }
            }

            "moveMouseTo" -> {
                // Move mouse to coordinates or element with offset
                when (args.size) {
                    2 -> {
                        val x = arg0?.toDoubleOrNull()
                        val y = arg1?.toDoubleOrNull()
                        if (x != null && y != null) {
                            driver.moveMouseTo(x, y)
                        } else {
                            // Treat as selector + deltaX (deltaY defaults to 0)
                            val sel = arg0 ?: return null
                            driver.moveMouseTo(sel, arg1?.toIntOrNull() ?: 0)
                        }
                    }
                    3 -> driver.moveMouseTo(arg0!!, arg1!!.toIntOrNull() ?: 0, arg2!!.toIntOrNull() ?: 0)
                    else -> null
                }
            }

            "dragAndDrop" -> {
                // Perform drag and drop from an element with x,y offset
                if (args.size >= 3) {
                    driver.dragAndDrop(arg0!!, arg1!!.toIntOrNull() ?: 0, arg2!!.toIntOrNull() ?: 0)
                } else {
                    null
                }
            }

            "outerHTML" -> {
                // Get HTML markup of the page or a specific element
                if (args.isEmpty()) {
                    driver.outerHTML()
                } else {
                    driver.outerHTML(arg0!!)
                }
            }

            "selectFirstTextOrNull" -> {
                // Get text content of the first element matching the selector
                if (args.isNotEmpty()) driver.selectFirstTextOrNull(arg0!!) else null
            }

            "selectTextAll" -> {
                // Get text content of all elements matching the selector
                if (args.isNotEmpty()) driver.selectTextAll(arg0!!) else null
            }

            "selectFirstAttributeOrNull" -> {
                // Get attribute value of the first element matching the selector
                if (args.size >= 2) {
                    driver.selectFirstAttributeOrNull(arg0!!, arg1!!)
                } else {
                    null
                }
            }

            "selectAttributes" -> {
                // Get all attributes of the first element matching the selector
                if (args.isNotEmpty()) driver.selectAttributes(arg0!!) else null
            }

            "selectAttributeAll" -> {
                // Get specified attribute of all elements matching the selector
                when (args.size) {
                    2 -> driver.selectAttributeAll(arg0!!, arg1!!)
                    4 -> driver.selectAttributeAll(
                        arg0!!,
                        arg1!!,
                        arg2!!.toIntOrNull() ?: 0,
                        arg3!!.toIntOrNull() ?: 10000
                    )

                    else -> if (args.size >= 2) driver.selectAttributeAll(arg0!!, arg1!!) else null
                }
            }

            "setAttribute" -> {
                // Set attribute on the first element matching the selector
                if (args.size >= 3) {
                    driver.setAttribute(arg0!!, arg1!!, arg2!!)
                } else {
                    null
                }
            }

            "setAttributeAll" -> {
                // Set attribute on all elements matching the selector
                if (args.size >= 3) {
                    driver.setAttributeAll(arg0!!, arg1!!, arg2!!)
                } else {
                    null
                }
            }

            "selectHyperlinks" -> {
                // Extract hyperlinks from elements matching the selector
                when (args.size) {
                    1 -> driver.selectHyperlinks(arg0!!)
                    3 -> driver.selectHyperlinks(
                        arg0!!,
                        arg1!!.toIntOrNull() ?: 1,
                        arg2!!.toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectHyperlinks(arg0!!) else null
                }
            }

            "selectAnchors" -> {
                // Extract anchor elements matching the selector
                when (args.size) {
                    1 -> driver.selectAnchors(arg0!!)
                    3 -> driver.selectAnchors(
                        arg0!!,
                        arg1!!.toIntOrNull() ?: 1,
                        arg2!!.toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectAnchors(arg0!!) else null
                }
            }

            "selectImages" -> {
                // Extract image sources from elements matching the selector
                when (args.size) {
                    1 -> driver.selectImages(arg0!!)
                    3 -> driver.selectImages(
                        arg0!!,
                        arg1!!.toIntOrNull() ?: 1,
                        arg2!!.toIntOrNull() ?: Integer.MAX_VALUE
                    )

                    else -> if (args.isNotEmpty()) driver.selectImages(arg0!!) else null
                }
            }

            "evaluate" -> {
                // Execute JavaScript and return the result
                when (args.size) {
                    1 -> driver.evaluate(arg0!!)
                    2 -> driver.evaluate(arg0!!, arg1!!)
                    else -> if (args.isNotEmpty()) driver.evaluate(arg0!!) else null
                }
            }

            "evaluateDetail" -> {
                // Execute JavaScript and return detailed evaluation result
                if (args.isNotEmpty()) driver.evaluateDetail(arg0!!) else null
            }

            "clickablePoint" -> {
                // Get the clickable point of an element
                if (args.isNotEmpty()) driver.clickablePoint(arg0!!) else null
            }

            "boundingBox" -> {
                // Get the bounding box of an element
                if (args.isNotEmpty()) driver.boundingBox(arg0!!) else null
            }

            "newJsoupSession" -> {
                // Create a new Jsoup session with current page context
                driver.newJsoupSession()
            }

            "loadJsoupResource" -> {
                // Load a resource using Jsoup with current page context
                if (args.isNotEmpty()) driver.loadJsoupResource(arg0!!) else null
            }

            "loadResource" -> {
                // Load a resource without browser rendering
                if (args.isNotEmpty()) driver.loadResource(arg0!!) else null
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

            "getCookies" -> {
                // Get all cookies from the current page
                driver.getCookies()
            }

            "deleteCookies" -> {
                // Delete cookies with various parameter options
                when (args.size) {
                    1 -> driver.deleteCookies(arg0!!, null, null, null)
                    2 -> driver.deleteCookies(arg0!!, arg1!!)
                    4 -> driver.deleteCookies(arg0!!, arg1, arg2, arg3)
                    else -> if (args.isNotEmpty()) driver.deleteCookies(arg0!!, null, null, null) else null
                }
            }

            "clearBrowserCookies" -> {
                // Clear all browser cookies
                driver.clearBrowserCookies()
            }

            "waitForSelector" -> {
                // Wait for element to appear in DOM with optional timeout
                when (args.size) {
                    1 -> driver.waitForSelector(arg0!!)
                    2 -> driver.waitForSelector(arg0!!, arg1!!.toLongOrNull() ?: 30000L)
                    else -> if (args.isNotEmpty()) driver.waitForSelector(arg0!!) else null
                }
            }

            "waitForPage" -> {
                // Wait for navigation to a specific URL
                if (args.size >= 2) {
                    driver.waitForPage(arg0!!, java.time.Duration.ofMillis(arg1!!.toLongOrNull() ?: 30000L))
                } else if (args.isNotEmpty()) {
                    driver.waitForPage(arg0!!, java.time.Duration.ofMillis(30000L))
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
                if (args.isNotEmpty()) driver.exists(arg0!!) else null
            }

            "isVisible" -> {
                // Check if element is visible
                if (args.isNotEmpty()) driver.isVisible(arg0!!) else null
            }

            "visible" -> {
                // Alias for isVisible
                if (args.isNotEmpty()) driver.visible(arg0!!) else null
            }

            "isHidden" -> {
                // Check if element is hidden
                if (args.isNotEmpty()) driver.isHidden(arg0!!) else null
            }

            "isChecked" -> {
                // Check if element is checked
                if (args.isNotEmpty()) driver.isChecked(arg0!!) else null
            }

            "bringToFront" -> {
                // Bring browser window to front
                driver.bringToFront()
            }

            "focus" -> {
                // Focus on an element
                if (args.isNotEmpty()) driver.focus(arg0!!) else null
            }

            "fill" -> {
                // Clear and fill text into an element
                if (args.size >= 2) {
                    driver.fill(arg0!!, arg1!!)
                } else {
                    null
                }
            }

            "press" -> {
                // Press a keyboard key on an element
                if (args.size >= 2) {
                    driver.press(arg0!!, arg1!!)
                } else {
                    null
                }
            }

            "clickTextMatches" -> {
                // Click element whose text content matches a pattern
                when (args.size) {
                    2 -> driver.clickTextMatches(arg0!!, arg1!!)
                    3 -> driver.clickTextMatches(arg0!!, arg1!!, arg2!!.toIntOrNull() ?: 1)
                    else -> if (args.size >= 2) driver.clickTextMatches(arg0!!, arg1!!) else null
                }
            }

            "clickMatches" -> {
                // Click element whose attribute matches a pattern
                when (args.size) {
                    3 -> driver.clickMatches(arg0!!, arg1!!, arg2!!)
                    4 -> driver.clickMatches(arg0!!, arg1!!, arg2!!, arg3!!.toIntOrNull() ?: 1)
                    else -> if (args.size >= 3) driver.clickMatches(arg0!!, arg1!!, arg2!!) else null
                }
            }

            "clickNthAnchor" -> {
                // Click the nth anchor element in the DOM
                when (args.size) {
                    1 -> driver.clickNthAnchor(arg0!!.toIntOrNull() ?: 0)
                    2 -> driver.clickNthAnchor(arg0!!.toIntOrNull() ?: 0, arg1!!)
                    else -> if (args.isNotEmpty()) driver.clickNthAnchor(arg0!!.toIntOrNull() ?: 0) else null
                }
            }

            "check" -> {
                // Check a checkbox or radio button
                if (args.isNotEmpty()) driver.check(arg0!!) else null
            }

            "uncheck" -> {
                // Uncheck a checkbox
                if (args.isNotEmpty()) driver.uncheck(arg0!!) else null
            }

            "scrollTo" -> {
                // Scroll to bring element into view
                if (args.isNotEmpty()) driver.scrollTo(arg0!!) else null
            }

            "scrollToScreen" -> {
                // Scroll to a specific screen position by number
                if (args.isNotEmpty()) {
                    driver.scrollToScreen(arg0!!.toDoubleOrNull() ?: 0.5)
                } else {
                    driver.scrollToScreen(0.5)
                }
            }

            "selectFirstPropertyValueOrNull" -> {
                // Get property value of first element matching selector
                if (args.size >= 2) {
                    driver.selectFirstPropertyValueOrNull(arg0!!, arg1!!)
                } else {
                    null
                }
            }

            "selectPropertyValueAll" -> {
                // Get property values from all elements matching selector
                when (args.size) {
                    2 -> driver.selectPropertyValueAll(arg0!!, arg1!!)
                    4 -> driver.selectPropertyValueAll(
                        arg0!!,
                        arg1!!,
                        arg2!!.toIntOrNull() ?: 0,
                        arg3!!.toIntOrNull() ?: 10000
                    )

                    else -> if (args.size >= 2) driver.selectPropertyValueAll(arg0!!, arg1!!) else null
                }
            }

            "setProperty" -> {
                // Set property on first element matching selector
                if (args.size >= 3) {
                    driver.setProperty(arg0!!, arg1!!, arg2!!)
                } else {
                    null
                }
            }

            "setPropertyAll" -> {
                // Set property on all elements matching the selector
                if (args.size >= 3) {
                    driver.setPropertyAll(arg0!!, arg1!!, arg2!!)
                } else {
                    null
                }
            }

            "evaluateValue" -> {
                // Execute JavaScript and return JSON value
                when (args.size) {
                    1 -> driver.evaluateValue(arg0!!)
                    2 -> driver.evaluateValue(arg0!!, arg1!!)
                    else -> if (args.isNotEmpty()) driver.evaluateValue(arg0!!) else null
                }
            }

            "evaluateValueDetail" -> {
                // Execute JavaScript and return detailed JSON value
                if (args.isNotEmpty()) driver.evaluateValueDetail(arg0!!) else null
            }

            "delay" -> {
                // Pause execution for specified milliseconds
                if (args.isNotEmpty()) {
                    driver.delay(arg0!!.toLongOrNull() ?: 1000L)
                } else {
                    driver.delay(1000L)
                }
            }

            // Navigation history controls
            "goBack" -> {
                driver.goBack()
            }

            "goForward" -> {
                driver.goForward()
            }

            else -> {
                // Command not supported or implemented
                null
            }
        }
    }

    companion object {

        const val TOOL_CALL_LIST = """
navigateTo(url: String)
waitForSelector(selector: String, timeoutMillis: Long = 5000)
exists(selector: String): Boolean
isVisible(selector: String): Boolean
focus(selector: String)
click(selector: String)
fill(selector: String, text: String)
press(selector: String, key: String)
check(selector: String)
uncheck(selector: String)
scrollDown(count: Int = 1)
scrollUp(count: Int = 1)
scrollTo(selector: String)
scrollToTop()
scrollToBottom()
scrollToMiddle(ratio: Double = 0.5)
scrollToScreen(screenNumber: Double)
clickTextMatches(selector: String, pattern: String, count: Int = 1)
clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
clickNthAnchor(n: Int, rootSelector: String = "body")
waitForNavigation(oldUrl: String = "", timeoutMillis: Long = 5000): Long
goBack()
goForward()
delay(millis: Long = 1000)
    """

        val SUPPORTED_TOOL_CALLS = TOOL_CALL_LIST.split("\n").filter { it.contains("(") }.map { it.trim() }

        @Suppress("unused")
        val SUPPORTED_ACTIONS = SUPPORTED_TOOL_CALLS.map { it.substringBefore("(") }

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
            "navigateTo", "open", "waitForNavigation", "scrollDown", "scrollUp", "scrollToTop", "scrollToBottom",
            "scrollToMiddle", "mouseWheelDown", "mouseWheelUp", "waitForPage", "bringToFront", "delay",
            "instruct", "getCookies", "deleteCookies", "clearBrowserCookies", "pause", "stop", "currentUrl",
            "url", "documentURI", "baseURI", "referrer", "pageSource", "newJsoupSession", "loadJsoupResource",
            "loadResource", "waitUntil"
        )

        val MAY_NAVIGATE_ACTIONS = setOf("navigateTo", "click", "goBack", "goForward")

        /**
         * Parses a function call from a text string into its components.
         * Uses a robust state machine to correctly handle:
         * - Strings with commas and escaped quotes/backslashes
         * - Nested parentheses inside arguments
         * - Optional whitespace and trailing commas
         */
        fun parseKotlinFunctionExpression(input: String): ToolCall? {
            val s = input.trim().removeSuffix(";")
            if (s.isEmpty()) return null

            // Scan once to find the top-level '(' and its matching ')', respecting quotes/escapes
            var inSingle = false
            var inDouble = false
            var escape = false
            var depth = 0
            var openIdx = -1
            var closeIdx = -1

            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (escape) {
                    escape = false
                    i++
                    continue
                }
                when {
                    inSingle -> {
                        if (c == '\\') {
                            escape = true
                        } else if (c == '\'') {
                            inSingle = false
                        }
                    }
                    inDouble -> {
                        if (c == '\\') {
                            escape = true
                        } else if (c == '"') {
                            inDouble = false
                        }
                    }
                    else -> {
                        when (c) {
                            '\'' -> inSingle = true
                            '"' -> inDouble = true
                            '(' -> {
                                if (openIdx == -1) {
                                    openIdx = i
                                    depth = 1
                                } else {
                                    depth++
                                }
                            }
                            ')' -> {
                                if (openIdx != -1) {
                                    depth--
                                    if (depth == 0) {
                                        closeIdx = i
                                        i = s.length // break
                                        continue
                                    }
                                }
                            }
                        }
                    }
                }
                i++
            }

            if (openIdx == -1 || closeIdx == -1 || closeIdx <= openIdx) return null

            val header = s.take(openIdx).trim()
            val argsRegion = s.substring(openIdx + 1, closeIdx)

            val dot = header.lastIndexOf('.')
            if (dot <= 0 || dot >= header.length - 1) return null
            val objectName = header.take(dot).trim()
            val functionName = header.substring(dot + 1).trim()
            if (objectName.isEmpty() || functionName.isEmpty()) return null

            val argsList = splitTopLevelArgs(argsRegion)
            val normalized = argsList.mapNotNull { tok ->
                val t = tok.trim()
                if (t.isEmpty()) null else unquoteAndUnescape(t)
            }
            val args = normalized.withIndex().associate { it.index.toString() to it.value }
            return ToolCall(objectName, functionName, args)
        }

        // Split arguments by commas at top level, honoring quotes, escapes, and nested parentheses.
        private fun splitTopLevelArgs(s: String): List<String> {
            val out = mutableListOf<String>()
            if (s.isBlank()) return out
            var inSingle = false
            var inDouble = false
            var escape = false
            var depth = 0
            val buf = StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (escape) {
                    buf.append(c)
                    escape = false
                    i++
                    continue
                }
                when {
                    inSingle -> {
                        when (c) {
                            '\\' -> {
                                escape = true
                                buf.append(c)
                            }
                            '\'' -> {
                                inSingle = false
                                buf.append(c)
                            }
                            else -> buf.append(c)
                        }
                    }
                    inDouble -> {
                        when (c) {
                            '\\' -> {
                                escape = true
                                buf.append(c)
                            }
                            '"' -> {
                                inDouble = false
                                buf.append(c)
                            }
                            else -> buf.append(c)
                        }
                    }
                    else -> {
                        when (c) {
                            '\'' -> {
                                inSingle = true
                                buf.append(c)
                            }
                            '"' -> {
                                inDouble = true
                                buf.append(c)
                            }
                            '(' -> {
                                depth++
                                buf.append(c)
                            }
                            ')' -> {
                                if (depth > 0) depth--
                                buf.append(c)
                            }
                            ',' -> {
                                if (depth == 0) {
                                    out.add(buf.toString())
                                    buf.setLength(0)
                                } else {
                                    buf.append(c)
                                }
                            }
                            else -> buf.append(c)
                        }
                    }
                }
                i++
            }
            // Last token (may be empty on trailing comma)
            if (buf.isNotEmpty()) {
                out.add(buf.toString())
            }
            return out
        }

        // Remove one level of matching quotes and unescape backslash sequences inside.
        private fun unquoteAndUnescape(token: String): String {
            if (token.length >= 2) {
                val first = token.first()
                val last = token.last()
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    return unescape(token.substring(1, token.length - 1))
                }
            }
            return token.trim()
        }

        // Unescape known sequences; preserve unknown ones (e.g. \p -> \p)
        private fun unescape(s: String): String {
            val sb = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (val n = s[i + 1]) {
                        '\\' -> { sb.append('\\'); i += 2 }
                        '"' -> { sb.append('"'); i += 2 }
                        '\'' -> { sb.append('\''); i += 2 }
                        'n' -> { sb.append('\n'); i += 2 }
                        'r' -> { sb.append('\r'); i += 2 }
                        't' -> { sb.append('\t'); i += 2 }
                        'b' -> { sb.append('\b'); i += 2 }
                        'f' -> { sb.append('\u000C'); i += 2 }
                        else -> {
                            // Unknown escape: keep the backslash and the char
                            sb.append('\\').append(n)
                            i += 2
                        }
                    }
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }

        // Basic string escaper to safely embed values inside Kotlin string literals
        private fun String.esc(): String = this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        fun toolCallToExpression(tc: ToolCall): String? = when (tc.name) {
            // Navigation
            "navigateTo" -> tc.args["url"]?.toString()?.let { "driver.navigateTo(\"${it.esc()}\")" }
            // Backward compatibility for older prompts
            "goto" -> tc.args["url"]?.toString()?.let { "driver.navigateTo(\"${it.esc()}\")" }
            // Wait
            "waitForSelector" -> tc.args["selector"]?.toString()?.let { sel ->
                "driver.waitForSelector(\"${sel.esc()}\", ${(tc.args["timeoutMillis"] ?: 5000)})"
            }
            // Status checking (first batch of new tools)
            "exists" -> tc.args["selector"]?.toString()?.let { "driver.exists(\"${it.esc()}\")" }
            "isVisible" -> tc.args["selector"]?.toString()?.let { "driver.isVisible(\"${it.esc()}\")" }
            "focus" -> tc.args["selector"]?.toString()?.let { "driver.focus(\"${it.esc()}\")" }
            // Basic interactions
            "click" -> tc.args["selector"]?.toString()?.let { "driver.click(\"${it.esc()}\")" }
            "fill" -> tc.args["selector"]?.toString()?.let { s ->
                val text = tc.args["text"]?.toString()?.esc() ?: ""
                "driver.fill(\"${s.esc()}\", \"$text\")"
            }
            "press" -> tc.args["selector"]?.toString()?.let { s -> tc.args["key"]?.toString()?.let { k -> "driver.press(\"${s.esc()}\", \"${k.esc()}\")" } }
            "check" -> tc.args["selector"]?.toString()?.let { "driver.check(\"${it.esc()}\")" }
            "uncheck" -> tc.args["selector"]?.toString()?.let { "driver.uncheck(\"${it.esc()}\")" }
            // Scrolling
            "scrollDown" -> "driver.scrollDown(${tc.args["count"] ?: 1})"
            "scrollUp" -> "driver.scrollUp(${tc.args["count"] ?: 1})"
            "scrollTo" -> tc.args["selector"]?.toString()?.let { "driver.scrollTo(\"${it.esc()}\")" }
            "scrollToTop" -> "driver.scrollToTop()"
            "scrollToBottom" -> "driver.scrollToBottom()"
            "scrollToMiddle" -> "driver.scrollToMiddle(${tc.args["ratio"] ?: 0.5})"
            "scrollToScreen" -> tc.args["screenNumber"]?.let { n -> "driver.scrollToScreen(${n})" }
            // Advanced clicks
            "clickTextMatches" -> tc.args["selector"]?.toString()?.let { s ->
                val pattern = tc.args["pattern"]?.toString()?.esc() ?: return@let null
                val count = tc.args["count"] ?: 1
                "driver.clickTextMatches(\"${s.esc()}\", \"$pattern\", $count)"
            }

            "clickMatches" -> tc.args["selector"]?.toString()?.let { s ->
                val attr = tc.args["attrName"]?.toString()?.esc() ?: return@let null
                val pattern = tc.args["pattern"]?.toString()?.esc() ?: return@let null
                val count = tc.args["count"] ?: 1
                "driver.clickMatches(\"${s.esc()}\", \"$attr\", \"$pattern\", $count)"
            }

            "clickNthAnchor" -> tc.args["n"]?.let { n ->
                val root = tc.args["rootSelector"]?.toString() ?: "body"
                "driver.clickNthAnchor(${n}, \"${root.esc()}\")"
            }
            // Enhanced navigation
            "waitForNavigation" -> {
                val oldUrl = tc.args["oldUrl"]?.toString() ?: ""
                val timeout = tc.args["timeoutMillis"] ?: 5000L
                "driver.waitForNavigation(\"${oldUrl.esc()}\", ${timeout})"
            }

            "goBack" -> "driver.goBack()"
            "goForward" -> "driver.goForward()"
            // Screenshots
            "captureScreenshot" -> {
                val sel = tc.args["selector"]?.toString()
                if (sel.isNullOrBlank()) "driver.captureScreenshot()" else "driver.captureScreenshot(\"${sel.esc()}\")"
            }
            // Timing
            "delay" -> "driver.delay(${tc.args["millis"] ?: 1000})"
            else -> null
        }
    }
}
