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
                println("1. onBeforeLoad | $url")
            }
            onBeforeFetch.addLast { page ->
                println("2. onBeforeFetch | $url")
            }
            onBeforeBrowserLaunch.addLast {
                println("3. onBeforeBrowserLaunch | $url")
            }
            onAfterBrowserLaunch.addLast { driver ->
                println("4. onAfterBrowserLaunch | $url")
            }
            onAfterFetch.addLast { page ->
                println("5. onAfterFetch | $url")
            }
            onBeforeParse.addLast { page ->
                println("6. onBeforeParse | $url")
            }
            onBeforeHtmlParse.addLast { page ->
                println("7. onBeforeHtmlParse | $url")
            }
            onBeforeExtract.addLast { page ->
                println("8. onBeforeHtmlParse | $url")
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

    val urls = LinkExtractors.fromResource("seeds.txt")
        .map { hyperlinkCreator("$it -refresh") }
    val context = PulsarContexts.create().submitAll(urls)
    // feel free to submit millions of urls here
    // ...
    context.await()
}
