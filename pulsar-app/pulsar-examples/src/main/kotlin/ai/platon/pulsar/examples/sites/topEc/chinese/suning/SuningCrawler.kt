package ai.platon.pulsar.examples.sites.topEc.chinese.suning

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.session.PulsarSession

class SuningCrawler(
    val portalUrl: String = "https://search.suning.com/微单/&zw=0?safp=d488778a.shuma.44811515285.1",
    val args: String = "-i 1s -ii 5m -ol a[href~=product] -ignoreFailure",
    val session: PulsarSession = PulsarContexts.createSession()
) {
    fun crawl() {
        session.loadOutPages(portalUrl, args)
    }
}

fun main() {
    SuningCrawler().crawl()
    PulsarContexts.await()
}
