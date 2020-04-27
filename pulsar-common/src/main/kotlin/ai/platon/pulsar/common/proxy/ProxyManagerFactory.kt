package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.atomic.AtomicReference

class ProxyManagerFactory(val conf: ImmutableConfig): AutoCloseable {

    private val proxyManager = AtomicReference<ProxyMonitor>()

    fun get(): ProxyMonitor = createIfAbsent(conf)

    override fun close() {
        proxyManager.get()?.use { it.close() }
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyMonitor {
        if (proxyManager.get() == null) {
            synchronized(ProxyManagerFactory::class) {
                if (proxyManager.get() == null) {
                    val clazz = conf.getClass(PROXY_MANAGER_CLASS, ProxyMonitor::class.java)
                    proxyManager.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyMonitor)
                    proxyManager.get().start()
                }
            }
        }
        return proxyManager.get()
    }
}
