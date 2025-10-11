package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.impl.ProxyHubParser
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import kotlin.test.Test

class ProxyHubParserTest {
    private val conf = MutableConfig()
    private val parser = ProxyHubParser(conf)

    @Test
    fun `test parseProxyHub`() {
        val map = ProxyHubParser.mockProxyHubResponse()
        val response = pulsarObjectMapper().writeValueAsString(map)
        val proxies = parser.parse(response)
        println(proxies)
        assert(proxies.size == 2)
    }
}