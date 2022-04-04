package ai.platon.pulsar.examples.sites.ebay

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://www.ebay.com/b/Dolce-Gabbana-Bags-Handbags-for-Women/169291/bn_716146"
    val args = "-i 1s -ii 5d -ol a[href~=itm] -ignoreFailure"
    PulsarContexts.createSession().loadOutPages(portalUrl, args)
}
