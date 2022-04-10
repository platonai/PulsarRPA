package ai.platon.pulsar.examples.sites.topEc.chinese.dangdang

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.session.PulsarSession

class DangdangCrawler(
    val portalUrl: String = "http://category.dangdang.com/cid4010209.html",
    val args: String = "-i 1s -ii 5d -ol a[href~=product] -ignoreFailure",
    val session: PulsarSession = PulsarContexts.createSession()
) {
    fun crawl() {
        session.loadOutPages(portalUrl, args)
    }
}

fun main() = DangdangCrawler().crawl()
