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

interface EventHandlerPipeline {
    val size: Int
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty
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

class VoidEventHandlerPipeline: VoidEventHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<VoidEventHandler>()

    override val size: Int
        get() = registeredHandlers.size

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

class UrlAwareHandlerPipeline: UrlAwareHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<(UrlAware) -> Unit>()

    override val size: Int
        get() = registeredHandlers.size

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

class UrlAwareFilterPipeline: UrlAwareFilter(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<(UrlAware) -> UrlAware?>()

    override val size: Int
        get() = registeredHandlers.size

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

class UrlFilterPipeline: UrlFilter(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<UrlFilter>()

    override val size: Int
        get() = registeredHandlers.size

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

class UrlHandlerPipeline: UrlHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<(String) -> Unit>()

    override val size: Int
        get() = registeredHandlers.size

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

class WebPageHandlerPipeline: WebPageHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<WebPageHandler>()

    override val size: Int
        get() = registeredHandlers.size

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

class UrlAwareWebPageHandlerPipeline: UrlAwareWebPageHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<(UrlAware, WebPage?) -> Unit>()

    override val size: Int
        get() = registeredHandlers.size

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

class HtmlDocumentHandlerPipeline: HtmlDocumentHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<HtmlDocumentHandler>()

    override val size: Int
        get() = registeredHandlers.size

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

class WebDriverHandlerPipeline: WebDriverHandler(), EventHandlerPipeline {
    private val registeredHandlers = mutableListOf<WebDriverHandler>()

    override val size: Int
        get() = registeredHandlers.size

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
    val onFilter: UrlFilterPipeline
    val onNormalize: UrlFilterPipeline
    val onBeforeLoad: UrlHandlerPipeline
    val onBeforeFetch: WebPageHandlerPipeline
    val onBeforeBrowserLaunch: VoidEventHandlerPipeline
    val onAfterBrowserLaunch: WebDriverHandlerPipeline
    val onAfterFetch: WebPageHandlerPipeline
    val onBeforeParse: WebPageHandlerPipeline
    val onBeforeHtmlParse: WebPageHandlerPipeline
    val onBeforeExtract: WebPageHandlerPipeline
    val onAfterExtract: HtmlDocumentHandlerPipeline
    val onAfterHtmlParse: HtmlDocumentHandlerPipeline
    val onAfterParse: WebPageHandlerPipeline
    val onAfterLoad: WebPageHandlerPipeline
}

abstract class AbstractLoadEventHandler(
    override val onFilter: UrlFilterPipeline = UrlFilterPipeline(),
    override val onNormalize: UrlFilterPipeline = UrlFilterPipeline(),
    override val onBeforeLoad: UrlHandlerPipeline = UrlHandlerPipeline(),
    override val onBeforeFetch: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeBrowserLaunch: VoidEventHandlerPipeline = VoidEventHandlerPipeline(),
    override val onAfterBrowserLaunch: WebDriverHandlerPipeline = WebDriverHandlerPipeline(),
    override val onAfterFetch: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeParse: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeHtmlParse: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onBeforeExtract: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onAfterExtract: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    override val onAfterHtmlParse: HtmlDocumentHandlerPipeline = HtmlDocumentHandlerPipeline(),
    override val onAfterParse: WebPageHandlerPipeline = WebPageHandlerPipeline(),
    override val onAfterLoad: WebPageHandlerPipeline = WebPageHandlerPipeline()
): LoadEventHandler

open class DefaultLoadEventHandler: AbstractLoadEventHandler()

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
    val onBeforeCheckDOMState: WebPageWebDriverHandlerPipeline
    val onAfterCheckDOMState: WebPageWebDriverHandlerPipeline
    val onBeforeComputeFeature: WebPageWebDriverHandlerPipeline
    val onAfterComputeFeature: WebPageWebDriverHandlerPipeline
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

    override val onBeforeCheckDOMState: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onAfterCheckDOMState: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onBeforeComputeFeature: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
    override val onAfterComputeFeature: WebPageWebDriverHandlerPipeline = WebPageWebDriverHandlerPipeline()
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

class ExpressionSimulateEventHandler(
    val beforeComputeExpressions: Iterable<String> = listOf(),
    val afterComputeExpressions: Iterable<String> = listOf()
): AbstractSimulateEventHandler() {
    constructor(bcExpressions: String, acExpressions2: String, delimiters: String = ";"): this(
        bcExpressions.split(delimiters), acExpressions2.split(delimiters))

    init {
        onBeforeComputeFeature.addFirst(object: AbstractWebPageWebDriverHandler() {
            override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
                return evaluate(driver, beforeComputeExpressions)
            }
        })

        onAfterComputeFeature.addFirst(object: AbstractWebPageWebDriverHandler() {
            override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
                return evaluate(driver, afterComputeExpressions)
            }
        })
    }
}

class DefaultSimulateEventHandler: AbstractSimulateEventHandler()

interface CrawlEventHandler {
    val onFilter: UrlAwareFilterPipeline
    val onNormalize: UrlAwareFilterPipeline
    val onBeforeLoad: UrlAwareHandlerPipeline
    val onLoad: UrlAwareHandlerPipeline
    val onAfterLoad: UrlAwareWebPageHandlerPipeline
}

abstract class AbstractCrawlEventHandler(
    override val onFilter: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onNormalize: UrlAwareFilterPipeline = UrlAwareFilterPipeline(),
    override val onBeforeLoad: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onLoad: UrlAwareHandlerPipeline = UrlAwareHandlerPipeline(),
    override val onAfterLoad: UrlAwareWebPageHandlerPipeline = UrlAwareWebPageHandlerPipeline()
): CrawlEventHandler

class DefaultCrawlEventHandler: AbstractCrawlEventHandler()

interface PulsarEventHandler {
    val loadEventHandler: LoadEventHandler
    val simulateEventHandler: SimulateEventHandler
    val crawlEventHandler: CrawlEventHandler
}

abstract class AbstractPulsarEventHandler(
    override val loadEventHandler: AbstractLoadEventHandler,
    override val simulateEventHandler: AbstractSimulateEventHandler,
    override val crawlEventHandler: AbstractCrawlEventHandler
): PulsarEventHandler

open class DefaultPulsarEventHandler(
    loadEventHandler: DefaultLoadEventHandler = DefaultLoadEventHandler(),
    simulateEventHandler: DefaultSimulateEventHandler = DefaultSimulateEventHandler(),
    crawlEventHandler: DefaultCrawlEventHandler = DefaultCrawlEventHandler()
): AbstractPulsarEventHandler(loadEventHandler, simulateEventHandler, crawlEventHandler)
