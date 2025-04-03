package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractBrowser(
    override val id: BrowserId,
    override val settings: BrowserSettings
): Browser, AutoCloseable, AbstractEventEmitter<BrowserEvents>() {
    companion object {
        protected val SEQUENCER = AtomicInteger()
        val DEFAULT_USER_AGENT = "PulsarRPA Robot/1.0"
    }

    private val logger = getLogger(this)

    /**
     * All drivers, including the recovered drivers and the reused drivers.
     * */
    protected val _drivers = ConcurrentHashMap<String, WebDriver>()
    protected val _recoveredDrivers = ConcurrentHashMap<String, WebDriver>()
    protected val _reusedDrivers = ConcurrentHashMap<String, WebDriver>()

    protected val initialized = AtomicBoolean()
    private val closed = AtomicBoolean()
    protected var lastActiveTime = Instant.now()

    override val instanceId: Int = SEQUENCER.incrementAndGet()

    override val userAgent get() = DEFAULT_USER_AGENT

    var userAgentOverride = getRandomUserAgentOrNull()

    override val navigateHistory = NavigateHistory()
    override val drivers: Map<String, WebDriver> get() = _drivers

    /**
     * The associated data.
     * */
    override val data: MutableMap<String, Any?> = mutableMapOf()

    override val isConnected: Boolean get() = isActive

    override val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    
    override val isPermanent: Boolean get() = id.privacyAgent.isPermanent

    override val isActive get() = AppContext.isActive && !closed.get() && initialized.get()

    override val isClosed get() = closed.get()

    override val readableState: String get() = buildReadableState()

    val isGUI get() = settings.isGUI
    val idleTimeout = Duration.ofMinutes(10)

    init {
        attach()
    }

    override suspend fun listDrivers(): List<WebDriver> {
        return _drivers.values.toList()
    }

    override suspend fun findDriver(url: String): WebDriver? {
        return _drivers.values.firstOrNull { it.currentUrl() == url }
    }
    
    override suspend fun findDriver(urlRegex: Regex): WebDriver? {
        return _drivers.values.firstOrNull { urlRegex.matches(it.currentUrl()) }
    }

    override suspend fun findDrivers(urlRegex: Regex): List<WebDriver> {
        return _drivers.values.filter { urlRegex.matches(it.currentUrl()) }
    }

    override fun destroyDriver(driver: WebDriver) {
        // Nothing to do
    }

    override fun destroyForcibly() {

    }

    override suspend fun clearCookies() {
        val driver = drivers.values.firstOrNull() ?: newDriver()
        driver.clearBrowserCookies()
    }

    fun onInitialize() {
        initialized.set(true)
    }

    fun onWillNavigate(entry: NavigateEntry) {
        navigateHistory.add(entry)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            detach()
            _recoveredDrivers.clear()
            _drivers.values.filterIsInstance<AbstractWebDriver>().forEach {
                // tasks are return as soon as possible and should be cancelled
                it.cancel()
                // the driver should be retired
                it.retire()
            }
            _drivers.values.forEach { runCatching { it.close() }.onFailure { warnForClose(this, it) } }
            _drivers.clear()
        }
    }

    open fun maintain() {
        // Nothing to do
    }

    private fun getRandomUserAgentOrNull() = if (settings.isUserAgentOverridingEnabled) {
        settings.userAgent.getRandomUserAgent()
    } else null

    /**
     * Attach default event handlers
     * */
    protected fun attach() {
        on(BrowserEvents.initialize) { onInitialize() }
        on(BrowserEvents.willNavigate) { entry: NavigateEntry -> onWillNavigate(entry) }
        on(BrowserEvents.maintain) { maintain() }
    }

    /**
     * Detach default event handlers
     * */
    protected fun detach() {
        off(BrowserEvents.initialize)
        off(BrowserEvents.willNavigate)
        off(BrowserEvents.maintain)
    }

    private fun buildReadableState(): String {
        val sb = StringBuilder()
        if (isActive) {
            sb.append("Active")
        } else {
            sb.append("Inactive")
        }
        if (isClosed) {
            sb.append(",Closed")
        }
        if (isPermanent) {
            sb.append(",Permanent")
        }
        if (isIdle) {
            sb.append(",Idle")
        }
        if (isConnected) {
            sb.append(",Connected")
        } else {
            sb.append(",Disconnected")
        }
        return sb.toString()
    }

    abstract fun newDriverUnmanaged(url: String = ""): AbstractWebDriver

    /**
     * Create a new driver and add it to the driver tree.
     * */
    protected fun newDriverIfAbsent(url: String, id: String, recovered: Boolean): AbstractWebDriver {
        // a Chrome tab id is like 'AE740895CB3F63220C3A3C751EF1F6E4'
        var driver = _drivers[id]
        if (driver != null) {
            return driver as AbstractWebDriver
        }

        driver = doNewDriver(url, id, recovered)

        addToDriverTree(driver)

        return driver
    }

    protected fun doNewDriver(url: String, id: String, recovered: Boolean): AbstractWebDriver {
        if (!recovered) {
            val driver = _recoveredDrivers.values.firstOrNull { it is AbstractWebDriver && !it.isReused }
            if (driver is AbstractWebDriver) {
                driver.isReused = true
                _reusedDrivers[id] = driver
                logger.info("Reuse recovered driver | {} | {}", id, url)
                return driver
            }
        }

//        val devTools = createDevTools(chromeTab, toolsConfig)
        val driver = newDriverUnmanaged(url)
        _drivers[id] = driver

        if (recovered) {
            driver.isRecovered = true
            _recoveredDrivers[id] = driver
        }

        return driver
    }

    protected fun buildDriverTree() {
        drivers.values.forEach { addToDriverTree(it) }
    }

    protected fun addToDriverTree(driver: WebDriver) {
        if (driver is AbstractWebDriver) {
            val parentId = driver.parentId
            if (parentId > 0) {
                val parent = drivers[parentId.toString()]
                if (parent is AbstractWebDriver) {
                    driver.opener = parent
                    parent.outgoingPages.add(driver)

                    // logger.info("Add driver to tree | parent: {}, child: {} | {}", parent.chromeTab.url, driver.chromeTab.url, driver.chromeTab.id)
                }
            }
        }
    }
}
