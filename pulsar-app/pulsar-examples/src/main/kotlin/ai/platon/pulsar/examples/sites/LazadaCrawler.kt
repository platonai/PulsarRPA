package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.test.VerboseCrawler

private val portalUrl = "https://www.lazada.com.my/shop-pressure-cookers/"
private val args = """
        -i 1s -ii 1s -ol ".product-recommend-items__item-wrapper > a"
    """.trimIndent()

fun main() = withContext { VerboseCrawler(it).loadOutPages(portalUrl, args) }
