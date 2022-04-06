package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

interface EventHandler {
    val name: String
}

abstract class AbstractEventHandler: EventHandler {
    override val name: String = ""
}

abstract class VoidEventHandler: AbstractEventHandler() {
    abstract operator fun invoke()
}

abstract class UrlAwareHandler: (UrlAware) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(url: UrlAware)
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractEventHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(url: String)
}

abstract class UrlFilter: (String) -> String?, AbstractEventHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(page: WebPage)
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage?) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage?)
}

abstract class HtmlDocumentHandler: (WebPage, FeaturedDocument) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument)
}

abstract class FetchResultHandler: (FetchResult) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(page: FetchResult)
}

abstract class WebPageBatchHandler: (Iterable<WebPage>) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

abstract class FetchResultBatchHandler: (Iterable<FetchResult>) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(pages: Iterable<FetchResult>)
}

abstract class PrivacyContextHandler: (PrivacyContext) -> Unit, AbstractEventHandler() {
    abstract override operator fun invoke(privacyContext: PrivacyContext)
}

abstract class WebDriverHandler: (WebDriver) -> Any?, AbstractEventHandler() {
    abstract override operator fun invoke(driver: WebDriver): Any?
}

abstract class WebPageWebDriverHandler: (WebPage, WebDriver) -> Any?, AbstractEventHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): Any?
}

class VoidEventHandlerPipeline: VoidEventHandler() {
    private val registeredHandlers = mutableListOf<VoidEventHandler>()

    fun addFirst(handler: VoidEventHandler): VoidEventHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: VoidEventHandler): VoidEventHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: VoidEventHandler): VoidEventHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: VoidEventHandler): VoidEventHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke() {
        registeredHandlers.forEach { it() }
    }
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
    private val registeredHandlers = mutableListOf<(UrlAware, WebPage?) -> Unit>()

    fun addFirst(handler: (UrlAware, WebPage?) -> Unit): UrlAwareWebPageHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: (UrlAware, WebPage?) -> Unit): UrlAwareWebPageHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: (UrlAware, WebPage?) -> Unit): UrlAwareWebPageHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: (UrlAware, WebPage?) -> Unit): UrlAwareWebPageHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(url: UrlAware, page: WebPage?) {
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

class WebDriverHandlerPipeline: WebDriverHandler(), EventHandler {
    private val registeredHandlers = mutableListOf<WebDriverHandler>()

    fun addFirst(handler: WebDriverHandler): WebDriverHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: WebDriverHandler): WebDriverHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: WebDriverHandler): WebDriverHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: WebDriverHandler): WebDriverHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(driver: WebDriver) {
        registeredHandlers.forEach { it(driver) }
    }
}

interface LoadEventHandler {
    val onFilter: UrlFilter
    val onNormalize: UrlFilter
    val onBeforeLoad: UrlHandler
    val onBeforeFetch: WebPageHandler
    val onBeforeBrowserLaunch: VoidEventHandler
    val onAfterBrowserLaunch: WebDriverHandler
    val onAfterFetch: WebPageHandler
    val onBeforeParse: WebPageHandler
    val onBeforeHtmlParse: WebPageHandler
    /**
     * TODO: not used yet
     * */
    val onBeforeExtract: WebPageHandler
    /**
     * TODO: not used yet
     * */
    val onAfterExtract: HtmlDocumentHandler
    val onAfterHtmlParse: HtmlDocumentHandler
    val onAfterParse: WebPageHandler
    val onAfterLoad: WebPageHandler
}

