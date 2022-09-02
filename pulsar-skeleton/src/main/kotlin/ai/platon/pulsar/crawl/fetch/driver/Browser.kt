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
 * A Browser is created via {@link BrowserFactory#launch BrowserFactory.launch()}.
 */
interface Browser: AutoCloseable {
    val id: BrowserId

    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory: List<NavigateEntry>
    val drivers: Queue<WebDriver>
    val isIdle: Boolean

    /** Synchronization monitor for the "refresh" and "destroy" */
    val shutdownMonitor: Any
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

    fun onWillNavigate(entry: NavigateEntry)
}

abstract class AbstractBrowser(
    override val id: BrowserId,
    val browserSettings: BrowserSettings
): Browser {
    private val initializedLock = ReentrantLock()
    private val initialized = initializedLock.newCondition()

    protected val _navigateHistory = Collections.synchronizedList(mutableListOf<NavigateEntry>())
    protected val _drivers = ConcurrentLinkedQueue<WebDriver>()

    protected val closed = AtomicBoolean()
    protected var lastActiveTime = Instant.now()

    // remember, navigate history is small, so search is very fast for a list
    override val navigateHistory: List<NavigateEntry> get() = _navigateHistory
    override val drivers: Queue<WebDriver>  get() = _drivers

    override val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val isGUI get() = browserSettings.isGUI
    val idleTimeout = Duration.ofMinutes(10)

    /** Synchronization monitor for the "refresh" and "destroy" */
    override val shutdownMonitor = Any()

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
            this.shutdownHook = Thread { synchronized(shutdownMonitor) { close() } }
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

    override fun onWillNavigate(entry: NavigateEntry) {
        _navigateHistory.add(entry)
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
