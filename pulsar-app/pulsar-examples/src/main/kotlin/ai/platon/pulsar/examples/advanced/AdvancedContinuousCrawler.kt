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
        link.eventHandler.loadEventHandler.apply {
            onFilter.addLast { url ->
                url
            }
            onNormalize.addLast { url ->
                url
            }
            onBeforeLoad.addLast { url ->

            }
            onBeforeFetch.addLast { page ->

            }
            onBeforeBrowserLaunch.addLast {

            }
            onAfterBrowserLaunch.addLast { driver ->

            }
            onAfterFetch.addLast { page ->

            }
            onBeforeParse.addLast { page ->

            }
            onBeforeHtmlParse.addLast { page ->

            }
            onBeforeExtract.addLast { page ->

            }
            onAfterExtract.addLast { page: WebPage, document: FeaturedDocument ->

            }
            onAfterHtmlParse.addLast { page: WebPage, document: FeaturedDocument ->

            }
            onAfterParse.addLast { page ->

            }
            onAfterLoad.addLast { page ->

            }
        }

        link.eventHandler.simulateEventHandler.apply {
            onBeforeCheckDOMState.addLast { page: WebPage, driver: WebDriver ->

            }
            onAfterCheckDOMState.addLast { page: WebPage, driver: WebDriver ->

            }
            onBeforeComputeFeature.addLast { page: WebPage, driver: WebDriver ->

            }
            onAfterComputeFeature.addLast { page: WebPage, driver: WebDriver ->

            }
        }

        link.eventHandler.crawlEventHandler.apply {
            onFilter.addLast { url: UrlAware ->
                url
            }
            onNormalize.addLast { url: UrlAware ->
                url
            }
            onBeforeLoad.addLast { url: UrlAware ->

            }
            onAfterLoad.addLast { url, page ->

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
