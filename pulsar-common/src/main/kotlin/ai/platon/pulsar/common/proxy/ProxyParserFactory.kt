package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_PARSER_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.impl.ProxyHubParser
import java.util.concurrent.ConcurrentHashMap

class ProxyParserFactory(
    private val conf: ImmutableConfig
) {
    private val logger = getLogger(this)
    private val parsers = ConcurrentHashMap<String, ProxyParser>()

    fun get(): ProxyParser {
        return computeIfAbsent(conf)
    }

    private fun computeIfAbsent(conf: ImmutableConfig): ProxyParser {
        synchronized(ProxyParserFactory::class) {
            val clazz = getProxyParserClass(conf)
            val loader = parsers.computeIfAbsent(clazz.name) {
                clazz.constructors.first { it.parameters.size == 1 }.newInstance(conf) as ProxyParser
            }

            return loader
        }
    }

    private fun getProxyParserClass(conf: ImmutableConfig): Class<*> {
        val defaultClazz = ProxyHubParser::class.java
        return try {
            conf.getClass(PROXY_PARSER_CLASS, defaultClazz)
        } catch (e: Exception) {
            logger.warn("Can not create the proxy parser {}({}), use default ({})",
                PROXY_PARSER_CLASS, conf[PROXY_PARSER_CLASS], defaultClazz.simpleName)
            defaultClazz
        }
    }
}
