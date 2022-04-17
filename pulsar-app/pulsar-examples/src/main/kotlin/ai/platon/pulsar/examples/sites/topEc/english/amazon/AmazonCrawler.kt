package ai.platon.pulsar.examples.sites.topEc.english.amazon

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-i 1s -ii 5s -ol a[href~=/dp/] -ignoreFailure"

    val session = SQLContexts.createSession()
    // session.loadOutPages(portalUrl, args)
    session.load(portalUrl, args)
    println("Done.")
}
