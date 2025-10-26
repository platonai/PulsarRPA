package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay
import kotlin.test.Test

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebDriverSimpleTests: WebDriverTestBase() {
    companion object {
        // Mock EC server URLs for testing
        const val PRODUCT_LIST_URL = "http://localhost:18080/ec/b?node=1292115012"
        const val PRODUCT_DETAIL_URL = "http://localhost:18080/ec/dp/B0E000001"
    }

    private val url = "https://www.amazon.com/"
    // private val url2 = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val url2 = PRODUCT_LIST_URL
    private val productURL = PRODUCT_DETAIL_URL

    @Test
    fun testScrollDown() {
        runEnhancedWebDriverTest(browser) { driver -> visit(PRODUCT_DETAIL_URL, driver) }
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

        runEnhancedWebDriverTest(browser) { driver ->
            openEnhanced(productURL, driver)
            val response = driver.chat("Tell me something about this page", "#productTitle")
            printlnPro(response)
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