interface LoadEventPipelineHandler: LoadEventHandler {
    val onFilterPipeline: UrlFilterPipeline
    val onNormalizePipeline: UrlFilterPipeline
    val onBeforeLoadPipeline: UrlHandlerPipeline
    val onBeforeFetchPipeline: WebPageHandlerPipeline
    val onBeforeBrowserLaunchPipeline: VoidEventHandlerPipeline
    val onAfterBrowserLaunchPipeline: WebDriverHandlerPipeline
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
    override val onFilter: UrlFilter = UrlFilterPipeline(),
    override val onNormalize: UrlFilter = UrlFilterPipeline(),
    override val onBeforeLoad: UrlHandler = UrlHandlerPipeline(),
    override val onBeforeFetch: WebPageHandler = WebPageHandlerPipeline(),
    override val onBeforeBrowserLaunch: VoidEventHandler = VoidEventHandlerPipeline(),
    override val onAfterBrowserLaunch: WebDriverHandler = WebDriverHandlerPipeline(),
    override val onAfterFetch: WebPageHandler = WebPageHandlerPipeline(),
    override val onBeforeParse: WebPageHandler = WebPageHandlerPipeline(),
    override val onBeforeHtmlParse: WebPageHandler = WebPageHandlerPipeline(),
    override val onBeforeExtract: WebPageHandler = WebPageHandlerPipeline(),
    override val onAfterExtract: HtmlDocumentHandler = HtmlDocumentHandlerPipeline(),
    override val onAfterHtmlParse: HtmlDocumentHandler = HtmlDocumentHandlerPipeline(),
    override val onAfterParse: WebPageHandler = WebPageHandlerPipeline(),
    override val onAfterLoad: WebPageHandler = WebPageHandlerPipeline()
): LoadEventHandler

class EmptyLoadEventHandler: LoadEventHandler {
    override val onFilter: UrlFilter = UrlFilterPipeline()
    override val onNormalize: UrlFilter = UrlFilterPipeline()
    override val onBeforeLoad: UrlHandler = UrlHandlerPipeline()
    override val onBeforeFetch: WebPageHandler = WebPageHandlerPipeline()
    override val onBeforeBrowserLaunch: VoidEventHandler = VoidEventHandlerPipeline()
    override val onAfterBrowserLaunch: WebDriverHandler = WebDriverHandlerPipeline()
    override val onAfterFetch: WebPageHandler = WebPageHandlerPipeline()
    override val onBeforeParse: WebPageHandler = WebPageHandlerPipeline()
    override val onBeforeHtmlParse: WebPageHandler = WebPageHandlerPipeline()
    override val onBeforeExtract: WebPageHandler = WebPageHandlerPipeline()
    override val onAfterExtract: HtmlDocumentHandler = HtmlDocumentHandlerPipeline()
    override val onAfterHtmlParse: HtmlDocumentHandler = HtmlDocumentHandlerPipeline()
    override val onAfterParse: WebPageHandler = WebPageHandlerPipeline()
    override val onAfterLoad: WebPageHandler = WebPageHandlerPipeline()
}

open class DefaultLoadEventHandler(
    final override val onFilterPipeline: UrlFilterPipeline = UrlFilterPipeline(),
    final override val onNormalizePipeline: UrlFilterPipeline = UrlFilterPipeline(),
    final override val onBeforeLoadPipeline: UrlHandlerPipeline = UrlHandlerPipeline(),
    final override val onBeforeFetchPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onBeforeBrowserLaunchPipeline: VoidEventHandlerPipeline = VoidEventHandlerPipeline(),
    final override val onAfterBrowserLaunchPipeline: WebDriverHandlerPipeline = WebDriverHandlerPipeline(),
    final override val onAfterFetchPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onBeforeParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onBeforeHtmlParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onBeforeExtractPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onAfterExtractPipeline: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    final override val onAfterHtmlParsePipeline: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    final override val onAfterParsePipeline: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    final override val onAfterLoadPipeline: WebPageHandlerPipeline = WebPageHandlerPipeline()
): AbstractLoadEventHandler(
    onFilterPipeline, onNormalizePipeline,
    onBeforeLoadPipeline,

    onBeforeFetchPipeline,
    onBeforeBrowserLaunchPipeline, onAfterBrowserLaunchPipeline,
    onAfterFetchPipeline,

    onBeforeParsePipeline, onBeforeHtmlParsePipeline,
    onBeforeExtractPipeline, onAfterExtractPipeline,
    onAfterHtmlParsePipeline, onAfterParsePipeline,
    onAfterLoadPipeline
), LoadEventPipelineHandler

abstract class AbstractWebDriverHandler: WebDriverHandler() {
    private val logger = getLogger(AbstractWebDriverHandler::class)

