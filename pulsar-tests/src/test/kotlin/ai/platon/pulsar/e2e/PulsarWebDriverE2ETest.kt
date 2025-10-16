package ai.platon.pulsar.e2e

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.IOException
import java.nio.file.Path
import java.text.MessageFormat
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("E2ETest")
open class PulsarWebDriverE2ETest : WebDriverTestBase() {

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

    private val screenshotDir = AppPaths.TEST_DIR.resolve("screenshot")

    @BeforeTest
    fun setup() {
        logger.warn("Tests may fall because of page layout changing")

        session.globalCache.resetCaches()
    }

    @AfterTest
    fun tearDown() {
        session.globalCache.resetCaches()
    }

    @Test
    fun `When navigate to a HTML page then the navigate state are correct`() = runEnhancedWebDriverTest(browser) { driver ->
        openEnhanced(e2eProductUrl, driver, 1)

        val navigateEntry = driver.navigateEntry
        assertTrue("Expect documentTransferred") { navigateEntry.documentTransferred }
        assertTrue { navigateEntry.networkRequestCount.get() > 0 }
        assertTrue { navigateEntry.networkResponseCount.get() > 0 }

        require(driver is AbstractWebDriver)
        assertEquals(200, driver.mainResponseStatus)
        assertTrue { driver.mainResponseStatus == 200 }
        assertTrue { driver.mainResponseHeaders.isNotEmpty() }
        assertEquals(200, navigateEntry.mainResponseStatus)
        assertTrue { navigateEntry.mainResponseStatus == 200 }
        assertTrue { navigateEntry.mainResponseHeaders.isNotEmpty() }
    }

    @Test
    fun `when open a HTML page then script is injected`() = runEnhancedWebDriverTest(e2eOriginUrl, browser) { driver ->
        var detail = driver.evaluateDetail("typeof(window)")
        println(detail)
        // assertNotNull(detail?.value)

        detail = driver.evaluateDetail("typeof(document)")
        println(detail)
        // assertNotNull(detail?.value)

        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)

        detail = driver.evaluateDetail("JSON.stringify(__pulsar_CONFIGS)")
        val value = detail?.value?.toString()
        assertNotNull(value)
        println(value)
        assertTrue { value.contains("viewPortWidth") }

