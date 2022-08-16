package ai.platon.pulsar.test

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.session.PulsarSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
        options.ensureEventHandler().simulateEventHandler.onBeforeFetch.addLast { page, driver ->
            visit(url2, driver)
            null
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
