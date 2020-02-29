package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.proxy.ProxyManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PrivacyContextFactory(
        /**
         * The web driver pool
         * TODO: web driver pool should be created by privacy context, not singleton
         * */
        val driverPool: WebDriverPool,
        val proxyManager: ProxyManager,
        val immutableConfig: ImmutableConfig
) {
    companion object {
        private val globalActiveContext = AtomicReference<BrowserPrivacyContext>()
        val retiredContexts = ConcurrentLinkedQueue<BrowserPrivacyContext>()
    }

    val activeContext get() = getOrCreate()

    fun reset() {
        // we need to freeze all running tasks and reset driver pool and proxy
        val context = globalActiveContext.get()
        context?.use { it.close() }
        context?.waitUntilClosed()
        globalActiveContext.getAndSet(null)?.let { retiredContexts.add(it) }
    }

    private fun getOrCreate(): BrowserPrivacyContext {
        if (globalActiveContext.get() == null) {
            // globalActiveContext.compareAndSet(null, BrowserPrivacyContext(driverPool, proxyManager, immutableConfig))
            globalActiveContext.set(BrowserPrivacyContext(driverPool, proxyManager, immutableConfig))
        }
        return globalActiveContext.get()
    }
}
