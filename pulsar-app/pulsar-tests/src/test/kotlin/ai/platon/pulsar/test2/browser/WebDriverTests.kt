package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay
import kotlin.test.Test

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebDriverTests: TestBase() {
    private val logger = getLogger(this)
    private val url = "https://www.amazon.com/"
    private val url2 = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"

    @Test
    fun testScrollDown() {
        val options = session.options("-refresh")
        options.event.browseEventHandlers.onWillFetch.addLast { _, driver ->
            visit(url2, driver)
        }
        session.load(url, options)
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
