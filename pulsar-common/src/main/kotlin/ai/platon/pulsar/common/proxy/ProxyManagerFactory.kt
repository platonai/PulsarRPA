package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.atomic.AtomicReference

class ProxyManagerFactory(val conf: ImmutableConfig): AutoCloseable {
    companion object {
        private fun createProxyManager(conf: ImmutableConfig): ProxyManager {
            val clazz = conf.getClass(PROXY_MANAGER_CLASS, ProxyManager::class.java)
            return clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyManager
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