    open val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(500)
            "type" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    open var verbose = false

    override val name: String = ""

    override fun invoke(driver: WebDriver): Any? {
        return runBlocking { invokeDeferred(driver) }
    }

    abstract suspend fun invokeDeferred(driver: WebDriver): Any?

    protected suspend fun evaluate(driver: WebDriver, expressions: Iterable<String>): Any? {
        var value: Any? = null
        val validExpressions = expressions
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .filterNot { it.startsWith("// ") }
        validExpressions.forEach {
            logger.takeIf { verbose }?.info("Evaluate expression >>>$it<<<")
            val v = evaluate(driver, it)
            if (v is String) {
                val s = Strings.stripNonPrintableChar(v)
                logger.takeIf { verbose }?.info("Result >>>$s<<<")
            } else if (v is Int || v is Long) {
                logger.takeIf { verbose }?.info("Result >>>$v<<<")
            }
            value = v
        }
        return value
    }

    protected suspend fun evaluate(driver: WebDriver, expression: String): Any? {
        delayPolicy("evaluate").takeIf { it > 0 }?.let { delay(it) }
        return driver.evaluate(expression)
    }
}

abstract class AbstractWebPageWebDriverHandler: WebPageWebDriverHandler() {
    private val logger = getLogger(AbstractWebPageWebDriverHandler::class)

    open val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(500)
            "type" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    open var verbose = false

    override val name: String = ""

    override fun invoke(page: WebPage, driver: WebDriver): Any? {
        return runBlocking { invokeDeferred(page, driver) }
    }

    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any?

    protected suspend fun evaluate(driver: WebDriver, expressions: Iterable<String>): Any? {
        var value: Any? = null
        val validExpressions = expressions
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .filterNot { it.startsWith("// ") }
        validExpressions.forEach {
            logger.takeIf { verbose }?.info("Evaluate expression >>>$it<<<")
            val v = evaluate(driver, it)
            if (v is String) {
                val s = Strings.stripNonPrintableChar(v)
                logger.takeIf { verbose }?.info("Result >>>$s<<<")
            } else if (v is Int || v is Long) {
                logger.takeIf { verbose }?.info("Result >>>$v<<<")
            }
            value = v
        }
        return value
    }

    protected suspend fun evaluate(driver: WebDriver, expression: String): Any? {
        delayPolicy("evaluate").takeIf { it > 0 }?.let { delay(it) }
        return driver.evaluate(expression)
    }
}

open class EmptyWebDriverHandler: AbstractWebPageWebDriverHandler() {
    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        return null
    }
}

interface SimulateEventHandler {
    val onBeforeCheckDOMState: AbstractWebPageWebDriverHandler
    val onAfterCheckDOMState: AbstractWebPageWebDriverHandler
    val onBeforeComputeFeature: AbstractWebPageWebDriverHandler
    val onAfterComputeFeature: AbstractWebPageWebDriverHandler
}

interface SimulateEventPipelineHandler: SimulateEventHandler {
    val onBeforeCheckDOMStatePipeline: WebPageWebDriverHandlerPipeline
    val onAfterCheckDOMStatePipeline: WebPageWebDriverHandlerPipeline
    val onBeforeComputeFeaturePipeline: WebPageWebDriverHandlerPipeline
    val onAfterComputeFeaturePipeline: WebPageWebDriverHandlerPipeline
}

abstract class AbstractSimulateEventHandler: SimulateEventHandler {
    open val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "click" -> 500L + Random.nextInt(500)
            "type" -> 500L + Random.nextInt(500)
            else -> 100L + Random.nextInt(500)
        }
    }

    open var verbose = false

    protected suspend fun smartDelay() = delay(delayPolicy(""))

    protected suspend fun smartDelay(type: String) = delay(delayPolicy(type))
}

