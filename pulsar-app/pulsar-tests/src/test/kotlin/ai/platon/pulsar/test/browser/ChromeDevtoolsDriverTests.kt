package ai.platon.pulsar.test.browser

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.net.Proxy
import java.util.*
import kotlin.test.*

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ChromeDevtoolsDriverTests: WebDriverTestBase() {

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

    @BeforeTest
    fun setup() {
        // identity name mangler
        confuser.nameMangler = { script -> script }
    }

    @Test
    fun `Ensure js injected`() = runWebDriverTest { driver ->
        open(url, driver, 1)

        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
//            println(r)
//            readLine()

        assertEquals(2, r)
    }

    @Test
    fun `Ensure no injected js variables are seen`() = runWebDriverTest { driver ->
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
    }

    @Test
    fun `Ensure no injected document variables are seen`() {
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

    @Ignore("Ignored temporary, test failed")
    @Test
    fun testClickTextMatches() = runWebDriverTest { driver ->
        open(url, driver, 1)

        driver.clickTextMatches("a[href~=stores]", "Store")
        driver.waitForNavigation()
        driver.waitForSelector("body")
        assertNotEquals(url, driver.currentUrl())
    }

    @Test
    fun testClickMatches2() = runWebDriverTest { driver ->
        open(url, driver)

        driver.clickTextMatches("a[data-hook]", "See all reviews")
        driver.waitForNavigation()
        driver.waitForSelector("body")
        // assertNotEquals(url, driver.currentUrl())
    }

    @Test
    fun testClickNthAnchor() = runWebDriverTest { driver ->
        open(url, driver)

        val href = driver.clickNthAnchor(100, "body")
        println(href)

        driver.waitForNavigation()
        driver.waitForSelector("body")
        driver.scrollDown(5)
    }

    @Test
    fun testMouseMove() = runWebDriverTest(url) { driver ->
        repeat(10) { i ->
            val x = 100.0 + 2 * i
            val y = 100.0 + 3 * i

            driver.moveMouseTo(x, y)

            delay(500)
        }
    }

    @Test
    fun testMouseWheel() = runWebDriverTest(url) { driver ->
        driver.mouseWheelDown(5)
        val box = driver.boundingBox("body")
        println(box)
        assertNotNull(box)

        delay(3000)

        driver.mouseWheelUp(5)

        val box2 = driver.boundingBox("body")
        println(box2)
        assertNotNull(box2)
        // assertTrue { box2.height > box.height }
    }

    @Test
    fun testCaptureScreenshot() {
        System.setProperty("debugLevel", "0")

        runWebDriverTest { driver ->
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
        }
    }

    @Test
    fun testDragAndHold() {
        val walmartUrl = "https://www.walmart.com/ip/584284401"
        // 2022.09.06:
        // override the user agent, and walmart shows robot check page.
        BrowserSettings.enableUserAgentOverriding()
        val driver = driverFactory.create()

        runBlocking {
            open(walmartUrl, driver)

            driver.waitForNavigation()
            driver.waitForSelector("body")

            val result = driver.evaluate("__pulsar_utils__.doForAllFrames('HOLD', 'ME')")
            println(result)
        }
    }

    @Test
    fun testProxyAuthorization() {
        val proxyEntry = ProxyEntry("127.0.0.1", 10808, "abc", "abc", Proxy.Type.SOCKS)
        if (!NetUtil.testTcpNetwork(proxyEntry.host, proxyEntry.port)) {
            logger.info("To run this test case, you should rise a local proxy server with proxy: {}", proxyEntry.toURI())
            return
        }

        val browserId = BrowserId.RANDOM
        browserId.setProxy(proxyEntry)

        val browser = driverFactory.launchBrowser(browserId)
        val driver = browser.newDriver()

        runBlocking {
            driver.navigateTo("https://www.baidu.com/")
            driver.waitForNavigation()
            driver.waitForSelector("body")
            delay(1000)
            val source = driver.pageSource()
            assertTrue { source != null && source.length > 1000 }
        }
    }

    @Throws(IOException::class)
    private fun exportScreenshot(filename: String, screenshot: String) {
        val path = screenshotDir.resolve(filename)
        val bytes = Base64.getDecoder().decode(screenshot)
        AppFiles.saveTo(bytes, path, true)
    }
}
