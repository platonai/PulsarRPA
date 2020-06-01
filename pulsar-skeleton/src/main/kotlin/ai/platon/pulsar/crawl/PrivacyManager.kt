package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.PreemptChannelSupport
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
): PreemptChannelSupport("PrivacyManager"), AutoCloseable {
    companion object {
        val globalAutoRefreshContext = AtomicReference<PrivacyContext>()
        val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    }

    private val closed = AtomicBoolean()
    abstract var activeContext: PrivacyContext
    @Deprecated("Should not used in a auto refreshing behaviour", ReplaceWith("activeContext"))
    abstract val autoRefreshContext: PrivacyContext
    val isActive get() = !closed.get()

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
            return preempt {
                // normal tasks must wait until all preemptive tasks are finished, but no new task enters the
                // critical section

                if (activeContext.isLeaked) {
                    // close the current context
                    // until the old context is closed entirely
                    activeContext.close()
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
            globalAutoRefreshContext.get()?.close()
            activeContext.close()
        }
    }
}
