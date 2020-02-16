package ai.platon.pulsar.examples

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool

fun main() {
    val proxyPool = PulsarEnv.getBean(ProxyPool::class.java)
    var cmd = ""
    var q = false

    while (!q) {
        print("Get next proxy?y:")
        cmd = readLine()?:""
        cmd = cmd.trim().toLowerCase()

        when (cmd) {
            "y" -> println(proxyPool.poll())
            "q" -> q = true
        }
    }
}
