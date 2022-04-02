package ai.platon.pulsar.examples.sites.tmall

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://list.tmall.com/search_product.htm?q=大家电"
    val args = "-i 1s -ii 5m -ol a[href~=detail] -ignoreFailure"
    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
