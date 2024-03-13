package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.warnInterruptible
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.BrowserEvents
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserManager(
    val conf: ImmutableConfig
): AutoCloseable {
    private val logger = getLogger(this)
    private val closed = AtomicBoolean()
    private val browserFactory = BrowserFactory()
    private val _browsers = ConcurrentHashMap<BrowserId, Browser>()
    private val historicalBrowsers = ConcurrentLinkedDeque<Browser>()
    private val closedBrowsers = ConcurrentLinkedDeque<Browser>()
    /**
     * The active browsers
     * */
    val browsers: Map<BrowserId, Browser> = _browsers
    /**
     * Launch a browser. If the browser with the id is already launched, return the existing one.
     * */
    @Throws(BrowserLaunchException::class)
    fun launch(browserId: BrowserId, driverSettings: WebDriverSettings, capabilities: Map<String, Any>): Browser {
        val launcherOptions = LauncherOptions(driverSettings)
        if (driverSettings.isSupervised) {
            launcherOptions.supervisorProcess = driverSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)
        return launchIfAbsent(browserId, launcherOptions, launchOptions)
    }
    /**
     * Find an existing browser.
     * */
    @Synchronized
    fun findBrowser(browserId: BrowserId) = browsers[browserId]

    /**
     * Close a browser.
     * */
    @Synchronized
    fun closeBrowser(browserId: BrowserId) {
        val browser = _browsers.remove(browserId)
        if (browser != null) {
            kotlin.runCatching { browser.close() }.onFailure { warnInterruptible(this, it) }
            closedBrowsers.add(browser)
        }
    }

    @Synchronized
    fun destroyBrowserForcibly(browserId: BrowserId) {
        historicalBrowsers.filter { browserId == it.id }.forEach { browser ->
            kotlin.runCatching { browser.destroyForcibly() }.onFailure { warnInterruptible(this, it) }
            closedBrowsers.add(browser)
        }
    }

    @Synchronized
    fun closeBrowser(browser: Browser) {
        closeBrowser(browser.id)
    }

    @Synchronized
    fun closeDriver(driver: WebDriver) {
        kotlin.runCatching { driver.close() }.onFailure { warnInterruptible(this, it) }
    }

    @Synchronized
    fun findLeastValuableDriver(): WebDriver? {
        val drivers = browsers.values.flatMap { it.drivers.values }
        return findLeastValuableDriver(drivers)
    }

    @Synchronized
    fun closeLeastValuableDriver() {
        val driver = findLeastValuableDriver()
        if (driver != null) {
            closeDriver(driver)
        }
    }

    /**
     * Destroy the zombie browsers forcibly, kill the associated browser processes,
     * release all allocated resources, regardless of whether the browser is closed or not.
     * */
    @Synchronized
    fun destroyZombieBrowsersForcibly() {
        val zombieBrowsers = historicalBrowsers - browsers.values.toSet() - closedBrowsers
        if (zombieBrowsers.isNotEmpty()) {
            logger.warn("There are {} zombie browsers, cleaning them ...", zombieBrowsers.size)
            zombieBrowsers.forEach { browser ->
                logger.info("Closing zombie browser | {}", browser.id)
                kotlin.runCatching { browser.destroyForcibly() }.onFailure { warnInterruptible(this, it) }
            }
        }
    }

    private fun findLeastValuableDriver(drivers: Iterable<WebDriver>): WebDriver? {
        return drivers.filter { !it.isReady && !it.isWorking }.minByOrNull { it.lastActiveTime }
    }

    fun maintain() {
        browsers.values.forEach {
            it.emit(BrowserEvents.willMaintain)
            it.emit(BrowserEvents.maintain)
            it.emit(BrowserEvents.didMaintain)
        }
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _browsers.values.forEach { browser ->
                kotlin.runCatching { browser.close() }.onFailure { warnInterruptible(this, it) }
            }
            _browsers.clear()
        }
    }

    @Throws(BrowserLaunchException::class)
    private fun launchIfAbsent(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val browser = _browsers[browserId]
        if (browser != null) {
            return browser
        }

        synchronized(browserFactory) {
            return _browsers.computeIfAbsent(browserId) {
                browserFactory.launch(browserId, launcherOptions, launchOptions)
            }.also { historicalBrowsers.add(it) }
        }
    }
}
