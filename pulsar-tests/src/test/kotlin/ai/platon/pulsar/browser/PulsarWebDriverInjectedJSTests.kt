package ai.platon.pulsar.browser

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test injected JS
 * */
class PulsarWebDriverInjectedJSTests : WebDriverTestBase() {

    val testURL get() = "$generatedAssetsBaseURL/interactive-4.html"

    @Test
    fun `test evaluate that returns primitive value`() = runWebDriverTest(testURL, browser) { driver ->
        val code = """1+1"""

        val result = driver.evaluate(code)
        assertEquals(2, result)
    }

    @Test
    fun `test evaluate that returns JS Object`() = runWebDriverTest(testURL, browser) { driver ->
        val code = """document"""

        val result = driver.evaluateDetail(code)
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
}
