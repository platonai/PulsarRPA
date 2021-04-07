package ai.platon.pulsar.client.examples

import ai.platon.pulsar.client.Scraper
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.sql.SqlTemplate
import kotlin.system.exitProcess

fun main() {
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql/crawl"
    val sqls = mapOf(
        "asin" to "x-asin.sql"
    ).entries
        .map { it.key to "$resourcePrefix/${it.value}" }
        .associate { it.first to SqlTemplate.load(it.second).template }

    val urls = LinkExtractors.fromResource("urls/amazon-product-urls.txt").take(12)
    val hosts = listOf("crawl0", "crawl1", "crawl2", "crawl3")
//    val hosts = listOf("localhost")
    urls.chunked(4).parallelStream().forEach { chunck ->
        val host = hosts.random()
        val scraper = Scraper(host, authToken)
        val uuids = scraper.scrapeAll(chunck, sqls)
        scraper.awaitAll(uuids)
    }

    exitProcess(0)
}
