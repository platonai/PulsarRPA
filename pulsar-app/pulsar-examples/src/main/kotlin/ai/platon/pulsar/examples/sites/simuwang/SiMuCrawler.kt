package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.DefaultPulsarEventPipelineHandler
import ai.platon.pulsar.crawl.event.CloseMaskLayerHandler
import ai.platon.pulsar.crawl.event.LoginHandler
import ai.platon.pulsar.ql.context.SQLContexts

open class SiMuCrawler {
    // general parameters
    val portalUrl = "https://dc.simuwang.com/"
    val args = "-i 30s -ii 30s -ol a[href~=product] -tl 10"
    // login parameters
    val loginUrl = portalUrl
    val username = System.getenv("EXOTIC_SIMUWANG_USERNAME")
    val password = System.getenv("EXOTIC_SIMUWANG_PASSWORD")
    val activateSelector = ".comp-login-b2"
    // mask layer handling
    val closeMaskLayerSelector = ".comp-alert-btn"

    val context = SQLContexts.create()
    val session = context.createSession()
    val eventHandler = DefaultPulsarEventPipelineHandler().also {
        val loginHandler = LoginHandler(loginUrl, username, password, activateSelector)
        it.loadEventPipelineHandler.onAfterBrowserLaunchPipeline.addLast(loginHandler)

        val closeMaskLayerHandler = CloseMaskLayerHandler(closeMaskLayerSelector)
        it.simulateEventPipelineHandler.onAfterCheckDOMStatePipeline.addLast(closeMaskLayerHandler)
    }
    val options = session.options(args, eventHandler)

    open fun crawl() {
        // load out pages
        val pages = session.loadOutPages(portalUrl, options)
        // parse to jsoup documents
        val documents = pages.map { session.parse(it) }
        // use the documents
        // ...
        // wait for all done
        context.await()
    }
}

fun main() = SiMuCrawler().crawl()
