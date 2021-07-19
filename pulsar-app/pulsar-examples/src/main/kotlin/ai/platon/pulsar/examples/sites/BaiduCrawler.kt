package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler
import java.time.Duration

fun main() {
    val portalUrl = "https://www.baidu.com/"

    withContext {
        Crawler(it).load(portalUrl, "-i 1s")
    }
}
