package ai.platon.pulsar.crawl.experimental

import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document

enum class EventType {
    crawlWillLoad,

    willNavigate,
    navigated,
    willInteract,
    didInteract,
    willCheckDOMState,
    didDOMStateCheck,
    willComputeFeature,
    featureComputed,
    willStopTab,
    tabStopped,
}

interface GlobalEventListener {

    fun onCrawlWillLoad(handler: (UrlAware) -> Unit)

    fun onCrawlLoad(handler: (UrlAware) -> Unit)

    fun onCrawlLoaded(handler: (UrlAware, WebPage) -> Unit)

    fun onWillLoad(handler: (UrlAware) -> Unit)

    fun onWillFetch(handler: (UrlAware, WebPage) -> Unit)

    fun onWillLaunchBrowser(handler: (String) -> Unit)

    fun onBrowserLaunched(handler: (UrlAware, WebPage) -> Unit)

    fun onFetched(handler: (WebPage) -> Unit)

    fun onWillParse(handler: (WebPage) -> Unit)

    fun onWillParseHTMLDocument(handler: (WebPage) -> Unit)

    fun onWillExtractData(handler: (WebPage, Document) -> Unit)

    fun onDataExtracted(handler: (WebPage, Document) -> Unit)

    fun onHTMLDocumentParsed(handler: (WebPage, Document) -> Unit)

    fun onParsed(handler: (WebPage) -> Unit)

    fun onLoaded(handler: (WebPage) -> Unit)
    
    fun onBrowserWillFetch(handler: (WebPage, WebDriver) -> Unit)
    fun onBrowserFetched(handler: (WebPage, WebDriver) -> Unit)

    fun onWillNavigate(handler: (WebPage, WebDriver) -> Unit)
    fun onNavigated(handler: (WebPage, WebDriver) -> Unit)

    fun onWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit)
    fun onDOMStateChecked(handler: (WebPage, WebDriver) -> Unit)

    fun onWillComputeFeature(handler: (WebPage, WebDriver) -> Unit)
    fun onFeatureComputed(handler: (WebPage, WebDriver) -> Unit)

    fun onWillInteract(handler: (WebPage, WebDriver) -> Unit)
    fun onDidInteract(handler: (WebPage, WebDriver) -> Unit)

    fun onWillStopTab(handler: (WebPage, WebDriver) -> Unit)
    fun onTabStopped(handler: (WebPage, WebDriver) -> Unit)
}

abstract class AbstractGlobalEventListener: GlobalEventListener {
    protected val listeners = AbstractEventEmitter<EventType>()

    override fun onCrawlWillLoad(handler: (UrlAware) -> Unit) {
        listeners.on(EventType.crawlWillLoad, handler)
    }

    override fun onCrawlLoad(handler: (UrlAware) -> Unit) {}

    override fun onCrawlLoaded(handler: (UrlAware, WebPage) -> Unit) {}

    override fun onWillLoad(handler: (UrlAware) -> Unit) {}

    override fun onWillFetch(handler: (UrlAware, WebPage) -> Unit) {}

    override fun onWillLaunchBrowser(handler: (String) -> Unit) {}

    override fun onBrowserLaunched(handler: (UrlAware, WebPage) -> Unit) {}

    override fun onFetched(handler: (WebPage) -> Unit) {}

    override fun onWillParse(handler: (WebPage) -> Unit) {}

    override fun onWillParseHTMLDocument(handler: (WebPage) -> Unit) {}

    override fun onWillExtractData(handler: (WebPage, Document) -> Unit) {}

    override fun onDataExtracted(handler: (WebPage, Document) -> Unit) {}

    override fun onHTMLDocumentParsed(handler: (WebPage, Document) -> Unit) {}

    override fun onParsed(handler: (WebPage) -> Unit) {}

    override fun onLoaded(handler: (WebPage) -> Unit) {}

    override fun onBrowserWillFetch(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onBrowserFetched(handler: (WebPage, WebDriver) -> Unit) {}

    override fun onWillNavigate(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onNavigated(handler: (WebPage, WebDriver) -> Unit) {}

    override fun onWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onDOMStateChecked(handler: (WebPage, WebDriver) -> Unit) {}

    override fun onWillComputeFeature(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onFeatureComputed(handler: (WebPage, WebDriver) -> Unit) {}

    override fun onWillInteract(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onDidInteract(handler: (WebPage, WebDriver) -> Unit) {}

    override fun onWillStopTab(handler: (WebPage, WebDriver) -> Unit) {}
    override fun onTabStopped(handler: (WebPage, WebDriver) -> Unit) {}

    fun <T> notify(type: EventType, param: T) = listeners.emit(type, param)
    fun <T, T2> notify(type: EventType, param: T, param2: T2) = listeners.emit(type, param, param2)
    fun <T, T2, T3> notify(type: EventType, param: T, param2: T2, param3: T3) = listeners.emit(type, param, param2, param3)
}
