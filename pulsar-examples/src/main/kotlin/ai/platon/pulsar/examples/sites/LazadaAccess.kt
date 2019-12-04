package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.WebAccess

/**
 * Test for Thai language
 * */
fun main() {
    val portalUrl = "https://www.lazada.com.my/shop-pressure-cookers/"
    val args = """
        -ic -i 1s -ii 1s -ol ".product-recommend-items__item-wrapper > a" -query .product-briefing
    """.trimIndent()
    WebAccess().loadOutPages(portalUrl, args)
}
