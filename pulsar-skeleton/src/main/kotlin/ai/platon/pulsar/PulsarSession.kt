package ai.platon.pulsar

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.BeanFactory
import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.concurrent.ExpiringItem
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.firstTextOrNull
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
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
        val context: AbstractPulsarContext,
        /**
         * The session scope volatile config, every setting is supposed to be changed at any time and any place
         * */
        val sessionConfig: VolatileConfig,
        /**
         * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
         * */
        val id: Int = nextId
) : AutoCloseable {

    companion object {
        const val ID_CAPACITY = 1_000_000
        const val ID_START = 1_000_000
        const val ID_END = ID_START + ID_CAPACITY - 1

        private val idGen = AtomicInteger()
        private val nextId get() = ID_START + idGen.incrementAndGet()
        private fun opts() = LoadOptions.create()
    }

    private val log = LoggerFactory.getLogger(PulsarSession::class.java)

    val unmodifiedConfig get() = context.unmodifiedConfig
    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * TODO: session scoped?
     * */
    val sessionBeanFactory = BeanFactory(sessionConfig)

    val display = "$id"

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && context.isActive

    private val variables = ConcurrentHashMap<String, Any>()
    private var enableCache = true
    private val pageCache get() = context.globalCache.pageCache
    private val documentCache get() = context.globalCache.documentCache
    private val closableObjects = mutableSetOf<AutoCloseable>()

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) = ensureActive { closableObjects.add(closable) }

    fun disableCache() = run { enableCache = false }

    fun normalize(url: String, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalize(url, initOptions(options), toItemOption)

    fun normalizeOrNull(url: String?, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalizeOrNull(url, initOptions(options), toItemOption)

    fun normalize(urls: Iterable<String>, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalize(urls, options, toItemOption).onEach { initOptions(it.options) }

    fun normalize(url: UrlAware, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalize(url, initOptions(options), toItemOption)

    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalizeOrNull(url, initOptions(options), toItemOption)

    fun normalize(urls: Collection<UrlAware>, options: LoadOptions = opts(), toItemOption: Boolean = false) =
            context.normalize(urls, options, toItemOption).onEach { initOptions(it.options) }

    /**
     * Inject a url
     *
     * @param url The url followed by options
     * @return The web page created
     */
    fun inject(url: String): WebPage = ensureActive { context.inject(normalize(url)) }

    fun get(url: String): WebPage = ensureActive { context.get(url) }

    fun getOrNull(url: String): WebPage? = ensureActive { context.getOrNull(url) }

    fun exists(url: String): Boolean = ensureActive { context.exists(url) }

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
    fun load(url: String, options: LoadOptions = opts()): WebPage = load(normalize(url, options))

    @Throws(Exception::class)
    fun load(normUrl: NormUrl): WebPage {
        ensureActive()

        initOptions(normUrl.options)

        if (enableCache) {
            val (url, options) = normUrl
            val now = Instant.now()
            return pageCache.getDatum(url, options.expires, now)
                    ?: context.load(normUrl).also { pageCache.put(url, ExpiringItem(it, now)) }
        }

        return context.load(normUrl)
    }

    @Throws(Exception::class)
    suspend fun loadDeferred(url: String, options: LoadOptions = opts()) = loadDeferred(normalize(url, options))

    @Throws(Exception::class)
    suspend fun loadDeferred(normUrl: NormUrl): WebPage {
        initOptions(normUrl.options)

        if (enableCache) {
            val (url, options) = normUrl
            val now = Instant.now()
            return pageCache.getDatum(url, options.expires, now)
                    ?: context.loadDeferred(normUrl).also { pageCache.put(url, ExpiringItem(it, now)) }
        }

        return context.loadDeferred(normUrl)
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions = opts(), areItems: Boolean = false): Collection<WebPage> {
        ensureActive()
        val normUrls = normalize(urls, options, areItems)
        val opts = normUrls.firstOrNull()?.options ?: return listOf()

        return if (enableCache) {
            loadAllWithCache(normUrls, opts)
        } else {
            context.loadAll(normUrls, opts)
        }
    }

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions = opts(), areItems: Boolean = false): Collection<WebPage> {
        ensureActive()
        options.preferParallel = true
        val normUrls = normalize(urls, options, areItems)
        val opt = normUrls.firstOrNull()?.options ?: return listOf()

        return if (enableCache) {
            loadAllWithCache(normUrls, opt)
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
    fun loadOutPages(portalUrl: String, args: String) = loadOutPages(portalUrl, LoadOptions.parse(args))

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = opts()): Collection<WebPage> {
        val outlinkSelector = appendSelectorIfMissing(options.outLinkSelector, "a")

        val normUrl = normalize(portalUrl, options)

        val opts = normUrl.options
        val links = loadDocument(normUrl).select(outlinkSelector) {
            parseLink(it, !opts.noNorm, opts.ignoreUrlQuery)?.substringBeforeLast("#")
        }.mapNotNullTo(mutableSetOf()) { it }.take(opts.topLinks)

        return loadAll(links, normUrl.options.createItemOptions())
    }

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    fun parse(page: WebPage, noCache: Boolean = false) = parse0(page, noCache)

    fun loadDocument(url: String, args: String) = loadDocument(url, LoadOptions.parse(args))

    fun loadDocument(url: String, options: LoadOptions = opts()): FeaturedDocument {
        ensureActive()
        val normUrl = normalize(url, options)
        return parse(load(normUrl))
    }

    fun loadDocument(normUrl: NormUrl) = parse(load(normUrl))

    fun scrape(url: String, args: String, fieldCss: Iterable<String>): Map<String, String?> {
        val document = loadDocument(url, args)
        return fieldCss.associateWith { document.selectFirstOrNull(it)?.text() }
    }

    fun scrape(url: String, args: String, fieldCss: Map<String, String>): Map<String, String?> {
        val document = loadDocument(url, args)
        return fieldCss.entries.associate { it.key to document.selectFirstOrNull(it.value)?.text() }
    }

    fun scrape(url: String, args: String, restrictCss: String, fieldCss: Iterable<String>): List<Map<String, String?>> {
        return loadDocument(url, args).select(restrictCss).map { ele ->
            fieldCss.associateWith { ele.selectFirstOrNull(it)?.text() }
        }
    }

    fun scrape(url: String, args: String, restrictCss: String, fieldCss: Map<String, String>): List<Map<String, String?>> {
        return loadDocument(url, args).select(restrictCss).map { ele ->
            fieldCss.entries.associate { it.key to ele.selectFirstOrNull(it.value)?.text() }
        }
    }

    fun scrapeOutPages(portalUrl: String, args: String, fieldsCss: Iterable<String>) =
            scrapeOutPages(portalUrl, args, ":root", fieldsCss)

    fun scrapeOutPages(portalUrl: String,
                       args: String, restrictCss: String, fieldsCss: Iterable<String>): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
                .mapNotNull { it.selectFirstOrNull(restrictCss) }
                .map { ele -> fieldsCss.associateWith { ele.firstTextOrNull(it) } }
                .toList()
    }

    fun scrapeOutPages(portalUrl: String, args: String, fieldsCss: Map<String, String>) =
            scrapeOutPages(portalUrl, args, ":root", fieldsCss)

    fun scrapeOutPages(portalUrl: String,
                       args: String, restrictCss: String, fieldsCss: Map<String, String>): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
                .mapNotNull { it.selectFirstOrNull(restrictCss) }
                .map { ele -> fieldsCss.entries.associate { it.key to ele.firstTextOrNull(it.value) } }
                .toList()
    }

    fun cache(page: WebPage): WebPage = page.also { pageCache.putDatum(it.url, it) }
    fun disableCache(page: WebPage): WebPage? = pageCache.remove(page.url)?.datum

    fun cache(doc: FeaturedDocument): FeaturedDocument = doc.also { documentCache.putDatum(it.baseUri, it) }
    fun disableCache(doc: FeaturedDocument): FeaturedDocument? = documentCache.remove(doc.baseUri)?.datum

    fun disableCache(url: String): WebPage? {
        documentCache.remove(url)
        return pageCache.remove(url)?.datum
    }

    fun getVariable(name: String): Any? = let { variables[name] }

    fun setVariable(name: String, value: Any) = run { variables[name] = value }

    fun putSessionBean(obj: Any) = ensureActive { sessionBeanFactory.putBean(obj) }

    inline fun <reified T> getSessionBean(): T? = sessionBeanFactory.getBean()

    fun delete(url: String) = ensureActive { context.delete(url) }

    fun flush() = ensureActive { context.webDb.flush() }

    fun persist(page: WebPage) = ensureActive { context.webDb.put(page) }

    fun export(page: WebPage, ident: String = ""): Path {
        ensureActive()
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(page.contentAsString, path, true)
    }

    fun export(doc: FeaturedDocument, ident: String = ""): Path {
        ensureActive()
        val filename = AppPaths.fromUri(doc.baseUri, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(doc.prettyHtml, path, true)
    }

    fun exportTo(doc: FeaturedDocument, path: Path): Path {
        ensureActive()
        return AppFiles.saveTo(doc.prettyHtml.toByteArray(), path, true)
    }

    override fun equals(other: Any?) = other === this || (other is PulsarSession && other.id == id)

    override fun hashCode(): Int = id

    override fun toString(): String = "#$id"

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closableObjects.forEach { o -> o.close() }
            log.info("Session #{} is closed", display)
        }
    }

    private fun loadAllWithCache(urls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        ensureActive()
        urls.forEach { initOptions(it.options) }
        initOptions(options)

        val pages = ArrayList<WebPage>()
        val pendingUrls = ArrayList<NormUrl>()

        val now = Instant.now()
        for (url in urls) {
            val page = pageCache.getDatum(url.spec, options.expires, now)
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

    private fun parse0(page: WebPage, noCache: Boolean = false): FeaturedDocument {
        ensureActive()

        if (page.isNil) {
            return FeaturedDocument.NIL
        }

        if (noCache || !enableCache) {
            return context.parse(page)
        }

        return documentCache.computeIfAbsent(page.url) { context.parse(page) }
    }

    private fun parseLink(ele: Element, normalize: Boolean = false, ignoreQuery: Boolean = false): String? {
        var link = ele.attr("abs:href").takeIf { it.startsWith("http") } ?: return null
        if (normalize) {
            link = normalizeOrNull(link)?.spec ?: return null
        }

        return link.takeUnless { ignoreQuery } ?: Urls.getUrlWithoutParameters(link)
    }

    private fun initOptions(options: LoadOptions): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = sessionConfig
        }
        return options
    }

    private fun ensureActive() {
        if (!isActive) {
            throw IllegalApplicationContextStateException("Pulsar session $this is not active")
        }
    }

    private fun <T> ensureActive(action: () -> T): T
            = if (isActive) action() else throw IllegalApplicationContextStateException("Pulsar session is not alive")

    private fun <T> ensureActive(defaultValue: T, action: () -> T): T = defaultValue.takeIf { !isActive } ?: action()
}
