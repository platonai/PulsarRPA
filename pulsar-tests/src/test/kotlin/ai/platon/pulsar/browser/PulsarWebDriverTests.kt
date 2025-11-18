package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.printlnPro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PulsarWebDriverTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    val text = "awesome AI enabled Browser4!"

    @Test
    fun `test fill form with JavaScript`() = runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('$selector')")
        printlnPro(detail)

        val inputValue = driver.selectFirstPropertyValueOrNull(selector, "value")

        assertEquals(text, inputValue)
    }

    @Test
    fun `test fill`() = runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
        val selector = "input[id=input]"

        driver.fill(selector, text)

        val detail = driver.evaluateDetail("document.querySelector('input[id=input]').value")
        assertEquals(text, detail?.value)
    }

    @Test
    fun `test scrollBy`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val scrollY = driver.scrollBy(200.0, smooth = true)

        assertEquals(200.0, scrollY, 1.0)
        assertEquals(200.0, driver.evaluate("window.scrollY", 200.0), 1.0)
    }

    @Test
    fun `test selectFirstPropertyValueOrNull`() =
        runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
            val selector = "input[id=input]"

            driver.fill(selector, text)

            val propValue = driver.selectFirstPropertyValueOrNull(selector, "value")

            assertEquals(text, propValue)
        }

    @Test
    fun `test selectPropertyValueAll`() = runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
        val selector = "input"

        val propValues = driver.selectPropertyValueAll(selector, "tagName")
        printlnPro(propValues)
        assertEquals(listOf("INPUT", "INPUT", "INPUT"), propValues)
    }

    @Test
    fun `test setProperty`() = runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
        val selector = "input"
        val propName = "value"

        driver.setProperty(selector, propName, text)

        val propValue = driver.selectFirstPropertyValueOrNull(selector, propName)
        assertEquals(text, propValue)
    }

    @Test
    fun `test setPropertyAll`() = runEnhancedWebDriverTest(simpleDomURL, browser) { driver ->
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

    @Test
    fun `test scrollToBottom`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val bottomY = driver.scrollToBottom()
        val viewportHeight = (driver.evaluate("window.innerHeight", 0.0) as? Number)?.toDouble() ?: 0.0
        val totalHeight = (driver.evaluate(
            "Math.min(Math.max(document.documentElement.scrollHeight, document.body.scrollHeight), 15000)",
            0.0
        ) as? Number)?.toDouble() ?: 0.0
        val expectedBottomY = (totalHeight - viewportHeight).coerceAtLeast(0.0)
        val actualY = (driver.evaluate("window.scrollY", 0.0) as? Number)?.toDouble() ?: 0.0
        assertEquals(expectedBottomY, bottomY, 3.0)
        assertEquals(expectedBottomY, actualY, 3.0)
    }

    @Test
    fun `test scrollToTop`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        // First go to bottom to ensure movement
        driver.scrollToBottom()
        val topY = driver.scrollToTop()
        val actualY = (driver.evaluate("window.scrollY", -1.0) as? Number)?.toDouble() ?: -1.0
        assertEquals(0.0, topY, 1.0)
        assertEquals(0.0, actualY, 1.0)
    }

    @Test
    fun `test scrollToMiddle`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val ratio = 0.5
        val middleY = driver.scrollToMiddle(ratio)
        val viewportHeight = (driver.evaluate("window.innerHeight", 0.0) as? Number)?.toDouble() ?: 0.0
        val totalHeight = (driver.evaluate(
            "Math.min(Math.max(document.documentElement.scrollHeight, document.body.scrollHeight), 15000)",
            0.0
        ) as? Number)?.toDouble() ?: 0.0
        val maxScrollY = (totalHeight - viewportHeight).coerceAtLeast(0.0)
        val expectedMiddleY = maxScrollY * ratio
        val actualY = (driver.evaluate("window.scrollY", 0.0) as? Number)?.toDouble() ?: 0.0
        assertEquals(expectedMiddleY, middleY, 5.0)
        assertEquals(expectedMiddleY, actualY, 5.0)
    }

    @Test
    fun `test scrollToViewport`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val n = 2.0 // second viewport
        val y = driver.scrollToViewport(n, smooth = false)
        val viewportHeight = (driver.evaluate("window.innerHeight", 0.0) as? Number)?.toDouble() ?: 0.0
        val totalHeight = (driver.evaluate(
            "Math.min(Math.max(document.documentElement.scrollHeight, document.body.scrollHeight), 15000)",
            0.0
        ) as? Number)?.toDouble() ?: 0.0
        val maxScrollY = (totalHeight - viewportHeight).coerceAtLeast(0.0)
        val expectedY = ((n - 1.0) * viewportHeight).coerceIn(0.0, maxScrollY)
        val actualY = (driver.evaluate("window.scrollY", 0.0) as? Number)?.toDouble() ?: 0.0
        assertEquals(expectedY, y, 5.0)
        assertEquals(expectedY, actualY, 5.0)
    }
}
