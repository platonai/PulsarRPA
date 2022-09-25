package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import com.github.kklisura.cdt.protocol.ChromeDevTools
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class ChromeDevtoolsBrowser(
    id: BrowserId,
    val chrome: RemoteChrome,
    private val launcher: ChromeLauncher,
): AbstractBrowser(id, launcher.options.browserSettings) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowser::class.java)

    private val toolsConfig = DevToolsConfig()
    private val chromeTabs: List<ChromeTab>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.chromeTab }
    private val devtools: List<ChromeDevTools>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.devTools }

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(): ChromeDevtoolsDriver {
        try {
            // In chrome every tab is a separate process
            val chromeTab = createTab()
            return newDriver(chromeTab)
        } catch (e: ChromeDriverException) {
            throw WebDriverException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            throw WebDriverException("[Unexpected] Failed to create chrome devtools driver", e)
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun createTab(): ChromeTab {
        lastActiveTime = Instant.now()
        return kotlin.runCatching { chrome.createTab(ChromeImpl.ABOUT_BLANK_PAGE) }
            .getOrElse { throw WebDriverException("createTab", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun closeTab(tab: ChromeTab) {
        logger.debug("Closing tab | {}", tab.url)
        return chrome.runCatching { closeTab(tab) }.getOrElse { throw WebDriverException("closeTab", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun listTabs(): Array<ChromeTab> {
        return chrome.runCatching { listTabs() }.getOrElse { throw WebDriverException("listTabs", it) }
    }

    override fun maintain() {
        listTabs().filter { it.id !in drivers.keys }.map { newDriver(it) }

//        println("\n\n\n")
//        chromeTabs.forEach {
//            println(it.id + "\t" + it.parentId + "\t|\t" + it.url)
//        }

        closeUnmanagedIdleDrivers()
    }

    private fun newDriver(chromeTab: ChromeTab): ChromeDevtoolsDriver {
        val devTools = createDevTools(chromeTab, toolsConfig)
        return _drivers.computeIfAbsent(chromeTab.id) {
            ChromeDevtoolsDriver(chromeTab, devTools, browserSettings, this)
        } as ChromeDevtoolsDriver
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { logger.warn("Failed to close browser", it) }
            super.close()
        }
    }

    private fun closeUnmanagedIdleDrivers() {
        val chromeDrivers = drivers.values.filterIsInstance<ChromeDevtoolsDriver>()

        val unmanagedTabTimeout = Duration.ofSeconds(30)
        val unmanagedDrivers = chromeDrivers.filter { !it.isManaged }
            .filter { Duration.between(lastActiveTime, Instant.now()) > unmanagedTabTimeout }
        if (unmanagedDrivers.isNotEmpty()) {
            logger.debug("Closing {} unmanaged drivers", unmanagedDrivers.size)
            require(unmanagedDrivers.all { it.navigateHistory.isEmpty() }) {
                "Unmanaged driver should has no history"
            }
            unmanagedDrivers.forEach { it.close() }
        }
    }

    private fun doClose() {
        closeDrivers()

        chrome.close()
        launcher.close()

        logger.info("Browser is closed | #{}", id.display)
    }

    private fun closeDrivers() {
        if (drivers.isEmpty()) {
            return
        }

        logger.info("Closing browser with {} devtools ... | #{}", drivers.size, id)

        val nonSynchronized = drivers.toList().also { _drivers.clear() }
        nonSynchronized.parallelStream().forEach {
            it.runCatching { close() }.onFailure { logger.warn("Failed to close the devtool", it) }
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    private fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        return kotlin.runCatching { chrome.createDevTools(tab, config) }
            .getOrElse { throw WebDriverException("createDevTools", it) }
    }
}
