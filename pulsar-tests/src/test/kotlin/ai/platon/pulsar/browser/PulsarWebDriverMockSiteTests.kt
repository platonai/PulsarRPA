package ai.platon.pulsar.browser

import ai.platon.pulsar.common.ResourceLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class PulsarWebDriverMockSiteTests: WebDriverTestBase() {

    val text = "awesome AI enabled PulsarRPA!"

    @Test
    fun `test evaluate single line expressions`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val code = """(() => {\n  const a = 1;\n  const b = 2;\n  return a + b;\n})()"""

        val result = driver.evaluate(code)
        assertEquals(200, result)
    }

    @Test
    fun `test evaluate multiple line expressions`() = runWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
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

        driver.evaluateDetail("document.querySelector('$selector', 'value')")

        val detail = driver.evaluateDetail("document.querySelector('$selector')")
        println(detail)

        val inputValue = driver.selectFirstAttributeOrNull(selector, "value")

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
    fun `test buildDomTree`() = runWebDriverTest("${aiGenBaseURL}/interactive-page-1.html", browser) { driver ->
        val buildDomTreeJs = ResourceLoader.readAllLines("js/buildDomTree.js") { !it.startsWith("// ") }
            .joinToString("\n")
        val detail = driver.evaluateDetail(buildDomTreeJs)

        readln()
    }
}
