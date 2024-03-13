package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_LOADER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.warnInterruptible
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ProxyLoaderFactory(val conf: ImmutableConfig): AutoCloseable {
    private val logger = LoggerFactory.getLogger(ProxyLoaderFactory::class.java)

    private val proxyLoaders = ConcurrentHashMap<String, ProxyLoader>()
    
    var specifiedProxyLoader: ProxyLoader? = null
    
    fun get(): ProxyLoader = specifiedProxyLoader ?: computeIfAbsent(conf)

    override fun close() {
        specifiedProxyLoader?.runCatching { close() }?.onFailure { warnInterruptible(this, it) }
        proxyLoaders.values.forEach { it.runCatching { close() }.onFailure { warnInterruptible(this, it) } }
    }

    private fun computeIfAbsent(conf: ImmutableConfig): ProxyLoader {
        synchronized(ProxyLoaderFactory::class) {
            val clazz = getClass(conf)
            return proxyLoaders.computeIfAbsent(clazz.name) {
                clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyLoader
            }
        }
    }
    
    private fun getClass(conf: ImmutableConfig): Class<*> {
        val defaultClazz = FileProxyLoader::class.java
        return try {
            conf.getClass(PROXY_LOADER_CLASS, defaultClazz)
        } catch (e: Exception) {
            logger.warn("Configured proxy loader {}({}) is not found, use default ({})",
                PROXY_LOADER_CLASS, conf.get(PROXY_LOADER_CLASS), defaultClazz.simpleName)
            defaultClazz
        }
    }
}
