package ai.platon.pulsar.examples.sites.tools

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.test.VerboseCrawler

fun main() {
    withContext {
        val crawler = VerboseCrawler(it)
        """
https://bot.sannysoft.com/
https://intoli.com/blog/making-chrome-headless-undetectable/chrome-headless-test.html
https://arh.antoinevastel.com/bots/areyouheadless
        """.trimIndent().split("\n")
            .map { it.trim() }
            .filter { it.startsWith("http") }
            .filter { it.contains("sannysoft") }
            .forEach { crawler.open(it) }

        readLine()
    }
}
