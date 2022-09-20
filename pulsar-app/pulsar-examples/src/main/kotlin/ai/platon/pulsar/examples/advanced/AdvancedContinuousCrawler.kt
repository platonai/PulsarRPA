package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

fun main() {
    val hyperlinkCreator = { url: String ->
        val link = ListenableHyperlink(url)
        link.event.loadEvent.apply {
            onFilter.addLast { url ->
                url
            }
            onNormalize.addLast { url ->
                url
            }
            onWillLoad.addLast { url ->
                url
            }
            onWillFetch.addLast { page ->

            }
            onFetched.addLast { page ->

            }
            onWillParseHTMLDocument.addLast { page ->

            }
            onWillParseHTMLDocument.addLast { page ->

            }
            onWillExtractData.addLast { page ->

            }
            onDataExtracted.addLast { page: WebPage, document: FeaturedDocument ->

            }
            onHTMLDocumentParsed.addLast { page: WebPage, document: FeaturedDocument ->

            }
            onParsed.addLast { page ->

            }
            onLoaded.addLast { page ->

            }
        }

        link.event.browseEvent.apply {
            onWillLaunchBrowser.addLast { page ->

            }
            onBrowserLaunched.addLast { page, driver ->

            }
            onWillCheckDocumentState.addLast { page: WebPage, driver: WebDriver ->

            }
            onDocumentActuallyReady.addLast { page: WebPage, driver: WebDriver ->

            }
            onWillComputeFeature.addLast { page: WebPage, driver: WebDriver ->

            }
            onFeatureComputed.addLast { page: WebPage, driver: WebDriver ->

            }
        }

        link.event.crawlEvent.apply {
            onFilter.addLast { url: UrlAware ->
                url
            }
            onNormalize.addLast { url: UrlAware ->
                url
            }
            onWillLoad.addLast { url: UrlAware ->
                url
            }
            onLoaded.addLast { url, page ->

            }
        }
        link
    }

    // load urls from resource, and convert them into listenable hyperlinks
    val urls = LinkExtractors.fromResource("seeds.txt").map { hyperlinkCreator("$it -refresh") }
    // create a custom context
    val context = PulsarContexts.create("classpath:pulsar-beans/app-context.xml")
    // submit a batch of urls
    context.submitAll(urls)
    // feel free to submit millions of urls here
    // ...
    // wait until all done
    context.await()
}
