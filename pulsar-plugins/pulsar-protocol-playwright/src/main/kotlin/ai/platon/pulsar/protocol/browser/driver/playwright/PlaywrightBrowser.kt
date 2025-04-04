package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserUnavailableException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import java.nio.file.Path
import java.util.*

class PlaywrightBrowser(
    id: BrowserId,
    val browserContext: BrowserContext,
    settings: BrowserSettings
) : AbstractBrowser(id, settings) {
    private val logger = getLogger(this)

    companion object {
        private val playwright by lazy { Playwright.create() }

        @Throws(BrowserUnavailableException::class)
        fun connectOverCDP(port: Int): BrowserContext {
            return playwright.chromium()
                .connectOverCDP("http://localhost:$port/")
                .newContext() ?: throw BrowserUnavailableException("Failed to create browser context")
        }

        @Throws(BrowserUnavailableException::class)
        fun launchPersistentContext(
            userDataDir: Path,
            options: BrowserType.LaunchPersistentContextOptions
        ): BrowserContext {
            try {
                return playwright.chromium().launchPersistentContext(userDataDir, options)
            } catch (e: Exception) {
                throw BrowserUnavailableException("Failed to create browser context", e)
            }
        }
    }

    override val isConnected: Boolean get() = AppContext.isActive

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver() = newDriver("")

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(url: String): PlaywrightDriver {
        try {
            val driver = newDriverManaged(url)
            return driver
        } catch (e: WebDriverException) {
            throw BrowserUnavailableException("newDriver", e)
        } catch (e: Exception) {
            logger.warn("Failed to create new driver, rethrow | {}", e.message)
            throw WebDriverException("Failed to create chrome devtools driver | " + e.message)
        }
    }

    override fun close() {
        try {
            browserContext.close()
        } catch (e: Exception) {
            warnForClose(this, e, "Failed to close browser context")
        }
    }

    @Throws(WebDriverException::class)
    private fun newDriverManaged(url: String): PlaywrightDriver {
        val driver = newDriverUnmanaged(url)
        _drivers[driver.uniqueID] = driver
        return driver
    }

    @Throws(WebDriverException::class)
    private fun newDriverUnmanaged(url: String): PlaywrightDriver {
        val uniqueID = UUID.randomUUID().toString()
        val page = browserContext.newPage()

        if (url.isNotBlank()) {
            page.navigate(url)
        }

        val driver = PlaywrightDriver(uniqueID, this, page)

        return driver
    }
}
