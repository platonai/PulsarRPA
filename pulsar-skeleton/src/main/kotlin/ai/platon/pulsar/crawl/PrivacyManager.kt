package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

abstract class PrivacyManager(
        val immutableConfig: ImmutableConfig
): AutoCloseable {
    protected val log = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()
    val numPrivacyContexts = immutableConfig.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)
    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    /**
     * TODO: use a priority queue and every time we need a context, take the top one
     * */
    val activeContexts = ConcurrentHashMap<PrivacyContextId, PrivacyContext>()
    private val iterator = Iterables.cycle(activeContexts.values).iterator()

    @Synchronized
    open fun computeNextContext(): PrivacyContext {
        if (activeContexts.size < numPrivacyContexts) {
            return computeIfAbsent(PrivacyContextId.generate())
        }

        val context = iterator.next()
        if (context.isActive) {
            return context
        }

        activeContexts.remove(context.id)
        zombieContexts.add(context)
        context.close()

        return newContext(PrivacyContextId.generate()).also { activeContexts[it.id] = it }
    }

    open fun computeIfAbsent(id: PrivacyContextId) = activeContexts.computeIfAbsent(id) { newContext(it) }

    abstract fun newContext(id: PrivacyContextId): PrivacyContext

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            activeContexts.values.forEach { zombieContexts.add(it) }
            activeContexts.clear()

            zombieContexts.forEach {
                kotlin.runCatching { it.close() }.onFailure {
                    log.error("Failed to close privacy context", it)
                }
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
