package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

abstract class PrivacyContextMonitor(
        initialDelay: Long = 300,
        watchInterval: Long = 30
): ScheduledMonitor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(watchInterval))

abstract class PrivacyManager(
        val conf: ImmutableConfig,
        val numPrivacyContexts: Int = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
): AutoCloseable {
    protected val log = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()
    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    /**
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    val activeContexts = ConcurrentHashMap<PrivacyContextId, PrivacyContext>()
    private val iterator = Iterables.cycle(activeContexts.values).iterator()

    abstract suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult

    open fun computeNextContext(): PrivacyContext {
        val context = computeIfNecessary()?:synchronized(activeContexts) { iterator.next() }
        if (context.isActive) {
            return context
        }

        close(context)

        return computeIfAbsent(PrivacyContextId.generate())
    }

    open fun computeIfNecessary(): PrivacyContext? {
        if (activeContexts.size < numPrivacyContexts) {
            synchronized(activeContexts) {
                if (activeContexts.size < numPrivacyContexts) {
                    return computeIfAbsent(PrivacyContextId.generate())
                }
            }
        }

        return null
    }

    open fun computeIfAbsent(id: PrivacyContextId) = activeContexts.computeIfAbsent(id) { createUnmanagedContext(it) }

    abstract fun createUnmanagedContext(id: PrivacyContextId): PrivacyContext

    open fun close(privacyContext: PrivacyContext) {
        val id = privacyContext.id

        synchronized(activeContexts) {
            if (activeContexts.containsKey(id)) {
                activeContexts.remove(id)
                zombieContexts.add(privacyContext)
            }
        }

        privacyContext.close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            activeContexts.values.forEach { zombieContexts.add(it) }
            activeContexts.clear()

            zombieContexts.toList().parallelStream().forEach {
                kotlin.runCatching { it.close() }.onFailure { it.printStackTrace() }
            }
        }
    }

    private fun reportZombieContexts() {
        if (zombieContexts.isNotEmpty()) {
            val prefix = "The latest context throughput: "
            val postfix = " (success/sec)"
            zombieContexts.take(15)
                    .joinToString(", ", prefix, postfix) { String.format("%.2f", it.throughput) }
                    .let { log.info(it) }
        }
    }
}
