package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.test.VerboseCrawler

fun main() {
    val url = "https://www.baidu.com/"

    withContext {
        val crawler = VerboseCrawler(it)
        crawler.open(url)
        readLine()
    }
}
