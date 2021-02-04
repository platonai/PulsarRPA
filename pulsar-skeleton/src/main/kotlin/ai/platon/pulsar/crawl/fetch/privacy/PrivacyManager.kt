package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

abstract class PrivacyContextMonitor(
        initialDelay: Long = 300,
        watchInterval: Long = 30
): ScheduledMonitor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(watchInterval))

abstract class PrivacyManager(val conf: ImmutableConfig): AutoCloseable {
    protected val log = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()

    val privacyContextIdGenerator get() = PrivacyContextIdGeneratorFactory(conf).generator
    val isActive get() = !closed.get() && AppContext.isActive
    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    /**
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    val activeContexts = ConcurrentHashMap<PrivacyContextId, PrivacyContext>()

    /**
     * Run a task within this privacy manager
     * */
    abstract suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    /**
     * Create a new context or return an exist one
     * */
    abstract fun computeNextContext(): PrivacyContext

    /**
     * Create a new context or return an exist one
     * */
    abstract fun computeIfNecessary(): PrivacyContext

    /**
     * Create a context with [id] and add to active context list if not absent
     * */
    abstract fun computeIfAbsent(id: PrivacyContextId): PrivacyContext

    /**
     * Create a context and do not add to active context list
     * */
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
                    .joinToString(", ", prefix, postfix) { String.format("%.2f", it.meterSuccesses.meanRate) }
                    .let { log.info(it) }
        }
    }
}
