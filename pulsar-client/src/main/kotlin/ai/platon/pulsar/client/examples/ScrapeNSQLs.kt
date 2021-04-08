package ai.platon.pulsar.client.examples

import ai.platon.pulsar.client.Scraper
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.sql.SQLTemplate

fun main() {
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql/crawl"
    val sqls = mapOf(
        "asin" to "x-asin.sql",
        "sims-1" to "x-asin-sims-consolidated-1.sql",
        "sims-2" to "x-asin-sims-consolidated-2.sql",
        "sims-3" to "x-asin-sims-consolidated-3.sql",
        "sims-consider" to "x-asin-sims-consider.sql",
        "similar-items" to "x-similar-items.sql",
        "reviews" to "x-asin-reviews.sql"
    ).entries
        .map { it.key to "$resourcePrefix/${it.value}" }
        .associate { it.first to SQLTemplate.load(it.second).template }

    val urls = LinkExtractors.fromResource("urls/amazon-product-urls.txt").take(10)
    listOf("crawl0", "crawl1", "crawl2", "crawl3").parallelStream().forEach { host ->
        val scraper = Scraper(host, authToken)
        val uuids = scraper.scrapeAll(urls, sqls)
        scraper.awaitAll(uuids)
    }
}
