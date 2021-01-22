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
    override fun invoke(page: WebPage) { url.referer?.let { page.referrer = it } }
}

interface CrawlEventHandler {
    var onFilter: (String) -> String?
    var onNormalize: (String) -> String?
    var onBeforeLoad: (String) -> Unit
    var onBeforeFetch: (WebPage) -> Unit
    var onAfterFetch: (WebPage) -> Unit
    var onBeforeParse: (WebPage) -> Unit
    var onBeforeHtmlParse: (WebPage) -> Unit
    var onBeforeExtract: (WebPage) -> Unit
    var onAfterExtract: (WebPage, FeaturedDocument) -> Unit
    var onAfterHtmlParse: (WebPage, FeaturedDocument) -> Unit
    var onAfterParse: (WebPage) -> Unit
    var onAfterLoad: (WebPage) -> Unit
}

abstract class AbstractCrawlEventHandler(
        override var onFilter: (String) -> String? = { it },
        override var onNormalize: (String) -> String? = { it },
        override var onBeforeLoad: (String) -> Unit = {},
        override var onBeforeFetch: (WebPage) -> Unit = {},
        override var onAfterFetch: (WebPage) -> Unit = {},
        override var onBeforeParse: (WebPage) -> Unit = {},
        override var onBeforeHtmlParse: (WebPage) -> Unit = {},
        override var onBeforeExtract: (WebPage) -> Unit = {},
        override var onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        override var onAfterHtmlParse: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        override var onAfterParse: (WebPage) -> Unit = { _ -> },
        override var onAfterLoad: (WebPage) -> Unit = {}
): CrawlEventHandler

class DefaultCrawlEventHandler(
        onFilter: (String) -> String? = { it },
        onNormalize: (String) -> String? = { it },
        onBeforeLoad: (String) -> Unit = {},
        onBeforeFetch: (WebPage) -> Unit = {},
        onAfterFetch: (WebPage) -> Unit = {},
        onBeforeParse: (WebPage) -> Unit = {},
        onBeforeHtmlParse: (WebPage) -> Unit = {},
        onBeforeExtract: (WebPage) -> Unit = {},
        onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        onAfterHtmlParse: (WebPage, FeaturedDocument) -> Unit = { _, _ -> },
        onAfterParse: (WebPage) -> Unit = { _ -> },
        onAfterLoad: (WebPage) -> Unit = {}
): AbstractCrawlEventHandler(
        onFilter, onNormalize,
        onBeforeLoad,
        onBeforeFetch, onAfterFetch,
        onBeforeParse, onBeforeHtmlParse,
        onBeforeExtract, onAfterExtract,
        onAfterHtmlParse, onAfterParse,
        onAfterLoad
) {
    companion object {
        fun create(handler: CrawlEventHandler): DefaultCrawlEventHandler {
            return DefaultCrawlEventHandler(
                    handler.onFilter,
                    handler.onNormalize,
                    handler.onBeforeLoad,
                    handler.onBeforeFetch,
                    handler.onAfterFetch,
                    handler.onBeforeParse,
                    handler.onBeforeHtmlParse,
                    handler.onBeforeExtract,
                    handler.onAfterExtract,
                    handler.onAfterHtmlParse,
                    handler.onAfterParse,
                    handler.onAfterLoad
            )
        }
    }
}
