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
import com.microsoft.playwright.impl.PageImpl
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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

    private val closed = AtomicBoolean(false)

    override val isConnected: Boolean get() = AppContext.isActive && !isClosed

    override val isClosed: Boolean get() = closed.get() || super.isClosed

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver() = newDriver("")

    @Synchronized
    @Throws(BrowserUnavailableException::class)
    override fun newDriver(url: String): PlaywrightDriver {
        try {
            val driver = newDriverManaged(url)
            return driver
        } catch (e: WebDriverException) {
            throw BrowserUnavailableException("newDriver", e)
        } catch (e: Exception) {
            logger.warn("Failed to create new driver, rethrow | {}", e.message)
            // e.printStackTrace()
            throw BrowserUnavailableException("Failed to create chrome devtools driver", e)
        }
    }

    /**
     * Pages can be open in the browser, for example, by a click. We should recover the page
     * and create a web driver to manage it.
     * */
    override fun recoverUnmanagedPages() {
        try {
            recoverUnmanagedPages0()
        } catch (e: WebDriverException) {
            if (isActive) {
                logger.warn("Failed to recover unmanaged pages | {}", e.message)
            } else {
                logger.info("No page recovering, browser is closed.")
            }
        }
    }

    @Synchronized
    override fun close() {
        super.close()

        if (closed.compareAndSet(false, true)) {
            try {
                browserContext.close()
            } catch (e: Exception) {
                warnForClose(this, e, "Failed to close browser context")
            }
        }
    }

    @Throws(WebDriverException::class)
    @Synchronized
    internal fun newDriverManaged(url: String): PlaywrightDriver {
        val driver = newDriverUnmanaged(url)

        if (_drivers.isEmpty()) {
            addScriptToEvaluateOnNewDocument()
        }

        _drivers[driver.guid] = driver
        return driver
    }

    @Throws(WebDriverException::class)
    @Synchronized
    internal fun newDriverUnmanaged(url: String): PlaywrightDriver {
        val page = try {
            invoke("newPage") {
                check(isConnected)
                browserContext.newPage()
            }
        } catch (e: WebDriverException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to create new page, rethrow Exception | url: >>>$url<<<")
            e.printStackTrace()
            throw WebDriverException("Failed to create new page | url: >>>$url<<<", e)
        }

        if (url.isNotBlank()) {
            page.navigate(url)
        } else {
            page.navigate("about:blank")
        }

        val guid = UUID.randomUUID().toString()
        val driver = PlaywrightDriver(guid, this, page)
        check(driver.isActive)
        // println("Driver status: " + driver.status + " | " + url)

        return driver
    }

    @Throws(WebDriverException::class)
    private fun recoverUnmanagedPages0() {
        val pages = browserContext.pages()
        // the tab id is the key of the driver in drivers
        pages.filterIsInstance<PageImpl>().forEach { page ->
            // page.url()
//            val guid = getPageGUID(page)
//            if (!_drivers.containsKey(guid)) {
//                val driver = newDriverUnmanaged(page.url())
//                _drivers[guid] = driver
//            }
        }
    }

    @Throws(WebDriverException::class)
    private fun addScriptToEvaluateOnNewDocument() {
        val js = settings.scriptLoader.getPreloadJs(false)
        val scripts = settings.confuser.confuse(js)
        browserContext.addInitScript(scripts)
    }

    @Throws(WebDriverException::class)
    private fun <T> invoke(action: String, maxRetry: Int = 2, block: () -> T): T {
        var i = maxRetry
        var result = kotlin.runCatching { invoke0(action, block) }
            .onFailure {
                // no handler here
            }
        while (result.isFailure && i-- > 0) {
            result = kotlin.runCatching { invoke0(action, block) }
                .onFailure {
                    // no handler here
                }
        }

        return result.getOrElse { throw it }
    }

    @Throws(WebDriverException::class)
    private fun <T> invoke0(action: String, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            throw WebDriverException(cause = e)
        }
    }
}
