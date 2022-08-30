package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.persist.jackson.prettyPulsarObjectMapper
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class ChromeDevtoolsBrowser(
    id: BrowserId,
    val chrome: RemoteChrome,
    private val launcher: ChromeLauncher,
): AbstractBrowser(id, launcher.options.browserSettings) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowser::class.java)

    val drivers = ConcurrentLinkedQueue<ChromeDevtoolsDriver>()
    val driverCount get() = drivers.size
    private val toolsConfig = DevToolsConfig()

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(): ChromeDevtoolsDriver {
        try {
            // In chrome every tab is a separate process
            val chromeTab = createTab()
            val devTools = createDevTools(chromeTab, toolsConfig)

            return ChromeDevtoolsDriver(chromeTab, devTools, browserSettings, this)
        } catch (e: ChromeDriverException) {
            throw WebDriverException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            throw WebDriverException("[Unexpected] Failed to create chrome devtools driver", e)
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun createTab(): ChromeTab {
        activeTime = Instant.now()
        tabCount.incrementAndGet()

        return kotlin.runCatching { chrome.createTab(Chrome.ABOUT_BLANK_PAGE) }
            .getOrElse { throw WebDriverException("createTab", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun closeTab(tab: ChromeTab) {
        tabCount.decrementAndGet()
        return chrome.runCatching { closeTab(tab) }.getOrElse { throw WebDriverException("closeTab", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun listTabs(): Array<ChromeTab> {
        return chrome.runCatching { listTabs() }.getOrElse { throw WebDriverException("listTabs", it) }
    }

    fun getBrowserContexts() {

    }

    @Throws(WebDriverException::class)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            logger.info("Closing browser with {} devtools ... | {}", drivers.size, id)

            val nonSynchronized = drivers.toList().also { drivers.clear() }
            nonSynchronized.parallelStream().forEach {
                try {
                    it.close()
                    // should we?
                    it.awaitTermination()
                } catch (e: Exception) {
                    logger.warn("Failed to close the devtool", e)
                }
            }

            try {
                chrome.close()
                launcher.close()
            } catch (e: Exception) {
                logger.warn("Failed to close the browser", e)
            }

            logger.info("Browser is closed | {}", id.display)
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    private fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        return kotlin.runCatching { chrome.createDevTools(tab, config) }
            .getOrElse { throw WebDriverException("createDevTools", it) }
    }

    class BrowserShutdownHookRegistry(private val browser: Browser): ChromeLauncher.ShutdownHookRegistry {
        override fun register(thread: Thread) {
            Runtime.getRuntime().addShutdownHook(browser.shutdownHookThread)
            // Runtime.getRuntime().addShutdownHook(thread)
        }

        override fun remove(thread: Thread) {
            // TODO: java.lang.IllegalStateException: Shutdown in progress
            Runtime.getRuntime().removeShutdownHook(browser.shutdownHookThread)
            // Runtime.getRuntime().removeShutdownHook(thread)
        }
    }
}
