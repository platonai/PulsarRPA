package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.examples.WebAccess

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    val portalUrl = "https://www.baidu.com/"
    val args = """
        -ic -i 1s -ii 1s -tl 5 -ol "a[href~=item]"
    """.trimIndent()
    WebAccess().use { it.load(portalUrl, args) }
}
