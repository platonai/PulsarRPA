package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.lang.*
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.CopyOnWriteArrayList

abstract class VoidHandler: PFunction0<Unit>, AbstractHandler() {
    abstract override operator fun invoke()
}

abstract class UrlAwareHandler: (UrlAware) -> UrlAware?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> String?, AbstractHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class UrlFilter: (String) -> String?, AbstractHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage): Any?
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage?) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage?): Any?
}

abstract class HTMLDocumentHandler: (WebPage, FeaturedDocument) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument): Any?
}

abstract class PrivacyContextHandler: (PrivacyContext) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(privacyContext: PrivacyContext): Any?
}

abstract class WebDriverHandler: (WebDriver) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(driver: WebDriver): Any?
    abstract suspend fun invoke(page: WebPage, driver: WebDriver): Any?
}

abstract class WebPageWebDriverHandler: (WebPage, WebDriver) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): Any?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any?
}

abstract class WebDriverFetchResultHandler: (WebPage, WebDriver) -> FetchResult?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): FetchResult?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): FetchResult?
}

abstract class PageDatumHandler: (String, PageDatum) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(pageSource: String, pageDatum: PageDatum): Any?
}

open class VoidEventHandler: AbstractChainedHandler0<Unit>()

open class UrlAwareEventHandler: AbstractChainedHandler1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlAwareEventFilter: AbstractChainedHandler1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlFilterEventHandler: AbstractChainedHandler1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlEventHandler: AbstractChainedHandler1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class WebPageEventHandler: AbstractChainedHandler1<WebPage, Any?>()

open class UrlAwareWebPageEventHandler: AbstractChainedHandler2<UrlAware, WebPage?, Any?>()

open class HTMLDocumentEventHandler: AbstractChainedHandler2<WebPage, FeaturedDocument, Any?>()

open class PageDatumEventHandler: AbstractChainedHandler2<String, PageDatum, Any?>()

open class WebPageWebDriverEventHandler: AbstractChainedDHandler2<WebPage, WebDriver, Any?>()
