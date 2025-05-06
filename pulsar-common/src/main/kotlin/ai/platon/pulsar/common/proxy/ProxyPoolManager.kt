package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.FileCommand
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class ProxyPoolManager(
    val proxyPool: ProxyPool,
    val conf: ImmutableConfig
) : AutoCloseable {
    // Set the active proxy idle, for test purpose
    private val isForceIdle get() = FileCommand.check(AppConstants.CMD_PROXY_FORCE_IDLE, 15)

    var lastActiveTime = Instant.EPOCH
    var idleTimeout = conf.getDuration(CapabilityTypes.PROXY_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    open val isIdle get() = (numRunningTasks.get() == 0 && idleTime > idleTimeout) || isForceIdle

    val numRunningTasks = AtomicInteger()
    var statusString: String = ""
    var verbose = false

    val activeProxyEntries = ConcurrentSkipListMap<Path, ProxyEntry>()
    val workingProxyEntries = ConcurrentSkipListSet<ProxyEntry>()

    val isEnabled get() = isProxyEnabled(conf)
    val isDisabled get() = !isEnabled

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()

    @Throws(NoProxyException::class)
    fun getProxy(contextDir: Path, fingerprint: Fingerprint): ProxyEntry {
        val proxy = fingerprint.proxyEntry ?: proxyPool.take()

        if (proxy != null) {
            val proxyEntry0 = activeProxyEntries.computeIfAbsent(contextDir) { proxy }
            proxyEntry0.startWork()
        } else {
            throw NoProxyException("No proxy found in pool ${proxyPool.javaClass.simpleName} | $proxyPool")
        }

        return proxy
    }

    /**
     * Run the task, if the proxy is disabled, call the innovation directly
     * */
    @Throws(NoProxyException::class)
    open suspend fun <R> runWith(proxyEntry: ProxyEntry?, task: suspend () -> R): R {
        return if (isDisabled) task() else runWith0(proxyEntry, task)
    }

    /**
     * Run the task with the proxy
     * */
    @Throws(NoProxyException::class)
    private suspend fun <R> runWith0(proxyEntry: ProxyEntry?, task: suspend () -> R): R {
        return try {
            lastActiveTime = Instant.now()
            proxyEntry?.also {
                it.lastActiveTime = lastActiveTime
                workingProxyEntries.add(it)
            }
            numRunningTasks.incrementAndGet()
            task()
        } finally {
            lastActiveTime = Instant.now()
            proxyEntry?.also {
                it.lastActiveTime = lastActiveTime
                workingProxyEntries.remove(it)
            }
            numRunningTasks.decrementAndGet()
        }
    }

    override fun toString() = statusString

    override fun close() {

    }

    companion object {
        fun isProxyEnabled(conf: ImmutableConfig): Boolean {
            val proxyRotationURL = conf["PROXY_ROTATION_URL"]

            return proxyRotationURL != null
        }
    }
}
