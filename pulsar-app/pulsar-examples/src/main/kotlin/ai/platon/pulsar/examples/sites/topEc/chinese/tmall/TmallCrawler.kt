package ai.platon.pulsar.examples.sites.topEc.chinese.tmall

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.DefaultPulsarEventHandler
import ai.platon.pulsar.crawl.event.LoginHandler
import ai.platon.pulsar.examples.sites.topEc.chinese.taobao.TaobaoLoginHandler
import ai.platon.pulsar.session.PulsarSession

class TmallCrawler(
    val portalUrl: String = "https://list.tmall.com/search_product.htm?q=大家电",
    val args: String = "-i 1s -ii 5m -ol a[href~=detail] -ignoreFailure",
    val session: PulsarSession = PulsarContexts.createSession()
) {
    // login parameters
    val username = System.getenv("PULSAR_TAOBAO_USERNAME") ?: "MustFallUsername"
    val password = System.getenv("PULSAR_TAOBAO_PASSWORD") ?: "MustFallPassword"

    fun crawl() {
        val options = session.options(args)

        val loginHandler = TaobaoLoginHandler(username, password, warnUpUrl = portalUrl)
        options.eventHandler.loadEventHandler.onAfterBrowserLaunch.addLast(loginHandler)

        session.loadOutPages(portalUrl, options)
    }
}

fun main() = TmallCrawler().crawl()
