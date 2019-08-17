package ai.platon.pulsar.proxy

import ai.platon.pulsar.proxy.server.HttpProxyServer
import ai.platon.pulsar.proxy.server.HttpProxyServerConfig

object NormalHttpProxyServer {

    fun main(args: Array<String>) {
        val serverConfig = HttpProxyServerConfig(1, 1, 1)
        HttpProxyServer(serverConfig).start(9999)
    }
}
