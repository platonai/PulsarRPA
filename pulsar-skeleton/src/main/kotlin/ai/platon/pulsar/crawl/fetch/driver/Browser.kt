package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Similar to puppeteer's Browser
 * */
interface Browser: AutoCloseable {
    val id: BrowserId

    val shutdownHookThread: Thread

    val tabCount: AtomicInteger
    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory: List<NavigateEntry>
    val isIdle: Boolean

    @Throws(WebDriverException::class)
    fun newDriver(): WebDriver
    @Throws(InterruptedException::class)
    fun await()
    @Throws(InterruptedException::class)
    fun signalAll()
}

abstract class AbstractBrowser(
    override val id: BrowserId,
    val browserSettings: BrowserSettings
): Browser {
    val isGUI get() = browserSettings.isGUI

    override val tabCount = AtomicInteger()
    // remember, navigate history is small, so search is very fast for a list
    override val navigateHistory = Collections.synchronizedList(mutableListOf<NavigateEntry>())
    var activeTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    override val isIdle get() = Duration.between(activeTime, Instant.now()) > idleTimeout

    private val initializedLock = ReentrantLock()
    private val initialized = initializedLock.newCondition()

    protected val closed = AtomicBoolean()

    override val shutdownHookThread: Thread = Thread { this.close() }

    @Throws(InterruptedException::class)
    override fun await() {
        initializedLock.withLock { initialized.await() }
    }

    @Throws(InterruptedException::class)
    override fun signalAll() {
        initializedLock.withLock { initialized.signalAll() }
    }
}
