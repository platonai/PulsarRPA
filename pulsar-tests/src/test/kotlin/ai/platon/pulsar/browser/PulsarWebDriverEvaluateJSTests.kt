package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.js.JsUtils
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PulsarWebDriverEvaluateJSTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    val text = "awesome AI enabled Browser4!"

    protected val expressions = """
            typeof(window)

            typeof(window.history)
            window.history
            window.history.length

            typeof(document)
            document.location
            document.baseURI

            typeof(document.body)
            document.body.clientWidth

            typeof(__pulsar_)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

    suspend fun evaluateExpressions(driver: WebDriver, type: String) {
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            printlnPro(String.format("%-6s%-40s%s", type, expression, detail))
        }
    }

    @Test
    fun `test evaluate that returns primitive values`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val code = """1+1"""

            val result = driver.evaluate(code)
            assertEquals(2, result)
        }

    @Test
    fun `test evaluate that returns object`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val code = """__pulsar_utils__.getConfig()"""

        val result = driver.evaluateDetail(code)
        printlnPro(result)
        assertNotNull(result)
        assertNull(result.value)
        assertNull(result.exception)
        assertEquals("Object", result.className)
        assertEquals("Object", result.description)

        val result2 = driver.evaluateValueDetail(code)
        printlnPro(result2)
        assertNotNull(result2)
        assertNull(result2.exception)
        assertNull(result2.className)
        assertNull(result2.description)
        val value2 = result2.value
        assertNotNull(value2)
        assertEquals("java.util.LinkedHashMap", value2::class.qualifiedName)
        assertTrue { value2 is Map<*, *> }
        value2 as Map<*, *>
        assertEquals(browser.settings.viewportSize.width, value2["viewPortWidth"])

        val propertyNames = value2["propertyNames"]
        assertNotNull(propertyNames)
        assertEquals("java.util.ArrayList", propertyNames::class.qualifiedName)
        assertTrue { propertyNames is List<*> }
    }

    @Test
    fun `test evaluate single line expressions`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val code = "(() => {\n  const a = 1;\n  const b = 2;\n  return a + b;\n})()"

            val result = driver.evaluate(code)
            assertEquals(3, result)
        }

    @Test
    fun `test evaluate multi-line expressions`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val code = """
() => {
  const a = 10;
  const b = 20;
  return a * b;
}
        """.trimIndent()

            val result = driver.evaluate(JsUtils.toIIFE(code))
            assertEquals(200, result)

            val code2 = """
  const a = 10;
  const b = 20;
  return a * b;
        """.trimIndent()

            // converted to "// ❌ Unsupported format: not a valid JS function"
            // so it's an empty expressions sent to the browser

            val result2 = driver.evaluateValueDetail(JsUtils.toIIFE(code2))
            printlnPro(result2)
            assertNotNull(result2)
            val exception = result2.exception
            assertNull(exception)
            // assertIs<JsException>(exception)
        }

    @Test
    fun `test evaluate IIFE (Immediately Invoked Function Expression)`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val code = """
(() => {
  const a = 10;
  const b = 20;
  return a * b;
})()
        """.trimIndent()

            val result = driver.evaluate(code)
            assertEquals(200, result)
        }

    @Test
    fun `test buildDomTree`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        var buildDomTreeJs = ResourceLoader.readString("js/build_dom_tree.js")
        buildDomTreeJs = buildDomTreeJs.trimEnd { it.isWhitespace() || it == ';' }
        // logPrintln(StringUtils.abbreviateMiddle(buildDomTreeJs, "...", 500))

        val expression = """
                ($buildDomTreeJs)()
            """.trimIndent()
        val evaluation = driver.evaluateValueDetail(expression)
        assertNotNull(evaluation)
        evaluation.description = null
        printlnPro(StringUtils.abbreviateMiddle(evaluation.toString(), "...", 500))
        val value = evaluation.value
        assertNotNull(value)
        value as Map<*, *>
        val rootId = value["rootId"]
        assertNotNull(rootId)
        val map = value["map"]
        assertNotNull(map)
        map as Map<*, *>
        val node = map["0"]
        printlnPro(node)
        assertNotNull(node)

        sleepSeconds(10)
    }

    @Test
    fun `when open a JSON page then script is injected`() = runWebDriverTest(jsonUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)

        evaluateExpressions(driver, "JSON")
    }

    @Test
    fun `when open a PLAIN TXT page then script is injected`() = runWebDriverTest(plainTextUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)

        evaluateExpressions(driver, "PLAIN TXT")
    }

    @Test
    fun `when open a CSV TXT page then script is not injected`() = runWebDriverTest(csvTextUrl) { driver ->
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            printlnPro(String.format("%-10s %-40s %s", "CSV TXT", expression, detail))
        }

        val nullExpressions = """
            typeof(__pulsar_)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

        nullExpressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            assertTrue { detail?.value == null || detail.value == "undefined" }
        }
    }

    @Test
    fun `test already-invoked IIFE is not double-wrapped and evaluates`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val iife = "(() => { return 3 })()"
            val expression = JsUtils.toIIFE(iife)
            // should normalize with trailing semicolon
            assertTrue(expression.trim().endsWith(";"))
            val result = driver.evaluate(expression)
            assertEquals(3, result)
        }

    @Test
    fun `test arrow function with arguments via IIFE`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val arrow = "x => x * 2"
            val expression = JsUtils.toIIFE(arrow, "5")
            val result = driver.evaluate(expression)
            assertEquals(10, result)
        }

    @Test
    fun `test object literal IIFE returns object by value`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val obj = "{ answer: 42, nested: { ok: true } }"
            val expression = JsUtils.toIIFE(obj)
            val detail = driver.evaluateValueDetail(expression)
            assertNotNull(detail)
            assertNull(detail.exception)
            val value = detail.value
            assertNotNull(value)
            assertTrue(value is Map<*, *>)
            assertEquals(42, (value as Map<*, *>)["answer"])
            val nested = value["nested"]
            assertNotNull(nested)
            assertTrue(nested is Map<*, *>)
            assertEquals(true, (nested as Map<*, *>)["ok"])
        }

    @Test
    fun `test plain function IIFE passthrough`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val funcIife = "(function(){ return 2 * 3 })()"
            val expression = JsUtils.toIIFE(funcIife)
            var result = driver.evaluate(expression)
            assertEquals(6, result)

            result = driver.evaluate(JsUtils.toExpression(funcIife))
            assertEquals(6, result)
        }
}
