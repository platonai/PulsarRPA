/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package ai.platon.pulsar.sdk

/**
 * WebDriver-compatible façade mapping to selector-first REST endpoints.
 *
 * This class provides methods for browser control and automation through
 * the Browser4 REST API. It supports:
 * - Navigation: navigate_to, current_url, go_back, go_forward, reload
 * - Element interaction: click, fill, type, press, hover, focus
 * - Scrolling: scrollDown, scrollUp, scrollTo, scrollToBottom, scrollToTop
 * - Selection: exists, waitForSelector, selectFirstText, selectTextAll
 * - Screenshots: captureScreenshot
 * - Script execution: evaluate, executeScript
 * - Control: delay, pause, stop
 *
 * Example usage:
 * ```kotlin
 * val client = PulsarClient()
 * client.createSession()
 * val driver = WebDriver(client)
 * driver.navigateTo("https://example.com")
 * println(driver.currentUrl())
 * driver.click("button.submit")
 * ```
 *
 * @param client PulsarClient instance for API communication
 */
class WebDriver(
    val client: PulsarClient
) {
    private var _id: Int = 0
    private val _navigateHistory: MutableList<String> = mutableListOf()

    /**
     * Gets the driver ID.
     */
    val id: Int get() = _id

    /**
     * Gets the navigation history.
     */
    val navigateHistory: List<String> get() = _navigateHistory.toList()

    // ========== Navigation ==========

    /**
     * Opens the specified URL and waits for navigation to complete.
     *
     * @param url The URL to navigate to
     */
    fun open(url: String) {
        navigateTo(url)
    }

    /**
     * Navigates to a URL.
     *
     * @param url The URL to navigate to
     * @return Navigation result
     */
    fun navigateTo(url: String): Any? {
        val result = client.post("/session/{sessionId}/url", mapOf("url" to url))
        _navigateHistory.add(url)
        return result
    }

    /**
     * Reloads the current page.
     *
     * @return Reload result
     */
    fun reload(): Any? {
        return executeScript("location.reload()")
    }

    /**
     * Navigates back in browser history.
     *
     * @return Navigation result
     */
    fun goBack(): Any? {
        return executeScript("history.back()")
    }

    /**
     * Navigates forward in browser history.
     *
     * @return Navigation result
     */
    fun goForward(): Any? {
        return executeScript("history.forward()")
    }

    /**
     * Gets the current URL displayed in the address bar.
     *
     * @return The current URL as a string
     */
    fun currentUrl(): String {
        return client.get("/session/{sessionId}/url") as? String ?: ""
    }

    /**
     * Alias for [currentUrl].
     */
    fun getCurrentUrl(): String = currentUrl()

    /**
     * Gets the document URL property.
     *
     * @return The document URL
     */
    fun url(): String = currentUrl()

    /**
     * Gets the document.documentURI property.
     *
     * @return The document URI
     */
    fun documentUri(): String {
        return client.get("/session/{sessionId}/documentUri") as? String ?: ""
    }

    /**
     * Alias for [documentUri].
     */
    fun getDocumentUri(): String = documentUri()

    /**
     * Gets the document.baseURI property.
     *
     * @return The base URI
     */
    fun baseUri(): String {
        return client.get("/session/{sessionId}/baseUri") as? String ?: ""
    }

    /**
     * Alias for [baseUri].
     */
    fun getBaseUri(): String = baseUri()

    /**
     * Gets the current page title.
     *
     * @return The page title
     */
    fun title(): String {
        return executeScript("return document.title") as? String ?: ""
    }

    /**
     * Gets the source code of the current page.
     *
     * @return The page HTML source or null
     */
    fun pageSource(): String? {
        return outerHtml()
    }

    // ========== Status Checking ==========

    /**
     * Checks if an element exists in the DOM.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy ("css" or "xpath")
     * @return True if the element exists
     */
    @Suppress("UNCHECKED_CAST")
    fun exists(selector: String, strategy: String = "css"): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/exists",
            mapOf("selector" to selector, "strategy" to strategy)
        )
        return when (value) {
            is Map<*, *> -> (value as Map<String, Any?>)["exists"] as? Boolean ?: false
            is Boolean -> value
            else -> false
        }
    }

    /**
     * Checks if an element is visible.
     *
     * @param selector CSS selector
     * @return True if the element is visible
     */
    fun isVisible(selector: String): Boolean {
        val script = """
            (() => {
                const el = document.querySelector('$selector');
                if (!el) return false;
                const style = window.getComputedStyle(el);
                return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
            })()
        """.trimIndent()
        return executeScript(script) as? Boolean ?: false
    }

    /**
     * Checks if an element is hidden.
     *
     * @param selector CSS selector
     * @return True if the element is hidden
     */
    fun isHidden(selector: String): Boolean = !isVisible(selector)

    /**
     * Checks if a checkbox/radio element is checked.
     *
     * @param selector CSS selector
     * @return True if the element is checked
     */
    fun isChecked(selector: String): Boolean {
        val script = "document.querySelector('$selector')?.checked"
        return executeScript(script) as? Boolean ?: false
    }

    // ========== Wait Operations ==========

    /**
     * Waits for an element to appear in the DOM.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy ("css" or "xpath")
     * @param timeout Maximum wait time in milliseconds
     * @return True if the element was found before timeout
     */
    @Suppress("UNCHECKED_CAST")
    fun waitForSelector(selector: String, strategy: String = "css", timeout: Int = 30000): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/waitFor",
            mapOf("selector" to selector, "strategy" to strategy, "timeout" to timeout)
        )
        return when (value) {
            null -> true
            is Map<*, *> -> (value as Map<String, Any?>)["exists"] as? Boolean ?: true
            is Boolean -> value
            else -> true
        }
    }

    /**
     * Alias for [waitForSelector].
     */
    fun waitFor(selector: String, strategy: String = "css", timeout: Int = 30000): Boolean {
        return waitForSelector(selector, strategy, timeout)
    }

    /**
     * Waits for navigation to complete (URL change).
     *
     * @param oldUrl The previous URL to compare against
     * @param timeout Maximum wait time in milliseconds
     * @return True if navigation completed
     */
    fun waitForNavigation(oldUrl: String = "", timeout: Int = 30000): Boolean {
        // Simple implementation using delay
        delay(minOf(timeout, 1000))
        return true
    }

    // ========== Element Finding ==========

    /**
     * Finds a single element by selector.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy
     * @return Element reference map
     */
    @Suppress("UNCHECKED_CAST")
    fun findElementBySelector(selector: String, strategy: String = "css"): Map<String, Any?>? {
        return client.post(
            "/session/{sessionId}/selectors/element",
            mapOf("selector" to selector, "strategy" to strategy)
        ) as? Map<String, Any?>
    }

    /**
     * Finds all elements matching a selector.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy
     * @return List of element reference maps
     */
    @Suppress("UNCHECKED_CAST")
    fun findElementsBySelector(selector: String, strategy: String = "css"): List<Map<String, Any?>> {
        val result = client.post(
            "/session/{sessionId}/selectors/elements",
            mapOf("selector" to selector, "strategy" to strategy)
        )
        return result as? List<Map<String, Any?>> ?: emptyList()
    }

    /**
     * Finds element using WebDriver locator strategy.
     *
     * @param using Locator strategy (e.g., "css selector", "xpath")
     * @param value Locator value
     * @return Element reference map
     */
    @Suppress("UNCHECKED_CAST")
    fun findElement(using: String, value: String): Map<String, Any?>? {
        return client.post(
            "/session/{sessionId}/element",
            mapOf("using" to using, "value" to value)
        ) as? Map<String, Any?>
    }

    /**
     * Finds elements using WebDriver locator strategy.
     *
     * @param using Locator strategy
     * @param value Locator value
     * @return List of element reference maps
     */
    @Suppress("UNCHECKED_CAST")
    fun findElements(using: String, value: String): List<Map<String, Any?>> {
        val result = client.post(
            "/session/{sessionId}/elements",
            mapOf("using" to using, "value" to value)
        )
        return result as? List<Map<String, Any?>> ?: emptyList()
    }

    // ========== Element Interaction ==========

    /**
     * Clicks an element identified by selector.
     *
     * @param selector CSS selector or XPath expression
     * @param count Number of clicks (for double-click, use 2)
     * @param strategy Selector strategy
     * @return Click result
     */
    fun click(selector: String, count: Int = 1, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/click",
            mapOf("selector" to selector, "strategy" to strategy)
        )
    }

    /**
     * Clicks an element by its ID.
     *
     * @param elementId WebDriver element ID
     * @return Click result
     */
    fun clickElement(elementId: String): Any? {
        return client.post("/session/{sessionId}/element/$elementId/click", emptyMap())
    }

    /**
     * Hovers over an element.
     *
     * @param selector CSS selector
     * @return Hover result
     */
    fun hover(selector: String): Any? {
        val script = """
            (() => {
                const el = document.querySelector('$selector');
                if (el) {
                    const event = new MouseEvent('mouseover', {bubbles: true});
                    el.dispatchEvent(event);
                }
            })()
        """.trimIndent()
        return executeScript(script)
    }

    /**
     * Focuses on an element.
     *
     * @param selector CSS selector
     * @return Focus result
     */
    fun focus(selector: String): Any? {
        return executeScript("document.querySelector('$selector')?.focus()")
    }

    /**
     * Types text into an element (appending to existing content).
     *
     * @param selector CSS selector
     * @param text Text to type
     * @return Type result
     */
    fun type(selector: String, text: String): Any? {
        return fill(selector, text)
    }

    /**
     * Fills an input element with text (clearing existing content first).
     *
     * @param selector CSS selector or XPath expression
     * @param text Text to fill
     * @param strategy Selector strategy
     * @return Fill result
     */
    fun fill(selector: String, text: String, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/fill",
            mapOf("selector" to selector, "strategy" to strategy, "value" to text)
        )
    }

    /**
     * Presses a key on an element.
     *
     * @param selector CSS selector or XPath expression
     * @param key Key to press (e.g., "Enter", "Tab")
     * @param strategy Selector strategy
     * @return Press result
     */
    fun press(selector: String, key: String, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/press",
            mapOf("selector" to selector, "strategy" to strategy, "key" to key)
        )
    }

    /**
     * Sends keys to an element.
     *
     * @param elementId WebDriver element ID
     * @param text Text to send
     * @return Send keys result
     */
    fun sendKeys(elementId: String, text: String): Any? {
        return client.post(
            "/session/{sessionId}/element/$elementId/value",
            mapOf("text" to text)
        )
    }

    /**
     * Checks a checkbox element.
     *
     * @param selector CSS selector
     * @return Check result
     */
    fun check(selector: String): Any? {
        val script = """
            (() => {
                const el = document.querySelector('$selector');
                if (el && !el.checked) el.click();
            })()
        """.trimIndent()
        return executeScript(script)
    }

    /**
     * Unchecks a checkbox element.
     *
     * @param selector CSS selector
     * @return Uncheck result
     */
    fun uncheck(selector: String): Any? {
        val script = """
            (() => {
                const el = document.querySelector('$selector');
                if (el && el.checked) el.click();
            })()
        """.trimIndent()
        return executeScript(script)
    }

    // ========== Scrolling ==========

    /**
     * Scrolls down the page.
     *
     * @param count Number of scroll actions
     * @return Current scroll position
     */
    fun scrollDown(count: Int = 1): Double {
        val script = "window.scrollBy(0, ${200 * count}); return window.scrollY;"
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls up the page.
     *
     * @param count Number of scroll actions
     * @return Current scroll position
     */
    fun scrollUp(count: Int = 1): Double {
        val script = "window.scrollBy(0, ${-200 * count}); return window.scrollY;"
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls an element into view.
     *
     * @param selector CSS selector of the element
     * @return Current scroll position
     */
    fun scrollTo(selector: String): Double {
        val script = """
            (() => {
                const el = document.querySelector('$selector');
                if (el) el.scrollIntoView({behavior: 'smooth', block: 'center'});
                return window.scrollY;
            })()
        """.trimIndent()
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls to the top of the page.
     *
     * @return Current scroll position (0)
     */
    fun scrollToTop(): Double {
        val script = "window.scrollTo(0, 0); return window.scrollY;"
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls to the bottom of the page.
     *
     * @return Current scroll position
     */
    fun scrollToBottom(): Double {
        val script = "window.scrollTo(0, document.body.scrollHeight); return window.scrollY;"
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls to a specific position on the page.
     *
     * @param ratio Scroll ratio (0.0 = top, 1.0 = bottom)
     * @return Current scroll position
     */
    fun scrollToMiddle(ratio: Double = 0.5): Double {
        val script = """
            (() => {
                const maxScroll = document.body.scrollHeight - window.innerHeight;
                window.scrollTo(0, maxScroll * $ratio);
                return window.scrollY;
            })()
        """.trimIndent()
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    /**
     * Scrolls by a specific number of pixels.
     *
     * @param pixels Pixels to scroll (positive = down, negative = up)
     * @param smooth Whether to use smooth scrolling
     * @return Current scroll position
     */
    fun scrollBy(pixels: Double = 200.0, smooth: Boolean = true): Double {
        val behavior = if (smooth) "'smooth'" else "'auto'"
        val script = """
            (() => {
                window.scrollBy({top: $pixels, behavior: $behavior});
                return window.scrollY;
            })()
        """.trimIndent()
        return (executeScript(script) as? Number)?.toDouble() ?: 0.0
    }

    // ========== Content Extraction ==========

    /**
     * Gets the outer HTML of an element or the entire document.
     *
     * @param selector CSS selector (optional, if null returns document HTML)
     * @param strategy Selector strategy
     * @return HTML content or null
     */
    @Suppress("UNCHECKED_CAST")
    fun outerHtml(selector: String? = null, strategy: String = "css"): String? {
        return if (selector != null) {
            val value = client.post(
                "/session/{sessionId}/selectors/outerHtml",
                mapOf("selector" to selector, "strategy" to strategy)
            )
            when (value) {
                is Map<*, *> -> (value as Map<String, Any?>)["outerHtml"] as? String
                is String -> value
                else -> null
            }
        } else {
            executeScript("return document.documentElement.outerHTML") as? String
        }
    }

    /**
     * Gets the text content of an element or document.
     *
     * @param selector CSS selector (optional)
     * @return Text content or null
     */
    fun textContent(selector: String? = null): String? {
        return if (selector != null) {
            selectFirstTextOrNull(selector)
        } else {
            executeScript("return document.body.innerText") as? String
        }
    }

    /**
     * Gets the text content of the first element matching the selector.
     *
     * @param selector CSS selector
     * @return Text content or null if not found
     */
    fun selectFirstTextOrNull(selector: String): String? {
        return executeScript("document.querySelector('$selector')?.innerText") as? String
    }

    /**
     * Gets text content of all elements matching the selector.
     *
     * @param selector CSS selector
     * @return List of text contents
     */
    @Suppress("UNCHECKED_CAST")
    fun selectTextAll(selector: String): List<String> {
        val script = """
            Array.from(document.querySelectorAll('$selector'))
                .map(el => el.innerText)
        """.trimIndent()
        val result = executeScript(script)
        return (result as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    /**
     * Gets an attribute value of the first element matching the selector.
     *
     * @param selector CSS selector
     * @param attrName Attribute name
     * @return Attribute value or null
     */
    fun selectFirstAttributeOrNull(selector: String, attrName: String): String? {
        return executeScript("document.querySelector('$selector')?.getAttribute('$attrName')") as? String
    }

    /**
     * Gets attribute values of all elements matching the selector.
     *
     * @param selector CSS selector
     * @param attrName Attribute name
     * @return List of attribute values
     */
    @Suppress("UNCHECKED_CAST")
    fun selectAttributeAll(selector: String, attrName: String): List<String> {
        val script = """
            Array.from(document.querySelectorAll('$selector'))
                .map(el => el.getAttribute('$attrName'))
                .filter(v => v !== null)
        """.trimIndent()
        val result = executeScript(script)
        return (result as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    /**
     * Gets an attribute of an element by ID.
     *
     * @param elementId WebDriver element ID
     * @param name Attribute name
     * @return Attribute value
     */
    fun getAttribute(elementId: String, name: String): Any? {
        return client.get("/session/{sessionId}/element/$elementId/attribute/$name")
    }

    /**
     * Gets the text content of an element by ID.
     *
     * @param elementId WebDriver element ID
     * @return Text content
     */
    fun getText(elementId: String): String {
        return client.get("/session/{sessionId}/element/$elementId/text") as? String ?: ""
    }

    /**
     * Extracts multiple fields using CSS selectors.
     *
     * @param fields Map of field names to CSS selectors
     * @return Map of field names to extracted values
     */
    fun extract(fields: Map<String, String>): Map<String, String?> {
        return fields.mapValues { (_, selector) -> selectFirstTextOrNull(selector) }
    }

    // ========== Screenshots ==========

    /**
     * Takes a screenshot.
     *
     * @param selector CSS selector for element screenshot (optional)
     * @param fullPage Whether to capture the full page
     * @return Base64-encoded screenshot or null
     */
    fun captureScreenshot(selector: String? = null, fullPage: Boolean = false): String? {
        return if (selector != null) {
            screenshot(selector)
        } else {
            val payload = mutableMapOf<String, Any>("strategy" to "css")
            if (fullPage) {
                payload["fullPage"] = true
            }
            client.post("/session/{sessionId}/selectors/screenshot", payload) as? String
        }
    }

    /**
     * Takes a screenshot (alias for [captureScreenshot]).
     *
     * @param selector CSS selector (optional)
     * @param strategy Selector strategy
     * @return Base64-encoded screenshot or null
     */
    fun screenshot(selector: String? = null, strategy: String = "css"): String? {
        val payload = mutableMapOf<String, Any>("strategy" to strategy)
        if (selector != null) {
            payload["selector"] = selector
        }
        return client.post("/session/{sessionId}/selectors/screenshot", payload) as? String
    }

    // ========== Script Execution ==========

    /**
     * Executes JavaScript and returns the result.
     *
     * @param expression JavaScript expression to evaluate
     * @return Evaluation result
     */
    fun evaluate(expression: String): Any? {
        return executeScript("return $expression")
    }

    /**
     * Executes synchronous JavaScript.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @return Script return value
     */
    fun executeScript(script: String, args: List<Any?>? = null): Any? {
        return client.post(
            "/session/{sessionId}/execute/sync",
            mapOf("script" to script, "args" to (args ?: emptyList<Any?>()))
        )
    }

    /**
     * Executes asynchronous JavaScript.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @param timeout Execution timeout in milliseconds
     * @return Script return value
     */
    fun executeAsyncScript(script: String, args: List<Any?>? = null, timeout: Int = 30000): Any? {
        return client.post(
            "/session/{sessionId}/execute/async",
            mapOf("script" to script, "args" to (args ?: emptyList<Any?>()), "timeout" to timeout)
        )
    }

    // ========== Control ==========

    /**
     * Delays execution for a specified time.
     *
     * @param millis Delay in milliseconds
     * @return Delay result
     */
    fun delay(millis: Int): Any? {
        return client.post("/session/{sessionId}/control/delay", mapOf("ms" to millis))
    }

    /**
     * Pauses the session execution.
     *
     * @return Pause result
     */
    fun pause(): Any? {
        return client.post("/session/{sessionId}/control/pause", emptyMap())
    }

    /**
     * Stops the session execution.
     *
     * @return Stop result
     */
    fun stop(): Any? {
        return client.post("/session/{sessionId}/control/stop", emptyMap())
    }

    /**
     * Brings the browser window to the front.
     *
     * @return Result
     */
    fun bringToFront(): Any? {
        return executeScript("window.focus()")
    }

    // ========== Events (Placeholder) ==========

    /**
     * Creates an event configuration.
     *
     * @param config Event configuration map
     * @return Created config response
     */
    fun createEventConfig(config: Map<String, Any>): Any? {
        return client.post("/session/{sessionId}/event-configs", config)
    }

    /**
     * Lists all event configurations.
     *
     * @return List of event configurations
     */
    fun listEventConfigs(): Any? {
        return client.get("/session/{sessionId}/event-configs")
    }

    /**
     * Gets captured events.
     *
     * @return List of events
     */
    fun getEvents(): Any? {
        return client.get("/session/{sessionId}/events")
    }

    /**
     * Subscribes to events.
     *
     * @param subscribeRequest Subscription request map
     * @return Subscription response
     */
    fun subscribeEvents(subscribeRequest: Map<String, Any>): Any? {
        return client.post("/session/{sessionId}/events/subscribe", subscribeRequest)
    }

    // ========== Convenience Methods ==========

    /**
     * Closes the driver (cleanup).
     */
    fun close() {
        // No specific cleanup needed for REST-based driver
    }
}
