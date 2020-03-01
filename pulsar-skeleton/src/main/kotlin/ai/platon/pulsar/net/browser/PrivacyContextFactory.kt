package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.proxy.ProxyManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: multiple context support
 * */
class PrivacyContextFactory(
        /**
         * The web driver pool
         * TODO: web driver pool should be created by a privacy context, not a singleton
         * */
        val driverPool: WebDriverPool,
        val proxyManager: ProxyManager,
        val immutableConfig: ImmutableConfig
) {
    companion object {
        private val globalActiveContext = AtomicReference<BrowserPrivacyContext>()
        val zombieContexts = ConcurrentLinkedQueue<BrowserPrivacyContext>()
    }

    val activeContext
        @Synchronized
        get() = getOrCreate()

    @Synchronized
    fun reset() {
        // we need to freeze all running tasks and reset driver pool and proxy
        val context = globalActiveContext.get()
        context?.use { it.close() }
//        context?.waitUntilClosed()
        globalActiveContext.getAndSet(null)?.let { zombieContexts.add(it) }
    }

    private fun getOrCreate(): BrowserPrivacyContext {
        if (globalActiveContext.get() == null) {
            globalActiveContext.set(BrowserPrivacyContext(driverPool, proxyManager, immutableConfig))
        }
        return globalActiveContext.get()
    }
}
