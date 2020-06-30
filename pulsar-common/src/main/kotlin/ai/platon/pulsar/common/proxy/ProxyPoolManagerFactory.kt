package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_MONITOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyPoolManagerFactory(
        val proxyPool: ProxyPool,
        val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyPoolManagerFactory::class.java)

    private val proxyPoolMonitorRef = AtomicReference<ProxyPoolManager>()
    fun get(): ProxyPoolManager = createIfAbsent(conf)

    override fun close() {
        proxyPoolMonitorRef.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyPoolManager {
        if (proxyPoolMonitorRef.get() == null) {
            synchronized(ProxyPoolManagerFactory::class) {
                if (proxyPoolMonitorRef.get() == null) {
                    val defaultClazz = ProxyPoolManager::class.java
                    val clazz = try {
                        conf.getClass(PROXY_POOL_MONITOR_CLASS, defaultClazz)
                    } catch (e: Exception) {
                        log.warn("Configured proxy pool monitor {}({}) is not found, use default ({})",
                                PROXY_POOL_MONITOR_CLASS, conf.get(PROXY_POOL_MONITOR_CLASS), defaultClazz.name)
                        defaultClazz
                    }
                    val ref = clazz.constructors.first { it.parameters.size == 2 }.newInstance(proxyPool, conf)
                    proxyPoolMonitorRef.set(ref as? ProxyPoolManager)
                }
            }
        }

        return proxyPoolMonitorRef.get()
    }
}
