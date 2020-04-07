package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: multiple context support
 * */
abstract class PrivacyContextManager(
        val immutableConfig: ImmutableConfig
): Freezable() {
    companion object {
        val globalActiveContext = AtomicReference<PrivacyContext>()
        val zombieContexts = ConcurrentLinkedQueue<PrivacyContext>()
    }

    abstract val activeContext: PrivacyContext

    fun reset() {
        freeze {
            globalActiveContext.getAndSet(null)?.apply {
                zombieContexts.add(this)
                use { it.close() }
            }
        }
    }
}
