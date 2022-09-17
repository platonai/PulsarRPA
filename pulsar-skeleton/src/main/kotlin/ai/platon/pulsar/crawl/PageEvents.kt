package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import kotlin.random.Random

interface LoadEvent {
    val onFilter: UrlFilterPipeline

    val onNormalize: UrlFilterPipeline

    val onWillLoad: UrlHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillLoad"))
    val onBeforeLoad: UrlHandlerPipeline get() = onWillLoad

    val onWillFetch: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillFetch"))
    val onBeforeFetch: WebPageHandlerPipeline get() = onWillFetch

    val onWillLaunchBrowser: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillLaunchBrowser"))
    val onBeforeBrowserLaunch: WebPageHandlerPipeline get() = onWillLaunchBrowser

    val onBrowserLaunched: WebPageWebDriverHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onBrowserLaunched"))
    val onAfterBrowserLaunch: WebPageWebDriverHandlerPipeline get() = onBrowserLaunched

    val onFetched: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onFetched"))
    val onAfterFetch: WebPageHandlerPipeline get() = onFetched

    val onWillParse: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillParseHTMLDocument"))
    val onBeforeParse: WebPageHandlerPipeline get() = onWillParse

    val onWillParseHTMLDocument: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillParseHTMLDocument"))
    val onBeforeHtmlParse: WebPageHandlerPipeline get() = onWillParseHTMLDocument

    val onWillExtractData: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onWillExtract"))
    val onBeforeExtractData: WebPageHandlerPipeline get() = onWillExtractData

    val onDataExtracted: HTMLDocumentHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onExtracted"))
    val onAfterExtract: HTMLDocumentHandlerPipeline get() = onDataExtracted

    val onHTMLDocumentParsed: HTMLDocumentHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onHTMLDocumentParsed"))
    val onAfterHtmlParse: HTMLDocumentHandlerPipeline get() = onHTMLDocumentParsed

    val onParsed: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onParsed"))
    val onAfterParse: WebPageHandlerPipeline get() = onParsed

    val onLoaded: WebPageHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onLoaded"))
    val onAfterLoad: WebPageHandlerPipeline get() = onLoaded

    fun combine(other: LoadEvent): LoadEvent
}

abstract class AbstractLoadEvent(
    override val onFilter: UrlFilterPipeline = UrlFilterPipeline(),
    override val onNormalize: UrlFilterPipeline = UrlFilterPipeline(),
    override val onWillLoad: UrlHandlerPipeline = UrlHandlerPipeline(),
    override val onWillFetch: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onWillLaunchBrowser: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBrowserLaunched: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline(),
    override val onFetched: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onWillParse: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onWillParseHTMLDocument: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onWillExtractData: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onDataExtracted: HTMLDocumentHandlerPipeline = HTMLDocumentHandlerPipeline(),
    override val onHTMLDocumentParsed: HTMLDocumentHandlerPipeline = HTMLDocumentHandlerPipeline(),
    override val onParsed: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onLoaded: WebPageHandlerPipeline = WebPageHandlerPipeline()
): LoadEvent {

    override fun combine(other: LoadEvent): AbstractLoadEvent {
        onFilter.addLast(other.onFilter)
        onNormalize.addLast(other.onNormalize)
        onWillLoad.addLast(other.onWillLoad)
        onWillFetch.addLast(other.onWillFetch)
        onWillLaunchBrowser.addLast(other.onWillLaunchBrowser)
        onBrowserLaunched.addLast(other.onBrowserLaunched)
        onFetched.addLast(other.onFetched)
        onWillParse.addLast(other.onWillParse)
        onWillParseHTMLDocument.addLast(other.onWillParseHTMLDocument)
        onWillExtractData.addLast(other.onWillExtractData)
        onDataExtracted.addLast(other.onDataExtracted)
        onHTMLDocumentParsed.addLast(other.onHTMLDocumentParsed)
        onParsed.addLast(other.onParsed)
        onLoaded.addLast(other.onLoaded)

        return this
    }
}

open class DefaultLoadEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractLoadEvent() {
    override val onBrowserLaunched = WebPageWebDriverHandlerPipeline()
        .addLast { page, driver ->
            rpa.warnUpBrowser(page, driver)
        }
}

interface CrawlEvent {
    val onFilter: UrlAwareFilterPipeline

