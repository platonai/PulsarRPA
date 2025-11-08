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

class PulsarWebDriverMockSiteTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    val text = "awesome AI enabled Browser4!"

    @Test
    fun `test fill form with JavaScript`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('$selector')")
        printlnPro(detail)

        val inputValue = driver.selectFirstPropertyValueOrNull(selector, "value")

        assertEquals(text, inputValue)
    }

    @Test
    fun `test fill`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('input[id=input]').value")
        assertEquals(text, detail?.value)
    }

    @Test
    fun `test selectFirstPropertyValueOrNull`() =
        runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
            val selector = "input[id=input]"

            driver.fill(selector, text)

            val propValue = driver.selectFirstPropertyValueOrNull(selector, "value")

            assertEquals(text, propValue)
        }

    @Test
    fun `test selectPropertyValueAll`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"

        val propValues = driver.selectPropertyValueAll(selector, "tagName")
        printlnPro(propValues)
        assertEquals(listOf("INPUT", "INPUT", "INPUT"), propValues)
    }

    @Test
    fun `test setProperty`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"
        val propName = "value"

        driver.setProperty(selector, propName, text)

        val propValue = driver.selectFirstPropertyValueOrNull(selector, propName)
        assertEquals(text, propValue)
    }

    @Test
    fun `test setPropertyAll`() = runEnhancedWebDriverTest("$assetsBaseURL/dom.html", browser) { driver ->
        val selector = "input"
        val propName = "value"

        driver.setPropertyAll(selector, propName, text)

        val propValues = driver.selectPropertyValueAll(selector, propName)
        printlnPro(propValues)
        assertEquals(listOf(text, text, text), propValues)
    }

    @Test
    fun `test deleteCookies`() = runEnhancedWebDriverTest("$assetsPBaseURL/cookie.html", browser) { driver ->
        var cookies = driver.getCookies()

        printlnPro(cookies.toString())

        assertTrue(cookies.toString()) { cookies.isNotEmpty() }
        val cookie = cookies[0]
        assertEquals("token", cookie["name"])
        assertEquals("abc123", cookie["value"])
        assertEquals("127.0.0.1", cookie["domain"])
        assertEquals("/", cookie["path"])

        driver.deleteCookies("token", url = assetsPBaseURL) // OK
        // driver.deleteCookies("token", url = "$assetsPBaseURL/cookie.html") // OK

        cookies = driver.getCookies()
        assertTrue(cookies.toString()) { cookies.isEmpty() }
    }

    @Test
    fun `test clearBrowserCookies`() = runEnhancedWebDriverTest("$assetsPBaseURL/cookie.html", browser) { driver ->
        var cookies = driver.getCookies()

        printlnPro(cookies.toString())

        assertTrue(cookies.toString()) { cookies.isNotEmpty() }
        val cookie = cookies[0]
        assertEquals("token", cookie["name"])
        assertEquals("abc123", cookie["value"])
        assertEquals("127.0.0.1", cookie["domain"])
        assertEquals("/", cookie["path"])

        driver.clearBrowserCookies()

        cookies = driver.getCookies()
        assertTrue(cookies.toString()) { cookies.isEmpty() }
    }
}

