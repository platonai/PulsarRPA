package ai.platon.pulsar.test

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws
import kotlin.test.assertNotNull

class ChromeWebDriverTests: TestBase() {

    private val url = "https://www.amazon.com/dp/B00BTX5926"
    private val driverFactory get() = session.context.getBean(WebDriverFactory::class)
    private val fieldSelectors = listOf(
        "#productTitle",
        "#acrPopover",
        "#acrCustomerReviewText",
        "#productOverview_feature_div",
        "#featurebullets_feature_div",
        "#prodDetails",
        "#reviewsMedley"
    )

    private val screenshotDir = AppPaths.WEB_CACHE_DIR
        .resolve("tests")
        .resolve("screenshot")

    @Test
    fun testCaptureScreenshot() {
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
            driver.evaluate("__pulsar_utils__.compute()")
            driver.stopLoading()

            fieldSelectors.forEach { selector ->
                driver.scrollTo(selector)
                val screenshot = driver.captureScreenshot(selector)
                assertNotNull(screenshot)
                val filename = selector.replace("[#.]".toRegex(), "f")
                exportScreenshot("$filename.jpg", screenshot)
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
