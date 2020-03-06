package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.Crawler

/**
 * Test for Thai language
 * */
fun main() {
    val portalUrl = "https://shopee.co.th/กระเป๋าเป้ผู้ชาย-cat.49.1037.10297?page=1"

//    val portalUrl = "https://th.xiapibuy.com/m/super-men-sale"
    val args = """
        -ic -i 1s -ii 1s -ol ".shopee-search-item-result__item a" -query .product-briefing -sc 10
    """.trimIndent()
    Crawler().loadOutPages(portalUrl, args)
}
