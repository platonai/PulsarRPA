package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.AppConstants.PROXY_SERVER_PORT_BASE
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_ENABLE_FORWARD_SERVER
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class ProxyManager(
        private val conf: ImmutableConfig
): AutoCloseable {

    private val log = LoggerFactory.getLogger(ProxyManager::class.java)

    private val numRunningTasks = AtomicInteger()
    private var numFailedPages = 0
    private var numFailedTests = 0

    private var lastActiveTime = Instant.now()
    private var idleTime = Duration.ZERO
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()

    open val isEnabled get() = conf.getBoolean(PROXY_ENABLE_FORWARD_SERVER, true)
    var report: String = ""
    var port = PROXY_SERVER_PORT_BASE
    var verbose = false
    var autoRefresh = true
    open val currentProxyEntry: ProxyEntry? = null

    val isDisabled get() = !isEnabled

    open fun start() {
        if (isDisabled) {
            log.warn("Proxy manager is disabled")
            return
        }
    }

    /**
     * Run the task despite the proxy manager is disabled, it it's disabled, call the innovation directly
     * */
    open fun <R> runAnyway(task: () -> R): R {
        return if (isDisabled) {
            task()
        } else {
            run(task)
        }
    }

    /**
     * Run the task in the proxy manager
     * */
    open fun <R> run(task: () -> R): R {
        if (isClosed || isDisabled) {
            throw ProxyException("Proxy manager is " + if (isClosed) "closed" else "disabled")
        }

        idleTime = Duration.ZERO

        if (!ensureOnline()) {
            throw ProxyException("Failed to wait for a online proxy")
        }

        return try {
            numRunningTasks.incrementAndGet()
            task()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    open fun ensureOnline(): Boolean {
        if (isDisabled || isClosed) {
            return false
        }

        return true
    }

    open fun changeProxyIfOnline(excludedProxy: ProxyEntry, ban: Boolean) {
        if (isDisabled || isClosed) {
            return
        }

        if (!ensureOnline()) {
            return
        }
    }

    override fun close() {
        if (isEnabled && closed.compareAndSet(false, true)) {
        }
    }
}
