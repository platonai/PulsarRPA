package ai.platon.pulsar.browser.js.stagehand

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.browser.common.ScriptLoader
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Test injected stagehand JS
 * */
class PulsarWebDriverInjectedStagehandJSTests : WebDriverTestBase() {

    val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"

    @Test
    fun `ensure getNodeFromXpath is injected`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """/html/body/div/div[2]/h2"""

        val result = driver.evaluate("typeof(window.getNodeFromXpath)")
        assertNotNull(result)
        assertEquals("function", result)
    }

    @Test
    fun `test getNodeFromXpath`() = runWebDriverTest(testURL, browser) { driver ->
//        val xpath = """/html/body/div/div[2]/h2"""
        val xpath = """/html/body/h2"""

        val result = driver.evaluateValue("window.getNodeFromXpath('$xpath')")
        assertEquals("Dynamic List Management", result)
    }

    @Test
    fun `test getScrollableElementXpathsSync`() = runWebDriverTest(testURL, browser) { driver ->
        val result = driver.evaluateValue("window.getScrollableElementXpathsSync()")
        println(result)
    }
}
