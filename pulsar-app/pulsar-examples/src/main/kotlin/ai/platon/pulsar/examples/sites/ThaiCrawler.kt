package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler

private val portalUrl = "https://shopee.co.th/กระเป๋าเป้ผู้ชาย-cat.49.1037.10297?page=1"

private val args = """
        -i 1s -ii 1s -ol ".shopee-search-item-result__item a" -query .product-briefing -sc 10
    """.trimIndent()

fun main() = withContext {
    BrowserSettings.withGUI()
    Crawler(it).loadOutPages(portalUrl, args)
}
