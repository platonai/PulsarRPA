package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.atomic.AtomicReference

class ProxyManagerFactory(val conf: ImmutableConfig): AutoCloseable {

    private val proxyManager = AtomicReference<ProxyManager>()

    fun get(): ProxyManager {
        createProxyManagerIfAbsent(conf)
        return proxyManager.get()
    }

    override fun close() {
        proxyManager.get()?.use { it.close() }
    }

    private fun createProxyManagerIfAbsent(conf: ImmutableConfig) {
        if (proxyManager.get() == null) {
            synchronized(ProxyManagerFactory::class) {
                if (proxyManager.get() == null) {
                    val clazz = conf.getClass(PROXY_MANAGER_CLASS, ProxyManager::class.java)
                    proxyManager.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyManager)
                    proxyManager.get().start()
                }
            }
        }
    }
}
