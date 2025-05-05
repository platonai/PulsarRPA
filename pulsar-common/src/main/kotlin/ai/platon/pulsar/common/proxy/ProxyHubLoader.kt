package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_HUB_URL
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.urls.UrlUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration

open class ProxyHubLoader(conf: ImmutableConfig) : ProxyLoader(conf) {
    private val logger = LoggerFactory.getLogger(FileProxyLoader::class.java)

    val baseUrl get() = conf[PROXY_HUB_URL]

    @Synchronized
    @Throws(IOException::class)
    override fun updateProxies(reloadInterval: Duration): List<ProxyEntry> {
        if (baseUrl.isNullOrBlank()) {
            return listOf()
        }

        return kotlin.runCatching { loadProxies(reloadInterval) }
            .onFailure { logger.warn("Failed to update proxies, {}", it.message) }
            .getOrElse { listOf() }
    }

    @Throws(IOException::class)
    fun loadProxies(): List<ProxyEntry> = loadProxies(Duration.ZERO)

    @Throws(IOException::class)
    fun loadProxies(reloadInterval: Duration): List<ProxyEntry> {
        if (baseUrl.isNullOrBlank()) {
            return listOf()
        }

        val url = UrlUtils.getURLOrNull(baseUrl) ?: throw IOException("No base url is set")
        val response = url.readText()
        if (response.isBlank()) {
            return listOf()
        }

        return parseProxyHub(response)
    }

    internal fun parseProxyHub(response: String): List<ProxyEntry> {
        val tree = pulsarObjectMapper().readTree(response)
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

    companion object {
        fun mockProxyHubResponse(): Map<String, Any> {
            val proxies = listOf(
                ProxyEntry(
                    "127.0.0.1",
                    10908,
                ),
                ProxyEntry(
                    "127.0.0.1",
                    10909,
                )
            )

            return mapOf(
                "status" to "success",
                "message" to "Proxies retrieved successfully",
                "data" to mapOf(
                    "proxies" to proxies.map { proxy ->
                        mapOf(
                            "host" to proxy.host,
                            "port" to proxy.port,
                            "username" to proxy.username,
                            "password" to proxy.password,
                            "type" to proxy.type,
                            "declaredTTL" to proxy.declaredTTL
                        )
                    }
                )
            )
        }
    }
}