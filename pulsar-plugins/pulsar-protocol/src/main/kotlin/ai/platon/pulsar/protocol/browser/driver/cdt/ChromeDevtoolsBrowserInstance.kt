package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowserInstance
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
    @Throws(Exception::class)
    override fun launch() {
        if (launched.compareAndSet(false, true)) {
            val shutdownHookRegistry = ChromeDevtoolsDriver.ShutdownHookRegistry()
            launcher = ChromeLauncher(
                options = launcherOptions,
                shutdownHookRegistry = shutdownHookRegistry
            )
            chrome = launcher.launch(launchOptions)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun createTab(): ChromeTab {
        activeTime = Instant.now()
        tabCount.incrementAndGet()
        return chrome.createTab(Chrome.ABOUT_BLANK_PAGE)
    }

    @Synchronized
    fun closeTab(tab: ChromeTab) {
        tabCount.decrementAndGet()
        chrome.closeTab(tab)
    }

    @Synchronized
    fun listTab(): Array<ChromeTab> {
        return chrome.getTabs()
    }

    @Synchronized
    @Throws(Exception::class)
    fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        val devTools= chrome.createDevTools(tab, config)
        devToolsList.add(devTools)
        return devTools
    }

    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            logger.info("Closing {} devtools ... | {}", devToolsList.size, id.display)

            val nonSynchronized = devToolsList.toList().also { devToolsList.clear() }
            nonSynchronized.parallelStream().forEach {
                try {
                    it.close()
                    // should we?
                    it.waitUntilClosed()
                } catch (e: Exception) {
                    logger.warn("Failed to close the dev tool", e)
                }
            }

            chrome.close()
            launcher.close()

            logger.info("Browser instance is closed | {}", id.display)
        }
    }
}
