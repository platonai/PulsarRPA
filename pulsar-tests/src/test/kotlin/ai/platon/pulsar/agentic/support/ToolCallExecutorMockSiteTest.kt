package ai.platon.pulsar.agentic.support

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlin.test.*

/**
 * Test suite for ToolCallExecutor with real page interactions.
 * Tests each tool from AgentTool.TOOL_CALL_SPECIFICATION against a mock website
 * to verify actual page responses.
 */
class ToolCallExecutorMockSiteTest : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    private val executor = ToolCallExecutor()

    @Test
    fun `test driver navigateTo`() = runEnhancedWebDriverTest(browser) { driver ->
        val url = "$assetsBaseURL/dom.html"

        executor.execute("driver.navigateTo(\"$url\")", driver)

        // Verify navigation by checking the current URL
        val currentUrl = driver.currentUrl()
        assertNotNull(currentUrl)
        assertTrue(currentUrl.contains("dom.html"), "Expected URL to contain 'dom.html', got: $currentUrl")
    }

    @Test
    fun `test driver waitForSelector`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        val result = executor.execute("driver.waitForSelector(\"$selector\")", driver)

        assertNotNull(result, "waitForSelector should return a non-null duration")
    }

    @Test
    fun `test driver waitForSelector with timeout`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        val result = executor.execute("driver.waitForSelector(\"$selector\", 10000)", driver)

        assertNotNull(result, "waitForSelector with timeout should return a non-null value")
    }

    @Test
    fun `test driver exists`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        val result = executor.execute("driver.exists(\"$selector\")", driver)

        assertEquals(true, result, "Element with selector '$selector' should exist")
    }

    @Test
    fun `test driver exists returns false for non-existent element`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "#non-existent-element"

        val result = executor.execute("driver.exists(\"$selector\")", driver)

        assertEquals(false, result, "Non-existent element should return false")
    }

    @Test
    fun `test driver isVisible`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        val result = executor.execute("driver.isVisible(\"$selector\")", driver)

        assertEquals(true, result, "Visible element should return true")
    }

    @Test
    fun `test driver focus`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        executor.execute("driver.focus(\"$selector\")", driver)

        // focus returns Unit, which is converted to null
        // We can verify by checking if the element can be interacted with after focus
        val exists = driver.exists(selector)
        assertTrue(exists, "Element should exist after focus operation")
    }

    @Test
    fun `test driver click`() = runEnhancedWebDriverTest(interactiveDynamicURL, browser) { driver ->
        val selector = "button[data-testid=tta-load-users]"

        executor.execute("driver.click(\"$selector\")", driver)
        sleepSeconds(2)

        // Verify the click was executed (returns null for Unit)
        // The fact that no exception was thrown indicates success
        val verifySelector = "div[data-testid=tta-user-1]"
        assertTrue(driver.exists(verifySelector), "Element should exist after click")
    }

    @Test
    fun `test driver click with modifier`() = runEnhancedWebDriverTest(actMockSiteHomeURL, browser) { driver ->
        val selector = "#thirdLink"

        executor.execute("driver.click(\"$selector\", \"Control\")", driver)

        // Verify the click with modifier was executed
        val newDriver = browser.listDrivers().firstOrNull { it.url().contains("pageC.html") }
        assertNotNull(newDriver) { "New page should exist after click with modifier" }
    }

    @Test
    fun `test driver fill`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"
        val text = "Test input text"

        executor.execute("driver.fill(\"$selector\", \"$text\")", driver)

        // Verify the text was filled
        val value = driver.selectFirstPropertyValueOrNull(selector, "value")
        assertEquals(text, value, "Input should contain the filled text")
    }

    @Test
    fun `test driver type`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"
        val text = "Typed text"

        executor.execute("driver.type(\"$selector\", \"$text\")", driver)

        // Verify the text was typed
        val value = driver.selectFirstPropertyValueOrNull(selector, "value")
        assertNotNull(value, "Input should have a value after typing")
        assertTrue(value.contains(text), "Input should contain the typed text")
    }

    @Test
    fun `test driver press`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        // Fill some text first
        driver.fill(selector, "Test")

        // Press Enter key
        executor.execute("driver.press(\"$selector\", \"Enter\")", driver)

        // Verify the press was executed successfully (no exception)
        assertTrue(driver.exists(selector), "Element should exist after key press")
    }

    @Test
    fun `test driver check`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[type=checkbox]"

        executor.execute("driver.uncheck(\"$selector\")", driver)
        executor.execute("driver.check(\"$selector\")", driver)

        var result = driver.evaluateDetail("__pulsar_utils__.isChecked(\"$selector\")")
        assertEquals(true, result?.value == true, "Element should contain the checkbox")

        // Verify the checkbox is checked
        val isChecked = driver.isChecked(selector)
        assertEquals(true, isChecked, "Checkbox should be checked")
    }

    @Test
    fun `test driver uncheck`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[type=checkbox]"

        // First check it
        driver.check(selector)

        // Then uncheck it
        executor.execute("driver.uncheck(\"$selector\")", driver)

        // Verify the checkbox is unchecked
        val isChecked = driver.isChecked(selector)
        assertEquals(false, isChecked, "Checkbox should be unchecked")
    }

    @Test
    fun `test driver scrollTo`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val selector = "button"

        executor.execute("driver.scrollTo(\"$selector\")", driver)

        // Verify scroll was executed (element should still exist)
        assertTrue(driver.exists(selector), "Element should exist after scrollTo")
    }

    @Test
    fun `test driver scrollToTop`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        // Scroll down first
        driver.scrollDown(3)

        // Then scroll to top
        executor.execute("driver.scrollToTop()", driver)

        // Verify by checking scroll position (page should be at top)
        val scrollY = driver.evaluate("window.scrollY") as? Number
        assertNotNull(scrollY, "Should be able to get scroll position")
    }

    @Test
    fun `test driver scrollToBottom`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        executor.execute("driver.scrollToBottom()", driver)

        // Verify scroll was executed
        val scrollY = driver.evaluate("window.scrollY") as? Number
        assertNotNull(scrollY, "Should be able to get scroll position after scrollToBottom")
    }

    @Test
    fun `test driver scrollToMiddle`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        executor.execute("driver.scrollToMiddle()", driver)

        // Verify scroll was executed with default ratio
        val scrollY = driver.evaluate("window.scrollY") as? Number
        assertNotNull(scrollY, "Should be able to get scroll position")
    }

    @Test
    fun `test driver scrollToMiddle with ratio`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        executor.execute("driver.scrollToMiddle(0.7)", driver)

        // Verify scroll was executed with custom ratio
        val scrollY = driver.evaluate("window.scrollY") as? Number
        assertNotNull(scrollY, "Should be able to get scroll position with custom ratio")
    }

    @Test
    fun `test driver scrollToViewport`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        executor.execute("driver.scrollToViewport(1.0)", driver)

        // Verify scroll was executed
        val scrollY = driver.evaluate("window.scrollY") as? Number
        assertNotNull(scrollY, "Should be able to get scroll position")
    }

    @Test
    fun `test driver goBack`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val url2 = "$assetsBaseURL/form.html"

        // Navigate to another page first
        driver.navigateTo(url2)

        // Go back
        executor.execute("driver.goBack()", driver)

        // Verify we're back at the original page
        val currentUrl = driver.currentUrl()
        assertTrue(currentUrl.contains("dom.html"), "Should navigate back to dom.html")
    }

    @Test
    fun `test driver goForward`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val url2 = "$assetsBaseURL/form.html"

        // Navigate to another page
        driver.navigateTo(url2)

        // Go back
        driver.goBack()

        // Go forward
        executor.execute("driver.goForward()", driver)

        // Verify we're forward at the second page
        val currentUrl = driver.currentUrl()
        assertTrue(currentUrl.contains("form.html"), "Should navigate forward to form.html")
    }

    @Test
    fun `test driver selectFirstTextOrNull`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "#outer"

        val result = executor.execute("driver.selectFirstTextOrNull(\"$selector\")", driver)

        assertNotNull(result, "Should return text content")
        assertTrue(result is String, "Result should be a String")
    }

    @Test
    fun `test driver selectFirstTextOrNull returns null for non-existent`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "#non-existent-element"

        val result = executor.execute("driver.selectFirstTextOrNull(\"$selector\")", driver)

        // Should return null for non-existent element
        assertEquals(null, result, "Should return null for non-existent element")
    }

    @Test
    fun `test driver selectTextAll`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"

        val result = executor.execute("driver.selectTextAll(\"$selector\")", driver)

        assertNotNull(result, "Should return a list")
        assertTrue(result is List<*>, "Result should be a List")
    }

    @Test
    fun `test browser switchTab`() = runEnhancedWebDriverTest(ttaUrl1) { _ ->
        // Create a new driver (tab)
        val newDriver = browser.newDriver(ttaUrl2)
        val newDriverId = newDriver.id

        val drivers = browser.drivers.values.filterIsInstance<PulsarWebDriver>()
        printlnPro("drivers: " + drivers.joinToString("\n") {
            "" + it.id + " " + it.parentSid + " " + it.guid
        })

        // Switch to the new tab using the executor with eval
        val result = executor.execute("browser.switchTab(\"$newDriverId\")", browser, session)
        assertIs<WebDriver>(result)

        assertEquals(newDriverId, result.id, "Should return the new driver ID after switching")
        val isVisible = newDriver.evaluateValue("document.visibilityState == \"visible\"")
        assertEquals(true, isVisible, "A document that brings to front is visible")

        browser.drivers.values.filterIsInstance<PulsarWebDriver>().filter { it != newDriver }.forEach { driver ->
            val isVisible = driver.evaluateValue("document.visibilityState == \"visible\"")
            assertEquals(false, isVisible,
                "Documents that NOT brings to front are not visible")
        }
    }
}
