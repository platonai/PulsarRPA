package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl.Companion.ABOUT_BLANK_PAGE
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_REUSE_RECOVERED_DRIVERS
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class ChromeDevtoolsBrowser(
    id: BrowserId, val chrome: RemoteChrome, private val launcher: ChromeLauncher
) : AbstractBrowser(id, launcher.options.browserSettings) {
    
    private val logger = LoggerFactory.getLogger(ChromeDevtoolsBrowser::class.java)
    
    private val toolsConfig = DevToolsConfig()
    
    private val conf get() = browserSettings.conf
    
    private val reuseRecoveredDriver get() = conf.getBoolean(BROWSER_REUSE_RECOVERED_DRIVERS, false)
    
    private val chromeTabs: List<ChromeTab>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.chromeTab }
    
    private val devtools: List<ChromeDevTools>
        get() = drivers.values.filterIsInstance<ChromeDevtoolsDriver>().map { it.devTools }
    
    override val isActive get() = super.isActive && chrome.isActive
    
    override val userAgent get() = chrome.version.userAgent ?: DEFAULT_USER_AGENT
    
    @Synchronized
    @Throws(WebDriverException::class)
    fun createTab() = createTab(ABOUT_BLANK_PAGE)
    
    @Synchronized
    @Throws(WebDriverException::class)
    fun createTab(url: String): ChromeTab {
        lastActiveTime = Instant.now()
        return chrome.runCatching { createTab(url) }.getOrElse { throw WebDriverException("createTab", it) }
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
        return chrome.runCatching { listTabs() }.getOrElse { throw WebDriverException("listTabs", it) }
    }
    
    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver() = newDriver(ABOUT_BLANK_PAGE)
    
    @Synchronized
    @Throws(WebDriverException::class)
    override fun newDriver(url: String): ChromeDevtoolsDriver {
        try {
            // In chrome every tab is a separate process
            val chromeTab = createTab(url)
            return newDriverIfAbsent(chromeTab, false)
        } catch (e: ChromeDriverException) {
            logger.warn(e.stringify())
            throw WebDriverException("Failed to create chrome devtools driver | " + e.message)
        } catch (e: Exception) {
            logger.warn(e.stringify())
            throw WebDriverException("[Unexpected] Failed to create chrome devtools driver", e)
        }
    }
    
    //    @Synchronized
    @Throws(WebDriverException::class)
    override suspend fun listDrivers(): List<WebDriver> {
        recoverUnmanagedPages()
        return drivers.values.toList()
    }
    
    //    @Synchronized
    @Throws(WebDriverException::class)
    override suspend fun findDriver(url: String): ChromeDevtoolsDriver? {
        recoverUnmanagedPages()
        return drivers.values.filterIsInstance<ChromeDevtoolsDriver>().firstOrNull { currentUrl(it) == url }
    }
    
    override suspend fun findDrivers(urlRegex: Regex): WebDriver? {
        recoverUnmanagedPages()
        return drivers.values.filterIsInstance<ChromeDevtoolsDriver>().firstOrNull { currentUrl(it).matches(urlRegex) }
    }
    
    override fun destroyDriver(driver: WebDriver) {
        if (driver is ChromeDevtoolsDriver) {
            val chromeTab = driver.chromeTab
            val chromeTabId = chromeTab.id
            
            _recoveredDrivers.remove(chromeTabId)
            _reusedDrivers.remove(chromeTabId)
            _drivers.remove(chromeTabId)
            
            runCatching { driver.doClose() }.onFailure { warnForClose(this, it) }
            runCatching { closeTab(driver.chromeTab) }.onFailure { warnForClose(this, it) }
        }
    }
    
    override fun maintain() {
        recoverUnmanagedPages()
        closeRecoveredIdleDrivers()
    }
    
    @Synchronized
    override fun destroyForcibly() {
        runCatching {
            close()
            launcher.destroyForcibly()
        }.onFailure { warnForClose(this, it) }
    }
    
    /**
     * Closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close()
     *
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { warnForClose(this, it) }
            super.close()
        }
    }
    
    private suspend fun currentUrl(driver: WebDriver) = driver.currentUrl()
    
    /**
     * Create a new driver and add it to the driver tree.
     * */
    private fun newDriverIfAbsent(chromeTab: ChromeTab, recovered: Boolean): ChromeDevtoolsDriver {
        // a Chrome tab id is like 'AE740895CB3F63220C3A3C751EF1F6E4'
        var driver = _drivers[chromeTab.id]
        if (driver != null) {
            return driver as ChromeDevtoolsDriver
        }

        driver = doNewDriver(chromeTab, recovered)
        
        addToDriverTree(driver)
        
        return driver
    }
    
    private fun doNewDriver(chromeTab: ChromeTab, recovered: Boolean): ChromeDevtoolsDriver {
        if (!recovered && reuseRecoveredDriver) {
            val driver = _recoveredDrivers.values.firstOrNull { it is ChromeDevtoolsDriver && !it.isReused }
            if (driver is ChromeDevtoolsDriver) {
                driver.isReused = true
                _reusedDrivers[driver.chromeTab.id] = driver
                logger.info("Reuse recovered driver | {}", chromeTab.url)
                return driver
            }
        }

        val devTools = createDevTools(chromeTab, toolsConfig)
        val driver = ChromeDevtoolsDriver(chromeTab, devTools, browserSettings, this)
        _drivers[chromeTab.id] = driver
        
        if (recovered) {
            driver.isRecovered = true
            _recoveredDrivers[chromeTab.id] = driver
        }

        return driver
    }

    private fun buildDriverTree() {
        drivers.values.forEach { addToDriverTree(it) }
    }
    
    private fun addToDriverTree(driver: WebDriver) {
        if (driver is ChromeDevtoolsDriver) {
            val parentId = driver.chromeTab.parentId
            if (parentId != null) {
                val parent = drivers[parentId]
                if (parent is ChromeDevtoolsDriver) {
                    driver.opener = parent
                    parent.outgoingPages.add(driver)
                    
                    logger.info("Add driver to tree | parent: {}, child: {} | {}", parent.chromeTab.url, driver.chromeTab.url, driver.chromeTab.id)
                }
            }
        }
    }
    
    /**
     * Pages can be open in the browser, for example, by a click. We should recover the page
     * and create a web driver to manage it.
     *
     * TODO: capture events that open new pages
     * */
    private fun recoverUnmanagedPages() {
        try {
            recoverUnmanagedPages0()
        } catch (e: WebDriverException) {
            logger.warn("Failed to recover unmanaged pages | {}", e.message)
        }
    }
    
    @Throws(WebDriverException::class)
    private fun recoverUnmanagedPages0() {
        val tabs = listTabs()
        // the tab id is the key of the driver in drivers
        tabs.filter { it.id !in drivers.keys } // it is not created yet
            .filter { it.isPageType() } // handler HTML document only
            .filter { UrlUtils.isStandard(it.url) } // make sure the url is correct
            .forEach { tab ->
                // create a new driver and associate it with the tab
                val driver = newDriverIfAbsent(tab, true)
                reportNewDriver(tab, driver)
            }
    }

    private fun reportNewDriver(tab: ChromeTab, driver: WebDriver) {
        val parentId = tab.parentId
        if (parentId != null) {
            logger.info("Recover tab {} with parent: {} | driver: {}, opener: {}, siblings: {} | {}",
                tab.id, tab.parentId,
                driver.id, driver.opener?.id, driver.opener?.outgoingPages?.size ?: 0,
                tab.url
            )
        } else {
            logger.info("Recover tab {} with no parent | driver: {} | {}", tab.id, driver.id, tab.url)
        }
    }

    private fun closeRecoveredIdleDrivers() {
        val chromeDrivers = drivers.values.filterIsInstance<ChromeDevtoolsDriver>()
        
        val pageLoadTimeout = browserSettings.interactSettings.pageLoadTimeout
        val seconds = if (AppSystemInfo.isCriticalResources) 15L else pageLoadTimeout.seconds
        val unmanagedTabTimeout = Duration.ofSeconds(seconds)
        val isIdle =
            { driver: AbstractWebDriver -> Duration.between(driver.lastActiveTime, Instant.now()) > unmanagedTabTimeout }
        val unmanagedTimeoutDrivers = chromeDrivers.filter { it.isRecovered && !it.isReused && isIdle(it) }
        if (unmanagedTimeoutDrivers.isNotEmpty()) {
            logger.debug("Closing {} unmanaged drivers", unmanagedTimeoutDrivers.size)
            val hasHistory = unmanagedTimeoutDrivers.any { it.navigateHistory.isEmpty() }
            if (hasHistory) {
                logger.warn("Unmanaged driver should has no history, this indicates a bug")
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
        
        _recoveredDrivers.clear()
        _reusedDrivers.clear()
        _drivers.clear()
        
        if (drivers.isEmpty()) {
            return
        }
        
        logger.info("Closing browser with {} drivers/devtools ... | #{}", drivers.size, id)
        
        nonSynchronized.forEach { (id, driver) ->
            kotlin.runCatching { driver.close() }.onFailure { warnForClose(this, it) }
        }
    }
    
    @Synchronized
    @Throws(WebDriverException::class)
    private fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        return kotlin.runCatching { chrome.createDevTools(tab, config) }
            .getOrElse { throw WebDriverException("createDevTools", it) }
    }
}