abstract class AbstractSimulateEventPipelineHandler: AbstractSimulateEventHandler(), SimulateEventPipelineHandler {
    override val onBeforeCheckDOMState: AbstractWebPageWebDriverHandler get() = onBeforeCheckDOMStatePipeline
    override val onAfterCheckDOMState: AbstractWebPageWebDriverHandler get() = onAfterCheckDOMStatePipeline
    override val onBeforeComputeFeature: AbstractWebPageWebDriverHandler get() = onBeforeComputeFeaturePipeline
    override val onAfterComputeFeature: AbstractWebPageWebDriverHandler get() = onAfterComputeFeaturePipeline
}

class WebPageWebDriverHandlerPipeline: AbstractWebPageWebDriverHandler() {
    private val registeredHandlers = mutableListOf<AbstractWebPageWebDriverHandler>()

    fun addFirst(handler: AbstractWebPageWebDriverHandler): WebPageWebDriverHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: AbstractWebPageWebDriverHandler): WebPageWebDriverHandlerPipeline {
        handlers.forEach { addFirst(it) }
        return this
    }

    fun addLast(handler: AbstractWebPageWebDriverHandler): WebPageWebDriverHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: AbstractWebPageWebDriverHandler): WebPageWebDriverHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver) {
        registeredHandlers.forEach { it.invokeDeferred(page, driver) }
    }
}

class EmptySimulateEventHandler: AbstractSimulateEventHandler() {
    override val onBeforeCheckDOMState: AbstractWebPageWebDriverHandler get() = EmptyWebDriverHandler()
    override val onAfterCheckDOMState: AbstractWebPageWebDriverHandler get() = EmptyWebDriverHandler()
    override val onBeforeComputeFeature: AbstractWebPageWebDriverHandler get() = EmptyWebDriverHandler()
    override val onAfterComputeFeature: AbstractWebPageWebDriverHandler get() = EmptyWebDriverHandler()
}

class ExpressionSimulateEventHandler(
    val beforeComputeExpressions: Iterable<String> = listOf(),
    val afterComputeExpressions: Iterable<String> = listOf()
): AbstractSimulateEventHandler() {
    constructor(bcExpressions: String, acExpressions2: String, delimiters: String = ";"): this(
        bcExpressions.split(delimiters), acExpressions2.split(delimiters))

    override val onBeforeComputeFeature: AbstractWebPageWebDriverHandler = object: AbstractWebPageWebDriverHandler() {
        override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
            return evaluate(driver, beforeComputeExpressions)
        }
    }

    override val onAfterComputeFeature: AbstractWebPageWebDriverHandler = object: AbstractWebPageWebDriverHandler() {
        override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
            return evaluate(driver, afterComputeExpressions)
        }
    }

    override val onBeforeCheckDOMState: AbstractWebPageWebDriverHandler
        get() = TODO("Not yet implemented")

    override val onAfterCheckDOMState: AbstractWebPageWebDriverHandler
        get() = TODO("Not yet implemented")
}

class DefaultSimulateEventHandler(
    override val onBeforeCheckDOMStatePipeline: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline(),
    override val onAfterCheckDOMStatePipeline: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline(),
    override val onBeforeComputeFeaturePipeline: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline(),
    override val onAfterComputeFeaturePipeline: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline(),
): AbstractSimulateEventPipelineHandler()

interface CrawlEventHandler {
    val onFilter: (UrlAware) -> UrlAware?
    val onNormalize: (UrlAware) -> UrlAware?
    val onBeforeLoad: (UrlAware) -> Unit
    val onLoad: (UrlAware) -> Unit
    val onAfterLoad: (UrlAware, WebPage?) -> Unit
}

abstract class AbstractCrawlEventHandler(
    override val onFilter: (UrlAware) -> UrlAware? = { it },
    override val onNormalize: (UrlAware) -> UrlAware? = { it },
    override val onBeforeLoad: (UrlAware) -> Unit = {},
    override val onLoad: (UrlAware) -> Unit = {},
    override val onAfterLoad: (UrlAware, WebPage?) -> Unit = { _, _ -> }
): CrawlEventHandler

interface CrawlEventPipelineHandler: CrawlEventHandler {
    val onFilterPipeline: UrlAwareFilterPipeline
    val onNormalizePipeline: UrlAwareFilterPipeline
    val onBeforeLoadPipeline: UrlAwareHandlerPipeline
    val onLoadPipeline: UrlAwareHandlerPipeline
    val onAfterLoadPipeline: UrlAwareWebPageHandlerPipeline
}

