package ai.platon.pulsar.examples.sites.tools

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val session = PulsarContexts.createSession()
    session.load("https://www.tmall.com/", "-refresh")
    readLine()
}
