package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

class ProxyManagerFactory(val conf: ImmutableConfig): AutoCloseable {
    companion object {
        var proxyManagerClass: KClass<out ProxyManager> = ProxyManager::class
        private fun createProxyManager(conf: ImmutableConfig): ProxyManager {
            return proxyManagerClass.constructors.first { it.parameters.size == 1 }.call(conf)
        }
    }

    private val proxyManager = AtomicReference<ProxyManager>()

    fun get(): ProxyManager {
        proxyManager.compareAndSet(null, createProxyManager(conf))
        return proxyManager.get()
    }

    fun start() {
        proxyManager.get()?.start()
    }

    override fun close() {
        proxyManager.get()?.use { it.close() }
    }
}
