package ai.platon.pulsar.test

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromeWebDriverTests: TestBase() {

    private val logger = getLogger(this)
    private val url = "https://www.amazon.com/dp/B00BTX5926"
    private val asin = url.substringAfterLast("/dp/")
    private val driverFactory get() = session.context.getBean(WebDriverFactory::class)
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
    )

    private val screenshotDir = AppPaths.WEB_CACHE_DIR
        .resolve("tests")
        .resolve("screenshot")

    @Test
    fun testCaptureScreenshot() {
        System.setProperty("debugLevel", "100")

        val driver = driverFactory.create().also { it.startWork() }

        runBlocking {
            driver.navigateTo(url)
            driver.waitForSelector("body")
//            driver.bringToFront()
            var n = 10
            while (n-- > 0) {
                driver.scrollDown(1)
                delay(1000)
            }
            driver.moveMouseTo(0.12, 100.0)

            assertTrue { driver.exists("body") }
            val pageSource = driver.pageSource()
            assertNotNull(pageSource)
            assertTrue { pageSource.contains(asin) }

            driver.stopLoading()
            driver.evaluate("__pulsar_utils__.compute()")

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

    @Throws(IOException::class)
    private fun exportScreenshot(filename: String, screenshot: String) {
        val path = screenshotDir.resolve(filename)
        val bytes = Base64.getDecoder().decode(screenshot)
        AppFiles.saveTo(bytes, path, true)
    }
}
