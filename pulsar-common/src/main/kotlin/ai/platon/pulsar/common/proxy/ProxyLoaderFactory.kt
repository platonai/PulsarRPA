package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_LOADER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.impl.ProxyVendorLoader
import ai.platon.pulsar.common.warnForClose
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ProxyLoaderFactory(val conf: ImmutableConfig) : AutoCloseable {
    companion object {
        // TODO: a temporary solution
        val proxyParser = AtomicReference<ProxyParser>()
    }

    private val logger = LoggerFactory.getLogger(ProxyLoaderFactory::class.java)

    private val proxyLoaders = ConcurrentHashMap<String, ProxyLoader>()

    fun get(): ProxyLoader = computeIfAbsent(conf)

    override fun close() {
        proxyLoaders.values.forEach { it.runCatching { close() }.onFailure { warnForClose(this, it) } }
    }

    private fun computeIfAbsent(conf: ImmutableConfig): ProxyLoader {
        synchronized(ProxyLoaderFactory::class) {
            val clazz = getLoaderClass(conf)
            val loader = proxyLoaders.computeIfAbsent(clazz.name) {
                clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyLoader
            }

            if (proxyParser.get() != null) {
                loader.parser = proxyParser.get()
            }

            return loader
        }
    }

    private fun getLoaderClass(conf: ImmutableConfig): Class<*> {
        val defaultClazz = ProxyVendorLoader::class.java
        return try {
            conf.getClass(PROXY_LOADER_CLASS, defaultClazz)
        } catch (e: Exception) {
            logger.warn(
                "Configured proxy loader {}({}) is not found, use default ({})",
                PROXY_LOADER_CLASS, conf[PROXY_LOADER_CLASS], defaultClazz.simpleName
            )
            defaultClazz
        }
    }
}
