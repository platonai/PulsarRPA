package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserUnavailableException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.runBlocking
import java.util.*

class PlaywrightBrowser(
    id: BrowserId,
    val implementation: com.microsoft.playwright.Browser,
    settings: BrowserSettings
) : AbstractBrowser(id, settings) {
    private val logger = getLogger(this)

    companion object {
        private val playwright = Playwright.create()
    }

    constructor(port: Int, settings: BrowserSettings = BrowserSettings()) :
            this(BrowserId.RANDOM, playwright.chromium().connectOverCDP("http://localhost:$port/"), settings)

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver() = newDriver("")

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(url: String): PlaywrightDriver {
        try {
            // In chrome every tab is a separate process
            val uuid = UUID.randomUUID().toString()
            return newDriverIfAbsent(url, uuid, false) as PlaywrightDriver
        } catch (e: WebDriverException) {
            throw BrowserUnavailableException("newDriver", e)
        } catch (e: Exception) {
            logger.warn("Failed to create new driver, rethrow | {}", e.message)
            throw WebDriverException("Failed to create chrome devtools driver | " + e.message)
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriverUnmanaged(url: String): PlaywrightDriver {
        val driver = PlaywrightDriver(this, implementation.newPage())
        runBlocking {
            driver.navigateTo(url)
            driver.waitForNavigation()
        }
        return driver
    }
}
