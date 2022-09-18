package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

interface NamedHandler {
    val name: String
    val priority: Int
    val isRelevant: Boolean
}

abstract class AbstractNamedHandler: NamedHandler {
    override val name: String = ""
    override val priority: Int = 0
    override val isRelevant: Boolean = true
}

interface CombinedEventHandler {
    val size: Int
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty

    fun remove(handler: Any): Boolean
    fun clear()
}

abstract class VoidHandler: () -> Unit, AbstractNamedHandler() {
    abstract override operator fun invoke()
}

abstract class UrlAwareHandler: (UrlAware) -> UrlAware?, AbstractNamedHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractNamedHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> String?, AbstractNamedHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class UrlFilter: (String) -> String?, AbstractNamedHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(page: WebPage): Any?
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage?) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage?): Any?
}

abstract class HTMLDocumentHandler: (WebPage, FeaturedDocument) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument): Any?
}

abstract class PrivacyContextHandler: (PrivacyContext) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(privacyContext: PrivacyContext): Any?
}

abstract class WebDriverHandler: (WebDriver) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(driver: WebDriver): Any?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any?
}

abstract class WebPageWebDriverHandler: (WebPage, WebDriver) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): Any?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any?
}

abstract class WebDriverFetchResultHandler: (WebPage, WebDriver) -> FetchResult?, AbstractNamedHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): FetchResult?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): FetchResult?
}

abstract class AbstractCombinedEventHandler<T: NamedHandler>: CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<T>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: T): AbstractCombinedEventHandler<T> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: T): AbstractCombinedEventHandler<T> {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    abstract operator fun invoke()
}

class VoidEventHandler: VoidHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<VoidHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: () -> Unit) = addFirst(object: VoidHandler() {
        override fun invoke() = handler()
    })

    fun addFirst(handler: VoidHandler): VoidEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: () -> Unit) = addLast(object: VoidHandler() {
        override fun invoke() = handler()
    })

    fun addLast(handler: VoidHandler): VoidEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke() {
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { it() }
    }
}

class UrlAwareEventHandler: UrlAwareHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<UrlAwareHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: (UrlAware) -> UrlAware?) = addFirst(object: UrlAwareHandler() {
        override fun invoke(url: UrlAware) = handler.invoke(url)
    })

    fun addFirst(handler: UrlAwareHandler): UrlAwareEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: (UrlAware) -> UrlAware?) = addLast(object: UrlAwareHandler() {
        override fun invoke(url: UrlAware) = handler.invoke(url)
    })

    fun addLast(handler: UrlAwareHandler): UrlAwareEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(url: UrlAware): UrlAware? {
        var result: UrlAware? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(url) }
        return result
    }
}

class UrlAwareEventFilter: UrlAwareFilter(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<UrlAwareFilter>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: (UrlAware) -> UrlAware?) = addFirst(object: UrlAwareFilter() {
        override fun invoke(url: UrlAware) = handler(url)
    })

    fun addFirst(handler: UrlAwareFilter): UrlAwareEventFilter {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: (UrlAware) -> UrlAware?) = addLast(object: UrlAwareFilter() {
        override fun invoke(url: UrlAware) = handler(url)
    })

    fun addLast(handler: UrlAwareFilter): UrlAwareEventFilter {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(url: UrlAware): UrlAware? {
        var result: UrlAware? = url
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(url) }
        return result
    }
}

class UrlFilterEventHandler: UrlFilter(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<UrlFilter>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: UrlFilter): UrlFilterEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (String) -> String?) = addFirst(object : UrlFilter() {
        override fun invoke(url: String) = handler(url)
    })

    fun addLast(handler: UrlFilter): UrlFilterEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(handler: (String) -> String?) = addLast(object : UrlFilter() {
        override fun invoke(url: String) = handler(url)
    })

    fun remove(handler: (String) -> String) {
        registeredHandlers.removeIf { it == handler }
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(url: String): String? {
        var result: String? = url
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(url) }
        return result
    }
}

