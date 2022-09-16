package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import java.util.*

/**
 * The Browser defines methods and events for to manage the real browser.
 */
interface Browser: AutoCloseable {
    val id: BrowserId
    val userAgent: String?

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
