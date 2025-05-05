package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.ImmutableConfig
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Load proxies from proxy vendors
 */
abstract class ProxyLoader(
    val conf: ImmutableConfig
) : AutoCloseable {

    protected val startTime = Instant.now()
    protected val closed = AtomicBoolean()

    open var parser: ProxyParser? = null

    val isActive get() = !closed.get()

    @Throws(ProxyException::class)
    fun updateProxies() = updateProxies(Duration.ZERO)

    @Throws(ProxyException::class)
    abstract fun updateProxies(reloadInterval: Duration): List<ProxyEntry>

    protected fun parse(text: String, format: String): List<ProxyEntry> {
        return parser?.parse(text, format) ?: emptyList()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
