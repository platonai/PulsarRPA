package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Manage all events in crawl phase of the webpage lifecycle.
 * */
interface CrawlEvent {

    val onWillLoad: UrlAwareEventHandler

    val onLoad: UrlAwareEventHandler

    val onLoaded: UrlAwareWebPageEventHandler

    fun chain(other: CrawlEvent): CrawlEvent
}

/**
 * Manage all events in the load phase of the webpage lifecycle.
 * */
interface LoadEvent {

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

    val onWillCheckDocumentState: WebPageWebDriverEventHandler
    /**
     * Fire when the document is actually ready. The document state is checked(computed)
     * using an algorithm in javascript.
     * */
    val onDocumentActuallyReady: WebPageWebDriverEventHandler

    val onWillScroll: WebPageWebDriverEventHandler
    val onDidScroll: WebPageWebDriverEventHandler

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
    var loadEvent: LoadEvent
    /**
     * The browse phase event handler
     * */
    var browseEvent: BrowseEvent
    /**
     * The crawl phase event handler
     * */
    var crawlEvent: CrawlEvent
    /**
     * Chain the other page event handler to the tail of this one.
     * */
    fun chain(other: PageEvent): PageEvent
}
