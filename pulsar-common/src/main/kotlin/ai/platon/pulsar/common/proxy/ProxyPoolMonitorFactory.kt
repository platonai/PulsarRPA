package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_MONITOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyPoolMonitorFactory(val conf: ImmutableConfig): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyPoolMonitorFactory::class.java)

    private val proxyPoolMonitorRef = AtomicReference<ProxyPoolMonitor>()
    fun get(): ProxyPoolMonitor = createIfAbsent(conf)

    override fun close() {
        proxyPoolMonitorRef.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyPoolMonitor {
        if (proxyPoolMonitorRef.get() == null) {
            synchronized(ProxyPoolMonitorFactory::class) {
                if (proxyPoolMonitorRef.get() == null) {
                    val clazz = try {
                        conf.getClass(PROXY_POOL_MONITOR_CLASS, ProxyPoolMonitor::class.java)
                    } catch (e: Exception) {
                        log.warn("Proxy pool monitor {} is not found in config, use default", PROXY_POOL_MONITOR_CLASS)
                        ProxyPoolMonitor::class.java
                    }
                    proxyPoolMonitorRef.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyPoolMonitor)
                }
            }
        }
        return proxyPoolMonitorRef.get()
    }
}