    val onNormalize: UrlAwareFilterPipeline

    val onWillLoad: UrlAwareHandlerPipeline

    val onLoad: UrlAwareHandlerPipeline

    val onLoaded: UrlAwareWebPageHandlerPipeline

    fun combine(other: CrawlEvent): CrawlEvent
}

abstract class AbstractCrawlEvent(
    override val onFilter: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onNormalize: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onWillLoad: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onLoad: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onLoaded: UrlAwareWebPageHandlerPipeline = UrlAwareWebPageHandlerPipeline()
): CrawlEvent {
    override fun combine(other: CrawlEvent): CrawlEvent {
        onFilter.addLast(other.onFilter)
        onNormalize.addLast(other.onNormalize)
        onWillLoad.addLast(other.onWillLoad)
        onLoad.addLast(other.onLoad)
        onLoaded.addLast(other.onLoaded)
        return this
    }
}

class DefaultCrawlEvent: AbstractCrawlEvent()

/**
 * @see [EmulateEvent].
 *
 * About emulate, simulate, mimic and imitate:
 * 1. Emulate is usually used with someone as an object.
 * 2. Simulate has the idea of copying something so that the copy pretends to be the original thing.
 * 3. Mimic, a person who imitate mannerisms of others.
 * 4. Imitate is the most general of the four words, can be used in all the three senses.
 * */
interface SimulateEvent {
    @Deprecated("Old fashioned name", ReplaceWith("onWillFetch"))
    val onBeforeFetch: WebPageWebDriverHandlerPipeline get() = onWillFetch
    val onWillFetch: WebPageWebDriverHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onFetched"))
    val onAfterFetch: WebPageWebDriverHandlerPipeline get() = onFetched
    val onFetched: WebPageWebDriverHandlerPipeline

    @Deprecated("Old fashioned name", ReplaceWith("onWillNavigate"))
    val onBeforeNavigate: WebPageWebDriverHandlerPipeline get() = onWillNavigate
    val onWillNavigate: WebPageWebDriverHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onNavigated"))
    val onAfterNavigate: WebPageWebDriverHandlerPipeline get() = onNavigated
    val onNavigated: WebPageWebDriverHandlerPipeline

    @Deprecated("Old fashioned name", ReplaceWith("onWillCheckDOMState"))
    val onBeforeCheckDOMState: WebPageWebDriverHandlerPipeline get() = onWillCheckDOMState
    val onWillCheckDOMState: WebPageWebDriverHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onDOMStateChecked"))
    val onAfterCheckDOMState: WebPageWebDriverHandlerPipeline get() = onDOMStateChecked
    val onDOMStateChecked: WebPageWebDriverHandlerPipeline

    @Deprecated("Old fashioned name", ReplaceWith("onWillComputeFeature"))
    val onBeforeComputeFeature: WebPageWebDriverHandlerPipeline get() = onWillComputeFeature
    val onWillComputeFeature: WebPageWebDriverHandlerPipeline
    @Deprecated("Old fashioned name", ReplaceWith("onFeatureComputed"))
    val onAfterComputeFeature: WebPageWebDriverHandlerPipeline get() = onFeatureComputed
    val onFeatureComputed: WebPageWebDriverHandlerPipeline

    val onWillInteract: WebPageWebDriverHandlerPipeline
    val onDidInteract: WebPageWebDriverHandlerPipeline

    val onWillStopTab: WebPageWebDriverHandlerPipeline
    val onTabStopped: WebPageWebDriverHandlerPipeline

    fun combine(other: SimulateEvent): SimulateEvent
}

abstract class AbstractSimulateEvent: SimulateEvent {
    open val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(500)
            "type" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    open var verbose = false

    protected suspend fun smartDelay() = delay(delayPolicy(""))

    protected suspend fun smartDelay(type: String) = delay(delayPolicy(type))

    override val onWillFetch: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onFetched: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override val onWillNavigate: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onNavigated: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override val onWillCheckDOMState: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onDOMStateChecked: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override val onWillComputeFeature: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onFeatureComputed: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override val onWillInteract: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onDidInteract: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override val onWillStopTab: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onTabStopped: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()

