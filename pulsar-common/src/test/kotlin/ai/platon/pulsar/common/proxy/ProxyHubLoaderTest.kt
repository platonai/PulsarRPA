package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.impl.ProxyHubLoader
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import java.net.URI
import kotlin.test.Test

class ProxyHubLoaderTest {
    private val conf = MutableConfig()
    private val proxyLoader = ProxyHubLoader(conf)
    private val proxyHubUrl = "http://localhost:8192/api/proxies"

    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(NetUtil.testHttpNetwork(URI.create(proxyHubUrl).toURL()))
    }

    @Test
    fun `test LoadProxies`() {
        conf[ProxyHubLoader.PROXY_HUB_URL] = proxyHubUrl
        val proxies = proxyLoader.loadProxies()
        println(proxies)
    }
}
