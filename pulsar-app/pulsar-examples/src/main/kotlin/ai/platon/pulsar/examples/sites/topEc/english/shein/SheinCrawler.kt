package ai.platon.pulsar.examples.sites.topEc.english.shein

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://us.shein.com/New-in-Trends-sc-00654187.html"
    val args = "-i 1d -ii 5d -ol a[href~=-cat-] -ignoreFailure"
    PulsarContexts.createSession().loadOutPages(portalUrl, args)
}