        detail = driver.evaluateDetail("JSON.stringify(__pulsar_utils__.getConfig())")
        val value2 = detail?.value?.toString()
        assertNotNull(value2)
        println(value2)
        assertTrue { value2.contains("viewPortWidth") }
    }

    @Test
    fun `open a HTML page and compute metadata`() = runEnhancedWebDriverTest(e2eOriginUrl, browser) { driver ->
        driver.evaluate("__pulsar_utils__.scrollToMiddle()")
        var detail = driver.evaluateDetail("__pulsar_utils__.compute()")
        println(detail)

        detail = driver.evaluateDetail("__pulsar_utils__.getActiveDomMessage()")
        println(detail)
        val data = detail?.value?.toString()
        assertNotNull(data)

        val message = ActiveDOMMessage.fromJson(data)
        val urls = message.urls
        assertNotNull(urls)
        assertEquals(e2eOriginUrl, urls.URL)

        val metadata = message.metadata
        assertNotNull(metadata)
        println(prettyPulsarObjectMapper().writeValueAsString(metadata))
        assertEquals(1920, metadata.viewPortWidth)
        assertEquals(1080, metadata.viewPortHeight)
        // Assumptions.assumeTrue(metadata.scrollTop > metadata.viewPortHeight)
        assertTrue { metadata.scrollTop >= 0 }
        assertTrue { metadata.scrollLeft.toInt() == 0 }
        assertTrue { metadata.clientWidth > 0 } // 1683 on my laptop
        assertTrue { metadata.clientHeight > 0 } // 986 on my laptop
    }

    @Test
    fun test_selectAttributeAll() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(e2eProductUrl)

        val selector = "body a[href]"
        driver.waitForSelector(selector)

        println("Selecting attributes: ")

        var links = driver.selectAttributeAll(selector, "href")
        assertTrue { links.isNotEmpty() }

        println("Top 10 href: ")
        links.take(10).forEach { println(it) }

        links = driver.selectAttributeAll(selector, "abs:href")
        println("NOTE: abs:href not supported by WebDriver.selectAttributeXXX()")
        println("Abs:href: ")
        links.forEach { println(it) }
        // assertTrue { links.isEmpty() }
    }

    @Test
    fun testClickTextMatches() = runEnhancedWebDriverTest(browser) { driver ->
        openEnhanced(e2eProductUrl, driver, 1)
//        driver.waitForSelector("a[href*=stores]")
        driver.waitForSelector("a[href*=HUAWEI]")

        // should match the anchor text "Brand: HUAWEI"
//        driver.clickTextMatches("a[href*=stores]", "Store")
        driver.clickTextMatches("a[href*=HUAWEI]", "HUAWEI")
        driver.waitForNavigation()
        driver.waitForSelector("body")

        // expected url like: https://www.amazon.com/stores/Apple/page/77D9E1F7-0337-4282-9DB6-B6B8FB2DC98D?ref_=ast_bln
        val currentUrl = driver.currentUrl()
        println("The page should be redirected")
        println("Current url: $currentUrl")

        val pageSource = driver.pageSource()
        Assumptions.assumeTrue { (pageSource?.length ?: 0) > 1000 }
        Assumptions.assumeTrue { pageSource?.contains("HUAWEI", ignoreCase = true) == true }

        assertNotEquals(e2eProductUrl, currentUrl)
        // assertContains(currentUrl, "HUAWEI", ignoreCase = true)
    }

    @Test
    fun testClickNthAnchor() = runEnhancedWebDriverTest(e2eOriginUrl, browser) { driver ->
        driver.clickNthAnchor(100, "body")
//        println(href)

        driver.waitForNavigation()
        driver.waitForSelector("body")
        driver.scrollDown(5)
    }

    @Test
    fun testMouseMove() = runEnhancedWebDriverTest(e2eOriginUrl, browser) { driver ->
        repeat(10) { i ->
            val x = 100.0 + 2 * i
            val y = 100.0 + 3 * i

            driver.moveMouseTo(x, y)

            delay(500)
        }
    }

    @Test
    fun testMouseWheel() = runEnhancedWebDriverTest(e2eOriginUrl, browser) { driver ->
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
    fun testKeyPress() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(e2eProductUrl)
        driver.waitForSelector("#productTitle")

        assertTrue { driver.exists("#productTitle") }

        var text = driver.selectFirstTextOrNull("#productTitle")
        println("Product title: $text")

        // val selector = "#nav-search input[placeholder*=Search]"
        val selector = "form input[type=text]"
        text = driver.selectFirstAttributeOrNull(selector, "placeholder")
        println("Search bar - placeholder - 1 - driver.selectFirstAttributeOrNull() : <$text>")
        assertTrue("Placeholder should not be empty") { !text.isNullOrBlank() }
        text = driver.selectAttributeAll(selector, "placeholder").joinToString()
        println("Search bar - placeholder - 2 - driver.selectAttributeAll() : <$text>")
        assertTrue("Placeholder should not be empty") { !text.isNullOrBlank() }

        text = driver.selectAttributeAll(selector, "value").joinToString()
        println("Search bar value (should be empty) - 1: <$text>")
        assertEquals("", text)

        "Mate".forEach { ch ->
            driver.press(selector, "$ch")
        }
        driver.press(selector, "Digit6")
        driver.press(selector, "0")

        delay(1000)

        MessageFormat.format("{0} key pressed {0}", PopularEmoji.SPARKLES).also { println(it) }

        var evaluate = driver.evaluateDetail("document.querySelector('$selector').value")
        println("Search bar evaluate result - driver.evaluateDetail() : $evaluate")
        println("Search bar value - driver.evaluateDetail() : <${evaluate?.value}>")
        // assertEquals("Mate60", evaluate?.value)

        text = driver.selectAttributeAll(selector, "value").joinToString()
        println("Search bar value - 3 - selectAttributeAll() : <$text>")
//            assertEquals("Mate60", text)

        val html = driver.outerHTML(selector)
        println("Search bar html: >>>\n$html\n<<<")
        assertNotNull(html)
        // assertTrue { html.contains("Mate60") }

        evaluate = driver.evaluateDetail("document.querySelector('$selector').value")
        println("Search bar evaluate result - driver.evaluateDetail() : >>>\n$evaluate\n<<<")
        println("Search bar value - driver.evaluateDetail() : <${evaluate?.value}>")
        // assertEquals("Mate60", evaluate?.value)

        // TODO: FIXME: enter seems not working
        driver.press(selector, "Enter")
        driver.waitForNavigation()
        assertTrue { driver.currentUrl() != e2eProductUrl }
    }

    @Test
    fun testTypeText() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(e2eProductUrl)
        driver.waitForSelector("#productTitle")

        assertTrue { driver.exists("#productTitle") }

        var text = driver.selectFirstTextOrNull("#productTitle")
        println("Product title: $text")

        // val selector = "#nav-search input[placeholder*=Search]"
        val selector = "form input[type=text]"
        text = driver.selectFirstAttributeOrNull(selector, "placeholder")
        println("Search bar - placeholder - 1 - driver.selectFirstAttributeOrNull() : <$text>")
        assertTrue("Placeholder should not be empty") { !text.isNullOrBlank() }
        text = driver.selectAttributeAll(selector, "placeholder").joinToString()
        println("Search bar - placeholder - 2 - driver.selectAttributeAll() : <$text>")
        assertTrue("Placeholder should not be empty") { !text.isNullOrBlank() }

        text = driver.selectAttributeAll(selector, "value").joinToString()
        println("Search bar value (should be empty) - 1: <$text>")
        assertEquals("", text)

        driver.type(selector, "Mate60")

        MessageFormat.format("{0} text typed {0}", PopularEmoji.SPARKLES).also { println(it) }

        var evaluate = driver.evaluateDetail("document.querySelector('$selector').value")
        println("Search bar evaluate result - driver.evaluateDetail() : $evaluate")
        println("Search bar value - driver.evaluateDetail() : <${evaluate?.value}>")
        assertEquals("Mate60", evaluate?.value)

        text = driver.selectAttributeAll(selector, "value").joinToString()
        println("Search bar value - 3: $text")
