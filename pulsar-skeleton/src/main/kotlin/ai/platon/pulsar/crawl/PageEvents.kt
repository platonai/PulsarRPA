package ai.platon.pulsar.crawl

import ai.platon.pulsar.crawl.event.*

/**
 * Event handlers during the crawl phase of the webpage lifecycle.
 * */
interface CrawlEventHandlers {

    /**
     * Fire when the url is about to be loaded.
     * */
    val onWillLoad: UrlAwareEventHandler

    /**
     * Fire to load the url.
     * */
    @Deprecated("No such event handler required", level = DeprecationLevel.WARNING)
    val onLoad: UrlAwareEventHandler

    /**
     * Fire when the url is loaded.
     * */
    val onLoaded: UrlAwareWebPageEventHandler

    /**
     * Chain the other crawl event handler to the tail of this one.
     * */
    fun chain(other: CrawlEventHandlers): CrawlEventHandlers
}

/**
 * Event handlers during the loading phase of the webpage lifecycle.
 * */
interface LoadEventHandlers {

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
    fun chain(other: LoadEventHandlers): LoadEventHandlers
}

/**
 * Event handlers during the browsing phase of the webpage lifecycle.
 * */
interface BrowseEventHandlers {
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
     * Fire when the document is actually ready. The actuallyReady state is determined using an algorithm that is executed
     * within the browser.
     *
     * This actuallyReady state differs from the standard Document.readyState, which describes the loading state of the
     * document. When Document.readyState changes, a readystatechange event fires on the document object.
     *
     * @see [https://developer.mozilla.org/en-US/docs/Web/API/Document/readyState]
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
    fun chain(other: BrowseEventHandlers): BrowseEventHandlers
}

/**
 * The `PageEventHandlers` class specifies all event handlers that are triggered at various stages of a webpage’s lifecycle.
 *
 * The events are fall into three groups:
 *
 * 1. [LoadEventHandlers] triggers in loading stage.
 * 2. [BrowseEventHandlers] triggers in browsing stage.
 * 3. [CrawlEventHandlers] triggers in crawl stage, which is before and after loading the page.
 * */
interface PageEventHandlers {
    /**
     * Event handlers during the loading stage.
     * */
    var loadEventHandlers: LoadEventHandlers
    /**
     * Event handlers during the browsing stage.
     * */
    var browseEventHandlers: BrowseEventHandlers
    /**
     * Event handlers during the crawl stage.
     * */
    var crawlEventHandlers: CrawlEventHandlers
    /**
     * Chain the other page event handlers to the tail of this one.
     * */
    fun chain(other: PageEventHandlers): PageEventHandlers
    
    @Deprecated("Use loadEventHandlers instead", ReplaceWith("loadEventHandlers"))
    var loadEvent: LoadEvent
    @Deprecated("Use browseEventHandlers instead", ReplaceWith("browseEventHandlers"))
    var browseEvent: BrowseEvent
    @Deprecated("Use crawlEventHandlers instead", ReplaceWith("crawlEventHandlers"))
    var crawlEvent: CrawlEvent
}

@Deprecated("Use PageEventHandlers instead", ReplaceWith("PageEventHandlers"))
typealias LoadEvent = LoadEventHandlers

@Deprecated("Use PageEventHandlers instead", ReplaceWith("PageEventHandlers"))
typealias BrowseEvent = BrowseEventHandlers

@Deprecated("Use PageEventHandlers instead", ReplaceWith("PageEventHandlers"))
typealias CrawlEvent = CrawlEventHandlers

@Deprecated("Use PageEventHandlers instead", ReplaceWith("PageEventHandlers"))
typealias PageEvent = PageEventHandlers
