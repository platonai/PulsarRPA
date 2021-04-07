package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import java.util.*

interface EventHandler {
    val name: String
}

abstract class AbstractHandler: EventHandler {
    override val name: String = ""
}

abstract class UrlAwareHandler: (UrlAware) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware)
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(url: String)
}

abstract class UrlFilter: (String) -> String?, AbstractHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage)
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage)
}

abstract class HtmlDocumentHandler: (WebPage, FeaturedDocument) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument)
}

abstract class FetchResultHandler: (FetchResult) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(page: FetchResult)
}

abstract class WebPageBatchHandler: (Iterable<WebPage>) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

abstract class FetchResultBatchHandler: (Iterable<FetchResult>) -> Unit, AbstractHandler() {
    abstract override operator fun invoke(pages: Iterable<FetchResult>)
}

class UrlAwareHandlerPipeline: UrlAwareHandler() {
    private val registeredHandlers = mutableListOf<(UrlAware) -> Unit>()

    fun addFirst(handler: (UrlAware) -> Unit): UrlAwareHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: (UrlAware) -> Unit): UrlAwareHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: (UrlAware) -> Unit): UrlAwareHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: (UrlAware) -> Unit): UrlAwareHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(url: UrlAware) {
        registeredHandlers.forEach { it(url) }
    }
}

class UrlAwareFilterPipeline: UrlAwareFilter() {
    private val registeredHandlers = mutableListOf<(UrlAware) -> UrlAware?>()

    fun addFirst(handler: (UrlAware) -> UrlAware): UrlAwareFilterPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: (UrlAware) -> UrlAware): UrlAwareFilterPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: (UrlAware) -> UrlAware): UrlAwareFilterPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: (UrlAware) -> UrlAware): UrlAwareFilterPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(url: UrlAware): UrlAware? {
        var result: UrlAware? = url
        registeredHandlers.forEach {
            result = it(url)
        }
        return result
    }
}

class UrlFilterPipeline: UrlFilter() {
    private val registeredHandlers = mutableListOf<UrlFilter>()

    fun addFirst(handler: UrlFilter): UrlFilterPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (String) -> String?): UrlFilterPipeline {
        registeredHandlers.add(0, object: UrlFilter() {
            override fun invoke(url: String) = handler(url)
        })
        return this
    }

    fun addFirst(vararg handlers: UrlFilter): UrlFilterPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: UrlFilter): UrlFilterPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: UrlFilter): UrlFilterPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    fun addLast(handler: (String) -> String?): UrlFilterPipeline {
        registeredHandlers.add(object: UrlFilter() {
            override fun invoke(url: String) = handler(url)
        })
        return this
    }

    override operator fun invoke(url: String): String? {
        var result: String? = url
        registeredHandlers.forEach {
            result = it(url)
        }
        return result
    }
}

class UrlHandlerPipeline: UrlHandler() {
    private val registeredHandlers = mutableListOf<(String) -> Unit>()

    fun addFirst(handler: UrlHandler): UrlHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (String) -> Unit): UrlHandlerPipeline {
        registeredHandlers.add(0, object: UrlHandler() {
            override fun invoke(url: String) = handler(url)
        })
        return this
    }

    fun addFirst(vararg handlers: UrlHandler): UrlHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: UrlHandler): UrlHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: UrlHandler): UrlHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    fun addLast(handler: (String) -> Unit): UrlHandlerPipeline {
        registeredHandlers.add(object: UrlHandler() {
            override fun invoke(url: String) = handler(url)
        })
        return this
    }

    override operator fun invoke(url: String) {
        registeredHandlers.forEach { it(url) }
    }
}

class WebPageHandlerPipeline: WebPageHandler() {
    private val registeredHandlers = mutableListOf<WebPageHandler>()

    fun addFirst(handler: WebPageHandler): WebPageHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (WebPage) -> Unit): WebPageHandlerPipeline {
        registeredHandlers.add(0, object: WebPageHandler() {
            override fun invoke(page: WebPage) = handler(page)
        })
        return this
    }

    fun addFirst(vararg handlers: WebPageHandler): WebPageHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: WebPageHandler): WebPageHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(handler: (WebPage) -> Unit): WebPageHandlerPipeline {
        registeredHandlers += object: WebPageHandler() {
            override fun invoke(page: WebPage) = handler(page)
        }
        return this
    }

    fun addLast(vararg handlers: WebPageHandler): WebPageHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(page: WebPage) {
        registeredHandlers.forEach { it(page) }
    }
}

