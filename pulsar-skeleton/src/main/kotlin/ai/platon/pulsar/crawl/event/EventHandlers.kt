package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.lang.*
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.CopyOnWriteArrayList

interface PHandler

abstract class AbstractHandler: PHandler, PFunction {
    override val name: String = ""
    override val priority: Int = 0
    override val isRelevant: Boolean = true
}

abstract class AbstractHandler0<R>: AbstractHandler(), PFunction0<R>

abstract class AbstractHandler1<T, R>: AbstractHandler(), PFunction1<T, R>

abstract class AbstractHandler2<T, T2, R>: AbstractHandler(), PFunction2<T, T2, R>

abstract class AbstractHandler3<T, T2, T3, R>: AbstractHandler(), PFunction3<T, T2, T3, R>

abstract class AbstractDHandler0<R>: AbstractHandler(), PDFunction0<R>

abstract class AbstractDHandler1<T, R>: AbstractHandler(), PDFunction1<T, R>

abstract class AbstractDHandler2<T, T2, R>: AbstractHandler(), PDFunction2<T, T2, R>

abstract class AbstractDHandler3<T, T2, T3, R>: AbstractHandler(), PDFunction3<T, T2, T3, R>

interface CombinedHandler {
    val size: Int
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty

    fun remove(handler: Any): Boolean
    fun clear()
}

abstract class AbstractCombinedHandler0<R>: AbstractHandler0<R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction0<R>): AbstractCombinedHandler0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction0<R>): AbstractCombinedHandler0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractHandler0<R>() {
        override fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractHandler0<R>() {
        override fun invoke() = handler()
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it() }
        return r
    }
}

abstract class AbstractCombinedHandler1<T, R>: AbstractHandler1<T, R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction1<T, R>): AbstractCombinedHandler1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction1<T, R>): AbstractCombinedHandler1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractHandler1<T, R>() {
        override fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractHandler1<T, R>() {
        override fun invoke(param: T) = handler(param)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(param: T): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param) }
        return r
    }
}

abstract class AbstractCombinedHandler2<T, T2, R>: AbstractHandler2<T, T2, R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction2<T, T2, R>): AbstractCombinedHandler2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction2<T, T2, R>): AbstractCombinedHandler2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T, T2) -> R) = addFirst(object: AbstractHandler2<T, T2, R>() {
        override fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: (T, T2) -> R) = addLast(object: AbstractHandler2<T, T2, R>() {
        override fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(param: T, param2: T2): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param, param2) }
        return r
    }
}

abstract class AbstractCombinedDHandler0<R>: AbstractDHandler0<R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction0<R>): AbstractCombinedDHandler0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction0<R>): AbstractCombinedDHandler0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractDHandler0<R>() {
        override suspend fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractDHandler0<R>() {
        override suspend fun invoke() = handler()
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it() }
        return r
    }
}

abstract class AbstractCombinedDHandler1<T, R>: AbstractDHandler1<T, R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction1<T, R>): AbstractCombinedDHandler1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction1<T, R>): AbstractCombinedDHandler1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractDHandler1<T, R>() {
        override suspend fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractDHandler1<T, R>() {
        override suspend fun invoke(param: T) = handler(param)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(param: T): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param) }
        return r
    }
}

abstract class AbstractCombinedDHandler2<T, T2, R>: AbstractDHandler2<T, T2, R>(), CombinedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction2<T, T2, R>): AbstractCombinedDHandler2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction2<T, T2, R>): AbstractCombinedDHandler2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: suspend (T, T2) -> R) = addFirst(object: AbstractDHandler2<T, T2, R>() {
        override suspend fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: suspend (T, T2) -> R) = addLast(object: AbstractDHandler2<T, T2, R>() {
        override suspend fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(param: T, param2: T2): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param, param2) }
        return r
    }
}

abstract class VoidHandler: PFunction0<Unit>, AbstractHandler() {
    abstract override operator fun invoke()
}

abstract class UrlAwareHandler: (UrlAware) -> UrlAware?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlAwareFilter: (UrlAware) -> UrlAware?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware): UrlAware?
}

abstract class UrlHandler: (String) -> String?, AbstractHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class UrlFilter: (String) -> String?, AbstractHandler() {
    abstract override operator fun invoke(url: String): String?
}

abstract class WebPageHandler: (WebPage) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage): Any?
}

abstract class UrlAwareWebPageHandler: (UrlAware, WebPage?) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(url: UrlAware, page: WebPage?): Any?
}

abstract class HTMLDocumentHandler: (WebPage, FeaturedDocument) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, document: FeaturedDocument): Any?
}

abstract class PrivacyContextHandler: (PrivacyContext) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(privacyContext: PrivacyContext): Any?
}

abstract class WebDriverHandler: (WebDriver) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(driver: WebDriver): Any?
    abstract suspend fun invoke(page: WebPage, driver: WebDriver): Any?
}

abstract class WebPageWebDriverHandler: (WebPage, WebDriver) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): Any?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any?
}

abstract class WebDriverFetchResultHandler: (WebPage, WebDriver) -> FetchResult?, AbstractHandler() {
    abstract override operator fun invoke(page: WebPage, driver: WebDriver): FetchResult?
    abstract suspend fun invokeDeferred(page: WebPage, driver: WebDriver): FetchResult?
}

abstract class PageDatumHandler: (String, PageDatum) -> Any?, AbstractHandler() {
    abstract override operator fun invoke(pageSource: String, pageDatum: PageDatum): Any?
}

open class VoidEventHandler: AbstractCombinedHandler0<Unit>()

open class UrlAwareEventHandler: AbstractCombinedHandler1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlAwareEventFilter: AbstractCombinedHandler1<UrlAware, UrlAware>() {
    override fun invoke(url: UrlAware): UrlAware? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlFilterEventHandler: AbstractCombinedHandler1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class UrlEventHandler: AbstractCombinedHandler1<String, String?>() {
    override fun invoke(url: String): String? {
        return if (isEmpty) url else super.invoke(url)
    }
}

open class WebPageEventHandler: AbstractCombinedHandler1<WebPage, Any?>()

open class UrlAwareWebPageEventHandler: AbstractCombinedHandler2<UrlAware, WebPage?, Any?>()

open class HTMLDocumentEventHandler: AbstractCombinedHandler2<WebPage, FeaturedDocument, Any?>()

open class PageDatumEventHandler: AbstractCombinedHandler2<String, PageDatum, Any?>()

open class WebPageWebDriverEventHandler: AbstractCombinedDHandler2<WebPage, WebDriver, Any?>()
