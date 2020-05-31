package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyMonitorFactory(val conf: ImmutableConfig): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyMonitorFactory::class.java)

    private val proxyMonitor = AtomicReference<ProxyPoolMonitor>()
    fun get(): ProxyPoolMonitor = createIfAbsent(conf)

    override fun close() {
        proxyMonitor.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyPoolMonitor {
        if (proxyMonitor.get() == null) {
            synchronized(ProxyMonitorFactory::class) {
                if (proxyMonitor.get() == null) {
                    val clazz = try {
                        conf.getClass(PROXY_MANAGER_CLASS, ProxyPoolMonitor::class.java)
                    } catch (e: Exception) {
                        log.warn("Proxy manager {} is not found, use default", PROXY_MANAGER_CLASS)
                        ProxyPoolMonitor::class.java
                    }
                    proxyMonitor.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyPoolMonitor)
                }
            }
        }
        return proxyMonitor.get()
    }
}
