package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowserInstance
import ai.platon.pulsar.crawl.fetch.driver.BrowserInstance
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class ChromeDevtoolsBrowserInstance(
    id: BrowserInstanceId,
    launcherOptions: LauncherOptions,
    launchOptions: ChromeOptions
): AbstractBrowserInstance(id, launcherOptions, launchOptions) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowserInstance::class.java)

    lateinit var launcher: ChromeLauncher
    lateinit var chrome: RemoteChrome
    val devToolsList = ConcurrentLinkedQueue<RemoteDevTools>()
    val devToolsCount get() = devToolsList.size

    @Synchronized
    @Throws(WebDriverException::class)
    override fun launch() {
        if (launched.compareAndSet(false, true)) {
            val shutdownHookRegistry = BrowserShutdownHookRegistry(this)
            launcher = ChromeLauncher(
                options = launcherOptions,
                shutdownHookRegistry = shutdownHookRegistry
            )
            chrome = launcher.launch(launchOptions)
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun createTab(): ChromeTab {
        activeTime = Instant.now()
        tabCount.incrementAndGet()
        return chrome.createTab(Chrome.ABOUT_BLANK_PAGE)
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun closeTab(tab: ChromeTab) {
        tabCount.decrementAndGet()
        chrome.closeTab(tab)
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun listTab(): Array<ChromeTab> {
        return chrome.getTabs()
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        val devTools= chrome.createDevTools(tab, config)
        devToolsList.add(devTools)
        return devTools
    }

    @Throws(WebDriverException::class)
    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            logger.info("Closing browser with {} devtools ... | {}", devToolsList.size, id)

            val nonSynchronized = devToolsList.toList().also { devToolsList.clear() }
            nonSynchronized.parallelStream().forEach {
                try {
                    it.close()
                    // should we?
                    it.waitUntilClosed()
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

    class BrowserShutdownHookRegistry(private val browserInstance: BrowserInstance): ChromeLauncher.ShutdownHookRegistry {
        override fun register(thread: Thread) {
            Runtime.getRuntime().addShutdownHook(browserInstance.shutdownHookThread)
            // Runtime.getRuntime().addShutdownHook(thread)
        }

        override fun remove(thread: Thread) {
            // TODO: java.lang.IllegalStateException: Shutdown in progress
            Runtime.getRuntime().removeShutdownHook(browserInstance.shutdownHookThread)
            // Runtime.getRuntime().removeShutdownHook(thread)
        }
    }
}
