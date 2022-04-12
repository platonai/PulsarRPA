package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface BrowserInstance: AutoCloseable {
    val id: BrowserInstanceId
    val launcherOptions: LauncherOptions
    val launchOptions: ChromeOptions

    val tabCount: AtomicInteger
    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory: List<NavigateEntry>
    val isIdle: Boolean

    fun launch()
    fun await()
    fun signalAll()
}

abstract class AbstractBrowserInstance(
    override val id: BrowserInstanceId,
    override val launcherOptions: LauncherOptions,
    override val launchOptions: ChromeOptions
): BrowserInstance {
    val isGUI get() = launcherOptions.browserSettings.isGUI

    override val tabCount = AtomicInteger()
    // remember, navigate history is small, so search is very fast for a list
    override val navigateHistory = mutableListOf<NavigateEntry>()
    var activeTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    override val isIdle get() = Duration.between(activeTime, Instant.now()) > idleTimeout

    private val initializedLock = ReentrantLock()
    private val initialized = initializedLock.newCondition()

    protected val launched = AtomicBoolean()
    protected val closed = AtomicBoolean()

    override fun await() {
        initializedLock.withLock { initialized.await() }
    }

    override fun signalAll() {
        initializedLock.withLock { initialized.signalAll() }
    }
}
