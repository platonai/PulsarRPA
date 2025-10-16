package ai.platon.pulsar.browser.js.stagehand

import ai.platon.pulsar.WebDriverTestBase
import kotlin.test.*

/**
 * Test injected stagehand JS
 * */
class PulsarWebDriverInjectedStagehandJSTests : WebDriverTestBase() {

    val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"

    @Test
    fun `ensure getNodeFromXpath is injected`() = runEnhancedWebDriverTest(testURL, browser) { driver ->
        val expression = """/html/body/div/div[2]/h2"""

        val result = driver.evaluate("typeof(window.getNodeFromXpath)")
        assertNotNull(result)
        assertEquals("function", result)
    }

    @Test
    fun `test getNodeFromXpath`() = runEnhancedWebDriverTest(testURL, browser) { driver ->
        val xpath = """/html/body/div/div[2]/h2"""

        val result = driver.evaluateValue("window.getNodeFromXpath('$xpath')")
        assertNotNull(result)
        assertTrue { result.toString().contains("h2") }
    }

    @Test
    fun `test getScrollableElementXpathsSync`() = runEnhancedWebDriverTest(testURL, browser) { driver ->
        val result = driver.evaluateValue("window.getScrollableElementXpathsSync()")
        println(result)
    }
}
