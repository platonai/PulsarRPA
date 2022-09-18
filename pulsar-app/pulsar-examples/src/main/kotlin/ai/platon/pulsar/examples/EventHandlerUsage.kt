package ai.platon.pulsar.examples

import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.event.impl.DefaultPageEvent
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.atomic.AtomicInteger

class PrintFlowEvent: DefaultPageEvent() {
    private val sequencer = AtomicInteger()
    private val seq get() = sequencer.incrementAndGet()

    init {
        loadEvent.apply {
            onFilter.addLast { url ->
                println("$seq. onFilter")
                url
            }
            onNormalize.addLast { url ->
                println("$seq. onNormalize")
                url
            }
            onWillLoad.addLast { url ->
                println("$seq. onBeforeLoad")
                null
            }
            onWillFetch.addLast { page ->
                println("$seq. onBeforeFetch")
            }
            onWillLaunchBrowser.addLast { page ->
                println("$seq. onBeforeBrowserLaunch")
            }
            onBrowserLaunched.addLast { page, driver ->
                println("$seq. onAfterBrowserLaunch")
            }
            onFetched.addLast { page ->
                println("$seq. onAfterFetch")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. onBeforeParse")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. onBeforeHtmlParse")
            }
            onWillExtractData.addLast { page ->
                println("$seq. onBeforeExtract")
            }
            onDataExtracted.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onAfterExtract")
            }
            onHTMLDocumentParsed.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onAfterHtmlParse")
            }
            onParsed.addLast { page ->
                println("$seq. onAfterParse")
            }
            onLoaded.addLast { page ->
                println("$seq. onAfterLoad")
            }
        }

        simulateEvent.apply {
            onWillCheckDOMState.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onBeforeCheckDOMState")
            }
            onDOMStateChecked.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onAfterCheckDOMState")
            }
            onWillComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onBeforeComputeFeature")
            }
            onFeatureComputed.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onAfterComputeFeature")
            }
        }

        crawlEvent.apply {
            onFilter.addLast { url: UrlAware ->
                println("$seq. onFilter")
                url
            }
            onNormalize.addLast { url: UrlAware ->
                println("$seq. onNormalize")
                url
            }
            onWillLoad.addLast { url: UrlAware ->
                println("$seq. onBeforeLoad")
                url
            }
            onLoaded.addLast { url, page ->
                println("$seq. onAfterLoad")
            }
        }
    }
}

/**
 * Demonstrates how to use event handlers.
 * */
fun main() {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val session = PulsarContexts.createSession()
    val link = ListenableHyperlink(
        portalUrl, args = "-refresh -parse", event = PrintFlowEvent())

    // submit the link to the fetch pool.
    session.submit(link)

    // wait until all done.
    PulsarContexts.await()
}
