package ai.platon.pulsar.common.proxy

import java.util.concurrent.ConcurrentHashMap

object ProxyParserFactory {
    val parsers = ConcurrentHashMap<String, ProxyParser>()

    fun register(key: String, parser: ProxyParser) {
        parsers[key] = parser
    }

    fun registerIfAbsent(key: String, parser: ProxyParser) {
        parsers.computeIfAbsent(key) { parser }
    }

    fun get(key: String): ProxyParser? {
        return parsers[key]
    }

    fun getOrCreateDefault(mappingFunction: (String) -> ProxyParser): ProxyParser {
        return getOrCreate("", mappingFunction)
    }

    fun getOrCreate(key: String, mappingFunction: (String) -> ProxyParser): ProxyParser {
        return parsers.computeIfAbsent(key, mappingFunction)
    }
}
