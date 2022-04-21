package ai.platon.pulsar.examples.sites.ec.patpat

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val portalUrl = "https://us.patpat.com/category/Baby.html"
    val args = """-i 1s -ii 30d -outLink a[href~=product] -ignoreFailure"""
    SQLContexts.createSession().loadOutPages(portalUrl, args)
}
