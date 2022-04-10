package ai.platon.pulsar.examples.sites.tmall

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    // TODO: login is required
    val portalUrl = "https://list.tmall.com/search_product.htm?q=大家电"
    val args = "-i 1s -ii 5m -ol a[href~=detail] -ignoreFailure"
    PulsarContexts.createSession().loadOutPages(portalUrl, args)
}
