package ai.platon.pulsar.test.browser

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.test.TestBase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromeWebDriverTests: TestBase() {

    private val logger = getLogger(this)
    private val url = "https://www.amazon.com/dp/B00BTX5926"
    private val asin = url.substringAfterLast("/dp/")
    private val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    private val confuser get() = BrowserSettings.confuser
    private val fieldSelectors = mapOf(
        "01productTitle" to "#productTitle",
        "02acrPopover" to "#acrPopover",
        "03acrCustomerReviewText" to "#acrCustomerReviewText",
        "04productOverview" to "#productOverview_feature_div",
        "05featureBullets" to "#featurebullets_feature_div",
        "06prodDetails" to "#prodDetails",
        "07customerReviews" to "#reviewsMedley",
        "08review1" to "#cm-cr-dp-review-list div[data-hook=review]:nth-child(1)",
        "09review2" to "#cm-cr-dp-review-list div[data-hook=review]:nth-child(2)",
        "10review3" to "#cm-cr-dp-review-list div[data-hook=review]:nth-child(3)",
        "11review4" to "#cm-cr-dp-review-list div[data-hook=review]:nth-child(4)",
        "12review5" to "#cm-cr-dp-review-list div[data-hook=review]:nth-child(5)",
    )

    private val screenshotDir = AppPaths.WEB_CACHE_DIR
        .resolve("tests")
        .resolve("screenshot")

    @Test
    fun `Ensure no injected js variables are seen`() {
        confuser.nameMangler = { script -> script }
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver, 3)

            val windowVariables = driver.evaluate("JSON.stringify(Object.keys(window))").toString()
            assertTrue { windowVariables.contains("document") }
            assertTrue { windowVariables.contains("setTimeout") }
            assertTrue { windowVariables.contains("scrollTo") }

            val variables = windowVariables.split(",")
                .map { it.trim('\"') }
                .filter { it.contains("__pulsar_") }
            assertEquals(0, variables.size)

            var result = driver.evaluate("typeof(__pulsar_utils__)").toString()
            assertEquals("function", result)

            val injectedNames = listOf(
                "__pulsar_utils__",
                "__pulsar_NodeFeatureCalculator",
                "__pulsar_NodeTraversor"
            )
            injectedNames.forEach { name ->
                result = driver.evaluate("typeof($name)").toString()
                assertEquals("function", result)
            }

            result = driver.evaluate("typeof(window.__pulsar_utils__)").toString()
            assertEquals("undefined", result)

            result = driver.evaluate("typeof(document.__pulsar_setAttributeIfNotBlank)").toString()
            assertEquals("function", result)

            driver.stop()
        }

        confuser.reset()
    }

    @Test
    fun `Ensure no injected document variables are seen`() {
        confuser.nameMangler = { script -> script }
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver, 3)

            val nodeVariables = driver.evaluate("JSON.stringify(Object.keys(document))").toString()
//            assertTrue { nodeVariables.contains("querySelector") }
//            assertTrue { nodeVariables.contains("textContent") }

            println(nodeVariables)

            val variables = nodeVariables.split(",")
                .map { it.trim('\"') }
                .filter { it.contains("__pulsar_") }
            println(variables.joinToString("\n"))

            var result = driver.evaluate("typeof(document.__pulsar_setAttributeIfNotBlank)").toString()
            assertEquals("function", result)

            driver.stop()
        }

        confuser.reset()
    }

    @Test
    fun testOpenNewTab() {
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            driver.clickMatches("ol li a", "href", "product-reviews")
            driver.waitForNavigation()
            driver.waitForSelector("body")
            driver.scrollDown(5)
        }
    }

    @Test
    fun testClickMatches() {
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            driver.clickMatches("ol li a", "href", "product-reviews")
            driver.waitForNavigation()
            driver.waitForSelector("body")
            assertNotEquals(url, driver.currentUrl())
        }
    }

    @Test
    fun testClickMatches2() {
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            driver.clickMatches("a[data-hook]", "See all reviews")
            driver.waitForNavigation()
            driver.waitForSelector("body")
            // assertNotEquals(url, driver.currentUrl())
        }
    }

    @Test
    fun testClickNthAnchor() {
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            val href = driver.clickNthAnchor(100, "body")
            println(href)

            driver.waitForNavigation()
            driver.waitForSelector("body")
            driver.scrollDown(5)
        }
    }

    @Test
    fun testMouseWheel() {
        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            driver.mouseWheelDown(5)

            val box = driver.boundingBox("body")
            println(box)
            assertNotNull(box)

            delay(3000)

            driver.mouseWheelUp(5)

            val box2 = driver.boundingBox("body")
            println(box2)
            assertNotNull(box2)
            assertTrue { box2.y < box.y }

            driver.stop()
        }
    }

    @Test
    fun testCaptureScreenshot() {
        System.setProperty("debugLevel", "0")

        val driver = driverFactory.create()

        runBlocking {
            open(url, driver)

            assertTrue { driver.exists("body") }
            val pageSource = driver.pageSource()
            assertNotNull(pageSource)
            assertTrue { pageSource.contains(asin) }

            fieldSelectors.forEach { (name, selector) ->
                val screenshot = driver.captureScreenshot(selector)

                if (screenshot != null) {
                    exportScreenshot("$name.jpg", screenshot)
                    delay(1000)
                } else {
                    logger.info("Can not take screenshot for {}", selector)
                }
            }

            driver.stop()
        }
    }

    private suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 5) {
        driver.navigateTo(url)
        driver.waitForSelector("body")
        driver.bringToFront()
        var n = scrollCount
        while (n-- > 0) {
            driver.scrollDown(1)
            delay(1000)
        }
    }

    @Throws(IOException::class)
    private fun exportScreenshot(filename: String, screenshot: String) {
        val path = screenshotDir.resolve(filename)
        val bytes = Base64.getDecoder().decode(screenshot)
        AppFiles.saveTo(bytes, path, true)
    }
}
