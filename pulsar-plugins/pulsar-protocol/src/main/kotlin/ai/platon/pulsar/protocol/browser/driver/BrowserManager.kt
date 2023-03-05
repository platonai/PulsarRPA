package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.BrowserEvents
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserManager(
    val conf: ImmutableConfig
): AutoCloseable {
    private val logger = getLogger(this)
    private val closed = AtomicBoolean()
    private val browserFactory = BrowserFactory()
    // TODO: use browser id as the key directly
    private val _browsers = ConcurrentHashMap<BrowserId, Browser>()
    private val closedBrowserIds = mutableListOf<BrowserId>()

    val browsers: Map<BrowserId, Browser> = _browsers

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

    @Synchronized
    fun closeBrowser(browserId: BrowserId) {
        val browser = _browsers.remove(browserId)
        if (browser != null) {
            runCatching { browser.close() }.onFailure { logger.warn(it.brief("Failed to close browser\n")) }
            closedBrowserIds.add(browserId)
        }
    }

    @Synchronized
    fun findBrowser(browserId: BrowserId) = browsers[browserId]

    @Synchronized
    fun closeBrowser(browser: Browser) {
        closeBrowser(browser.id)
    }

    @Synchronized
    fun closeDriver(driver: WebDriver) {
        kotlin.runCatching { driver.close() }.onFailure { logger.warn(it.brief("Failed to close driver\n")) }
    }

    @Synchronized
    fun findLeastValuableDriver(): WebDriver? {
        return browsers.values
            .mapNotNull { findLeastValuableDriver(it.drivers.values) }
            .minByOrNull { it.lastActiveTime }
    }

    @Synchronized
    fun closeLeastValuableDriver() {
        val driver = findLeastValuableDriver()
        if (driver != null) {
            closeDriver(driver)
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
                kotlin.runCatching { browser.close() }.onFailure { logger.warn(it.stringify()) }
            }
            _browsers.clear()
        }
    }

    @Throws(BrowserLaunchException::class)
    @Synchronized
    private fun launchIfAbsent(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        return _browsers.computeIfAbsent(browserId) {
            browserFactory.launch(browserId, launcherOptions, launchOptions)
        }
    }
}
