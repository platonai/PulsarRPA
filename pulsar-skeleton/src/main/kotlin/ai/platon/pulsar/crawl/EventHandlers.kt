package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.url.StatefulHyperlink
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
        if (url is StatefulHyperlink) {
            val referer = url.referer
            if (referer != null) {
                page.referrer = referer
            }
        }
    }
}
