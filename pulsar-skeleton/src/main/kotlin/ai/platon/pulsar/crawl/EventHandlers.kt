package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

interface Handler {
    val name: String
}

abstract class WebPageHandler: (WebPage) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(page: WebPage)
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(url: UrlAware, page: WebPage)
}

abstract class HtmlDocumentHandler: (WebPage, FeaturedDocument) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument)
}

abstract class FetchResultHandler: (FetchResult) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(page: FetchResult)
}

abstract class WebPageBatchHandler: (Iterable<WebPage>) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

abstract class FetchResultBatchHandler: (Iterable<FetchResult>) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(pages: Iterable<FetchResult>)
}

class AddRefererAfterFetchHandler(val url: UrlAware): WebPageHandler() {
    override fun invoke(page: WebPage) { url.referer?.let { page.referrer = it } }
}

class ChainedWebPageHandler: (WebPage) -> Unit, WebPageHandler() {
    private val handlers = mutableListOf<(WebPage) -> Unit>()

    fun addFirst(handler: (WebPage) -> Unit): ChainedWebPageHandler {
        handlers.add(0, handler)
        return this
    }

    fun addLast(handler: (WebPage) -> Unit): ChainedWebPageHandler {
        handlers.add(handler)
        return this
    }

    override operator fun invoke(page: WebPage) {
        handlers.forEach { it(page) }
    }
}

class ChainedUrlAwareWebPageHandler: (UrlAware, WebPage) -> Unit, UrlAwareWebPageHandler() {
    private val handlers = mutableListOf<(UrlAware, WebPage) -> Unit>()

    fun addFirst(handler: (UrlAware, WebPage) -> Unit): ChainedUrlAwareWebPageHandler {
        handlers.add(0, handler)
        return this
    }

    fun addLast(handler: (UrlAware, WebPage) -> Unit): ChainedUrlAwareWebPageHandler {
        handlers.add(handler)
        return this
    }

    override operator fun invoke(url: UrlAware, page: WebPage) {
        handlers.forEach { it(url, page) }
    }
}

class ChainedHtmlDocumentHandler: (WebPage, FeaturedDocument) -> Unit, HtmlDocumentHandler(), Handler {
    private val handlers = mutableListOf<(WebPage, FeaturedDocument) -> Unit>()

    fun addFirst(handler: (WebPage, FeaturedDocument) -> Unit): ChainedHtmlDocumentHandler {
        handlers.add(0, handler)
        return this
    }

    fun addLast(handler: (WebPage, FeaturedDocument) -> Unit): ChainedHtmlDocumentHandler {
        handlers.add(handler)
        return this
    }

    override fun invoke(page: WebPage, document: FeaturedDocument) {
        handlers.forEach { it(page, document) }
    }
}

/**
 * TODO: use pipeline and handler pattern, see Netty
 * */
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

interface StreamingCrawlerEventHandler {
    var onFilter: (UrlAware) -> UrlAware?
    var onNormalize: (UrlAware) -> UrlAware?
    var onBeforeLoad: (UrlAware) -> Unit
    var onAfterLoad: (UrlAware, WebPage) -> Unit
}

abstract class AbstractStreamingCrawlerEventHandler(
        override var onFilter: (UrlAware) -> UrlAware? = { it },
        override var onNormalize: (UrlAware) -> UrlAware? = { it },
        override var onBeforeLoad: (UrlAware) -> Unit = { _ -> },
        override var onAfterLoad: (UrlAware, WebPage) -> Unit = { _, _ -> }
): StreamingCrawlerEventHandler

class DefaultStreamingCrawlerEventHandler(
        onFilter: (UrlAware) -> UrlAware? = { it },
        onNormalize: (UrlAware) -> UrlAware? = { it },
        onBeforeLoad: (UrlAware) -> Unit = { _ -> },
        onAfterLoad: (UrlAware, WebPage) -> Unit = { _, _ -> }
): AbstractStreamingCrawlerEventHandler(
        onFilter, onNormalize, onBeforeLoad, onAfterLoad
)

class ChainedStreamingCrawlerEventHandler(
        onFilter: (UrlAware) -> UrlAware? = { it },
        onNormalize: (UrlAware) -> UrlAware? = { it },
        onBeforeLoad: (UrlAware) -> Unit = { _ -> },
        onAfterLoad: (UrlAware, WebPage) -> Unit = ChainedUrlAwareWebPageHandler()
): AbstractStreamingCrawlerEventHandler(
        onFilter, onNormalize, onBeforeLoad, onAfterLoad
) {
    fun addFirst(name: String, handler: (UrlAware, WebPage) -> Unit) {
        if (name == "onAfterLoad") {
            if (onAfterLoad is ChainedUrlAwareWebPageHandler) {
                (onAfterLoad as? ChainedUrlAwareWebPageHandler)?.addFirst(handler)
            } else {
                onAfterLoad = ChainedUrlAwareWebPageHandler().addFirst(handler).addFirst(onAfterLoad)
            }
        }
    }

    fun addLast(name: String, handler: (UrlAware, WebPage) -> Unit) {
        if (name == "onAfterLoad") {
            if (onAfterLoad is ChainedUrlAwareWebPageHandler) {
                (onAfterLoad as? ChainedUrlAwareWebPageHandler)?.addLast(handler)
            } else {
                onAfterLoad = ChainedUrlAwareWebPageHandler().addLast(handler).addLast(onAfterLoad)
            }
        }
    }
}