class UrlAwareWebPageHandlerPipeline: UrlAwareWebPageHandler() {
    private val registeredHandlers = mutableListOf<(UrlAware, WebPage) -> Unit>()

    fun addFirst(handler: (UrlAware, WebPage) -> Unit): UrlAwareWebPageHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: (UrlAware, WebPage) -> Unit): UrlAwareWebPageHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: (UrlAware, WebPage) -> Unit): UrlAwareWebPageHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: (UrlAware, WebPage) -> Unit): UrlAwareWebPageHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(url: UrlAware, page: WebPage) {
        registeredHandlers.forEach { it(url, page) }
    }
}

class HtmlDocumentHandlerPipeline: HtmlDocumentHandler(), EventHandler {
    private val registeredHandlers = mutableListOf<HtmlDocumentHandler>()

    fun addFirst(handler: HtmlDocumentHandler): HtmlDocumentHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (WebPage, FeaturedDocument) -> Unit): HtmlDocumentHandlerPipeline {
        registeredHandlers.add(0, object: HtmlDocumentHandler() {
            override fun invoke(page: WebPage, document: FeaturedDocument) = handler(page, document)
        })
        return this
    }

    fun addFirst(vararg handlers: HtmlDocumentHandler): HtmlDocumentHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: HtmlDocumentHandler): HtmlDocumentHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(handler: (WebPage, FeaturedDocument) -> Unit): HtmlDocumentHandlerPipeline {
        registeredHandlers += object: HtmlDocumentHandler() {
            override fun invoke(page: WebPage, document: FeaturedDocument) = handler(page, document)
        }
        return this
    }

    fun addLast(vararg handlers: HtmlDocumentHandler): HtmlDocumentHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override fun invoke(page: WebPage, document: FeaturedDocument) {
        registeredHandlers.forEach { it(page, document) }
    }
}

interface LoadEventHandler {
    var onFilter: UrlFilter
    var onNormalize: UrlFilter
    var onBeforeLoad: UrlHandler
    var onBeforeFetch: WebPageHandler
    var onAfterFetch: WebPageHandler
    var onBeforeParse: WebPageHandler
    var onBeforeHtmlParse: WebPageHandler
    var onBeforeExtract: WebPageHandler
    var onAfterExtract: HtmlDocumentHandler
    var onAfterHtmlParse: HtmlDocumentHandler
    var onAfterParse: WebPageHandler
    var onAfterLoad: WebPageHandler
}

interface LoadEventPipelineHandler: LoadEventHandler {
    val onFilterPipeline: UrlFilterPipeline
    val onNormalizePipeline: UrlFilterPipeline
    val onBeforeLoadPipeline: UrlHandlerPipeline
    val onBeforeFetchPipeline: WebPageHandlerPipeline
    val onAfterFetchPipeline: WebPageHandlerPipeline
    val onBeforeParsePipeline: WebPageHandlerPipeline
    val onBeforeHtmlParsePipeline: WebPageHandlerPipeline
    val onBeforeExtractPipeline: WebPageHandlerPipeline
    val onAfterExtractPipeline: HtmlDocumentHandlerPipeline
    val onAfterHtmlParsePipeline: HtmlDocumentHandlerPipeline
    val onAfterParsePipeline: WebPageHandlerPipeline
    val onAfterLoadPipeline: WebPageHandlerPipeline
}

abstract class AbstractLoadEventHandler(
        override var onFilter: UrlFilter = UrlFilterPipeline(),
        override var onNormalize: UrlFilter = UrlFilterPipeline(),
        override var onBeforeLoad: UrlHandler = UrlHandlerPipeline(),
        override var onBeforeFetch: WebPageHandler = WebPageHandlerPipeline(),
        override var onAfterFetch: WebPageHandler = WebPageHandlerPipeline(),
        override var onBeforeParse: WebPageHandler = WebPageHandlerPipeline(),
        override var onBeforeHtmlParse: WebPageHandler = WebPageHandlerPipeline(),
        override var onBeforeExtract: WebPageHandler = WebPageHandlerPipeline(),
        override var onAfterExtract: HtmlDocumentHandler = HtmlDocumentHandlerPipeline(),
        override var onAfterHtmlParse: HtmlDocumentHandler = HtmlDocumentHandlerPipeline(),
        override var onAfterParse: WebPageHandler = WebPageHandlerPipeline(),
        override var onAfterLoad: WebPageHandler = WebPageHandlerPipeline()
): LoadEventHandler

