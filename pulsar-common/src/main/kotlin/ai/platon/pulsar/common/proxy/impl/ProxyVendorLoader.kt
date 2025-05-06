package ai.platon.pulsar.common.proxy.impl

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_ROTATION_URL
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyLoader
import ai.platon.pulsar.common.proxy.ProxyParser
import ai.platon.pulsar.common.proxy.ProxyParserFactory
import ai.platon.pulsar.common.urls.UrlUtils
import java.time.Duration

open class ProxyVendorLoader(
    conf: ImmutableConfig
) : ProxyLoader(conf) {
    private val logger = getLogger(this)
    private val proxyRotationURLSpec get() = conf[PROXY_ROTATION_URL]
    private val proxyRotationURL get() = UrlUtils.getURLOrNull(proxyRotationURLSpec)

    override val parser: ProxyParser
        get() {
            val spec = proxyRotationURLSpec ?: return ProxyParserFactory.getOrCreateDefault { ProxyHubParser(conf) }

            val vendorProxyParser = ProxyParserFactory.get(spec)
            val universalProxyParser = ProxyParserFactory.get("UniversalProxyParser")
            return when {
                spec == "http://localhost:8192/api/proxies" -> {
                    ProxyParserFactory.getOrCreate(spec) { ProxyHubParser(conf) }
                }

                vendorProxyParser != null -> vendorProxyParser
                universalProxyParser != null -> universalProxyParser
                else -> ProxyParserFactory.getOrCreateDefault { ProxyHubParser(conf) }
            }
        }

    override fun updateProxies(reloadInterval: Duration): List<ProxyEntry> {
        val url = proxyRotationURL ?: return emptyList()

        return try {
            val text = url.readText()
            parser.parse(text, "auto")
        } catch (e: Exception) {
            logger.error("Failed to fetch proxies from provider | {}", url, e)
            emptyList()
        }
    }
}
