package ai.platon.pulsar.browser.js

import ai.platon.pulsar.browser.WebDriverTestBase
import ai.platon.pulsar.browser.common.ScriptLoader
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Test injected JS
 * */
class PulsarWebDriverInjectedJSTests : WebDriverTestBase() {

    val testURL get() = "$generatedAssetsBaseURL/injected-js.test.html"

    @Test
    fun `test evaluate that returns primitive value`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """1+1"""

        val result = driver.evaluate(expression)
        assertEquals(2, result)
    }

    @Test
    fun `test evaluate that returns JS Object`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """document"""

        val result = driver.evaluateDetail(expression)
        assertNotNull(result)

        println(result)
        // result: JsEvaluation(value=null, unserializableValue=null, className=HTMLDocument, description=#document, exception=null)

        assertNull(result.value)
        assertNull(result.exception)
        assertEquals("HTMLDocument", result.className)
    }

    @Test
    fun `test evaluateValueDetail that returns JS Object`() = runWebDriverTest(testURL, browser) { driver ->
        val code = """document"""

        val result = driver.evaluateValueDetail(code)
        assertNotNull(result)

        println(result)
        // JsEvaluation(value={location={ancestorOrigins={}, href=http://127.0.0.1:8182/generated/interactive-4.html, origin=http://127.0.0.1:8182, protocol=http:, host=127.0.0.1:8182, hostname=127.0.0.1, port=8182, pathname=/generated/interactive-4.html, search=, hash=, assign={}, reload={}, replace={}, toString={}}, jUGYzW_Data={trace={status={n=1, scroll=1, idl=0, st=c, r=st}, initStat={w=0, h=0, na=0, ni=0, nst=8, nnm=0}, lastStat={w=0, h=0, na=0, ni=0, nst=8, nnm=0}, lastD={w=0, h=0, na=0, ni=0, nst=0, nnm=0}, initD={w=0, h=0, na=0, ni=0, nst=0, nnm=0}}, urls={URL=http://127.0.0.1:8182/generated/interactive-4.html, baseURI=http://127.0.0.1:8182/generated/interactive-4.html, location=http://127.0.0.1:8182/generated/interactive-4.html, documentURI=http://127.0.0.1:8182/generated/interactive-4.html, referrer=}, metadata={viewPortWidth=1920, viewPortHeight=1080, scrollTop=228.00, scrollLeft=0.00, clientWidth=1683.00, clientHeight=986.00, screenNumber=0.23, dateTime=2025/5/6 21:43:14, timestamp=1746538994284}}}, unserializableValue=null, className=null, description=null, exception=null)

        println(prettyPulsarObjectMapper().writeValueAsString(result.value))

        assertNotNull(result.value)
        assertNull(result.exception)
        assertNull(result.className)
    }

    @Test
    fun `test __pulsar_NodeExt`() = runWebDriverTest(testURL, browser) { driver ->
        var result = driver.evaluateValue("__pulsar_NodeExt")
        println(result)

        result = driver.evaluateValue("__pulsar_NodeExt.prototype")
        println(result)

        result = driver.evaluateValue("document.body.nodeExt")
        println(result)
    }

    @Test
    fun `test getConfig`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """__pulsar_utils__.getConfig()"""

        val result = driver.evaluateValue(expression)
        println(result)
    }

    @Test
    fun `test queryComputedStyle`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """__pulsar_utils__.queryComputedStyle('button', ['color', 'background-color'])"""

        val result = driver.evaluateValue(expression)
        println(result)
        assertTrue { result is Map<*, *> }
        assertEquals("{color=f, background-color=007bff}", result.toString())
    }

    @Test
    fun `test JS selectAttributes`() {
        val driver = browser.newDriver()

        runBlocking {
            ScriptLoader.addInitParameter("ATTR_ELEMENT_NODE_DATA", AppConstants.PULSAR_ATTR_ELEMENT_NODE_DATA)
            driver.browser.settings.scriptLoader.reload()
            open(testURL, driver)

            val config = driver.evaluateValue("__pulsar_CONFIGS")
            println(config)

            val expression = """__pulsar_utils__.selectAttributes('section')"""

            val result = driver.evaluateValue(expression)
            println(result)
            assertNotNull(result)
            // println(result.javaClass.name)
            assertEquals("java.util.ArrayList", result.javaClass.name)
            assertTrue { result is List<*> }
            require(result is List<*>)
//        assertEquals("nd", result[2])
//        assertEquals("409.7 222 864 411.8|12|16,3,f", result[3])
            // schema: ['color', 'background-color', 'font-size']
            assertContains(result[3].toString(), "16,3,f")
        }
    }

    @Test
    fun `test JS queryComputedStyle`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """__pulsar_utils__.queryComputedStyle('button', ['color', 'background-color'])"""

        val result = driver.evaluateValue(expression)
        println(result)
        assertTrue { result is Map<*, *> }
        assertEquals("{color=f, background-color=007bff}", result.toString())
    }

    @Test
    fun `test JS compute`() = runWebDriverTest(testURL, browser) { driver ->
        val expression = """new __pulsar_NodeTraversor(new __pulsar_NodeFeatureCalculator()).traverse(document.body);"""

        val result = driver.evaluateValue(expression)
        println(result)
    }
}
