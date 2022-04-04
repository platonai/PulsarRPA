package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val context = SQLContexts.activate()
    val urls = LinkExtractors.fromResource("seeds.txt").take(10).map { "$it -refresh" }
    context.crawlPool.addAll(urls)
    context.await()
}
