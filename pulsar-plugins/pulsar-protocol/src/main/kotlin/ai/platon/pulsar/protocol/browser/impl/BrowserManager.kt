package ai.platon.pulsar.protocol.browser.impl

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.ChromiumFactory
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.fetch.driver.*
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserManager(
    val conf: ImmutableConfig
): AutoCloseable {
    private val logger = getLogger(this)
    private var registered = AtomicBoolean()
    private val closed = AtomicBoolean()
    private val browserFactory = ChromiumFactory()
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
    fun launch(browserId: BrowserId, browserSettings: BrowserSettings, capabilities: Map<String, Any>): Browser {
        registerAsClosableIfNecessary()

        val launcherOptions = LauncherOptions(browserSettings)
        if (browserSettings.isSupervised) {
            launcherOptions.supervisorProcess = browserSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(browserSettings.supervisorProcessArgs)
        }

        val launchOptions = browserSettings.createChromeOptions(capabilities)
        return launchIfAbsent(browserId, launcherOptions, launchOptions)
    }

    @Deprecated("Use findBrowserOrNull instead", ReplaceWith("findBrowserOrNull(browserId)"))
    @Synchronized
    fun findBrowser(browserId: BrowserId): Browser? = browsers[browserId]

    /**
     * Find an existing browser by id.
     * If the browser is not found, return null.
     *
     * @param browserId The browser id
     * @return The browser or null if not found
     * */
    @Synchronized
    fun findBrowserOrNull(browserId: BrowserId): Browser? = browsers[browserId]

    /**
     * Check if the browser is active.
     * */
    fun isActive(browserId: BrowserId): Boolean {
        val browser = findBrowserOrNull(browserId) as? AbstractBrowser
        return browser != null && browser.isActive
    }

    /**
     * Close a browser.
     * */
    @Synchronized
    fun closeBrowser(browserId: BrowserId) {
        val browser = _browsers.remove(browserId)
        if (browser is AbstractBrowser) {
            kotlin.runCatching { browser.close() }.onFailure { warnForClose(this, it) }
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
        kotlin.runCatching { driver.close() }.onFailure { warnForClose(this, it) }
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
                logger.info("Closing zombie browser | {}", browser.id.contextDir)
                kotlin.runCatching { browser.destroyForcibly() }.onFailure { warnInterruptible(this, it) }
            }
        }
    }

    private fun findLeastValuableDriver(drivers: Iterable<WebDriver>): WebDriver? {
        return drivers.filterIsInstance<AbstractWebDriver>()
            .filter { !it.isReady && !it.isWorking }
            .minByOrNull { it.lastActiveTime }
    }

    fun maintain() {
        browsers.values.forEach {
            require(it is AbstractBrowser)
            it.emit(BrowserEvents.willMaintain)
            it.emit(BrowserEvents.maintain)
            it.emit(BrowserEvents.didMaintain)
        }
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _browsers.values.forEach { browser ->
                require(browser is AbstractBrowser)
                kotlin.runCatching { browser.close() }.onFailure { warnForClose(this, it) }
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
            val browser1 = browserFactory.launch(browserId, launcherOptions, launchOptions)
            _browsers[browserId] = browser1
            historicalBrowsers.add(browser1)

            return browser1
        }
    }

    private fun registerAsClosableIfNecessary() {
        if (registered.compareAndSet(false, true)) {
            // Actually, it's safe to register multiple times, the manager will be closed only once, and the browsers
            // will be closed in the manager's close function.
            PulsarContexts.registerClosable(this, -100)
        }
    }
}
