package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler
import java.time.Duration

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
    System.setProperty(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, Duration.ofMinutes(3).toString())
    System.setProperty(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, "10")
    System.setProperty(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, Duration.ofSeconds(10).toString())

//    val portalUrl = "https://www.baidu.com/"
    val portalUrl = "https://tommyjohn.com/collections/loungewear-mens?sort-by=relevance&sort-order=descending"

    withContext {
        Crawler(it).load(portalUrl, "-i 1s")
    }
}
