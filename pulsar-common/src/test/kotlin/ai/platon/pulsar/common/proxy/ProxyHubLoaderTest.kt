package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.impl.ProxyHubLoader
import ai.platon.pulsar.common.proxy.impl.ProxyHubParser
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.junit.jupiter.api.Assumptions
import java.net.URI
import kotlin.test.Test

class ProxyHubLoaderTest {
    private val conf = MutableConfig()
    private val proxyLoader = ProxyHubLoader(conf)
    private val proxyHubUrl = "http://localhost:8192/api/proxies"
    private val parser = ProxyHubParser(conf)

    @Test
    fun `test parseProxyHub`() {
        val map = ProxyHubLoader.mockProxyHubResponse()
        val response = pulsarObjectMapper().writeValueAsString(map)
        val proxies = parser.parse(response)
        println(proxies)
        assert(proxies.size == 2)
    }

    @Test
    fun `test LoadProxies`() {
        Assumptions.assumeTrue(NetUtil.testHttpNetwork(URI.create(proxyHubUrl).toURL()))
        conf[ProxyHubLoader.PROXY_HUB_URL] = proxyHubUrl
        val proxies = proxyLoader.loadProxies()
        println(proxies)
    }
}
