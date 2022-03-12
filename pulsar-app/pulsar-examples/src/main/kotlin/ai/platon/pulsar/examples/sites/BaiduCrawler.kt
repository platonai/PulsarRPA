package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler

fun main() {
    val portalUrl = "https://antispider8.scrape.center/"

    withContext {
        val crawler = Crawler(it)
        crawler.load(portalUrl, "-refresh")
        "el-card__body"
    }
}
