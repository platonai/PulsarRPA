package ai.platon.pulsar.agentic.ai.support

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.TimeoutCancellationException
import java.time.Duration

class WebDriverToolCallExecutor {
    private val logger = getLogger(this)

    @Throws(TimeoutCancellationException::class, Exception::class)
    suspend fun execute(expression: String, driver: WebDriver): Any? {
        return try {
            val r = execute0(expression, driver)

            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: TimeoutCancellationException) {
            logger.warn("Timeout to execute | $expression", e)
            throw e
        } catch (e: Exception) {
            logger.warn("[Unexpected] Error executing expression: {} - {}", expression, e.stackTraceToString())
            throw e
        }
    }

    private suspend fun execute0(command: String, driver: WebDriver): Any? {
        // Extract function name and arguments from the command string
        val (objectName, functionName, args) = SimpleKotlinParser().parseFunctionExpression(command) ?: return null

        return doExecute(objectName, functionName, args, driver)
    }

    /**
     * Extract function name and arguments from the command string
     * */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, driver: WebDriver): Any? {
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
                // Click on an element with optional repeat count or modifier
                when (args.size) {
                    1 -> driver.click(arg0!!)
                    2 -> {
                        val countOrModifier = arg1!!
                        val count = countOrModifier.toIntOrNull()
                        if (count != null) {
                            driver.click(arg0!!, count)
                        } else {
                            driver.click(arg0!!, countOrModifier)
                        }
                    }
                    else -> if (args.isNotEmpty()) driver.click(arg0!!) else null
                }
            }

            "type" -> {
                // Type text into an element
                if (args.size >= 2) driver.type(arg0!!, arg1!!) else null
            }

            "textContent" -> {
                // Get the document's text content
                driver.textContent()
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
                    driver.waitForPage(arg0!!, Duration.ofMillis(arg1!!.toLongOrNull() ?: 30000L))
                } else if (args.isNotEmpty()) {
                    driver.waitForPage(arg0!!, Duration.ofMillis(30000L))
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

            "scrollToViewport" -> {
                // Scroll to a specific screen position by number
                if (args.isNotEmpty()) {
                    driver.scrollToViewport(arg0!!.toDoubleOrNull() ?: 1.0)
                } else {
                    driver.scrollToViewport(1.0)
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

}
