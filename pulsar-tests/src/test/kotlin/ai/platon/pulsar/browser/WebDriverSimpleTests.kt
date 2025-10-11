package ai.platon.pulsar.browser

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay
import kotlin.test.Test

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebDriverSimpleTests: WebDriverTestBase() {
    private val url = "https://www.amazon.com/"
    private val url2 = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val productURL = "https://www.amazon.com/dp/B08PP5MSVB?th=1"

    @Test
    fun testScrollDown() {
        runWebDriverTest(browser) { driver -> visit(mockAmazonHomeUrl, driver) }
    }

    @Test
    fun testScrollDown2() {
        val options = session.options("-refresh")
        options.eventHandlers.browseEventHandlers.onWillFetch.addLast { _, driver ->
            visit(url2, driver)
        }
        session.load(url, options)
    }

    @Test
    fun testChat() {
        if (!ChatModelFactory.hasModel(conf)) {
            return
        }

        runWebDriverTest(browser) { driver ->
            open(productURL, driver)
            val response = driver.chat("Tell me something about this page", "#productTitle")
            println(response)
        }
    }

    private suspend fun visit(url: String, driver: WebDriver) {
        try {
            logger.info("Visiting {}", url)

            driver.navigateTo(url)
            driver.waitForSelector("body")
            var n = 10
            while (n-- > 0) {
                driver.scrollDown(1)
//                driver.evaluate("__pulsar_utils__.scrollDown()")
                delay(1000)
            }

            logger.info("Visited {}", url)
        } catch (e: Exception) {
            logger.warn("Can not visit $url", e)
        }
    }
}
