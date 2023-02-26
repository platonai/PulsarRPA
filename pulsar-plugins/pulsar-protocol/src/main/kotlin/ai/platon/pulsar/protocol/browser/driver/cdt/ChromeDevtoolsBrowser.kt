package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.AppSystemInfo
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_REUSE_RECOVERED_DRIVERS
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import com.github.kklisura.cdt.protocol.ChromeDevTools
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class ChromeDevtoolsBrowser(
    id: BrowserId,
    val chrome: RemoteChrome,
    private val launcher: ChromeLauncher
): AbstractBrowser(id, launcher.options.browserSettings) {

    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowser::class.java)

    private val toolsConfig = DevToolsConfig()

    private val conf get() = browserSettings.conf

    private val reuseRecoveredDriver get() = conf.getBoolean(BROWSER_REUSE_RECOVERED_DRIVERS, false)

    private val chromeTabs: List<ChromeTab>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.chromeTab }

    private val devtools: List<ChromeDevTools>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.devTools }

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
        return runCatching { chrome.closeTab(tab) }.getOrElse { throw WebDriverException("closeTab", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    fun listTabs(): Array<ChromeTab> {
        return runCatching { chrome.listTabs() }.getOrElse { throw WebDriverException("listTabs", it) }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(): ChromeDevtoolsDriver {
        try {
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
    fun findDriver(url: String): ChromeDevtoolsDriver? {
        recoverUnmanagedPages()
        return drivers.values.filterIsInstance<ChromeDevtoolsDriver>().firstOrNull { currentUrl(it) == url }
    }

    override fun destroyDriver(driver: WebDriver) {
        if (driver is ChromeDevtoolsDriver) {
            val chromTab = driver.chromeTab
            val chromTabId = chromTab.id

            mutableRecoveredDrivers.remove(chromTabId)
            mutableReusedDrivers.remove(chromTabId)
            mutableDrivers.remove(chromTabId)

            kotlin.runCatching { closeTab(driver.chromeTab) }.onFailure { logger.warn(it.brief()) }
            kotlin.runCatching { driver.close() }.onFailure { logger.warn(it.brief()) }
        }
    }

    override fun maintain() {
        recoverUnmanagedPages()
        closeRecoveredIdleDrivers()
    }

    /**
     * Closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     *
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { logger.warn(it.brief("Failed to close browser\n")) }
            super.close()
        }
    }

    private fun currentUrl(driver: WebDriver) = runBlocking { driver.currentUrl() }

    /**
     * Create a new driver.
     * */
    private fun newDriver(chromeTab: ChromeTab, recovered: Boolean): ChromeDevtoolsDriver {
        if (!recovered && reuseRecoveredDriver) {
            val driver = mutableRecoveredDrivers.values
                .filterIsInstance<ChromeDevtoolsDriver>()
                .firstOrNull { !it.isReused }
            if (driver != null) {
                driver.isReused = true
                mutableReusedDrivers[driver.chromeTab.id] = driver

                val currentUrl = runBlocking { driver.currentUrl() }
                logger.debug("Reuse recovered driver | {}", currentUrl)
                return driver
            }
        }

        val devTools = createDevTools(chromeTab, toolsConfig)
        val driver = ChromeDevtoolsDriver(chromeTab, devTools, browserSettings, this)
        mutableDrivers[chromeTab.id] = driver

        if (recovered) {
            driver.isRecovered = true
            mutableRecoveredDrivers[chromeTab.id] = driver
        }

        return driver
    }

    /**
     * Pages can be open in the browser, for example, by a click. We should recover the page
     * and create a web driver to manage it.
     * */
    private fun recoverUnmanagedPages() {
        listTabs().asSequence()
            .filter { it.id !in drivers.keys } // unmanaged
            .filter { it.isPageType() } // handler HTML document only
            .filter { UrlUtils.isStandard(it.url) } // make sure the url is correct
            .map {
                logger.info("Recover tab {} | {}", it.id, it.url)
                // create a new driver and associate it with the tab
                newDriver(it, true)
            }
    }

    private fun closeRecoveredIdleDrivers() {
        val chromeDrivers = drivers.values.filterIsInstance<ChromeDevtoolsDriver>()

        val pageLoadTimeout = browserSettings.interactSettings.pageLoadTimeout
        val seconds = if (AppSystemInfo.isCriticalResources) 15L else pageLoadTimeout.seconds
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
            unmanagedTimeoutDrivers.forEach { destroyDriver(it) }
        }
    }

    private fun doClose() {
        closeDrivers()

        chrome.close()
        launcher.close()

        logger.info("Browser is closed | #{}", id.display)
    }

    private fun closeDrivers() {
        val nonSynchronized = drivers.toList()

        mutableRecoveredDrivers.clear()
        mutableReusedDrivers.clear()
        mutableDrivers.clear()

        if (drivers.isEmpty()) {
            return
        }

        logger.info("Closing browser with {} drivers/devtools ... | #{}", drivers.size, id)

        nonSynchronized.parallelStream().forEach { (id, driver) ->
            kotlin.runCatching { driver.close() }.onFailure { logger.warn(it.brief("Failed to close the devtool")) }
        }
    }

    @Synchronized
    @Throws(WebDriverException::class)
    private fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        return kotlin.runCatching { chrome.createDevTools(tab, config) }
            .getOrElse { throw WebDriverException("createDevTools", it) }
    }
}
