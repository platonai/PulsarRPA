package ai.platon.pulsar.browser

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.js.JsUtils
import ai.platon.pulsar.common.sleepSeconds
import org.apache.commons.lang3.StringUtils
import kotlin.test.*

class PulsarWebDriverMockSiteTests : WebDriverTestBase() {

    val text = "awesome AI enabled PulsarRPA!"

    @Test
    fun `test evaluate that returns primitive values`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val code = """1+1"""

        val result = driver.evaluate(code)
        assertEquals(2, result)
    }

    @Test
    fun `test evaluate that returns object`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val code = """__pulsar_utils__.getConfig()"""

        val result = driver.evaluateDetail(code)
        println(result)
        assertNotNull(result)
        assertNull(result.value)
        assertNull(result.exception)
        assertEquals("Object", result.className)
        assertEquals("Object", result.description)
        // assertEquals(2, result)

        val result2 = driver.evaluateValueDetail(code)
        println(result2)
        assertNotNull(result2)
        assertNull(result2.exception)
        assertNull(result2.className)
        assertNull(result2.description)
        val value2 = result2.value
        assertNotNull(value2)
        // println(value2::class.qualifiedName)
        assertEquals("java.util.LinkedHashMap", value2::class.qualifiedName)
        assertTrue { value2 is Map<*, *> }
        value2 as Map<*, *>
        assertEquals(browser.settings.viewportSize.width, value2["viewPortWidth"])

        val propertyNames = value2["propertyNames"]
        assertNotNull(propertyNames)
        // println(propertyNames::class.qualifiedName)
        assertEquals("java.util.ArrayList", propertyNames::class.qualifiedName)
        assertTrue { propertyNames is List<*> }
    }

    @Test
    fun `test evaluate single line expressions`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val code = "(() => {\n  const a = 1;\n  const b = 2;\n  return a + b;\n})()"

        val result = driver.evaluate(code)
        assertEquals(3, result)
    }

    @Test
    fun `test evaluate multi-line expressions`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
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
        println(result2)
        assertNotNull(result2)
        val exception = result2.exception
        assertNull(exception)
        // assertIs<JsException>(exception)
    }

    @Test
    fun `test evaluate IIFE (Immediately Invoked Function Expression)`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
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
    fun `test fill form with JavaScript`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('$selector')")
        println(detail)

        val inputValue = driver.selectFirstPropertyValueOrNull(selector, "value")

        assertEquals(text, inputValue)
    }

    @Test
    fun `test fill`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('input[id=input]').value")
        assertEquals(text, detail?.value)
    }

    @Test
    fun `test selectFirstPropertyValueOrNull`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val propValue = driver.selectFirstPropertyValueOrNull(selector, "value")

        assertEquals(text, propValue)
    }

    @Test
    fun `test selectPropertyValueAll`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"

        val propValues = driver.selectPropertyValueAll(selector, "tagName")
        println(propValues)
        assertEquals(listOf("INPUT", "INPUT", "INPUT"), propValues)
    }

    @Test
    fun `test setProperty`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"
        val propName = "value"

        driver.setProperty(selector, propName, text)

        val propValue = driver.selectFirstPropertyValueOrNull(selector, propName)
        assertEquals(text, propValue)
    }

    @Test
    fun `test setPropertyAll`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"
        val propName = "value"

        driver.setPropertyAll(selector, propName, text)

        val propValues = driver.selectPropertyValueAll(selector, propName)
        println(propValues)
        assertEquals(listOf(text, text, text), propValues)
    }

    @Test
    fun `test buildDomTree`() = runWebDriverTest("${generatedAssetsBaseURL}/interactive-1.html", browser) { driver ->
        var buildDomTreeJs = ResourceLoader.readString("js/buildDomTree.js")
        buildDomTreeJs = buildDomTreeJs.trimEnd { it.isWhitespace() || it == ';' }
        // println(StringUtils.abbreviateMiddle(buildDomTreeJs, "...", 500))

        val expression = """
                ($buildDomTreeJs)()
            """.trimIndent()
        val evaluation = driver.evaluateValueDetail(expression)
        assertNotNull(evaluation)
        evaluation.description = null
        println(StringUtils.abbreviateMiddle(evaluation.toString(), "...", 500))
        val value = evaluation.value
        assertNotNull(value)
        value as Map<*, *>
        val rootId = value["rootId"]
        assertNotNull(rootId)
        val map = value["map"]
        assertNotNull(map)
        map as Map<*, *>
        val node = map["0"]
        println(node)
        assertNotNull(node)

        sleepSeconds(10)
    }

    @Test
    fun `when open a JSON page then script is injected`() = runResourceWebDriverTest(jsonUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)

        evaluateExpressions(driver, "JSON")
    }

    @Test
    fun `when open a PLAIN TXT page then script is injected`() = runResourceWebDriverTest(plainTextUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)

        evaluateExpressions(driver, "PLAIN TXT")
    }

    @Test
    fun `when open a CSV TXT page then script is not injected`() = runResourceWebDriverTest(csvTextUrl) { driver ->
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            println(String.format("%-10s %-40s %s", "CSV TXT", expression, detail))
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
}
