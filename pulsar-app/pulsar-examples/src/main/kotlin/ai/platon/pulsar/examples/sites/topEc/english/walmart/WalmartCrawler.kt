package ai.platon.pulsar.examples.sites.topEc.english.walmart

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.rpa.DefaultBrowseRPA
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession

class WalmartRPA(
    val session: PulsarSession = PulsarContexts.createSession()
): DefaultBrowseRPA() {

    private val context get() = session.context

    fun options(args: String): LoadOptions {
        val options = session.options(args)

        val le = options.event.loadEvent
        le.onHTMLDocumentParsed.addLast { _, document: FeaturedDocument ->
            // use the document
        }

        val be = options.itemEvent.browseEvent
        be.onBrowserLaunched.addLast { page, driver ->
            warnUpBrowser(page, driver)
        }
        be.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
            waitForPreviousPage(page, driver)
        }
        be.onWillCheckDocumentState.addLast { page, driver ->
            // driver.waitForSelector("body h1[itemprop=name]")
        }

        return options
    }

    override suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
        visit("https://www.walmart.com/", driver)
        driver.waitForSelector("form[role=search]")
        super.warnUpBrowser(page, driver)
    }
}

class WalmartCrawler(private val session: PulsarSession = PulsarContexts.createSession()) {
    private val context = session.context

    private val rpa = WalmartRPA(session)

    private val parseHandler = { _: WebPage, document: FeaturedDocument -> }

    fun scrapeOutPages(portalUrl: String, args: String) {
        val options = rpa.options(args)
        val itemOptions = options.createItemOptions()
        val itemArgs = "$itemOptions -i 1d -requireSize 300000 -ignoreFailure"

        val document = session.loadDocument(portalUrl, options)

        val links = document.selectHyperlinks(options.outLinkSelector)
            .asSequence()
            .take(10000)
            .distinct()
            .map { ParsableHyperlink("$it $itemArgs", parseHandler) }
            .onEach {
                it.referrer = portalUrl
                it.event.chain(options.itemEvent)
            }
            .toList()
            .shuffled()

        context.submitAll(links).await()
    }
}

fun main(argv: Array<String>) {
    val portalUrl = ResourceLoader.readAllLines("seeds.walmart.txt")
        .filter { UrlUtils.isStandard(it) }
        .shuffled()
        .first()

    val args = "-i 1s -requireSize 250000 -ignoreFailure"
    WalmartCrawler().scrapeOutPages(portalUrl, args)
}
