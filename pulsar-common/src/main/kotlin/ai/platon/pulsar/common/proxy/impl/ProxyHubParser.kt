package ai.platon.pulsar.common.proxy.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyParser
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.slf4j.LoggerFactory

open class ProxyHubParser(conf: ImmutableConfig) : ProxyParser() {
    private val logger = LoggerFactory.getLogger(ProxyHubParser::class.java)

    override val name: String = "ProxyHub"

    override fun parse(text: String, format: String): List<ProxyEntry> {
        val tree = pulsarObjectMapper().readTree(text)
        val status = tree["status"]?.asText() ?: "failed"
        val message = tree["message"]
        if (status != "success") {
            logger.debug("Failed to load proxies | {}", message)
            return listOf()
        }

        return try {
            val proxies = tree["data"]["proxies"]
            return proxies.map { proxy ->
                val host = proxy["host"]?.asText() ?: ""
                val port = proxy["port"]?.asInt() ?: 0
                val username = proxy["username"]?.asText() ?: ""
                val password = proxy["password"]?.asText() ?: ""
                val type = proxy["type"]?.asText() ?: ""
                val declaredTTL = proxy["declaredTTL"]?.asText() ?: ""
                ProxyEntry.create(host, port, username, password, type, declaredTTL)
            }
        } catch (e: Exception) {
            logger.debug("Failed to parse proxies | {}", e.message)
            listOf()
        }
    }
}