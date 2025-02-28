package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractBrowser(
    override val id: BrowserId,
    val browserSettings: BrowserSettings
): Browser, AutoCloseable, AbstractEventEmitter<BrowserEvents>() {
    companion object {
        val DEFAULT_USER_AGENT = "PulsarRobot/1.0"
    }

    /**
     * All drivers, including the recovered drivers and the reused drivers.
     * */
    protected val _drivers = ConcurrentHashMap<String, WebDriver>()
    protected val _recoveredDrivers = ConcurrentHashMap<String, WebDriver>()
    protected val _reusedDrivers = ConcurrentHashMap<String, WebDriver>()

    protected val initialized = AtomicBoolean()
    private val closed = AtomicBoolean()
    protected var lastActiveTime = Instant.now()

    open val isActive get() = AppContext.isActive && !closed.get() && initialized.get()

    override val userAgent get() = DEFAULT_USER_AGENT

    var userAgentOverride = getRandomUserAgentOrNull()

    override val navigateHistory = NavigateHistory()
    override val drivers: Map<String, WebDriver> get() = _drivers
    /**
     * The associated data.
     * */
    override val data: MutableMap<String, Any?> = mutableMapOf()

    override val canConnect: Boolean get() = AppContext.isActive && !closed.get() && initialized.get()

    override val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    
    override val isPermanent: Boolean get() = id.privacyAgent.isPermanent
    
    val isGUI get() = browserSettings.isGUI
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

    private fun getRandomUserAgentOrNull() = if (browserSettings.isUserAgentOverridingEnabled) {
        browserSettings.userAgent.getRandomUserAgent()
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
}
