package ai.platon.pulsar.common.proxy.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyLoader
import ai.platon.pulsar.common.urls.URLUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration

open class ProxyHubLoader(conf: ImmutableConfig) : ProxyLoader(conf) {
    private val logger = LoggerFactory.getLogger(ProxyHubLoader::class.java)

    val baseUrl get() = conf[PROXY_HUB_URL]

    override val parser = ProxyHubParser(conf)

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

        val url = URLUtils.getURLOrNull(baseUrl) ?: throw IOException("No base url is set")
        val response = url.readText()
        if (response.isBlank()) {
            return listOf()
        }

        return parser.parse(response)
    }

    companion object {
        /**
         * The URL of ProxyHub server.
         *
         * [ProxyHub](https://github.com/platonai/ProxyHub)
         */
        const val PROXY_HUB_URL: String = "proxy.hub.url"
    }
}

