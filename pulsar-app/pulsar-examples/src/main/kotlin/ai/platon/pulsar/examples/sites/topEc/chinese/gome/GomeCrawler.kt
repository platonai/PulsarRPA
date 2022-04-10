package ai.platon.pulsar.examples.sites.topEc.chinese.gome

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.examples.sites.topEc.chinese.dangdang.DangdangCrawler
import ai.platon.pulsar.session.PulsarSession

class GomeCrawler(
    val portalUrl: String = "https://list.gome.com.cn/cat10000092.html",
    val args: String = "-i 1s -ii 5d -ol a[href~=item] -ignoreFailure",
    val session: PulsarSession = PulsarContexts.createSession()
) {
    fun crawl() {
        session.loadOutPages(portalUrl, args)
    }
}

fun main() {
    GomeCrawler().crawl()
    PulsarContexts.await()
}
