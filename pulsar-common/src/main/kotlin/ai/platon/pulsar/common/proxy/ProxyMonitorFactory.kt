package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.atomic.AtomicReference

class ProxyMonitorFactory(val conf: ImmutableConfig): AutoCloseable {

    private val proxyMonitor = AtomicReference<ProxyMonitor>()
    fun get(): ProxyMonitor = createIfAbsent(conf)

    override fun close() {
        proxyMonitor.getAndSet(null)?.use { it.close() }
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyMonitor {
        if (proxyMonitor.get() == null) {
            synchronized(ProxyMonitorFactory::class) {
                if (proxyMonitor.get() == null) {
                    val clazz = conf.getClass(PROXY_MANAGER_CLASS, ProxyMonitor::class.java)
                    proxyMonitor.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyMonitor)
                    proxyMonitor.get().start()
                }
            }
        }
        return proxyMonitor.get()
    }
}