    override fun combine(other: SimulateEvent): SimulateEvent {
        onWillFetch.addLast(other.onWillFetch)
        onFetched.addLast(other.onFetched)

        onWillNavigate.addLast(other.onWillNavigate)
        onNavigated.addLast(other.onNavigated)

        onWillCheckDOMState.addLast(other.onWillCheckDOMState)
        onDOMStateChecked.addLast(other.onDOMStateChecked)
        onWillComputeFeature.addLast(other.onWillComputeFeature)
        onFeatureComputed.addLast(other.onFeatureComputed)

        onWillInteract.addLast(other.onWillInteract)
        onDidInteract.addLast(other.onDidInteract)
        onWillStopTab.addLast(other.onWillStopTab)
        onTabStopped.addLast(other.onTabStopped)

        return this
    }
}

class DefaultSimulateEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractSimulateEvent() {

    override val onWillFetch = WebPageWebDriverHandlerPipeline().addLast { page, driver ->
        rpa.waitForReferrer(page, driver)
        rpa.waitForPreviousPage(page, driver)
    }
}

/**
 * @see [SimulateEvent]
 *
 * About emulate, simulate, mimic and imitate:
 * 1. Emulate is usually used with someone as an object.
 * 2. Simulate has the idea of copying something so that the copy pretends to be the original thing.
 * 3. Mimic, a person who imitate mannerisms of others.
 * 4. Imitate is the most general of the four words, can be used in all the three senses.
 * */
interface EmulateEvent {
    val onSniffPageCategory: PageDatumHandlerPipeline
    val onCheckHtmlIntegrity: PageDatumHandlerPipeline

    fun combine(other: EmulateEvent): EmulateEvent
}

abstract class AbstractEmulateEvent(
    override val onSniffPageCategory: PageDatumHandlerPipeline = PageDatumHandlerPipeline(),
    override val onCheckHtmlIntegrity: PageDatumHandlerPipeline = PageDatumHandlerPipeline(),
): EmulateEvent {
    override fun combine(other: EmulateEvent): EmulateEvent {
        onSniffPageCategory.addLast(other.onSniffPageCategory)
        onCheckHtmlIntegrity.addLast(other.onCheckHtmlIntegrity)
        return this
    }
}

class DefaultEmulateEvent: AbstractEmulateEvent() {
    override val onSniffPageCategory: PageDatumHandlerPipeline = PageDatumHandlerPipeline()
    override val onCheckHtmlIntegrity: PageDatumHandlerPipeline = PageDatumHandlerPipeline()
}

/**
 * Manage all events of a web page life cycle. The page events are visible to the end users.
 * */
interface PageEvent {
    val loadEvent: LoadEvent
    val simulateEvent: SimulateEvent
    val crawlEvent: CrawlEvent

    fun combine(other: PageEvent): PageEvent
}

abstract class AbstractPageEvent(
    override val loadEvent: LoadEvent,
    override val simulateEvent: SimulateEvent,
    override val crawlEvent: CrawlEvent
): PageEvent {

    override fun combine(other: PageEvent): PageEvent {
        loadEvent.combine(other.loadEvent)
        simulateEvent.combine(other.simulateEvent)
        crawlEvent.combine(other.crawlEvent)
        return this
    }
}

open class DefaultPageEvent(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    simulateEvent: SimulateEvent = DefaultSimulateEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
): AbstractPageEvent(loadEvent, simulateEvent, crawlEvent) {

}

open class PageEventTemplate(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    simulateEvent: SimulateEvent = DefaultSimulateEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
): AbstractPageEvent(loadEvent, simulateEvent, crawlEvent) {
    init {
        loadEvent.apply {
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
            onWillLaunchBrowser.addLast { page ->

            }
            onBrowserLaunched.addLast { page, driver ->

            }
            onFetched.addLast { page ->

            }
            onWillParse.addLast { page ->

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

        simulateEvent.apply {
            onWillNavigate.addLast { page, driver ->
            }

            onNavigated.addLast { page, driver ->
            }

            onWillCheckDOMState.addLast { page, driver ->
            }

            onDOMStateChecked.addLast { page, driver ->
            }

            onWillComputeFeature.addLast { page, driver ->
            }

            onFeatureComputed.addLast { page, driver ->
            }

            onWillInteract.addLast { page, driver ->
            }

            onDidInteract.addLast { page, driver ->
            }
        }

        crawlEvent.apply {
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
                url
            }
        }
    }
}
