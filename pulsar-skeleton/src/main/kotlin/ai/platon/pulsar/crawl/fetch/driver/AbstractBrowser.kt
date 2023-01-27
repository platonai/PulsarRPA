package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.common.ScriptLoader
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractBrowser(
    override val id: BrowserId,
    val browserSettings: BrowserSettings
): Browser, AbstractEventEmitter<BrowserEvents>() {

    protected val mutableNavigateHistory = Collections.synchronizedList(mutableListOf<NavigateEntry>())
    protected val mutableDrivers = ConcurrentHashMap<String, WebDriver>()

    protected val closed = AtomicBoolean()
    protected var lastActiveTime = Instant.now()

    override val userAgent = getRandomUserAgentOrNull()

    override val navigateHistory: List<NavigateEntry> get() = mutableNavigateHistory
    override val drivers: Map<String, WebDriver> get() = mutableDrivers

    override val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val confuser = ScriptConfuser()
    val scriptLoader = ScriptLoader(confuser, conf = browserSettings.conf)

    val isGUI get() = browserSettings.isGUI
    val idleTimeout = Duration.ofMinutes(10)

    init {
        attach()
    }

    override fun maintain() {
        // Nothing to do
    }

    override fun onWillNavigate(entry: NavigateEntry) {
        mutableNavigateHistory.add(entry)
    }

    override fun close() {
        detach()
        mutableDrivers.clear()
    }

    private fun getRandomUserAgentOrNull() = if (browserSettings.isUserAgentOverridingEnabled) {
        browserSettings.userAgent.getRandomUserAgent()
    } else null

    /**
     * Attach default event handlers
     * */
    protected fun attach() {
        on(BrowserEvents.willNavigate) { entry: NavigateEntry -> onWillNavigate(entry) }
    }

    /**
     * Detach default event handlers
     * */
    protected fun detach() {
        off(BrowserEvents.willNavigate)
    }
}