abstract class AbstractCrawlEventPipelineHandler(
    override val onFilterPipeline: UrlAwareFilterPipeline,
    override val onNormalizePipeline: UrlAwareFilterPipeline,
    override val onBeforeLoadPipeline: UrlAwareHandlerPipeline,
    override val onLoadPipeline: UrlAwareHandlerPipeline,
    override val onAfterLoadPipeline: UrlAwareWebPageHandlerPipeline
): CrawlEventPipelineHandler {
    override val onFilter: (UrlAware) -> UrlAware? = { url -> onFilterPipeline(url) }
    override val onNormalize: (UrlAware) -> UrlAware? = { url -> onNormalizePipeline(url) }
    override val onBeforeLoad: (UrlAware) -> Unit = { url -> onBeforeLoadPipeline(url) }
    override val onLoad: (UrlAware) -> Unit = { url -> onLoadPipeline(url) }
    override val onAfterLoad: (UrlAware, WebPage?) -> Unit = { url, page -> onAfterLoadPipeline(url, page) }
}

class EmptyCrawlEventHandler(
    override val onFilter: UrlAwareFilter = UrlAwareFilterPipeline(),
    override val onNormalize: UrlAwareFilter = UrlAwareFilterPipeline(),
    override val onBeforeLoad: UrlAwareHandler = UrlAwareHandlerPipeline(),
    override val onLoad: UrlAwareHandler = UrlAwareHandlerPipeline(),
    override val onAfterLoad: UrlAwareWebPageHandler = UrlAwareWebPageHandlerPipeline()
): AbstractCrawlEventHandler()

class DefaultCrawlEventHandler(
    override val onFilterPipeline: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onNormalizePipeline: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onBeforeLoadPipeline: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onLoadPipeline: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onAfterLoadPipeline: UrlAwareWebPageHandlerPipeline = UrlAwareWebPageHandlerPipeline()
): AbstractCrawlEventPipelineHandler(onFilterPipeline,
    onNormalizePipeline, onBeforeLoadPipeline, onLoadPipeline, onAfterLoadPipeline)

interface PulsarEventHandler {
    val loadEventHandler: LoadEventHandler
    val simulateEventHandler: SimulateEventHandler
    val crawlEventHandler: CrawlEventHandler
}

interface PulsarEventPipelineHandler: PulsarEventHandler {
    val loadEventPipelineHandler: LoadEventPipelineHandler
    val simulateEventPipelineHandler: SimulateEventPipelineHandler
    val crawlEventPipelineHandler: CrawlEventPipelineHandler
}

abstract class AbstractPulsarEventHandler(
    override val loadEventHandler: AbstractLoadEventHandler,
    override val simulateEventHandler: AbstractSimulateEventHandler,
    override val crawlEventHandler: AbstractCrawlEventHandler
): PulsarEventHandler

abstract class AbstractPulsarEventPipelineHandler(
    override val loadEventPipelineHandler: LoadEventPipelineHandler,
    override val simulateEventPipelineHandler: SimulateEventPipelineHandler,
    override val crawlEventPipelineHandler: CrawlEventPipelineHandler
): PulsarEventPipelineHandler {
    override val loadEventHandler: LoadEventHandler get() = loadEventPipelineHandler
    override val simulateEventHandler: SimulateEventHandler get() = simulateEventPipelineHandler
    override val crawlEventHandler: CrawlEventHandler get() = crawlEventPipelineHandler
}

open class DefaultPulsarEventPipelineHandler(
    loadEventPipelineHandler: LoadEventPipelineHandler = DefaultLoadEventHandler(),
    simulateEventPipelineHandler: SimulateEventPipelineHandler = DefaultSimulateEventHandler(),
    crawlEventPipelineHandler: CrawlEventPipelineHandler = DefaultCrawlEventHandler()
): AbstractPulsarEventPipelineHandler(loadEventPipelineHandler, simulateEventPipelineHandler, crawlEventPipelineHandler)