class UrlEventHandler: UrlHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<UrlHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: (String) -> String?) = addFirst(object : UrlHandler() {
        override fun invoke(url: String) = handler(url)
    })

    fun addFirst(handler: UrlHandler): UrlEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: (String) -> String?) = addLast(object : UrlHandler() {
        override fun invoke(url: String) = handler(url)
    })

    fun addLast(handler: UrlHandler): UrlEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    fun remove(handler: (String) -> String?) {
        registeredHandlers.removeIf { it == handler }
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(url: String): String? {
        var result: String? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(url) }
        return result
    }
}

class WebPageEventHandler: WebPageHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<WebPageHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: WebPageHandler): WebPageEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (WebPage) -> Any?) = addFirst(object : WebPageHandler() {
        override fun invoke(page: WebPage) = handler(page)
    })

    fun addLast(handler: (WebPage) -> Any?) = addLast(object: WebPageHandler() {
        override fun invoke(page: WebPage) = handler(page)
    })

    fun addLast(handler: WebPageHandler): WebPageEventHandler {
        registeredHandlers += handler
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(page: WebPage): Any? {
        var result: Any? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(page) }
        return result
    }
}

class UrlAwareWebPageEventHandler: UrlAwareWebPageHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<UrlAwareWebPageHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: UrlAwareWebPageHandler): UrlAwareWebPageEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (UrlAware, WebPage?) -> Any?) = addFirst(object : UrlAwareWebPageHandler() {
        override fun invoke(url: UrlAware, page: WebPage?) = handler(url, page)
    })

    fun addLast(handler: UrlAwareWebPageHandler): UrlAwareWebPageEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(handler: (UrlAware, WebPage?) -> Any?) = addLast(object : UrlAwareWebPageHandler() {
        override fun invoke(url: UrlAware, page: WebPage?) = handler(url, page)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(url: UrlAware, page: WebPage?): Any? {
        var result: Any? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(url, page) }
        return result
    }
}

class HTMLDocumentEventHandler: HTMLDocumentHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<HTMLDocumentHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: (WebPage, FeaturedDocument) -> Any?) = addFirst(object : HTMLDocumentHandler() {
        override fun invoke(page: WebPage, document: FeaturedDocument) = handler(page, document)
    })

    fun addFirst(handler: HTMLDocumentHandler): HTMLDocumentEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: (WebPage, FeaturedDocument) -> Any?) = addLast(object : HTMLDocumentHandler() {
        override fun invoke(page: WebPage, document: FeaturedDocument) = handler(page, document)
    })

    fun addLast(handler: HTMLDocumentHandler): HTMLDocumentEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override fun invoke(page: WebPage, document: FeaturedDocument): Any? {
        var result: Any? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(page, document) }
        return result
    }
}

class WebPageWebDriverEventHandler: AbstractWebPageWebDriverHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<WebPageWebDriverHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: suspend (WebPage, WebDriver) -> Any?): WebPageWebDriverEventHandler {
        registeredHandlers.add(0, object: AbstractWebPageWebDriverHandler() {
            override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
                return handler(page, driver)
            }
        })
        return this
    }

    fun addFirst(handler: WebPageWebDriverHandler): WebPageWebDriverEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: suspend (WebPage, WebDriver) -> Any?): WebPageWebDriverEventHandler {
        registeredHandlers.add(object: AbstractWebPageWebDriverHandler() {
            override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
                return handler(page, driver)
            }
        })
        return this
    }

    fun addLast(handler: WebPageWebDriverHandler): WebPageWebDriverEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        var result: Any? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it.invokeDeferred(page, driver) }
        return result
    }
}

abstract class AbstractWebDriverFetchResultHandler: WebDriverFetchResultHandler() {
    private val logger = getLogger(AbstractWebDriverFetchResultHandler::class)

    override fun invoke(page: WebPage, driver: WebDriver): FetchResult? {
        return runBlocking { invokeDeferred(page, driver) }
    }
}

abstract class PageDatumHandler: (String, PageDatum) -> Any?, AbstractNamedHandler() {
    abstract override operator fun invoke(pageSource: String, pageDatum: PageDatum): Any?
}

class PageDatumEventHandler: PageDatumHandler(), CombinedEventHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PageDatumHandler>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PageDatumHandler): PageDatumEventHandler {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(handler: (String, PageDatum) -> Any?) =
        addFirst(object : PageDatumHandler() {
            override fun invoke(pageSource: String, pageDatum: PageDatum) = handler(pageSource, pageDatum)
        })

