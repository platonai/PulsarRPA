package ai.platon.pulsar.examples.sites.tools

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val urls = """
https://bot.sannysoft.com/
https://intoli.com/blog/making-chrome-headless-undetectable/chrome-headless-test.html
https://arh.antoinevastel.com/bots/areyouheadless
        """.trimIndent().split("\n")
        .map { it.trim() }
        .filter { it.startsWith("http") }
        .filter { it.contains("sannysoft") }

    val session = PulsarContexts.createSession()
    urls.forEach { session.open(it) }
}
