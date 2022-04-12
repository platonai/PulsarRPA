package ai.platon.pulsar.examples.sites.topEc.chinese.gome

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://list.gome.com.cn/cat10000092.html"
    val args = "-i 1s -ii 5d -ol a[href~=item] -ignoreFailure"

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)

    PulsarContexts.await()
}
