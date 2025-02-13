package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.browser.common.SimpleScriptConfuser
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assumptions.assumeTrue

class WebDriverTestBase : TestBase() {

    companion object {
        protected const val PAGE_SOURCE_MIN_LENGTH = 100
    }

    protected val logger = getLogger(this)
    protected val warnUpUrl = "https://www.amazon.com/"
    protected val originUrl = "https://www.amazon.com/"
    protected val url = "https://www.amazon.com/dp/B0C1H26C46"
    protected val resourceUrl2 = "https://www.amazon.com/robots.txt"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.text]
     * */
    protected val plainTextUrl get() = "http://127.0.0.1:$port/text"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.csv]
     * */
    protected val csvTextUrl get() = "http://127.0.0.1:$port/csv"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.json]
     * */
    protected val jsonUrl get() = "http://127.0.0.1:$port/json"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.robots]
     * */
    protected val robotsUrl get() = "http://127.0.0.1:$port/robots.txt"
    protected val walmartUrl = "https://www.walmart.com/ip/584284401"
    protected val asin get() = url.substringAfterLast("/dp/")
    protected val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    protected val settings get() = driverFactory.driverSettings
    protected val confuser get() = settings.confuser as SimpleScriptConfuser

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
            
            typeof(__pulsar_utils__)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

//    protected lateinit var server: HttpServer

//    @BeforeTest
//    fun createHTTPServer() {
//        server = HttpServer.create(InetSocketAddress(port), 0)
//        // raise a simple HTTP server to serve plain text, csv, json, etc.
//        server.createContext("/text") {
//            val response = """
//User-agent: *
//Disallow: /exec/obidos/account-access-login
//Disallow: /exec/obidos/change-style
//Disallow: /exec/obidos/flex-sign-in
//Disallow: /exec/obidos/handle-buy-box
//Disallow: /exec/obidos/tg/cm/member/
//Disallow: /gp/aw/help/id=sss
//Disallow: /gp/cart
//Disallow: /gp/flex
//Disallow: /gp/product/e-mail-friend
//Disallow: /gp/product/product-availability
//Disallow: /gp/product/rate-this-item
//Disallow: /gp/sign-in
//Disallow: /gp/reader
//            """.trimIndent()
//            it.sendResponseHeaders(200, response.length.toLong())
//            it.responseBody.use {
//                it.write(response.toByteArray())
//            }
//        }
//
//        server.createContext("/csv") {
//            val response = """
//                id,name,price
//                1,Apple,1.99
//                2,Banana,2.99
//                3,Cherry,3.99
//            """.trimIndent()
//            it.sendResponseHeaders(200, response.length.toLong())
//
//            it.responseBody.use { it.write(response.toByteArray()) }
//        }
//
//        server.createContext("/json") {
//            val response = """
//                {
//                    "id": 1,
//                    "name": "Apple",
//                    "price": 1.99
//                }
//            """.trimIndent()
//            it.sendResponseHeaders(200, response.length.toLong())
//            it.responseBody.use { it.write(response.toByteArray()) }
//        }
//
//        server.start()
//    }
//
//    @AfterTest
//    fun shutdownHTTPServer() {
//        server.stop(0)
//    }

    suspend fun evaluateExpressions(driver: WebDriver, type: String) {
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            println(String.format("%-6s%-40s%s", type, expression, detail))
        }
    }

    protected fun runWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.launchTempBrowser().use {
                it.newDriver().use { driver ->
                    open(url, driver)

                    val pageSource = driver.pageSource()
                    val display = StringUtils.abbreviateMiddle(pageSource, "...", 100)
                    assumeTrue(
                        { (pageSource?.length ?: 0) > PAGE_SOURCE_MIN_LENGTH },
                        "Page source is too small | $display"
                    )

                    block(driver)
                }
            }
        }
    }

    protected fun runResourceWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.launchTempBrowser().use {
                it.newDriver().use { driver ->
                    openResource(url, driver)
                    block(driver)
                }
            }
        }
    }

    protected fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.launchTempBrowser().use {
                it.newDriver().use { driver ->
                    block(driver)
                }
            }
        }
    }

    protected fun runWebDriverTest(browserId: BrowserId, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.launchBrowser(browserId).use {
                it.newDriver().use { driver ->
                    block(driver)
                }
            }
        }
    }

    protected suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) {
        driver.navigateTo(url)
        driver.waitForSelector("body")
        driver.waitForSelector("input[id]")
//        driver.bringToFront()
        var n = scrollCount
        while (n-- > 0) {
            driver.scrollDown(1)
            delay(1000)
        }
        driver.scrollToTop()

        val pageSource = driver.pageSource()
        val display = StringUtils.abbreviateMiddle(pageSource, "...", 100)
        assumeTrue({ (pageSource?.length ?: 0) > PAGE_SOURCE_MIN_LENGTH }, "Page source is too small | $display")
    }

    protected suspend fun openResource(url: String, driver: WebDriver, scrollCount: Int = 1) {
        driver.navigateTo(url)
        driver.waitForNavigation()
        var n = scrollCount
        while (n-- > 0) {
            driver.scrollDown(1)
            delay(1000)
        }
        driver.scrollToTop()
    }
}
