package ai.platon.pulsar.common.proxy.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyLoader
import ai.platon.pulsar.common.urls.UrlUtils
import java.time.Duration

open class ProxyVendorLoader(
    conf: ImmutableConfig
) : ProxyLoader(conf) {
    private val logger = getLogger(this)
    private val proxyRotationURL get() = UrlUtils.getURLOrNull(conf["PROXY_ROTATION_URL"])

    override fun updateProxies(reloadInterval: Duration): List<ProxyEntry> {
        val parser = parser ?: return emptyList()
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
