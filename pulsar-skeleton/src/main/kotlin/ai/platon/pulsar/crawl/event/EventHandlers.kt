package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.lang.*
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.experimental.WebAsset

abstract class VoidHandler: PFunction0<Unit>, AbstractPHandler() {
    abstract override operator fun invoke()
}

abstract class UrlAwareHandler: (UrlAware) -> UrlAware?, AbstractPHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractPHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> String?, AbstractPHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class UrlFilter: (String) -> String?, AbstractPHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(page: WebPage): Any?
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage?) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage?): Any?
}

abstract class HTMLDocumentHandler: (WebPage, FeaturedDocument) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument): Any?
}

abstract class PrivacyContextHandler: (PrivacyContext) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(privacyContext: PrivacyContext): Any?
}

abstract class WebPageWebDriverHandler: (WebPage, WebDriver) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): Any?
}

abstract class PageDatumHandler: (String, PageDatum) -> Any?, AbstractPHandler() {
    abstract override operator fun invoke(pageSource: String, pageDatum: PageDatum): Any?
}

open class VoidEventHandler: AbstractChainedFunction0<Unit>()

open class UrlAwareEventHandler: AbstractChainedFunction1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlAwareEventFilter: AbstractChainedFunction1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlFilterEventHandler: AbstractChainedFunction1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlEventHandler: AbstractChainedFunction1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

/**
 * A readonly handler, the web asset will not be changed
 * */
open class WebAssetEventHandler: AbstractChainedFunction1<WebAsset, Any?>()

open class WebPageEventHandler: AbstractChainedFunction1<WebPage, Any?>()

/**
 * A readonly handler, the web asset will not be changed
 * */
open class UrlAwareWebAssetEventHandler: AbstractChainedFunction2<UrlAware, WebAsset?, Any?>()

open class UrlAwareWebPageEventHandler: AbstractChainedFunction2<UrlAware, WebPage?, Any?>()

/**
 * A readonly handler, the web asset will not be changed
 * */
open class WebAssetHTMLDocumentEventHandler: AbstractChainedFunction2<WebAsset, FeaturedDocument, Any?>()

open class WebPageHTMLDocumentEventHandler: AbstractChainedFunction2<WebPage, FeaturedDocument, Any?>()

typealias HTMLDocumentEventHandler = WebPageHTMLDocumentEventHandler

open class PageDatumEventHandler: AbstractChainedFunction2<String, PageDatum, Any?>()

/**
 * A readonly handler, the web asset will not be changed
 * */
open class WebAssetWebDriverEventHandler: AbstractChainedPDFunction2<WebAsset, WebDriver, Any?>()

open class WebPageWebDriverEventHandler: AbstractChainedPDFunction2<WebPage, WebDriver, Any?>()
