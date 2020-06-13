package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_LOADER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyLoaderFactory(val conf: ImmutableConfig): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyLoaderFactory::class.java)

    private val ProxyLoaderRef = AtomicReference<ProxyLoader>()
    fun get(): ProxyLoader = createIfAbsent(conf)

    override fun close() {
        ProxyLoaderRef.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyLoader {
        if (ProxyLoaderRef.get() == null) {
            synchronized(ProxyLoaderFactory::class) {
                if (ProxyLoaderRef.get() == null) {
                    val clazz = try {
                        conf.getClass(PROXY_LOADER_CLASS, FileProxyLoader::class.java)
                    } catch (e: Exception) {
                        log.warn("Proxy pool monitor {} is not found in config, use default", PROXY_LOADER_CLASS)
                        FileProxyLoader::class.java
                    }
                    ProxyLoaderRef.set(clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyLoader)
                }
            }
        }
        return ProxyLoaderRef.get()
    }
}
