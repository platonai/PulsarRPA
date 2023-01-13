package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.AppRuntime
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_REUSE_RECOVERED_DRIVERS
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import com.github.kklisura.cdt.protocol.ChromeDevTools
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ChromeDevtoolsBrowser(
    id: BrowserId,
    val chrome: RemoteChrome,
    private val launcher: ChromeLauncher
): AbstractBrowser(id, launcher.options.browserSettings) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowser::class.java)

    private val toolsConfig = DevToolsConfig()

    private val conf get() = browserSettings.conf

    private val reuseRecovered get() = conf.getBoolean(BROWSER_REUSE_RECOVERED_DRIVERS, false)

    private val chromeTabs: List<ChromeTab>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.chromeTab }

    private val devtools: List<ChromeDevTools>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.devTools }

    /**
     * is it better to use a global scheduled executor service?
     * */
    private val maintainExecutor = AtomicReference<ScheduledExecutorService>()

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(): ChromeDevtoolsDriver {
        try {
            startMaintainTimerIfNecessary()
            // In chrome every tab is a separate process
            val chromeTab = createTab()
            return newDriver(chromeTab, false)
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
        recoverUnmanagedPages()
        closeRecoveredIdleDrivers()
    }

    private fun startMaintainTimerIfNecessary() {
        if (maintainExecutor.compareAndSet(null, Executors.newSingleThreadScheduledExecutor())) {
            maintainExecutor.get()?.scheduleAtFixedRate(this::maintain, 60 * 2, 10, TimeUnit.SECONDS)
        }
    }

    /**
     * Create a new driver.
     * */
    private fun newDriver(chromeTab: ChromeTab, recovered: Boolean): ChromeDevtoolsDriver {
        if (!recovered && reuseRecovered) {
            val driver = drivers.values
                .filterIsInstance<ChromeDevtoolsDriver>()
                .firstOrNull { it.isRecovered && !it.isReused }
            if (driver != null) {
                driver.isReused = true
                val currentUrl = runBlocking { driver.currentUrl() }
                logger.debug("Reuse recovered driver | {}", currentUrl)
                return driver
            }
        }

        val devTools = createDevTools(chromeTab, toolsConfig)
        val driver = ChromeDevtoolsDriver(chromeTab, devTools, browserSettings, this)
        driver.isRecovered = recovered

        mutableDrivers[chromeTab.id] = driver

        return driver
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { logger.warn("Failed to close browser", it) }
            super.close()
        }
    }

    /**
     * Pages can be open in the browser, for example, by a click. We should recover the page
     * and create a web driver to manage it.
     * */
    private fun recoverUnmanagedPages() {
        listTabs().asSequence()
            .filter { it.id !in drivers.keys }
            .filter { it.type == ChromeTab.PAGE_TYPE }
            .filter { it.url?.startsWith("http") == true }
            .map {
                logger.info("Recover tab | {}", it.url)
                newDriver(it, true)
            }

        // TODO: debug active tabs
//        println("\n\n\n")
//        chromeTabs.forEach {
//            println(it.id + "\t" + it.parentId + "\t|\t" + it.url)
//        }
    }

    private fun closeRecoveredIdleDrivers() {
        val chromeDrivers = drivers.values.filterIsInstance<ChromeDevtoolsDriver>()

        val seconds = if (AppRuntime.isLowMemory) 15L else browserSettings.interactSettings.pageLoadTimeout.seconds
        val unmanagedTabTimeout = Duration.ofSeconds(seconds)
        val isIdle = { driver: WebDriver -> Duration.between(driver.lastActiveTime, Instant.now()) > unmanagedTabTimeout }
        val unmanagedTimeoutDrivers = chromeDrivers.filter { it.isRecovered && !it.isReused && isIdle(it) }
        if (unmanagedTimeoutDrivers.isNotEmpty()) {
            logger.debug("Closing {} unmanaged drivers", unmanagedTimeoutDrivers.size)
            val hasHistory = unmanagedTimeoutDrivers.any { it.navigateHistory.isEmpty() }
            if (hasHistory) {
                logger.warn("Unmanaged driver should has no history")
            }
//            require(unmanagedTimeoutDrivers.all { it.navigateHistory.isEmpty() }) {
//                "Unmanaged driver should have no history"
//            }
            unmanagedTimeoutDrivers.forEach { it.close() }
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

        val nonSynchronized = drivers.toList().also { mutableDrivers.clear() }
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
