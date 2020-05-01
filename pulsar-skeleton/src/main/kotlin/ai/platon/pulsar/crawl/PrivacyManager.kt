package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: multiple context support
 * TODO: can remove freezable super class
 * */
abstract class PrivacyManager(
        val immutableConfig: ImmutableConfig
): Freezable("PrivacyManager"), AutoCloseable {
    companion object {
        val globalAutoRefreshContext = AtomicReference<PrivacyContext>()
        val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    }

    private val closed = AtomicBoolean()
    abstract var activeContext: PrivacyContext
    @Deprecated("Should not used in a auto refreshing behaviour", ReplaceWith("activeContext"))
    abstract val autoRefreshContext: PrivacyContext
    val isActive get() = !closed.get() && PulsarEnv.isActive

    inline fun <reified C: PrivacyContext> refreshIfNecessary(crossinline mappingFunction: () -> C): C {
        synchronized(PrivacyContext::class.java) {
            if (globalAutoRefreshContext.get() !is C) {
                globalAutoRefreshContext.set(mappingFunction())
            }
            return globalAutoRefreshContext.get() as C
        }
    }

    inline fun <reified C: PrivacyContext> computeIfLeaked(crossinline mappingFunction: () -> C): C {
        synchronized(PrivacyContext::class.java) {
            if (!activeContext.isLeaked) {
                return activeContext as C
            }

            // Refresh the context if privacy leaked
            return freeze {
                if (activeContext.isLeaked) {
                    // all other tasks are waiting until freezer channel is closed
                    // close the current context
                    // until the old context is closed entirely
                    activeContext.use { it.close() }
                    zombieContexts.add(activeContext)

                    activeContext = mappingFunction()
                }
                activeContext as C
            }
        }
    }

    /**
     * Block until the current context is closed
     * */
    fun reset() {
        globalAutoRefreshContext.getAndSet(null)?.apply {
            zombieContexts.add(this)
            use { close() }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            globalAutoRefreshContext.get()?.use { it.close() }

            activeContext.use { it.close() }
        }
    }
}
