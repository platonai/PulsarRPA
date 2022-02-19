package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.examples.common.Crawler
import ai.platon.pulsar.ql.context.withSQLContext

fun main() {
    val seed = "https://ly.simuwang.com/"
    val args = "-i 1s -ii 10d -ol a[href~=roadshow] -tl 100"

    withSQLContext { cx ->
        val crawler = Crawler(cx)
        crawler.load(seed, args)
    }
}
