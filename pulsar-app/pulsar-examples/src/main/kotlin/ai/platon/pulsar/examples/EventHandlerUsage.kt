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
                println("$seq. onWillLoad")
                null
            }
            onWillFetch.addLast { page ->
                println("$seq. onWillFetch")
            }
            onFetched.addLast { page ->
                println("$seq. onFetched")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. onWillParseHTMLDocument")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. onWillParseHTMLDocument")
            }
            onWillExtractData.addLast { page ->
                println("$seq. onWillExtractData")
            }
            onDataExtracted.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onDataExtracted")
            }
            onHTMLDocumentParsed.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. onHTMLDocumentParsed")
            }
            onParsed.addLast { page ->
                println("$seq. onParsed")
            }
            onLoaded.addLast { page ->
                println("$seq. onLoaded")
            }
        }

        browseEvent.apply {
            onWillLaunchBrowser.addLast { page ->
                println("$seq. onWillLaunchBrowser")
            }
            onBrowserLaunched.addLast { page, driver ->
                println("$seq. onBrowserLaunched")
            }
            onWillCheckDocumentState.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onWillCheckDocumentState")
            }
            onDocumentActuallyReady.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onDocumentActuallyReady")
            }
            onWillComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onWillComputeFeature")
            }
            onFeatureComputed.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onFeatureComputed")
            }
            onWillInteract.addLast { page, driver ->
                println("$seq. onWillInteract")
            }
            onDidInteract.addLast { page, driver ->
                println("$seq. onWillInteract")
            }
            onWillStopTab.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onWillStopTab")
            }
            onTabStopped.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. onTabStopped")
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
