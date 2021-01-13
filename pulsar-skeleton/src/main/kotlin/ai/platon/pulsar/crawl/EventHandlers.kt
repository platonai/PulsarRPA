package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

abstract class NamedHandler {
    open val name: String = ""
}

abstract class WebPageHandler: (WebPage) -> Unit, NamedHandler() {
    abstract override operator fun invoke(page: WebPage)
}

abstract class HtmlDocumentHandler: (WebPage, FeaturedDocument) -> Unit, NamedHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument)
}

abstract class FetchResultHandler: (FetchResult) -> Unit, NamedHandler() {
    abstract override operator fun invoke(page: FetchResult)
}

abstract class WebPageBatchHandler: (Iterable<WebPage>) -> Unit, NamedHandler() {
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

abstract class FetchResultBatchHandler: (Iterable<FetchResult>) -> Unit, NamedHandler() {
    abstract override operator fun invoke(pages: Iterable<FetchResult>)
}

class AddRefererAfterFetchHandler(val url: UrlAware): WebPageHandler() {
    override fun invoke(page: WebPage) {
        url.referer?.let { page.referrer = it }
    }
}

interface CrawlEventHandler {
    var onBeforeLoad: (String) -> Unit
    var onBeforeFetch: (WebPage) -> Unit
    var onAfterFetch: (WebPage) -> Unit
    var onBeforeParse: (WebPage) -> Unit
    var onBeforeExtract: (WebPage) -> Unit
    var onAfterExtract: (WebPage, FeaturedDocument) -> Unit
    var onAfterParse: (WebPage, FeaturedDocument) -> Unit
    var onAfterLoad: (WebPage) -> Unit
}

abstract class AbstractCrawlEventHandler(
        override var onBeforeLoad: (String) -> Unit = {},
        override var onBeforeFetch: (WebPage) -> Unit = {},
        override var onAfterFetch: (WebPage) -> Unit = {},
        override var onBeforeParse: (WebPage) -> Unit = {},
        override var onBeforeExtract: (WebPage) -> Unit = {},
        override var onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        override var onAfterParse: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        override var onAfterLoad: (WebPage) -> Unit = {}
): CrawlEventHandler

class DefaultCrawlEventHandler(
        onBeforeLoad: (String) -> Unit = {},
        onBeforeFetch: (WebPage) -> Unit = {},
        onAfterFetch: (WebPage) -> Unit = {},
        onBeforeParse: (WebPage) -> Unit = {},
        onBeforeExtract: (WebPage) -> Unit = {},
        onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        onAfterParse: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        onAfterLoad: (WebPage) -> Unit = {}
): AbstractCrawlEventHandler(onBeforeLoad,
        onBeforeFetch, onAfterFetch, onBeforeParse, onBeforeExtract, onAfterExtract, onAfterParse, onAfterLoad)
