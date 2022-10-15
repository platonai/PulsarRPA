package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Manage all events in crawl phase of the webpage lifecycle.
 * */
interface CrawlEvent {
    @Deprecated("Url filtering should not be in PageEvent")
    val onFilter: UrlAwareEventFilter

    @Deprecated("No need to normalize in a crawler")
    val onNormalize: UrlAwareEventFilter

    val onWillLoad: UrlAwareEventHandler

    val onLoad: UrlAwareEventHandler

    val onLoaded: UrlAwareWebPageEventHandler

    fun chain(other: CrawlEvent): CrawlEvent
}

/**
 * Manage all events in the load phase of the webpage lifecycle.
 * */
interface LoadEvent {
    @Deprecated("Url filtering should not be in load phase, crawl phase is better")
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
 * Manage all events in the browse phase of the webpage lifecycle.
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
 * Manage all events in the lifecycle in a webpage.
 *
 * The events are fall into three groups:
 *
 * 1. [LoadEvent] in load phase
 * 2. [BrowseEvent] in browse phase
 * 3. [CrawlEvent] in crawl phase
 * */
interface PageEvent {
    /**
     * The load phase event handler
     * */
    val loadEvent: LoadEvent
    /**
     * The browse phase event handler
     * */
    val browseEvent: BrowseEvent
    /**
     * The crawl phase event handler
     * */
    val crawlEvent: CrawlEvent
    /**
     * Chain the other page event handler to the tail of this one.
     * */
    fun chain(other: PageEvent): PageEvent
}
