package ai.platon.pulsar.session

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.PulsarEventHandler
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.firstTextOrNull
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class AbstractPulsarSession(
    /**
     * The pulsar context
     * */
    override val context: AbstractPulsarContext,
    /**
     * The session scope volatile config, every setting is supposed to be changed at any time and any place
     * */
    override val sessionConfig: VolatileConfig,
    /**
     * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
     * */
    override val id: Int
) : PulsarSession {

    companion object {
        const val ID_CAPACITY = 1_000_000
        const val ID_START = 1_000_000
        const val ID_END = ID_START + ID_CAPACITY - 1

        private val idGen = AtomicInteger()

        val pageCacheHits = AtomicLong()
        val documentCacheHits = AtomicLong()

        fun generateNextId() = ID_START + idGen.incrementAndGet()
    }

    private val log = LoggerFactory.getLogger(AbstractPulsarSession::class.java)

    override val unmodifiedConfig get() = context.unmodifiedConfig

    /**
     * The session scoped bean factory
     * */
    override val sessionBeanFactory = BeanFactory(sessionConfig)

    override val display get() = "$id"

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && context.isActive

    private val variables = ConcurrentHashMap<String, Any>()
    private var enablePDCache = true
    override val globalCacheFactory get() = context.globalCacheFactory
    override val pageCache get() = context.globalCacheFactory.globalCache.pageCache
    override val documentCache get() = context.globalCacheFactory.globalCache.documentCache
    private val closableObjects = mutableSetOf<AutoCloseable>()

    /**
     * Close objects when the session is closing
     * */
    override fun registerClosable(closable: AutoCloseable) = ensureActive { closableObjects.add(closable) }

    override fun disablePDCache() = run { enablePDCache = false }

    /**
     * Create a new options, with a new volatile config
     * */
    override fun options(args: String, eventHandler: PulsarEventHandler?): LoadOptions {
        val opts = LoadOptions.parse(args, sessionConfig.toVolatileConfig())
        if (eventHandler != null) {
            opts.eventHandler = eventHandler
        }
        return opts
    }

    override fun property(name: String): String? {
        return sessionConfig[name] ?: unmodifiedConfig[name]
    }

    override fun property(name: String, value: String) {
        sessionConfig[name] = value
    }

    override fun normalize(url: String, args: String?): NormUrl = context.normalize(url, options(args ?: ""))

    override fun normalize(url: String, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(url, options, toItemOption)

    override fun normalizeOrNull(url: String?, options: LoadOptions, toItemOption: Boolean) =
        context.normalizeOrNull(url, options, toItemOption)

    override fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(urls, options, toItemOption)

    override fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(url, options, toItemOption)

    override fun normalizeOrNull(url: UrlAware?, options: LoadOptions, toItemOption: Boolean) =
        context.normalizeOrNull(url, options, toItemOption)

    override fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(urls, options, toItemOption)

    /**
     * Inject a url
     *
     * @param url The url followed by options
     * @return The web page created
     */
    override fun inject(url: String): WebPage = ensureActive { context.inject(normalize(url)) }

    override fun get(url: String): WebPage = ensureActive { context.get(url) }

    override fun getOrNull(url: String): WebPage? = ensureActive { context.getOrNull(url) }

    override fun exists(url: String): Boolean = ensureActive { context.exists(url) }

    override fun fetchState(page: WebPage, options: LoadOptions) = context.fetchState(page, options)

    /**
     * Open a page with [url]
     *
     * @param url     The url of the page to open
     * @return The web page
     */
    override fun open(url: String): WebPage = load(url, options("-refresh"))

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    override fun load(url: String, args: String): WebPage = load(url, options(args))

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    override fun load(url: String, options: LoadOptions): WebPage = load(normalize(url, options))

    override fun load(url: UrlAware, args: String): WebPage = load(normalize(url, options(args)))

    override fun load(url: UrlAware, options: LoadOptions): WebPage = load(normalize(url, options))

    override fun load(normUrl: NormUrl): WebPage {
        if (!enablePDCache) {
            return context.load(normUrl)
        }

        return createPageWithCachedCoreOrNull(normUrl) ?: loadAndCache(normUrl)
    }

    override suspend fun loadDeferred(url: String, options: LoadOptions) = loadDeferred(normalize(url, options))

    override suspend fun loadDeferred(url: UrlAware, args: String): WebPage =
        loadDeferred(normalize(url, options(args)))

    /**
     *
     * */
    override suspend fun loadDeferred(url: UrlAware, options: LoadOptions): WebPage =
        loadDeferred(normalize(url, options))

    override suspend fun loadDeferred(normUrl: NormUrl): WebPage {
        if (!enablePDCache) {
            return context.loadDeferred(normUrl)
        }

        return createPageWithCachedCoreOrNull(normUrl) ?: loadAndCacheDeferred(normUrl)
    }

    private fun loadAndCache(normUrl: NormUrl): WebPage {
        return context.load(normUrl).also {
            pageCache.putDatum(it.url, it)
        }
    }

    private suspend fun loadAndCacheDeferred(normUrl: NormUrl): WebPage {
        return context.loadDeferred(normUrl).also {
            pageCache.putDatum(it.url, it)
        }
    }

    /**
     * Create page with cached core, but not metadata. If the page might be changed, it should be fetched again.
     *
     * If the loading is not a read-only-loading, which might modify the page status, or the loading have event handlers,
     * in such cases, we must render the page in the browser again.
     *
     * TODO: handle the session cache and the FetchComponent cache
     * */
    private fun createPageWithCachedCoreOrNull(normUrl: NormUrl): WebPage? {
        if (!normUrl.options.readonly) {
            return null
        }

        // We have events to handle later
        if (normUrl.options.eventHandler?.loadEventHandler != null) {
            return null
        }

        val cachedPage = getCachedPageOrNull(normUrl)
        val page = FetchEntry.createPageShell(normUrl)

        if (cachedPage != null) {
            // the cached page can be or not be persisted, but not guaranteed
            // if a page is loaded from cache, the content remains unchanged and should not persist to database
            page.unsafeSetGPage(cachedPage.unbox())

            page.isCached = true
            page.tmpContent = cachedPage.tmpContent
            page.args = normUrl.args

            return page
        }

        return null
    }

    private fun getCachedPageOrNull(normUrl: NormUrl): WebPage? {
        val (url, options) = normUrl
        if (options.refresh) {
            // refresh the page, do not take cached version
            return null
        }

        val now = Instant.now()
        val page = pageCache.getDatum(url, options.expires, now)
        if (page != null && !options.isExpired(page.prevFetchTime)) {
            pageCacheHits.incrementAndGet()
            return page
        }

        return null
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    override fun loadAll(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean): List<WebPage> {
        val normUrls = normalize(urls, options, toItemOption)
        return context.loadAll(normUrls)
    }

    override fun loadAll(normUrls: Iterable<NormUrl>): List<WebPage> {
        return context.loadAll(normUrls)
    }

    override fun loadAsync(url: NormUrl): CompletableFuture<WebPage> {
        return context.loadAsync(url)
    }

    override fun loadAllAsync(urls: Iterable<NormUrl>): List<CompletableFuture<WebPage>> {
        return context.loadAllAsync(urls)
    }

    override fun submit(url: UrlAware): AbstractPulsarSession {
        context.submit(url)
        return this
    }

    override fun submitAll(urls: Iterable<UrlAware>): AbstractPulsarSession {
        context.submitAll(urls)
        return this
    }

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load args
     * @return The web pages
     */
    override fun loadOutPages(portalUrl: String, args: String) = loadOutPages(portalUrl, options(args))

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    override fun loadOutPages(portalUrl: String, options: LoadOptions): List<WebPage> {
        val normUrl = normalize(portalUrl, options)
        val opts = normUrl.options
        val itemOpts = normUrl.options.createItemOptions()
        val selector = opts.outLinkSelectorOrNull ?: return listOf()

        val links = loadDocument(normUrl)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)

        return loadAll(links, itemOpts)
    }

    override fun submitOutPages(portalUrl: String, args: String): AbstractPulsarSession =
        submitOutPages(portalUrl, options(args))

    override fun submitOutPages(portalUrl: String, options: LoadOptions): AbstractPulsarSession {
        val normUrl = normalize(portalUrl, options)
        val opts = normUrl.options
        val itemOpts = normUrl.options.createItemOptions()
        val selector = opts.outLinkSelectorOrNull ?: return this

        val outLinks = loadDocument(normUrl)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { ListenableHyperlink("$it $itemOpts") }
            .onEach { link -> itemOpts.eventHandler?.let { link.eventHandler = it } }

        submitAll(outLinks)

        return this
    }

    override fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>> {
        val normUrl = normalize(portalUrl, options)
        val opts = normUrl.options
        val itemOpts = normUrl.options.createItemOptions()
        val selector = opts.outLinkSelectorOrNull ?: return listOf()

        val outLinks = loadDocument(normUrl)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { NormUrl(it, itemOpts) }

        return loadAllAsync(outLinks)
    }

    override suspend fun loadResource(url: String, referer: String, args: String) = loadResource(url, referer, options(args))

    override suspend fun loadResource(url: String, referer: String, opts: LoadOptions): WebPage {
        opts.isResource = true
        opts.referrer = referer
        return loadDeferred(url, opts)
    }

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    override fun parse(page: WebPage, noCache: Boolean) = parse0(page, noCache)

    override fun loadDocument(url: String, args: String) = loadDocument(url, options(args))

    override fun loadDocument(url: String, options: LoadOptions): FeaturedDocument {
        val normUrl = normalize(url, options)
        if (enablePDCache) {
            val now = Instant.now()
            val document = documentCache.getDatum(url, options.expires, now)
            if (document != null) {
                return document
            }
        }
        return parse(load(normUrl))
    }

    override fun loadDocument(normUrl: NormUrl) = parse(load(normUrl))

    override fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?> {
        val document = loadDocument(url, args)
        return fieldSelectors.associateWith { document.selectFirstOrNull(it)?.text() }
    }

    override fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?> {
        val document = loadDocument(url, args)
        return fieldSelectors.entries.associate { it.key to document.selectFirstOrNull(it.value)?.text() }
    }

    override fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadDocument(url, args).select(restrictSelector).map { ele ->
            fieldSelectors.associateWith { ele.selectFirstOrNull(it)?.text() }
        }
    }

    override fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadDocument(url, args).select(restrictSelector).map { ele ->
            fieldSelectors.entries.associate { it.key to ele.selectFirstOrNull(it.value)?.text() }
        }
    }

    @ExperimentalApi
    override fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>) =
        scrapeOutPages(portalUrl, args, ":root", fieldSelectors)

    @ExperimentalApi
    override fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Iterable<String>) =
        scrapeOutPages(portalUrl, options, ":root", fieldSelectors)

    @ExperimentalApi
    override fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.associateWith { ele.firstTextOrNull(it) } }
            .toList()
    }

    @ExperimentalApi
    override fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, options).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.associateWith { ele.firstTextOrNull(it) } }
            .toList()
    }

    @ExperimentalApi
    override fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>) =
        scrapeOutPages(portalUrl, args, ":root", fieldSelectors)

    /**
     * Scrape out pages
     * */
    @ExperimentalApi
    override fun scrapeOutPages(portalUrl: String,
                       options: LoadOptions, fieldSelectors: Map<String, String>): List<Map<String, String?>> =
        scrapeOutPages(portalUrl, options, ":root", fieldSelectors)

    @ExperimentalApi
    override fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.entries.associate { it.key to ele.firstTextOrNull(it.value) } }
            .toList()
    }

    @ExperimentalApi
    override fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, options).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.entries.associate { it.key to ele.firstTextOrNull(it.value) } }
            .toList()
    }

    override fun getVariable(name: String): Any? = let { variables[name] }

    override fun setVariable(name: String, value: Any) = run { variables[name] = value }

    override fun putSessionBean(obj: Any) = ensureActive { sessionBeanFactory.putBean(obj) }

    inline fun <reified T> getSessionBean(): T? = sessionBeanFactory.getBean()

    override fun delete(url: String) = ensureActive { context.delete(url) }

    override fun flush() = ensureActive { context.webDb.flush() }

    override fun persist(page: WebPage) = ensureActive { context.webDb.put(page) }

    override fun export(page: WebPage, ident: String): Path {
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(page.contentAsString, path, true)
    }

    override fun export(doc: FeaturedDocument, ident: String): Path {
        val filename = AppPaths.fromUri(doc.baseUri, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(doc.prettyHtml, path, true)
    }

    override fun exportTo(doc: FeaturedDocument, path: Path): Path {
        return AppFiles.saveTo(doc.prettyHtml.toByteArray(), path, true)
    }

    override fun equals(other: Any?) = other === this || (other is AbstractPulsarSession && other.id == id)

    override fun hashCode(): Int = id

    override fun toString(): String = "#$id"

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closableObjects.forEach { o -> o.close() }
            log.info("Session is closed | #{}", display)
        }
    }

    private fun parse0(page: WebPage, noCache: Boolean = false): FeaturedDocument {
        val nil = FeaturedDocument.NIL

        if (page.isNil) {
            return nil
        }

        if (noCache) {
            return context.parse(page) ?: nil
        }

        val document = documentCache.getDatum(page.url)
        if (document != null) {
            documentCacheHits.incrementAndGet()
            return document
        }

        return context.parse(page) ?: nil
    }

    private fun parseNormalizedLink(ele: Element, normalize: Boolean = false, ignoreQuery: Boolean = false): String? {
        var link = ele.attr("abs:href").takeIf { it.startsWith("http") } ?: return null
        if (normalize) {
            link = normalizeOrNull(link)?.spec ?: return null
        }

        link = link.takeUnless { ignoreQuery } ?: UrlUtils.getUrlWithoutParameters(link)
        return link.substringBeforeLast("#")
    }

    private fun <T> ensureActive(action: () -> T): T =
        if (isActive) action() else throw IllegalApplicationContextStateException("Pulsar session is not alive")

    private fun <T> ensureActive(defaultValue: T, action: () -> T): T = defaultValue.takeIf { !isActive } ?: action()
}
