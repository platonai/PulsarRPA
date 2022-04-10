package ai.platon.pulsar.examples.sites.topEc.chinese.jd

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.session.PulsarSession

class JdCrawler(
    val portalUrl: String = "https://list.jd.com/list.html?cat=652,12345,12349",
    val args: String = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure",
    val session: PulsarSession = PulsarContexts.createSession()
) {
    fun crawl() {
        session.loadOutPages(portalUrl, args)
    }
}

fun main() = JdCrawler().crawl()
