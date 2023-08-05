package ai.platon.pulsar.test.browser

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.test.TestBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class WebDriverTestBase: TestBase() {

    protected val logger = getLogger(this)
    protected val warnUpUrl = "https://www.amazon.com/"
    protected val url = "https://www.amazon.com/dp/B09V3KXJPB"
    protected val asin get() = url.substringAfterLast("/dp/")
    protected val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    protected val settings get() = driverFactory.driverSettings
    protected val confuser get() = settings.confuser

    protected fun runWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.create().use { driver ->
                open(url, driver)
                block(driver)
            }
        }
    }
    
    protected fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            driverFactory.create().use { driver ->
                block(driver)
            }
        }
    }

    protected suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) {
        driver.navigateTo(warnUpUrl)
        driver.navigateTo(url)
        driver.waitForSelector("body")
//        driver.bringToFront()
        var n = scrollCount
        while (n-- > 0) {
            driver.scrollDown(1)
            delay(1000)
        }
        driver.scrollToTop()
    }
}
