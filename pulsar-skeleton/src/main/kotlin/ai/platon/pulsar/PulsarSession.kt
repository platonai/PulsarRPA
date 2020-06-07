package ai.platon.pulsar

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.selectNotNull
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class PulsarSession(
        /**
         * The pulsar context
         * */
        val context: PulsarContext,
        /**
         * The session scope volatile volatileConfig, every item is supposed to be changed at any time and any place
         * */
        val sessionConfig: VolatileConfig,
        /**
         * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
         * */
        val id: Int = 9000000 + idGen.incrementAndGet()
) : AutoCloseable {
    protected val log = LoggerFactory.getLogger(PulsarSession::class.java)
    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * TODO: session scoped?
     * */
    val sessionBeanFactory = BeanFactory(sessionConfig)
    private val variables = ConcurrentHashMap<String, Any>()
    private var enableCache = true
    // Session variables
    private val closableObjects = mutableSetOf<AutoCloseable>()
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && context.isActive

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) = ensureAlive { closableObjects.add(closable) }

    fun disableCache() = ensureAlive { enableCache = false }

    fun normalize(url: String, isItemOption: Boolean = false): NormUrl {
        ensureAlive()
        return context.normalize(url, isItemOption).also { initOptions(it.options) }
    }

    fun normalize(url: String, options: LoadOptions, isItemOption: Boolean = false): NormUrl {
        ensureAlive()
        return context.normalize(url, initOptions(options), isItemOption)
    }

    fun normalize(urls: Iterable<String>, isItemOption: Boolean = false): List<NormUrl> {
        ensureAlive()
        return context.normalize(urls, isItemOption).onEach { initOptions(it.options) }
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions, isItemOption: Boolean = false): List<NormUrl> {
        ensureAlive()
        return context.normalize(urls, options, isItemOption).onEach { initOptions(it.options) }
    }

    /**
     * Inject a url
     *
     * @param configuredUrl The url followed by volatileConfig options
     * @return The web page created
     */
    fun inject(configuredUrl: String): WebPage = ensureAlive { context.inject(configuredUrl) }

    fun getOrNil(url: String): WebPage = ensureAlive { context.getOrNil(url) }

    /**
     * Load a url with default options
     *
     * @param url The url followed by volatileConfig options
     * @return The Web page
     */
    @Throws(Exception::class)
    fun load(url: String): WebPage = load(normalize(url))

    @Throws(Exception::class)
    fun load(url: NormUrl): WebPage {
        ensureAlive()

        initOptions(url.options)

        return if (enableCache) {
            val cache = PulsarContext.pageCache
            cache.get(url.url) ?: context.load(url).also { cache.put(it.url, it) }
        } else {
            context.load(url)
        }
    }

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    @Throws(Exception::class)
    fun load(url: String, args: String): WebPage = load(url, LoadOptions.parse(args))

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    @Throws(Exception::class)
    fun load(url: String, options: LoadOptions): WebPage {
        ensureAlive()
        val normUrl = normalize(url, options)
        return load(normUrl)
    }

    @Throws(Exception::class)
    suspend fun loadDeferred(url: String): WebPage {
        ensureAlive()
        return loadDeferred(normalize(url))
    }

    @Throws(Exception::class)
    suspend fun loadDeferred(url: NormUrl): WebPage {
        ensureAlive()

        initOptions(url.options)

        return if (enableCache) {
            val cache = PulsarContext.pageCache
            cache.get(url.url) ?: context.loadDeferred(url).also { cache.put(it.url, it) }
        } else context.loadDeferred(url)
    }

    @Throws(Exception::class)
    suspend fun loadDeferred(url: String, options: LoadOptions): WebPage {
        ensureAlive()
        val normUrl = normalize(url, options)
        return loadDeferred(normUrl)
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions, itemPages: Boolean = false): Collection<WebPage> {
        ensureAlive()
        val normUrls = normalize(urls, options, itemPages)
        val opt = normUrls.firstOrNull()?.options ?: return listOf()

        return if (enableCache) {
            getCachedOrLoadAll(normUrls, opt)
        } else {
            context.loadAll(normUrls, opt)
        }
    }

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions, itemPages: Boolean = false): Collection<WebPage> {
        ensureAlive()
        options.preferParallel = true
        val normUrls = normalize(urls, options, itemPages)
        val opt = normUrls.firstOrNull()?.options ?: return listOf()

        return if (enableCache) {
            getCachedOrLoadAll(normUrls, opt)
        } else {
            context.loadAll(normUrls, opt)
        }
    }

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load args
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage> {
        return loadOutPages(portalUrl, LoadOptions.parse(args))
    }

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        val outlinkSelector = appendSelectorIfMissing(options.outlinkSelector, "a")
        val normUrl = normalize(portalUrl, options)

        val opt = normUrl.options
        val links = parse(load(normUrl)).document.selectNotNull(outlinkSelector) {
            getLink(it, !opt.noNorm, opt.ignoreUrlQuery)?.substringBeforeLast("#")
        }.toSet().take(opt.topLinks)

        return loadAll(links, normUrl.options.createItemOption())
    }

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    fun parse(page: WebPage, noCache: Boolean = false): FeaturedDocument {
        ensureAlive()

        if (page.isNil) {
            return FeaturedDocument.NIL
        }

        if (noCache) {
            return context.parse(page)
        }

        val url = page.url
        if (!enableCache) {
            return context.parse(page)
        }

        var document = PulsarContext.documentCache.get(url)
        if (document == null) {
            // TODO: review if the synchronization is correct and necessary
            synchronized(PulsarContext.documentCache) {
                document = PulsarContext.documentCache.get(url)
                if (document == null) {
                    document = context.parse(page)
                    PulsarContext.documentCache.put(url, document)
                }
            }
        }

        return document
    }

    fun loadAndParse(url: String, options: LoadOptions = LoadOptions.create()): FeaturedDocument {
        ensureAlive()
        val normUrl = normalize(url, options)
        return parse(load(normUrl))
    }

    fun cache(page: WebPage): WebPage = page.also { PulsarContext.pageCache.put(it.url, it) }

    fun removePageCache(url: String): WebPage? = PulsarContext.pageCache.remove(url)

    fun removePageCache(urls: Iterable<String>) = urls.forEach { removePageCache(it) }

    fun cache(doc: FeaturedDocument): FeaturedDocument = doc.also { PulsarContext.documentCache.put(it.location, it) }

    fun removeDocumentCache(url: String): FeaturedDocument? = PulsarContext.documentCache.remove(url)

    fun removeDocumentCache(urls: Iterable<String>) = urls.forEach { removeDocumentCache(it) }

    private fun getCachedOrGet(url: String): WebPage? {
        ensureAlive()
        var page: WebPage? = PulsarContext.pageCache.get(url)
        if (page != null) {
            return page
        }

        page = context.get(url)
        PulsarContext.pageCache.put(url, page)

        return page
    }

    private fun getCachedOrLoadAll(urls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        ensureAlive()
        urls.forEach { initOptions(it.options) }
        initOptions(options)

        val pages = ArrayList<WebPage>()
        val pendingUrls = ArrayList<NormUrl>()

        for (url in urls) {
            val page = PulsarContext.pageCache.get(url.url)
            if (page != null) {
                pages.add(page)
            } else {
                pendingUrls.add(url)
            }
        }

        val freshPages = if (options.preferParallel) {
            context.parallelLoadAll(pendingUrls, options)
        } else {
            context.loadAll(pendingUrls, options)
        }

        pages.addAll(freshPages)

        // Notice: we do not cache batch loaded pages, batch loaded pages are not used frequently
        // do not do this: sessionCachePutAll(freshPages);

        return pages
    }

    fun getVariable(name: String): Any? = ensureAlive(null) { variables[name] }

    fun setVariable(name: String, value: Any) = ensureAlive { variables[name] = value }

    fun putSessionBean(obj: Any) = ensureAlive { sessionBeanFactory.putBean(obj) }

    inline fun <reified T> getSessionBean(): T? {
        return sessionBeanFactory.getBean()
    }

    fun delete(url: String) = ensureAlive { context.delete(url) }

    fun flush() = ensureAlive { context.webDb.flush() }

    fun persist(page: WebPage) = ensureAlive { context.webDb.put(page) }

    fun export(page: WebPage, ident: String = ""): Path {
        ensureAlive()
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(page.contentAsString, path, true)
    }

    fun export(doc: FeaturedDocument, ident: String = ""): Path {
        ensureAlive()
        val filename = AppPaths.fromUri(doc.location, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(doc.prettyHtml, path, true)
    }

    fun exportTo(doc: FeaturedDocument, path: Path): Path {
        ensureAlive()
        return AppFiles.saveTo(doc.prettyHtml.toByteArray(), path, true)
    }

    override fun equals(other: Any?): Boolean {
        return other === this || (other is PulsarSession && other.id == id)
    }

    override fun hashCode(): Int = id

    override fun toString(): String = "#$id"

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            context.webDb.flush()
            closableObjects.forEach { o -> o.close() }

            log.debug("Pulsar session #{} is closed. Used memory: {}, free memory: {}",
                    id,
                    Strings.readableBytes(Systems.memoryUsed),
                    Strings.readableBytes(Systems.memoryFree))
        }
    }

    private fun getLink(ele: Element, normalize: Boolean = false, ignoreQuery: Boolean = false): String? {
        var link = ele.attr("abs:href")
        if (normalize) link = normalize(link).takeIf { it.isValid }?.url
        if (link != null && ignoreQuery) link = Urls.getUrlWithoutParameters(link)
        return link
    }

    private fun initOptions(options: LoadOptions): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = sessionConfig
        }
        return options
    }

    private fun ensureAlive(): Boolean {
        if (!isActive) {
            return false
        }

        return true
    }

    private fun <T> ensureAlive(action: () -> T): T
            = if (isActive) action() else throw RuntimeException("Pulsar session is not alive")

    private fun <T> ensureAlive(defaultValue: T, action: () -> T): T = defaultValue.takeIf { !isActive } ?: action()

    companion object {
        val SESSION_PAGE_CACHE_TTL = Duration.ofMinutes(5)!!
        const val SESSION_PAGE_CACHE_CAPACITY = 100

        val SESSION_DOCUMENT_CACHE_TTL = Duration.ofMinutes(10)!!
        const val SESSION_DOCUMENT_CACHE_CAPACITY = 100
        private val idGen = AtomicInteger()
    }
}
