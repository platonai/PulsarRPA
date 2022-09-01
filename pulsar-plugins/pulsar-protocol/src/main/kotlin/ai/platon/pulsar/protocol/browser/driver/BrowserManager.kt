package ai.platon.pulsar.protocol.browser.driver

//import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserManager(
    val conf: ImmutableConfig
): AutoCloseable {
    private val logger = getLogger(this)
    private val closed = AtomicBoolean()
    private val browserFactory = BrowserFactory()
    private val _browsers = ConcurrentHashMap<String, Browser>()

    val browsers: Map<String, Browser> = _browsers

    @Throws(DriverLaunchException::class)
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
    fun close(browserId: BrowserId) {
        _browsers.remove(browserId.userDataDir.toString())?.close()
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _browsers.values.forEach {
                it.runCatching { close() }.onFailure { logger.warn("Failed to close", it) }
            }
        }
    }

    @Throws(DriverLaunchException::class)
    @Synchronized
    private fun launchIfAbsent(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val userDataDir = browserId.userDataDir
        return _browsers.computeIfAbsent(userDataDir.toString()) {
            browserFactory.launch(browserId, launcherOptions, launchOptions)
        }
    }
}
