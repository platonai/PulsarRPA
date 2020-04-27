package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: multiple context support
 * TODO: can remove freezable super class
 * */
abstract class PrivacyManager(
        val immutableConfig: ImmutableConfig
): Freezable() {
    companion object {
        val globalActiveContext = AtomicReference<PrivacyContext>()
        val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    }

    abstract val activeContext: PrivacyContext

    inline fun <reified C: PrivacyContext> computeIfAbsent(crossinline mappingFunction: () -> C): C {
        return whenUnfrozen {
            if (globalActiveContext.get() !is C) {
                globalActiveContext.set(mappingFunction())
            }
            globalActiveContext.get() as C
        }
    }

    fun reset() {
        freeze {
            globalActiveContext.getAndSet(null)?.apply {
                zombieContexts.add(this)
                use { it.close() }
            }
        }
    }
}