open class DefaultLoadEventHandler(
    override val onFilterPipeline: UrlFilterPipeline = UrlFilterPipeline(),
    override val onNormalizePipeline: UrlFilterPipeline = UrlFilterPipeline(),
    override val onBeforeLoadPipeline: UrlHandlerPipeline = UrlHandlerPipeline(),
    override val onBeforeFetchPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onAfterFetchPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeHtmlParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeExtractPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onAfterExtractPipeline: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    override val onAfterHtmlParsePipeline: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    override val onAfterParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onAfterLoadPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline()
): AbstractLoadEventHandler(
    onFilterPipeline, onNormalizePipeline,
    onBeforeLoadPipeline,
    onBeforeFetchPipeline, onAfterFetchPipeline,
    onBeforeParsePipeline, onBeforeHtmlParsePipeline,
    onBeforeExtractPipeline, onAfterExtractPipeline,
    onAfterHtmlParsePipeline, onAfterParsePipeline,
    onAfterLoadPipeline
), LoadEventPipelineHandler

interface JsEventHandler {
    suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any?
    suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any?
}

abstract class AbstractJsEventHandler: JsEventHandler {
    private val log = getLogger(AbstractJsEventHandler::class)

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
            log.takeIf { verbose }?.info("Evaluate expression >>>$it<<<")
            val v = evaluate(driver, it)
            if (v is String) {
                val s = Strings.stripNonPrintableChar(v)
                log.takeIf { verbose }?.info("Result >>>$s<<<")
            } else if (v is Int || v is Long) {
                log.takeIf { verbose }?.info("Result >>>$v<<<")
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
    val beforeComputeExpressions: Iterable<String> = listOf(),
    val afterComputeExpressions: Iterable<String> = listOf()
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
    val onFilter: (UrlAware) -> UrlAware?
    val onNormalize: (UrlAware) -> UrlAware?
    val onBeforeLoad: (UrlAware) -> Unit
    val onLoad: (UrlAware) -> Unit
    val onAfterLoad: (UrlAware, WebPage) -> Unit
}

abstract class AbstractCrawlEventHandler(
    override val onFilter: (UrlAware) -> UrlAware? = { it },
    override val onNormalize: (UrlAware) -> UrlAware? = { it },
    override val onBeforeLoad: (UrlAware) -> Unit = {},
    override val onLoad: (UrlAware) -> Unit = {},
    override val onAfterLoad: (UrlAware, WebPage) -> Unit = { _, _ -> }
): CrawlEventHandler

open class DefaultCrawlEventHandler(
    val onFilterPipeline: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    val onNormalizePipeline: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    val onBeforeLoadPipeline: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    val onLoadPipeline: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    val onAfterLoadPipeline: UrlAwareWebPageHandlerPipeline = UrlAwareWebPageHandlerPipeline()
): AbstractCrawlEventHandler(onFilterPipeline, onNormalizePipeline, onBeforeLoadPipeline, onLoadPipeline, onAfterLoadPipeline)

/**
 * TODO: is it necessary?
 * */
class CrawlEventHandlerPipeline: CrawlEventHandler {
    private val registeredHandlers: MutableList<CrawlEventHandler> = Collections.synchronizedList(mutableListOf())

    fun addFirst(handler: CrawlEventHandler) {
        registeredHandlers.add(0, handler)
    }

    fun addFirst(vararg handlers: CrawlEventHandler) {
        handlers.forEach { addFirst(it) }
    }

    fun addLast(handler: CrawlEventHandler) {
        registeredHandlers.add(handler)
    }

    fun addLast(vararg handlers: CrawlEventHandler) {
        handlers.forEach { addFirst(it) }
    }

    override val onFilter: (UrlAware) -> UrlAware? = { url ->
        var result: UrlAware? = url
        registeredHandlers.forEach { result = it.onFilter(url) }
        result
    }

    override val onNormalize: (UrlAware) -> UrlAware?  = { url ->
        var result: UrlAware? = url
        registeredHandlers.forEach { result = it.onNormalize(url) }
        result
    }

    override val onBeforeLoad: (UrlAware) -> Unit = { url ->
        registeredHandlers.forEach { it.onBeforeLoad(url) }
    }

    override val onLoad: (UrlAware) -> Unit = { url ->
        registeredHandlers.forEach { it.onLoad(url) }
    }

    override val onAfterLoad: (UrlAware, WebPage) -> Unit = { url, page ->
        registeredHandlers.forEach { it.onAfterLoad(url, page) }
    }
}
