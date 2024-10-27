package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class WebDriverTestBase : TestBase() {
    
    protected val logger = getLogger(this)
    protected val warnUpUrl = "https://www.amazon.com/"
    protected val originUrl = "https://www.amazon.com/"
    protected val url = "https://www.amazon.com/dp/B0C1H26C46"
    protected val resourceUrl2 = "https://www.amazon.com/robots.txt"
    protected val plainTextUrl get() = "http://127.0.0.1:$port/text"
    protected val csvTextUrl get() = "http://127.0.0.1:$port/csv"
    protected val jsonUrl get() = "http://127.0.0.1:$port/json"
    protected val walmartUrl = "https://www.walmart.com/ip/584284401"
    protected val asin get() = url.substringAfterLast("/dp/")
    protected val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    protected val settings get() = driverFactory.driverSettings
    protected val confuser get() = settings.confuser
    
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
