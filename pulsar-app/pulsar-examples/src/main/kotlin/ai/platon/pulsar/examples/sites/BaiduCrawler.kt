package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler

fun main() {
    val portalUrl = "https://www.baidu.com/"

    withContext {
        Crawler(it).load(portalUrl, "-refresh")
    }
}
