package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import kotlin.random.Random

interface LoadEvent {
    val onFilter: UrlFilterEventHandler

    val onNormalize: UrlFilterEventHandler

    val onWillLoad: UrlEventHandler

    val onWillFetch: WebPageEventHandler

    val onWillLaunchBrowser: WebPageEventHandler

    val onBrowserLaunched: WebPageWebDriverEventHandler

    val onFetched: WebPageEventHandler

    val onWillParse: WebPageEventHandler

    val onWillParseHTMLDocument: WebPageEventHandler

    val onWillExtractData: WebPageEventHandler

    val onDataExtracted: HTMLDocumentEventHandler

    val onHTMLDocumentParsed: HTMLDocumentEventHandler

    val onParsed: WebPageEventHandler

    val onLoaded: WebPageEventHandler

    fun combine(other: LoadEvent): LoadEvent
}

abstract class AbstractLoadEvent(
    override val onFilter: UrlFilterEventHandler = UrlFilterEventHandler(),
    override val onNormalize: UrlFilterEventHandler = UrlFilterEventHandler(),
    override val onWillLoad: UrlEventHandler = UrlEventHandler(),
    override val onWillFetch: WebPageEventHandler = WebPageEventHandler(),
    override val onWillLaunchBrowser: WebPageEventHandler = WebPageEventHandler(),
    override val onBrowserLaunched: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFetched: WebPageEventHandler = WebPageEventHandler(),
    override val onWillParse: WebPageEventHandler = WebPageEventHandler(),
    override val onWillParseHTMLDocument: WebPageEventHandler = WebPageEventHandler(),
    override val onWillExtractData: WebPageEventHandler = WebPageEventHandler(),
    override val onDataExtracted: HTMLDocumentEventHandler = HTMLDocumentEventHandler(),
    override val onHTMLDocumentParsed: HTMLDocumentEventHandler = HTMLDocumentEventHandler(),
    override val onParsed: WebPageEventHandler = WebPageEventHandler(),
    override val onLoaded: WebPageEventHandler = WebPageEventHandler()
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
    override val onBrowserLaunched = WebPageWebDriverEventHandler()
        .addLast { page, driver ->
            rpa.warnUpBrowser(page, driver)
        }
}

interface CrawlEvent {
    val onFilter: UrlAwareEventFilter

    val onNormalize: UrlAwareEventFilter

    val onWillLoad: UrlAwareEventHandler

    val onLoad: UrlAwareEventHandler

    val onLoaded: UrlAwareWebPageEventHandler

    fun combine(other: CrawlEvent): CrawlEvent
}

abstract class AbstractCrawlEvent(
    override val onFilter: UrlAwareEventFilter = UrlAwareEventFilter(),
    override val onNormalize: UrlAwareEventFilter = UrlAwareEventFilter(),
    override val onWillLoad: UrlAwareEventHandler = UrlAwareEventHandler(),
    override val onLoad: UrlAwareEventHandler = UrlAwareEventHandler(),
    override val onLoaded: UrlAwareWebPageEventHandler = UrlAwareWebPageEventHandler()
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
    val onWillFetch: WebPageWebDriverEventHandler
    val onFetched: WebPageWebDriverEventHandler

    val onWillNavigate: WebPageWebDriverEventHandler
    val onNavigated: WebPageWebDriverEventHandler

    val onWillCheckDOMState: WebPageWebDriverEventHandler
    val onDOMStateChecked: WebPageWebDriverEventHandler

    val onWillComputeFeature: WebPageWebDriverEventHandler
    val onFeatureComputed: WebPageWebDriverEventHandler

    val onWillInteract: WebPageWebDriverEventHandler
    val onDidInteract: WebPageWebDriverEventHandler

    val onWillStopTab: WebPageWebDriverEventHandler
    val onTabStopped: WebPageWebDriverEventHandler

    fun combine(other: SimulateEvent): SimulateEvent
}

abstract class AbstractSimulateEvent(
    override val onWillFetch: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFetched: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillNavigate: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onNavigated: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillCheckDOMState: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onDOMStateChecked: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillComputeFeature: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFeatureComputed: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillInteract: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onDidInteract: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillStopTab: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onTabStopped: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler()
): SimulateEvent {

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

    override val onWillFetch = WebPageWebDriverEventHandler().addLast { page, driver ->
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
    val onSniffPageCategory: PageDatumEventHandler
    val onCheckHtmlIntegrity: PageDatumEventHandler

    fun combine(other: EmulateEvent): EmulateEvent
}

abstract class AbstractEmulateEvent(
    override val onSniffPageCategory: PageDatumEventHandler = PageDatumEventHandler(),
    override val onCheckHtmlIntegrity: PageDatumEventHandler = PageDatumEventHandler(),
): EmulateEvent {
    override fun combine(other: EmulateEvent): EmulateEvent {
        onSniffPageCategory.addLast(other.onSniffPageCategory)
        onCheckHtmlIntegrity.addLast(other.onCheckHtmlIntegrity)
        return this
    }
}

class DefaultEmulateEvent: AbstractEmulateEvent() {
    override val onSniffPageCategory: PageDatumEventHandler = PageDatumEventHandler()
    override val onCheckHtmlIntegrity: PageDatumEventHandler = PageDatumEventHandler()
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
