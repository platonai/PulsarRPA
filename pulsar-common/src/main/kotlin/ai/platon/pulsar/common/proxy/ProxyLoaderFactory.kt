package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_LOADER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyLoaderFactory(val conf: ImmutableConfig): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyLoaderFactory::class.java)

    private val proxyLoaderRef = AtomicReference<ProxyLoader>()
    fun get(): ProxyLoader = createIfAbsent(conf)

    override fun close() {
        proxyLoaderRef.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyLoader {
        if (proxyLoaderRef.get() == null) {
            synchronized(ProxyLoaderFactory::class) {
                if (proxyLoaderRef.get() == null) {
                    val defaultClazz = FileProxyLoader::class.java
                    val clazz = try {
                        conf.getClass(PROXY_LOADER_CLASS, defaultClazz)
                    } catch (e: Exception) {
                        log.warn("Configured proxy loader {}({}) is not found, use default ({})",
                                PROXY_LOADER_CLASS, conf.get(PROXY_LOADER_CLASS), defaultClazz.simpleName)
                        defaultClazz
                    }
                    proxyLoaderRef.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyLoader)
                }
            }
        }
        return proxyLoaderRef.get()
    }
}
