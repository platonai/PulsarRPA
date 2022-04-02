package ai.platon.pulsar.examples.sites.jd

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val args = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure"
    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
