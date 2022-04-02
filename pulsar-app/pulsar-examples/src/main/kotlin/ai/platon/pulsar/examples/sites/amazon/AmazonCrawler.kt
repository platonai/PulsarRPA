package ai.platon.pulsar.examples.sites.amazon

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-i 1s -ii 5m -ol a[href~=/dp/] -ignoreFailure"

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
