package ai.platon.pulsar.examples.sites.topEc.chinese.dangdang

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "http://category.dangdang.com/cid4010209.html"
    val args = "-i 1s -ii 5d -ol a[href~=product] -ignoreFailure"

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)

    PulsarContexts.await()
}
