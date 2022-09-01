package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Similar to puppeteer's Browser
 * */
interface Browser: AutoCloseable {
    val id: BrowserId

    val tabCount: AtomicInteger
    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory: List<NavigateEntry>
    val isIdle: Boolean

    /** Synchronization monitor for the "refresh" and "destroy" */
    val startupShutdownMonitor: Any
    /** Reference to the JVM shutdown hook, if registered */
    val shutdownHook: Thread?
    @Throws(IllegalStateException::class)
    fun registerShutdownHook()

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
    override val navigateHistory: MutableList<NavigateEntry> = Collections.synchronizedList(mutableListOf())
    var activeTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    override val isIdle get() = Duration.between(activeTime, Instant.now()) > idleTimeout

    private val initializedLock = ReentrantLock()
    private val initialized = initializedLock.newCondition()

    protected val closed = AtomicBoolean()

    /** Synchronization monitor for the "refresh" and "destroy" */
    override val startupShutdownMonitor = Any()

    /** Reference to the JVM shutdown hook, if registered */
    override var shutdownHook: Thread? = null

    /**
     * Register a shutdown hook with the JVM runtime, closing this context
     * on JVM shutdown unless it has already been closed at that time.
     *
     * Delegates to `doClose()` for the actual closing procedure.
     * @see Runtime.addShutdownHook
     *
     * @see .close
     * @see .doClose
     */
    @Throws(IllegalStateException::class)
    override fun registerShutdownHook() {
        if (this.shutdownHook == null) { // No shutdown hook registered yet.
            this.shutdownHook = Thread { synchronized(startupShutdownMonitor) { close() } }
            Runtime.getRuntime().addShutdownHook(this.shutdownHook)
        }
    }

    @Throws(InterruptedException::class)
    override fun await() {
        initializedLock.withLock { initialized.await() }
    }

    @Throws(InterruptedException::class)
    override fun signalAll() {
        initializedLock.withLock { initialized.signalAll() }
    }

    override fun close() {
        // If we registered a JVM shutdown hook, we don't need it anymore now:
        // We've already explicitly closed the context.
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            } catch (ex: IllegalStateException) {
                // ignore - VM is already shutting down
            }
        }
    }
}