//            assertEquals("Mate60", text)

        val html = driver.outerHTML(selector)
        println("Search bar html: $html")
        assertNotNull(html)
// assertTrue { html.contains("Mate60") }

        evaluate = driver.evaluateDetail("document.querySelector('$selector').value")
        println("Search bar evaluate result - driver.evaluateDetail() : $evaluate")
        println("Search bar value - driver.evaluateDetail() : <${evaluate?.value}>")
        assertEquals("Mate60", evaluate?.value)

        val lastUrl = driver.currentUrl()

        // TODO: FIXME: enter seems not working
        driver.press(selector, "Enter")
        driver.waitForNavigation(oldUrl = lastUrl)
        // assertTrue { driver.currentUrl() != lastUrl }
        val currentUrl = driver.currentUrl()
        Assumptions.assumingThat(currentUrl != lastUrl) {
            println("Current url: $currentUrl, last url: $lastUrl")
        }
    }

    @Test
    fun testCaptureScreenshot() = runEnhancedWebDriverTest(e2eProductUrl, browser) { driver ->
        driver.waitForSelector("#productTitle")
        assertTrue { driver.exists("body") }
        val pageSource = driver.pageSource()
        assertNotNull(pageSource)
        assertTrue { pageSource.contains(asin) }

        val paths = mutableListOf<Path>()
        fieldSelectors.entries.take(3).forEach { (name, selector) ->
            val screenshot = driver.runCatching { captureScreenshot(selector) }
                .onFailure { logger.info("Failed to captureScreenshot | $name - $selector") }
                .getOrNull()

            if (screenshot != null) {
                val path = exportScreenshot("$name.jpg", screenshot)
                paths.add(path)
                delay(1000)
            }
        }

        if (paths.isNotEmpty()) {
            println(String.format("%d screenshots are saved | %s", paths.size, paths[0].parent))
        }

        // assertTrue { paths.isNotEmpty() }
    }

    @Test
    fun testDragAndHold() = runEnhancedWebDriverTest(walmartUrl, browser) { driver ->
        // TODO: FIXME: dragAndHold not working on walmart.com
        val result = driver.evaluate("__pulsar_utils__.doForAllFrames('HOLD', 'ME')")
        println(result)
    }

    @Test
    fun `When call queryClientRects then return client rects`() = runEnhancedWebDriverTest(e2eProductUrl, browser) { driver ->
        driver.mouseWheelDown(5)
        val box = driver.boundingBox("body")
        // RectD(x=0.0, y=-600.0, width=1912.0, height=10538.828125)
        println(box)
        assertNotNull(box)

        delay(3000)

        driver.mouseWheelUp(5)

        val box2 = driver.boundingBox("body")
        // RectD(x=0.0, y=-150.0, width=1912.0, height=10538.828125)
        println(box2)
        assertNotNull(box2)

        var jsFun = "__pulsar_utils__.queryClientRects"
        var bodyInfo = driver.evaluate("$jsFun('body')")?.toString() ?: "unexpected"
        // [{0 0 1912 10538.8}, ]
        println("queryClientRects: $bodyInfo")

        jsFun = "__pulsar_utils__.queryClientRect"
        bodyInfo = driver.evaluate("$jsFun('body')")?.toString() ?: "unexpected"
        // [{0 0 1912 10538.8}, ]
        println("queryClientRect: $bodyInfo")

        jsFun = "document.body.scrollWidth"
        bodyInfo = driver.evaluate("$jsFun('body')")?.toString() ?: "unexpected"
        // [{0 0 1912 10538.8}, ]
        println("body.scrollWidth: $bodyInfo")

        jsFun = "document.body.clientWidth"
        bodyInfo = driver.evaluate("$jsFun('body')")?.toString() ?: "unexpected"
        // [{0 0 1912 10538.8}, ]
        println("body.clientWidth: $bodyInfo")
    }

    @Throws(IOException::class)
    private fun exportScreenshot(filename: String, screenshot: String): Path {
        val path = screenshotDir.resolve(filename)
        val bytes = Base64.getDecoder().decode(screenshot)
        return AppFiles.saveTo(bytes, path, true)
    }
}
