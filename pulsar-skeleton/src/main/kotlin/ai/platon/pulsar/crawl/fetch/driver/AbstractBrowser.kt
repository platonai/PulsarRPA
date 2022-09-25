package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.common.ScriptLoader
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractBrowser(
    override val id: BrowserId,
    val browserSettings: BrowserSettings
): Browser, AbstractEventEmitter<BrowserEvents>() {

    protected val _navigateHistory = Collections.synchronizedList(mutableListOf<NavigateEntry>())
    protected val _drivers = ConcurrentHashMap<String, WebDriver>()

    protected val closed = AtomicBoolean()
    protected var lastActiveTime = Instant.now()

    override val userAgent = getRandomUserAgentOrNull()

    override val navigateHistory: List<NavigateEntry> get() = _navigateHistory
    override val drivers: Map<String, WebDriver> get() = _drivers

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
        _navigateHistory.add(entry)
    }

    override fun close() {
        detach()
        _drivers.clear()
    }

    private fun getRandomUserAgentOrNull() = if (browserSettings.isUserAgentOverridingEnabled) {
        browserSettings.userAgent.getRandomUserAgent()
    } else null

    /**
     * Attach default event handlers
     * */
    private fun attach() {
        on(BrowserEvents.willNavigate) { entry: NavigateEntry -> onWillNavigate(entry) }
    }

    /**
     * Detach default event handlers
     * */
    private fun detach() {
        off(BrowserEvents.willNavigate)
    }
}
