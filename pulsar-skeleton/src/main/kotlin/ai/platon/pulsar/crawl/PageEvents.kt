package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Event handlers for the crawl phase of the webpage lifecycle.
 * */
interface CrawlEvent {
    @Deprecated("Url filtering should not be in PageEvent")
    val onFilter: UrlAwareEventFilter

    @Deprecated("No need to normalize in a crawler")
    val onNormalize: UrlAwareEventFilter

    /**
     * Fire when the url is about to be loaded.
     * */
    val onWillLoad: UrlAwareEventHandler

    /**
     * Fire to load the url.
     * */
    val onLoad: UrlAwareEventHandler

    /**
     * Fire when the url is loaded.
     * */
    val onLoaded: UrlAwareWebPageEventHandler

    /**
     * Chain the other crawl event handler to the tail of this one.
     * */
    fun chain(other: CrawlEvent): CrawlEvent
}

/**
 * Event handlers for the loading phase of the webpage lifecycle.
 * */
interface LoadEvent {
    @Deprecated("Url filtering should not be in load phase, crawl phase is better")
    val onFilter: UrlFilterEventHandler

    /**
     * Fire when the url is about to be normalized.
     * The event handlers normalize the url, for example, remove the fragment part of the url.
     * */
    val onNormalize: UrlFilterEventHandler

    /**
     * Fire when the url is about to be loaded.
     * */
    val onWillLoad: UrlEventHandler

    /**
     * Fire when the url is about to be fetched.
     * */
    val onWillFetch: WebPageEventHandler

    /**
     * Fire when the url is fetched.
     * */
    val onFetched: WebPageEventHandler

    /**
     * Fire when the webpage is about to be parsed.
     * */
    val onWillParse: WebPageEventHandler

    /**
     * Fire when the html document is about to be parsed.
     * */
    val onWillParseHTMLDocument: WebPageEventHandler

    /**
     * Fire when the data is about to be extracted.
     * */
    val onWillExtractData: WebPageEventHandler

    /**
     * Fire when the data is extracted.
     * */
    val onDataExtracted: HTMLDocumentEventHandler

    /**
     * Fire when the html document is parsed.
     * */
    val onHTMLDocumentParsed: HTMLDocumentEventHandler

    /**
     * Fire when the webpage is parsed.
     * */
    val onParsed: WebPageEventHandler

    /**
     * Fire when the webpage is loaded.
     * */
    val onLoaded: WebPageEventHandler

    /**
     * Chain the other load event handler to the tail of this one.
     * */
    fun chain(other: LoadEvent): LoadEvent
}

/**
 * Event handlers for the browsing phase of the webpage lifecycle.
 * */
interface BrowseEvent {
    /**
     * Fire when the browser is about to be launched.
     * */
    val onWillLaunchBrowser: WebPageEventHandler
    /**
     * Fire when the browser is launched.
     * */
    val onBrowserLaunched: WebPageWebDriverEventHandler

    /**
     * Fire when the url is about to be fetched.
     * */
    val onWillFetch: WebPageWebDriverEventHandler
    /**
     * Fire when the url is fetched.
     * */
    val onFetched: WebPageWebDriverEventHandler

    /**
     * Fire when the url is about to be navigated.
     * */
    val onWillNavigate: WebPageWebDriverEventHandler
    /**
     * Fire when the url is navigated.
     * */
    val onNavigated: WebPageWebDriverEventHandler

    /**
     * Fire when the url webpage about to interact.
     * */
    val onWillInteract: WebPageWebDriverEventHandler
    /**
     * Fire when the webpage is interacted.
     * */
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

    /**
     * Fire when the url is about to be scrolled.
     * */
    val onWillScroll: WebPageWebDriverEventHandler
    /**
     * Fire when the url is scrolled.
     * */
    val onDidScroll: WebPageWebDriverEventHandler

    /**
     * Fire when the feature is about to be computed.
     * */
    val onWillComputeFeature: WebPageWebDriverEventHandler
    /**
     * Fire when the feature is computed.
     * */
    val onFeatureComputed: WebPageWebDriverEventHandler

    /**
     * Fire when the tab is about to be stopped.
     * */
    val onWillStopTab: WebPageWebDriverEventHandler
    /**
     * Fire when the tab is stopped.
     * */
    val onTabStopped: WebPageWebDriverEventHandler

    /**
     * Chain the other browse event handler to the tail of this one.
     * */
    fun chain(other: BrowseEvent): BrowseEvent
}

/**
 * All event handlers of the webpage lifecycle.
 *
 * The events are fall into three groups:
 *
 * 1. [LoadEvent] fires in loading phase
 * 2. [BrowseEvent] fires in browsing phase
 * 3. [CrawlEvent] fires in crawling phase
 * */
interface PageEvent {
    /**
     * The load phase event handlers
     * */
    var loadEvent: LoadEvent
    /**
     * The browse phase event handlers
     * */
    var browseEvent: BrowseEvent
    /**
     * The crawl phase event handlers
     * */
    var crawlEvent: CrawlEvent
    /**
     * Chain the other page event handler to the tail of this one.
     * */
    fun chain(other: PageEvent): PageEvent
}
