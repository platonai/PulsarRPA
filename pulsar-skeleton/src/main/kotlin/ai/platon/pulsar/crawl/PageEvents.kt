package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Manage all events in crawl phrase of the webpage lifecycle.
 * */
interface CrawlEvent {
    @Deprecated("No need to filter in a crawler")
    val onFilter: UrlAwareEventFilter

    @Deprecated("No need to normalize in a crawler")
    val onNormalize: UrlAwareEventFilter

    val onWillLoad: UrlAwareEventHandler

    val onLoad: UrlAwareEventHandler

    val onLoaded: UrlAwareWebPageEventHandler

    fun chain(other: CrawlEvent): CrawlEvent
}

/**
 * Manage all events in the load phrase of the webpage lifecycle.
 * */
interface LoadEvent {
    val onFilter: UrlFilterEventHandler

    val onNormalize: UrlFilterEventHandler

    val onWillLoad: UrlEventHandler

    val onWillFetch: WebPageEventHandler

    val onFetched: WebPageEventHandler

    val onWillParse: WebPageEventHandler

    val onWillParseHTMLDocument: WebPageEventHandler

    val onWillExtractData: WebPageEventHandler

    val onDataExtracted: HTMLDocumentEventHandler

    val onHTMLDocumentParsed: HTMLDocumentEventHandler

    val onParsed: WebPageEventHandler

    val onLoaded: WebPageEventHandler

    fun chain(other: LoadEvent): LoadEvent
}

/**
 * Manage all events in the browse phrase of the webpage lifecycle.
 * */
interface BrowseEvent {
    val onWillLaunchBrowser: WebPageEventHandler
    val onBrowserLaunched: WebPageWebDriverEventHandler

    val onWillFetch: WebPageWebDriverEventHandler
    val onFetched: WebPageWebDriverEventHandler

    val onWillNavigate: WebPageWebDriverEventHandler
    val onNavigated: WebPageWebDriverEventHandler

    val onWillInteract: WebPageWebDriverEventHandler
    val onDidInteract: WebPageWebDriverEventHandler

    @Deprecated("Inappropriate name", ReplaceWith("onWillCheckDocumentState"))
    val onWillCheckDOMState: WebPageWebDriverEventHandler get() = onWillCheckDocumentState
    val onWillCheckDocumentState: WebPageWebDriverEventHandler
    @Deprecated("Inappropriate name", ReplaceWith("onDocumentActuallyReady"))
    val onDOMStateChecked: WebPageWebDriverEventHandler get() = onDocumentActuallyReady
    /**
     * Fire when the document is actually ready. The document state is checked(computed)
     * using an algorithm in javascript.
     * */
    val onDocumentActuallyReady: WebPageWebDriverEventHandler

    val onWillComputeFeature: WebPageWebDriverEventHandler
    val onFeatureComputed: WebPageWebDriverEventHandler

    val onWillStopTab: WebPageWebDriverEventHandler
    val onTabStopped: WebPageWebDriverEventHandler

    fun chain(other: BrowseEvent): BrowseEvent
}

/**
 * Manage all events in the webpage lifecycle.
 *
 * The events are separated into three groups:
 * 1. [LoadEvent] in load phrase
 * 2. [BrowseEvent] in browse phrase
 * 3. [CrawlEvent] in crawl phrase
 * */
interface PageEvent {
    val loadEvent: LoadEvent
    val browseEvent: BrowseEvent
    val crawlEvent: CrawlEvent

    fun chain(other: PageEvent): PageEvent
}
