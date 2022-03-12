package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractWebDriver(
        override val browserInstanceId: BrowserInstanceId,
        override val id: Int = 0
): Comparable<AbstractWebDriver>, WebDriver {

    enum class Status {
        UNKNOWN, FREE, WORKING, CANCELED, RETIRED, CRASHED, QUIT;

        val isFree get() = this == FREE
        val isWorking get() = this == WORKING
        val isCanceled get() = this == CANCELED
        val isRetired get() = this == RETIRED
        val isCrashed get() = this == CRASHED
        val isQuit get() = this == QUIT
    }

    var proxyEntry: ProxyEntry? = null

    var waitForTimeout = Duration.ofMinutes(1)

    override var lastActiveTime: Instant = Instant.now()

    override var idleTimeout: Duration = Duration.ofMinutes(10)

    override val name get() = javaClass.simpleName + "-" + id

    /**
     * The url to navigate
     * The browser might redirect, so it might not be the same with currentUrl()
     * */
    override var url: String = ""
    /**
     * Whether the web driver has javascript support
     * */
    override val supportJavascript: Boolean = true
    /**
     * Whether the web page source is mocked
     * */
    override val isMockedPageSource: Boolean = false
    /**
     * Driver status
     * */
    val status = AtomicReference(Status.UNKNOWN)

    val isFree get() = status.get().isFree
    val isWorking get() = status.get().isWorking
    val isNotWorking get() = !isWorking
    val isCrashed get() = status.get().isCrashed
    override val isRetired get() = status.get().isRetired
    override val isCanceled get() = status.get().isCanceled
    override val isQuit get() = status.get().isQuit

    override fun free() = status.set(Status.FREE)
    override fun startWork() = status.set(Status.WORKING)
    override fun retire() = status.set(Status.RETIRED)
    override fun cancel() {
        if (isCanceled) {
            return
        }

        if (status.compareAndSet(Status.WORKING, Status.CANCELED)) {
            runBlocking { stopLoading() }
        }
    }

    override suspend fun waitFor(selector: String): Long = waitFor(selector, waitForTimeout.toMillis())

    override suspend fun evaluateSilently(expression: String): Any? =
        takeIf { isWorking }?.runCatching { evaluate(expression) }

    override fun equals(other: Any?): Boolean = other is AbstractWebDriver && other.id == this.id

    override fun hashCode(): Int = id

    override fun compareTo(other: AbstractWebDriver): Int = id - other.id

    override fun toString(): String = sessionId?.let { "#$id-$sessionId" }?:"#$id(closed)"
}