    fun addLast(handler: PageDatumHandler): PageDatumEventHandler {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(handler: (String, PageDatum) -> Any?) = addLast(object : PageDatumHandler() {
        override fun invoke(pageSource: String, pageDatum: PageDatum) = handler(pageSource, pageDatum)
    })

    override fun remove(handler: Any): Boolean {
        return registeredHandlers.remove(handler)
    }

    override fun clear() {
        registeredHandlers.clear()
    }

    override operator fun invoke(pageSource: String, pageDatum: PageDatum): Any? {
        var result: Any? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { result = it(pageSource, pageDatum) }
        return result
    }
}

interface BrowseRPA {
    suspend fun warnUpBrowser(page: WebPage, driver: WebDriver)
    suspend fun waitForReferrer(page: WebPage, driver: WebDriver)
    suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver)
    suspend fun visit(url: String, driver: WebDriver)
}

open class DefaultBrowseRPA: BrowseRPA {
    companion object {
        const val PREV_PAGE_WILL_READY = 0
        const val PREV_PAGE_READY = 1
        const val PREV_PAGE_NEVER_READY = 2
    }

    private val isActive get() = AppContext.isActive
    private val logger = getLogger(this)

    override suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
        page.referrer?.let { visit(it, driver) }
    }

    override suspend fun waitForReferrer(page: WebPage, driver: WebDriver) {
        val referrer = page.referrer ?: return
        val referrerVisited = driver.browser.navigateHistory.any { it.url == referrer }
        if (!referrerVisited) {
            logger.debug("Visiting the referrer | {}", referrer)
            visit(referrer, driver)
        }
    }

    override suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 0
        var checkState = checkPreviousPage(driver)
        while (tick++ <= 180 && checkState.code == PREV_PAGE_WILL_READY) {
            if (checkState.message.isBlank()) {
                // No previous page, the browser has just started, don't crowd into.
                Runtimes.randomDelay(1_000, 10_000)
                break
            }

            // The last page does not load completely, wait for it.
            val shouldReport = (tick > 150 && tick % 10 == 0)
            if (shouldReport) {
                val urlToWait = checkState.message
                logger.info("Waiting for page | {} | {} <- {}", tick, urlToWait, page.url)
            }

            delay(1000L)
            checkState = checkPreviousPage(driver)
        }
    }

    override suspend fun visit(url: String, driver: WebDriver) {
        val display = driver.browser.id.display
        logger.info("Visiting with browser #{} | {}", display, url)

        driver.navigateTo(url)
        driver.waitForSelector("body")
        var n = 2 + Random.nextInt(5)
        while (n-- > 0 && isActive) {
            val deltaY = 100.0 + 20 * Random.nextInt(10)
            driver.mouseWheelDown(deltaY = deltaY)
            Runtimes.randomDelay(500, 500)
        }

        logger.debug("Visited | {}", url)
    }

    private fun checkPreviousPage(driver: WebDriver): CheckState {
        val navigateHistory = driver.browser.navigateHistory
        val now = Instant.now()

        val testNav = navigateHistory.lastOrNull { mayWaitFor(it, driver.navigateEntry) }

        val code = when {
            !isActive -> PREV_PAGE_NEVER_READY
            !driver.isWorking -> PREV_PAGE_NEVER_READY
            testNav == null -> PREV_PAGE_WILL_READY
            testNav.documentReadyTime > now -> PREV_PAGE_WILL_READY
            Duration.between(testNav.documentReadyTime, now).seconds > 10 -> PREV_PAGE_READY
            Duration.between(testNav.lastActiveTime, now).seconds > 60 -> PREV_PAGE_NEVER_READY
            else -> PREV_PAGE_WILL_READY
        }

        return CheckState(code, testNav?.url ?: "")
    }

    private fun mayWaitFor(currentEntry: NavigateEntry, testEntry: NavigateEntry): Boolean {
        val now = Instant.now()

        val may = testEntry.pageId > 0
                && !testEntry.stopped
                && testEntry.createTime < currentEntry.createTime
                && Duration.between(testEntry.lastActiveTime, now).seconds < 30

        return may
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
