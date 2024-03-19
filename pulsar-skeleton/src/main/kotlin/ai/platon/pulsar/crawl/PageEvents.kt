package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Event handlers during the crawl phase of the webpage lifecycle.
 * */
interface CrawlEvent {

    /**
     * Fire when the url is about to be loaded.
     * */
    val onWillLoad: UrlAwareEventHandler

    /**
     * Fire to load the url.
     * TODO: better name?
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
 * Event handlers during the loading phase of the webpage lifecycle.
 * */
interface LoadEvent {

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
 * Event handlers during the browsing phase of the webpage lifecycle.
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
     * Fire when the url is navigated, just like we clicked the `Go` button on the browser's navigation bar.
     * */
    val onNavigated: WebPageWebDriverEventHandler

    /**
     * Fire when the interaction with the webpage is about to begin.
     * */
    val onWillInteract: WebPageWebDriverEventHandler
    /**
     * Fire when the interactions with the webpage have been completed.
     * This event is fired after the interactions are completed, such as clicking a button, filling a form, and so on.
     *
     * This event is fired after the completion of the following actions:
     *
     * 1. Checking the document state
     * 2. Completing webpage scrolling
     * 3. Computing webpage features
     *
     * The event is fired before the following actions:
     * 1. Stopping the browser tab
     * */
    val onDidInteract: WebPageWebDriverEventHandler

    /**
     * Fire when the document state is about to be checked.
     * */
    val onWillCheckDocumentState: WebPageWebDriverEventHandler

    /**
     * Fire when the document is actually ready. The document state is checked(computed)
     * using an algorithm in javascript.
     * */
    val onDocumentActuallyReady: WebPageWebDriverEventHandler

    /**
     * Fire when we are about to perform scrolling on the page.
     * */
    val onWillScroll: WebPageWebDriverEventHandler
    /**
     * Fire when we have performed scrolling on the page.
     * */
    val onDidScroll: WebPageWebDriverEventHandler
    
    /**
     * Fire when we have performed scrolling on the page, at which point the document is considered not to change
     * unless other interactive actions occur. It is a good time to perform custom actions.
     *
     * Custom actions are defined by the user using code snippets that are written for a specific purpose, such as
     * clicking a button, filling a form, and so on.
     *
     * The event is fired after the completion of the following actions:
     * onDocumentActuallyReady, onWillScroll, onDidScroll
     *
     * The event is fired before the following actions:
     * onWillComputeFeature, onFeatureComputed, onDidInteract, onWillStopTab, onTabStopped
     * */
    val onDocumentSteady: WebPageWebDriverEventHandler

    /**
     * Fire when the webpage features are about to be computed.
     *
     * */
    val onWillComputeFeature: WebPageWebDriverEventHandler
    /**
     * Fire when the webpage features have been computed.
     * */
    val onFeatureComputed: WebPageWebDriverEventHandler

    /**
     * Fire when the browser tab is about to be stopped.
     *
     * This event is fired after the completion of the following actions:
     * 1. Checking the document state
     * 2. Completing webpage scrolling
     * 3. Computing webpage features
     * 4. Interacting with the webpage
     * */
    val onWillStopTab: WebPageWebDriverEventHandler
    /**
     * Fire when the browser tab is stopped.
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
