package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.SimpleScriptConfuser
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WebDriverTestBase : TestBase() {

    companion object {
        protected const val PAGE_SOURCE_MIN_LENGTH = 100
    }

    @Value("\${server.port}")
    val port: Int = 0

//    @Value("\${server.servlet.context-path}")
//    val contextPath: String = "/api"

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    protected val logger = getLogger(this)
    protected val warnUpUrl = "https://www.amazon.com/"
    protected val originUrl = "https://www.amazon.com/"
    protected val productUrl = "https://www.amazon.com/dp/B0C1H26C46"
    protected val resourceUrl2 = "https://www.amazon.com/robots.txt"

    protected val baseURL get() = "http://127.0.0.1:$port"

    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.text]
     * */
    protected val plainTextUrl get() = "$baseURL/text"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.csv]
     * */
    protected val csvTextUrl get() = "$baseURL/csv"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.json]
     * */
    protected val jsonUrl get() = "$baseURL/json"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.robots]
     * */
    protected val robotsUrl get() = "$baseURL/robots.txt"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.amazonHome]
     * */
    protected val mockAmazonHomeUrl get() = "$baseURL/amazon/home.htm"
    /**
     * @see [ai.platon.pulsar.test.rest.MockSiteController.amazonProduct]
     * */
    protected val mockAmazonProductUrl get() = "$baseURL/amazon/product.htm"

    protected val walmartUrl = "https://www.walmart.com/ip/584284401"
    protected val asin get() = productUrl.substringAfterLast("/dp/")
    protected val browserFactory = DefaultBrowserFactory()
    protected val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    protected val browser by lazy { browserFactory.launchDefaultBrowser() }
    protected val settings by lazy { BrowserSettings(conf) }
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
            
            typeof(__pulsar_)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

    suspend fun evaluateExpressions(driver: WebDriver, type: String) {
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            println(String.format("%-6s%-40s%s", type, expression, detail))
        }
    }

    protected fun runWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
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

    protected fun runWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
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

    protected fun runResourceWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
                it.newDriver().use { driver ->
                    openResource(url, driver)
                    block(driver)
                }
            }
        }
    }

    protected fun runResourceWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
                openResource(url, driver)
                block(driver)
            }
        }
    }

    protected fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
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

    protected fun runWebDriverTest(browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { block(it) }
        }
    }

    protected suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) {
        driver.navigateTo(url)
        driver.waitForSelector("body")
        driver.waitForSelector("input[id]")

        // make sure all metadata are available
        driver.evaluateDetail("__pulsar_utils__.waitForReady()")
        // make sure all metadata are available
        driver.evaluateDetail("__pulsar_utils__.compute()")

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
