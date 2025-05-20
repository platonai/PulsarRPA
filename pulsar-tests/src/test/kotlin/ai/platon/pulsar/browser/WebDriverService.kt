package ai.platon.pulsar.browser

import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assumptions.assumeTrue

open class WebDriverService(
    val browserFactory: BrowserFactory,
    val requiredPageSize: Int = 100
) {
    fun runWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
                it.newDriver().use { driver ->
                    open(url, driver)

                    val pageSource = driver.pageSource()
                    val display = StringUtils.abbreviateMiddle(pageSource, "...", 100)
                    assumeTrue(
                        { (pageSource?.length ?: 0) > requiredPageSize },
                        "Page source is too small | $display"
                    )

                    block(driver)
                }
            }
        }
    }

    fun runWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
                open(url, driver)

                val pageSource = driver.pageSource()
                val display = StringUtils.abbreviateMiddle(pageSource, "...", 100)
                assumeTrue(
                    { (pageSource?.length ?: 0) > requiredPageSize },
                    "Page source is too small | $display"
                )

                block(driver)
            }
        }
    }

    fun runResourceWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
                it.newDriver().use { driver ->
                    openResource(url, driver)
                    block(driver)
                }
            }
        }
    }

    fun runResourceWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
                openResource(url, driver)
                block(driver)
            }
        }
    }

    fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launchRandomTempBrowser().use {
                it.newDriver().use { driver ->
                    block(driver)
                }
            }
        }
    }

    fun runWebDriverTest(browserId: BrowserId, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browserFactory.launch(browserId).use {
                it.newDriver().use { driver ->
                    block(driver)
                }
            }
        }
    }

    fun runWebDriverTest(browser: Browser, block: suspend (driver: WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { block(it) }
        }
    }

    open suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) {
        driver.navigateTo(url)
        driver.waitForSelector("body")
//        driver.waitForSelector("input[id]")

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
        assumeTrue({ (pageSource?.length ?: 0) > requiredPageSize }, "Page source is too small | $display")
    }

    open suspend fun openResource(url: String, driver: WebDriver, scrollCount: Int = 1) {
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

open class FastWebDriverService(
    browserFactory: BrowserFactory,
    requiredPageSize: Int = 1
) : WebDriverService(browserFactory, requiredPageSize) {
    override suspend fun open(url: String, driver: WebDriver, scrollCount: Int) {
        driver.navigateTo(url)
        driver.delay(1000)

        // make sure all metadata are available
        driver.evaluateDetail("__pulsar_utils__.waitForReady()")
        // make sure all metadata are available
        driver.evaluateDetail("__pulsar_utils__.compute()")

        val pageSource = driver.pageSource()
        val display = StringUtils.abbreviateMiddle(pageSource, "...", 100)
        assumeTrue({ (pageSource?.length ?: 0) > requiredPageSize }, "Page source is too small | $display")
    }
}
