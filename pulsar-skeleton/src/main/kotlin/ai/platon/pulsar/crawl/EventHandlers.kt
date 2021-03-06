package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay

interface Handler {
    val name: String
}

abstract class UrlAwareHandler: (UrlAware) -> Unit, Handler {
    override val name: String = ""
    abstract override operator fun invoke(url: UrlAware)
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

class ChainedUrlAwareHandler: (UrlAware) -> Unit, UrlAwareHandler() {
    private val handlers = mutableListOf<(UrlAware) -> Unit>()

    fun addFirst(handler: (UrlAware) -> Unit): ChainedUrlAwareHandler {
        handlers.add(0, handler)
        return this
    }

    fun addLast(handler: (UrlAware) -> Unit): ChainedUrlAwareHandler {
        handlers.add(handler)
        return this
    }

    override operator fun invoke(url: UrlAware) {
        handlers.forEach { it(url) }
    }
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
interface LoadEventHandler {
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

abstract class AbstractLoadEventHandler(
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
): LoadEventHandler

class DefaultLoadEventHandler(
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
): AbstractLoadEventHandler(
        onFilter, onNormalize,
        onBeforeLoad,
        onBeforeFetch, onAfterFetch,
        onBeforeParse, onBeforeHtmlParse,
        onBeforeExtract, onAfterExtract,
        onAfterHtmlParse, onAfterParse,
        onAfterLoad
) {
    companion object {
        fun create(handler: LoadEventHandler): DefaultLoadEventHandler {
            return DefaultLoadEventHandler(
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

interface JsEventHandler {
    suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any?
    suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any?
}

abstract class AbstractJsEventHandler: JsEventHandler {
    open var delayMillis = 500L
    open var verbose = false

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return null
    }

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return null
    }

    protected suspend fun evaluate(driver: WebDriver, expressions: Iterable<String>): Any? {
        var value: Any? = null
        expressions.mapNotNull { it.trim().takeIf { it.isNotBlank() } }.filterNot { it.startsWith("// ") }.forEach {
//            log.takeIf { verbose }?.info("Evaluate expression >>>$it<<<")
            val v = evaluate(driver, it)
            if (v is String) {
                val s = Strings.stripNonPrintableChar(v)
//                log.takeIf { verbose }?.info("Result >>>$s<<<")
            } else if (v is Int || v is Long) {
//                log.takeIf { verbose }?.info("Result >>>$v<<<")
            }
            value = v
        }
        return value
    }

    protected suspend fun evaluate(driver: WebDriver, expression: String): Any? {
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        return driver.evaluate(expression)
    }
}

class DefaultJsEventHandler(
    val beforeComputeExpressions: Iterable<String>,
    val afterComputeExpressions: Iterable<String>
): AbstractJsEventHandler() {
    constructor(bcExpressions: String, acExpressions2: String, delimiters: String = ";"): this(
        bcExpressions.split(delimiters), acExpressions2.split(delimiters))

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return evaluate(driver, beforeComputeExpressions)
    }

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return evaluate(driver, afterComputeExpressions)
    }
}

interface CrawlEventHandler {
    var onFilter: (UrlAware) -> UrlAware?
    var onNormalize: (UrlAware) -> UrlAware?
    var onBeforeLoad: (UrlAware) -> Unit
    var onLoad: (UrlAware) -> Unit
    var onAfterLoad: (UrlAware, WebPage) -> Unit
}

abstract class AbstractCrawlEventHandler(
    override var onFilter: (UrlAware) -> UrlAware? = { it },
    override var onNormalize: (UrlAware) -> UrlAware? = { it },
    override var onBeforeLoad: (UrlAware) -> Unit = { _ -> },
    override var onLoad: (UrlAware) -> Unit = { _ -> },
    override var onAfterLoad: (UrlAware, WebPage) -> Unit = { _, _ -> }
): CrawlEventHandler

class DefaultCrawlEventHandler(
        onFilter: (UrlAware) -> UrlAware? = { it },
        onNormalize: (UrlAware) -> UrlAware? = { it },
        onBeforeLoad: (UrlAware) -> Unit = { _ -> },
        onLoad: (UrlAware) -> Unit = { _ -> },
        onAfterLoad: (UrlAware, WebPage) -> Unit = { _, _ -> }
): AbstractCrawlEventHandler(
        onFilter, onNormalize, onBeforeLoad, onLoad, onAfterLoad
)

class ChainedCrawlEventHandler(
        onFilter: (UrlAware) -> UrlAware? = { it },
        onNormalize: (UrlAware) -> UrlAware? = { it },
        onBeforeLoad: (UrlAware) -> Unit = { _ -> },
        onLoad: (UrlAware) -> Unit = { _ -> },
        onAfterLoad: (UrlAware, WebPage) -> Unit = ChainedUrlAwareWebPageHandler()
): AbstractCrawlEventHandler(
        onFilter, onNormalize, onBeforeLoad, onLoad, onAfterLoad
) {
    fun addFirst(name: String, handler: (UrlAware) -> Unit) {
        if (name == "onBeforeLoad") {
            when (onBeforeLoad) {
                is ChainedUrlAwareHandler -> (onBeforeLoad as ChainedUrlAwareHandler).addFirst(handler)
                else -> onBeforeLoad = ChainedUrlAwareHandler().addFirst(handler).addFirst(onBeforeLoad)
            }
        }
    }

    fun addLast(name: String, handler: (UrlAware) -> Unit) {
        if (name == "onBeforeLoad") {
            when (onBeforeLoad) {
                is ChainedUrlAwareHandler -> (onBeforeLoad as ChainedUrlAwareHandler).addLast(handler)
                else -> onBeforeLoad = ChainedUrlAwareHandler().addLast(onBeforeLoad).addLast(handler)
            }
        }
    }

    fun addFirst(name: String, handler: (UrlAware, WebPage) -> Unit) {
        if (name == "onAfterLoad") {
            when (onAfterLoad) {
                is ChainedUrlAwareWebPageHandler -> (onAfterLoad as ChainedUrlAwareWebPageHandler).addFirst(handler)
                else -> onAfterLoad = ChainedUrlAwareWebPageHandler().addFirst(handler).addFirst(onAfterLoad)
            }
        }
    }

    fun addLast(name: String, handler: (UrlAware, WebPage) -> Unit) {
        if (name == "onAfterLoad") {
            when (onAfterLoad) {
                is ChainedUrlAwareWebPageHandler -> (onAfterLoad as ChainedUrlAwareWebPageHandler).addLast(handler)
                else -> onAfterLoad = ChainedUrlAwareWebPageHandler().addLast(onAfterLoad).addLast(handler)
            }
        }
    }
}
