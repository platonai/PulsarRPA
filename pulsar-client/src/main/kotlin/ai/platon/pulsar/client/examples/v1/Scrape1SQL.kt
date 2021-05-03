package ai.platon.pulsar.client.examples.v1

import ai.platon.pulsar.client.v1.AsyncScraper
import kotlin.system.exitProcess

fun main() {
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"
    val sql = """
    select
        dom_first_text(dom, '#productTitle') as `title`,
        dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
        dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
        array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
        dom_base_uri(dom) as `baseUri`
    from
        load_and_select('https://www.amazon.com/dp/B00BTX5926', ':root')
    """.trimIndent()

    val host = "crawl0"
    val scraper = AsyncScraper(host, authToken)
    val uuid = scraper.scrape(sql)
    println(uuid)
    scraper.await(uuid)

    exitProcess(0)
}
