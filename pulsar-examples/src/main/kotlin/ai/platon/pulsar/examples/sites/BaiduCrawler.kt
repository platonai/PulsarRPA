package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.examples.common.Crawler

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    val portalUrl = "https://www.baidu.com/"

    Crawler().use { it.load(portalUrl, "-i 1s") }
}
