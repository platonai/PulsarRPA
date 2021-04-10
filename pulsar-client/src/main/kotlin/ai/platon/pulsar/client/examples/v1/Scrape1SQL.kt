package ai.platon.pulsar.client.examples.v1

import ai.platon.pulsar.client.v1.Scraper
import kotlin.system.exitProcess

fun main() {
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"
    val sql = """
        select 
            dom_base_uri(dom) as `url`,
            str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
            dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
            dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
            dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
            dom_first_text(dom, 'h2 a') as `title`,
            dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
        from load_and_select('https://www.amazon.com/s?k="Boys%27+Novelty+Belt+Buckles"&rh=n:9057119011&page=1 -i 1s  -retry', 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
    """.trimIndent()

    val host = "crawl0"
    val scraper = Scraper(host, authToken)
    val uuid = scraper.scrape(sql)
    println(uuid)
    scraper.await(uuid)

    exitProcess(0)
}
