package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.browser.common.ScriptConfuser.Companion.IDENTITY_NAME_MANGLER
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.Proxy
import java.nio.file.Path
import java.text.MessageFormat
import java.time.Duration
import java.util.*
import kotlin.test.*

class ChromeDevtoolsDriverTests : WebDriverTestBase() {
    
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
        session.globalCache.resetCaches()
        confuser.nameMangler = IDENTITY_NAME_MANGLER
    }
    
    @AfterTest
    fun tearDown() {
        session.globalCache.resetCaches()
        confuser.reset()
    }
    
    @Test
    fun `When navigate to a HTML page then the navigate state are correct`() = runWebDriverTest { driver ->
        open(url, driver, 1)
        
        val navigateEntry = driver.navigateEntry
        assertTrue { navigateEntry.documentTransferred }
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
    fun `when open a HTML page then script is injected`() = runWebDriverTest(originUrl) { driver ->
        var detail = driver.evaluateDetail("typeof(window)")
        println(detail)
        // assertNotNull(detail?.value)
        
        detail = driver.evaluateDetail("typeof(document)")
        println(detail)
        // assertNotNull(detail?.value)
        
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)
    }
    
    @Test
    fun `when open a PLAIN TXT page then script is injected`() = runResourceWebDriverTest(plainTextUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)
        
        evaluateExpressions(driver, "PLAIN TXT")
    }
    
    @Test
    fun `when open a CSV TXT page then script is injected`() = runResourceWebDriverTest(csvTextUrl) { driver ->
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            println(String.format("%-6s%-40s%s", "CSV TXT", expression, detail))
        }
        
        val nullExpressions = """
            typeof(__pulsar_utils__)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

        nullExpressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            assertTrue { detail?.value == null || detail.value == "undefined" }
        }
    }

    @Test
    fun `when open a JSON page then script is injected`() = runResourceWebDriverTest(jsonUrl) { driver ->
        val r = driver.evaluate("__pulsar_utils__.add(1, 1)")
        assertEquals(2, r)
        
        evaluateExpressions(driver, "JSON")
    }
    
    @Test
    fun `Ensure injected js variables are not seen`() = runWebDriverTest(originUrl) { driver ->
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
    fun `Ensure no injected document variables are seen`() = runWebDriverTest(originUrl) { driver ->
        val nodeVariables = driver.evaluate("JSON.stringify(Object.keys(document))").toString()
//            assertTrue { nodeVariables.contains("querySelector") }
//            assertTrue { nodeVariables.contains("textContent") }
        
        val variables = nodeVariables.split(",").map { it.trim('\"') }
        
        println(variables)
        
        val pulsarVariables = variables.filter { it.contains("__pulsar_") }
        assertTrue { pulsarVariables.isEmpty() }
        
        val result = driver.evaluate("typeof(document.__pulsar_setAttributeIfNotBlank)").toString()
        assertEquals("function", result)
    }
    
    @Test
    fun testOpenNewTab() = runWebDriverTest(url) { driver ->
        driver.clickMatches("ol li a", "href", "product-reviews")
        driver.waitForNavigation()
        driver.waitForSelector("body")
        driver.scrollDown(5)
    }
    
    @Test
    fun test_selectTextAll() = runWebDriverTest { driver ->
        driver.navigateTo(url)
        
        driver.waitForSelector("#productTitle")
        
        val timeout = Duration.ofSeconds(120)
        val remainingTime = driver.waitForSelector("#reviewsMedley", timeout) {
            driver.mouseWheelDown()
            driver.scrollTo("#reviewsMedley")
        }
        println("Remaining time: $remainingTime ms")
        assertTrue { !remainingTime.isNegative }
        assertTrue { driver.exists("#reviewsMedley") }
        
        driver.waitForSelector("a[data-hook=review]") { driver.mouseWheelDown() }
        val texts = driver.selectTextAll("a[data-hook=review-title]")
        assertTrue { texts.isNotEmpty() }
        texts.map { it.replace("\\s+".toRegex(), " ") }.forEach { text -> println(">>>$text<<<") }
    }
    
    @Test
    fun test_selectFirstAttributeOrNull() = runWebDriverTest { driver ->
        driver.navigateTo(url)
        
        driver.waitForSelector("#productTitle")
        
        val cssClass = driver.selectFirstAttributeOrNull("#productTitle", "class")
        assertNotNull(cssClass)
        println("Product title class: $cssClass")
    }
    
    @Test
    fun test_selectAttributes() = runWebDriverTest { driver ->
        driver.navigateTo(originUrl)
        
        val selector = "input[type=text]"
        driver.waitForSelector(selector)
        
        val attributes = driver.selectAttributes(selector)
        assertTrue { attributes.isNotEmpty() }
        println("attributes: $attributes")
    }
    
    @Test
    fun test_selectAttributeAll() = runWebDriverTest { driver ->
        driver.navigateTo(originUrl)
        
        val selector = ".product-shoveler a[href]"
        driver.waitForSelector(selector)
        
        println("Selecting attributes: ")
        
        var links = driver.selectAttributeAll(selector, "href")
        assertTrue { links.isNotEmpty() }
        
        println("Href: ")
        links.forEach { println(it) }
        
        links = driver.selectAttributeAll(selector, "abs:href")
        println("NOTE: abs:href not supported by WebDriver.selectAttributeXXX()")
        println("Abs:href: ")
        links.forEach { println(it) }
        // assertTrue { links.isEmpty() }
    }
    
    @Test
    fun testClickTextMatches() = runWebDriverTest { driver ->
        open(url, driver, 1)
        driver.waitForSelector("a[href*=stores]")
        
        // should match the anchor text "Visit the Apple Store"
        driver.clickTextMatches("a[href*=stores]", "Store")
        driver.waitForNavigation()
        driver.waitForSelector("body")
        
        // expected url like: https://www.amazon.com/stores/Apple/page/77D9E1F7-0337-4282-9DB6-B6B8FB2DC98D?ref_=ast_bln
        val currentUrl = driver.currentUrl()
        println(currentUrl)
        
        assertNotEquals(url, currentUrl)
        assertContains(currentUrl, "stores")
    }
    
    @Test
    fun testClickNthAnchor() = runWebDriverTest(originUrl) { driver ->
        driver.clickNthAnchor(100, "body")
//        println(href)
        
        driver.waitForNavigation()
        driver.waitForSelector("body")
        driver.scrollDown(5)
    }
    
    @Test
    fun testMouseMove() = runWebDriverTest(originUrl) { driver ->
        repeat(10) { i ->
            val x = 100.0 + 2 * i
            val y = 100.0 + 3 * i
            
            driver.moveMouseTo(x, y)
            
            delay(500)
        }
    }
    
    @Test
    fun testMouseWheel() = runWebDriverTest(originUrl) { driver ->
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
    fun testKeyPress() = runWebDriverTest { driver ->
        driver.navigateTo(url)
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
        assertEquals("Mate60", evaluate?.value)
        
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
        assertEquals("Mate60", evaluate?.value)
        
        driver.press(selector, "Enter")
        driver.waitForNavigation()
        assertTrue { driver.currentUrl() != url }
    }
    
    @Test
    fun testTypeText() = runWebDriverTest { driver ->
        driver.navigateTo(url)
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
        
        driver.press(selector, "Enter")
        driver.waitForNavigation(lastUrl)
        assertTrue { driver.currentUrl() != lastUrl }
    }
    
    @Test
    fun testCaptureScreenshot() = runWebDriverTest(url) { driver ->
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
    fun testDragAndHold() = runWebDriverTest(walmartUrl) { driver ->
        // 2022.09.06:
        // override the user agent, and walmart shows robot check page.
        // BrowserSettings.enableUserAgentOverriding()
        
        val result = driver.evaluate("__pulsar_utils__.doForAllFrames('HOLD', 'ME')")
        println(result)
    }
    
    @Test
    fun `When call queryClientRects then return client rects`() = runWebDriverTest(url) { driver ->
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
    
    @Test
    fun testProxyAuthorization() {
        val proxyEntry = ProxyEntry("127.0.0.1", 10808, "abc", "abc", Proxy.Type.SOCKS)
        if (!NetUtil.testTcpNetwork(proxyEntry.host, proxyEntry.port)) {
            logger.info(
                "To run this test case, you should rise a local proxy server with proxy: {}",
                proxyEntry.toURI()
            )
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
    private fun exportScreenshot(filename: String, screenshot: String): Path {
        val path = screenshotDir.resolve(filename)
        val bytes = Base64.getDecoder().decode(screenshot)
        return AppFiles.saveTo(bytes, path, true)
    }
}
