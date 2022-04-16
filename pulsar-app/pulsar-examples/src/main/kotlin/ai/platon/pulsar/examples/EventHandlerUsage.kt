package ai.platon.pulsar.examples

import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.DefaultPulsarEventHandler
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.atomic.AtomicInteger

class PrintFlowEventHandler: DefaultPulsarEventHandler() {
    private val sequencer = AtomicInteger()
    private val seq get() = sequencer.incrementAndGet()

    init {
        loadEventHandler.apply {
            onFilter.addLast { url ->
                println("$seq. onFilter")
                url
            }
            onNormalize.addLast { url ->
                println("$seq. onNormalize")
                url
            }
            onBeforeLoad.addLast { url ->
                println("$seq. onBeforeLoad")
            }
            onBeforeFetch.addLast { page ->
                println("$seq. onBeforeFetch")
            }
            onBeforeBrowserLaunch.addLast {
                println("$seq. onBeforeBrowserLaunch")
            }
            onAfterBrowserLaunch.addLast { driver ->
                println("$seq. onAfterBrowserLaunch")
            }
            onAfterFetch.addLast { page ->
                println("$seq. onAfterFetch")
            }
            onBeforeParse.addLast { page ->
                println("$seq. onBeforeParse")
            }
            onBeforeHtmlParse.addLast { page ->
                println("$seq. onBeforeHtmlParse")
            }
            onBeforeExtract.addLast { page ->
                println("$seq. onBeforeExtract")
            }
            onAfterExtract.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onAfterExtract")
            }
            onAfterHtmlParse.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onAfterHtmlParse")
            }
            onAfterParse.addLast { page ->
                println("$seq. onAfterParse")
            }
            onAfterLoad.addLast { page ->
                println("$seq. onAfterLoad")
            }
        }

        simulateEventHandler.apply {
            onBeforeCheckDOMState.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onBeforeCheckDOMState")
            }
            onAfterCheckDOMState.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onAfterCheckDOMState")
            }
            onBeforeComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onBeforeComputeFeature")
            }
            onAfterComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onAfterComputeFeature")
            }
        }

        crawlEventHandler.apply {
            onFilter.addLast { url: UrlAware ->
                println("$seq. onFilter")
                url
            }
            onNormalize.addLast { url: UrlAware ->
                println("$seq. onNormalize")
                url
            }
            onBeforeLoad.addLast { url: UrlAware ->
                println("$seq. onBeforeLoad")
            }
            onAfterLoad.addLast { url, page ->
                println("$seq. onAfterLoad")
            }
        }
    }
}

/**
 * Demonstrate how to use event handlers.
 * */
fun main() {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val session = PulsarContexts.createSession()
    val link = ListenableHyperlink(
        portalUrl, args = "-refresh -parse", eventHandler = PrintFlowEventHandler())

    // submit the link to the fetch pool.
    session.submit(link)

    // wait until all done.
    PulsarContexts.await()
}
