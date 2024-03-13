package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_MANAGER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.warnInterruptible
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ProxyPoolManagerFactory(
        val proxyPool: ProxyPool,
        val conf: ImmutableConfig
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(ProxyPoolManagerFactory::class.java)

    private val proxyPoolManagers = ConcurrentHashMap<String, ProxyPoolManager>()
    
    var specifiedProxyManager: ProxyPoolManager? = null
    
    fun get(): ProxyPoolManager = specifiedProxyManager ?: computeIfAbsent(conf)
    
    override fun close() {
        specifiedProxyManager?.runCatching { close() }?.onFailure { warnInterruptible(this, it) }
        proxyPoolManagers.values.forEach { it.runCatching { close() }.onFailure { warnInterruptible(this, it) } }
    }

    private fun computeIfAbsent(conf: ImmutableConfig): ProxyPoolManager {
        synchronized(ProxyPoolManagerFactory::class) {
            val clazz = getClass(conf)
            return proxyPoolManagers.computeIfAbsent(clazz.name) {
                // TODO: bad manner to construct the object
                clazz.constructors.first { it.parameters.size == 2 }.newInstance(proxyPool, conf) as ProxyPoolManager
            }
        }
    }

    private fun getClass(conf: ImmutableConfig): Class<*> = getClass(conf, PROXY_POOL_MANAGER_CLASS)
    
    private fun getClass(conf: ImmutableConfig, clazzName: String): Class<*> {
        val defaultClazz = ProxyPoolManager::class.java
        return try {
            conf.getClass(clazzName, defaultClazz)
        } catch (e: Exception) {
            logger.warn("Configured proxy loader {}({}) is not found, use default ({})",
                clazzName, conf.get(clazzName), defaultClazz.simpleName)
            defaultClazz
        }
    }
}
