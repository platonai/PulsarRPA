package ai.platon.pulsar.examples.sites.vpnrequired

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://www.lazada.com.my/shop-pressure-cookers/"
    val args = """
        -i 1s -ii 1s -ol ".product-recommend-items__item-wrapper > a"
    """.trimIndent()

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
