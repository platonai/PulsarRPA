package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

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

interface CrawlEvent {
    val onFilter: UrlAwareEventFilter

    val onNormalize: UrlAwareEventFilter

    val onWillLoad: UrlAwareEventHandler

    val onLoad: UrlAwareEventHandler

    val onLoaded: UrlAwareWebPageEventHandler

    fun combine(other: CrawlEvent): CrawlEvent
}

/**
 * The simulate events.
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

/**
 * Manage all events of a web page life cycle. The page events are visible to the end users.
 * */
interface PageEvent {
    val loadEvent: LoadEvent
    val simulateEvent: SimulateEvent
    val crawlEvent: CrawlEvent

    fun combine(other: PageEvent): PageEvent
}
