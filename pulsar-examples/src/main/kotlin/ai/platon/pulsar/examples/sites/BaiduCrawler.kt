package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.examples.Crawler

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    val portalUrl = "https://www.baidu.com/"
    val args = """
        -ic -i 1s -ii 1s -tl 5 -ol "a[href~=item]"
    """.trimIndent()
    Crawler().use { it.load(portalUrl, args) }
}
