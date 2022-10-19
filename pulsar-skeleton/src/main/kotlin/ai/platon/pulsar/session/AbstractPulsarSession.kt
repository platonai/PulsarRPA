package ai.platon.pulsar.session

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormURL
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.collectIfTo
import ai.platon.pulsar.dom.select.firstTextOrNull
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.MutableWebPage
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

    override val display get() = "$id"

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && context.isActive

    private val variables = ConcurrentHashMap<String, Any>()
    private var enablePDCache = true
    override val globalCache get() = context.globalCacheFactory.globalCache
    override val pageCache get() = context.globalCacheFactory.globalCache.pageCache
    override val documentCache get() = context.globalCacheFactory.globalCache.documentCache

    private val globalCacheFactoryOrNull get() = context.takeIf { isActive }?.globalCacheFactory
    private val pageCacheOrNull get() = globalCacheFactoryOrNull?.globalCache?.pageCache
    private val documentCacheOrNull get() = globalCacheFactoryOrNull?.globalCache?.documentCache

    private val closableObjects = mutableSetOf<AutoCloseable>()

    override fun registerClosable(closable: AutoCloseable) = ensureActive { closableObjects.add(closable) }

    override fun disablePDCache() = run { enablePDCache = false }

    override fun options(args: String, event: PageEvent?): LoadOptions {
        val opts = LoadOptions.parse(args, sessionConfig.toVolatileConfig())
        if (event != null) {
            opts.rawEvent = event
        }
        return opts
    }

    override fun property(name: String) = sessionConfig[name] ?: unmodifiedConfig[name]

    override fun property(name: String, value: String) = run { sessionConfig[name] = value }

    override fun normalize(url: String) = normalize(url, "")

    override fun normalize(url: String, args: String) = context.normalize(url, options(args))

    override fun normalize(url: String, options: LoadOptions) = context.normalize(url, options)

    override fun normalizeOrNull(url: String?, options: LoadOptions) = context.normalizeOrNull(url, options)

    override fun normalize(urls: Iterable<String>) = normalize(urls, options())

    override fun normalize(urls: Iterable<String>, args: String) = normalize(urls, options(args))

    override fun normalize(urls: Iterable<String>, options: LoadOptions) = context.normalize(urls, options)

    override fun normalize(url: UrlAware) = normalize(url, options())

    override fun normalize(url: UrlAware, args: String) = normalize(url, options(args))

    override fun normalize(url: UrlAware, options: LoadOptions) = context.normalize(url, options)

    override fun normalizeOrNull(url: UrlAware?, options: LoadOptions) = context.normalizeOrNull(url, options)

    override fun normalize(urls: Collection<UrlAware>) = normalize(urls, options())

    override fun normalize(urls: Collection<UrlAware>, args: String) = normalize(urls, options(args))

    override fun normalize(urls: Collection<UrlAware>, options: LoadOptions) = context.normalize(urls, options)

    override fun inject(url: String): WebPage = ensureActive { context.inject(normalize(url)) }

    override fun get(url: String): WebPage = ensureActive { context.get(url) }

    override fun getOrNull(url: String): WebPage? = ensureActive { context.getOrNull(url) }

    override fun exists(url: String): Boolean = ensureActive { context.exists(url) }

    override fun fetchState(page: WebPage, options: LoadOptions) = context.fetchState(page, options)

    override fun open(url: String): WebPage = load(url, "-refresh")

    override fun load(url: String): WebPage = load(url, options())

    override fun load(url: String, args: String): WebPage = load(url, options(args))

    override fun load(url: String, options: LoadOptions): WebPage = load(normalize(url, options))

    override fun load(url: UrlAware): WebPage = load(normalize(url, options()))

    override fun load(url: UrlAware, args: String): WebPage = load(normalize(url, options(args)))

    override fun load(url: UrlAware, options: LoadOptions): WebPage = load(normalize(url, options))

    override fun load(normURL: NormURL) = context.takeIf { !enablePDCache }?.load(normURL)
        ?: createPageWithCachedCoreOrNull(normURL)
        ?: loadAndCache(normURL)

    override suspend fun loadDeferred(url: String, args: String) = loadDeferred(normalize(url, options(args)))

    override suspend fun loadDeferred(url: String, options: LoadOptions) = loadDeferred(normalize(url, options))

    override suspend fun loadDeferred(url: UrlAware, args: String): WebPage =
        loadDeferred(normalize(url, options(args)))

    override suspend fun loadDeferred(url: UrlAware, options: LoadOptions): WebPage =
        loadDeferred(normalize(url, options))

    override suspend fun loadDeferred(normURL: NormURL) = context.takeIf { !enablePDCache }?.loadDeferred(normURL)
        ?: createPageWithCachedCoreOrNull(normURL)
        ?: loadAndCacheDeferred(normURL)

    override fun loadAll(urls: Iterable<String>) = loadAll(urls, options())

    override fun loadAll(urls: Iterable<String>, args: String) = loadAll(urls, options(args))

    override fun loadAll(urls: Iterable<String>, options: LoadOptions) = loadAll(normalize(urls, options))

    override fun loadAll(urls: Collection<UrlAware>) = loadAll(urls, options())

    override fun loadAll(urls: Collection<UrlAware>, args: String) = loadAll(urls, options(args))

    override fun loadAll(urls: Collection<UrlAware>, options: LoadOptions) = loadAll(normalize(urls, options))

    override fun loadAll(normURLs: List<NormURL>) = context.loadAll(normURLs)

    override fun loadAsync(url: String) = loadAsync(normalize(url))

    override fun loadAsync(url: String, args: String) = loadAsync(normalize(url, args))

    override fun loadAsync(url: String, options: LoadOptions) = loadAsync(normalize(url, options))

    override fun loadAsync(url: UrlAware) = loadAsync(normalize(url))

    override fun loadAsync(url: UrlAware, args: String) = loadAsync(normalize(url, args))

    override fun loadAsync(url: UrlAware, options: LoadOptions) = loadAsync(normalize(url, options))

    override fun loadAsync(url: NormURL) = context.loadAsync(url)

    override fun loadAllAsync(urls: Iterable<String>) = loadAllAsync(normalize(urls))

    override fun loadAllAsync(urls: Iterable<String>, args: String) = loadAllAsync(normalize(urls, args))

    override fun loadAllAsync(urls: Iterable<String>, options: LoadOptions) = loadAllAsync(normalize(urls, options))

    override fun loadAllAsync(urls: Collection<UrlAware>) = loadAllAsync(normalize(urls))

    override fun loadAllAsync(urls: Collection<UrlAware>, args: String) = loadAllAsync(normalize(urls, args))

    override fun loadAllAsync(urls: Collection<UrlAware>, options: LoadOptions) = loadAllAsync(normalize(urls, options))

    override fun loadAllAsync(urls: List<NormURL>) = context.loadAllAsync(urls)

    override fun submit(url: String) = submit(PlainUrl(url))

    override fun submit(url: String, args: String) = submit(PlainUrl(url, args))

    override fun submit(url: String, options: LoadOptions) =
        submit(ListenableHyperlink(url, args = options.toString(), event = options.event))

    override fun submit(url: UrlAware) = submit(url, "")

    override fun submit(url: UrlAware, args: String) = apply {
        context.submit(url.also { it.args = LoadOptions.normalize(url.args, args) })
    }

    override fun submitAll(urls: Iterable<String>) = submitAll(urls.map { PlainUrl(it) })

    override fun submitAll(urls: Iterable<String>, args: String) = submitAll(urls.map { PlainUrl(it, args) })

    override fun submitAll(urls: Iterable<String>, options: LoadOptions) =
        submitAll(urls.map { ListenableHyperlink(it, args = options.toString(), event = options.event) })

    override fun submitAll(urls: Collection<UrlAware>) = apply { context.submitAll(urls) }

    override fun submitAll(urls: Collection<UrlAware>, args: String) = apply {
        context.submitAll(urls.onEach { it.args = LoadOptions.normalize(it.args, args) })
    }

    override fun loadOutPages(portalUrl: String, args: String) = loadOutPages(portalUrl, options(args))

    override fun loadOutPages(portalUrl: String, options: LoadOptions) = loadOutPages0(portalUrl, options)

    override fun submitOutPages(portalUrl: String, args: String): AbstractPulsarSession =
        submitOutPages(portalUrl, options(args))

    override fun submitOutPages(portalUrl: String, options: LoadOptions) = submitOutPages0(portalUrl, options)

    override fun loadOutPagesAsync(portalUrl: String, args: String) = loadOutPagesAsync(portalUrl, options(args))

    override fun loadOutPagesAsync(portalUrl: String, options: LoadOptions) = loadOutPagesAsync0(portalUrl, options)

    override fun loadResource(url: String, referrer: String, args: String) = loadResource(url, referrer, options(args))

    override fun loadResource(url: String, referrer: String, options: LoadOptions) =
        load(url, options.apply { isResource = true }.also { it.referrer = referrer })

    override suspend fun loadResourceDeferred(url: String, referrer: String, args: String) =
        loadResourceDeferred(url, referrer, options(args))

    override suspend fun loadResourceDeferred(url: String, referrer: String, options: LoadOptions) =
        loadDeferred(url, options.apply { isResource = true }.also { it.referrer = referrer })

    override fun parse(page: WebPage) = parse0(page, false)

    override fun parse(page: WebPage, noCache: Boolean) = parse0(page, noCache)

    override fun loadDocument(url: String) = loadDocument(url, options())

    override fun loadDocument(url: String, args: String) = loadDocument(url, options(args))

    override fun loadDocument(url: String, options: LoadOptions): FeaturedDocument {
        val normURL = normalize(url, options)
        if (enablePDCache) {
            val now = Instant.now()
            val document = documentCacheOrNull?.getDatum(url, options.expires, now)
            if (document != null) {
                return document
            }
        }
        return parse(load(normURL))
    }

    override fun loadDocument(normURL: NormURL) = parse(load(normURL))

    override fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?> =
        scrape(url, options(args), fieldSelectors)

    override fun scrape(url: String, options: LoadOptions, fieldSelectors: Iterable<String>): Map<String, String?> {
        val document = loadDocument(url, options)
        return fieldSelectors.associateWith { document.selectFirstOrNull(it)?.text() }
    }

    override fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?> =
        scrape(url, options(args), fieldSelectors)

    override fun scrape(url: String, options: LoadOptions, fieldSelectors: Map<String, String>): Map<String, String?> {
        val document = loadDocument(url, options)
        return fieldSelectors.entries.associate { it.key to document.selectFirstOrNull(it.value)?.text() }
    }

    override fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> = scrape(url, options(args), restrictSelector, fieldSelectors)

    override fun scrape(
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadDocument(url, options).select(restrictSelector).map { ele ->
            fieldSelectors.associateWith { ele.selectFirstOrNull(it)?.text() }
        }
    }

    override fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> = scrape(url, options(args), restrictSelector, fieldSelectors)

    override fun scrape(
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadDocument(url, options).select(restrictSelector).map { ele ->
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

    override fun equals(other: Any?) = other === this || (other is PulsarSession && other.id == id)

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

        val document = documentCacheOrNull?.getDatum(page.url)
        if (document != null) {
            documentCacheHits.incrementAndGet()
            return document
        }

        return context.parse(page) ?: nil
    }

    private fun loadAndCache(normURL: NormURL): WebPage {
        return context.load(normURL).also {
            pageCacheOrNull?.putDatum(it.url, it)
        }
    }

    private suspend fun loadAndCacheDeferred(normURL: NormURL): WebPage {
        return context.loadDeferred(normURL).also {
            pageCacheOrNull?.putDatum(it.url, it)
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
    private fun createPageWithCachedCoreOrNull(normURL: NormURL): WebPage? {
        if (!normURL.options.readonly) {
            return null
        }

        // We have events to handle, so do not use the cached version
        if (normURL.options.rawEvent != null) {
            return null
        }

        val cachedPage = getCachedPageOrNull(normURL)
        val page = FetchEntry.createPageShell(normURL)

        if (cachedPage != null) {
            // the cached page can be or not be persisted, but not guaranteed
            // if a page is loaded from cache, the content remains unchanged and should not persist to database
            if (cachedPage is MutableWebPage) {
                page.unsafeSetGPage(cachedPage.unbox())
            }

            page.isCached = true
            page.tmpContent = cachedPage.tmpContent
            page.args = normURL.args

            return page
        }

        return null
    }

    private fun getCachedPageOrNull(normURL: NormURL): WebPage? {
        val (url, options) = normURL
        if (options.refresh) {
            // refresh the page, do not take cached version
            return null
        }

        val now = Instant.now()
        val page = pageCacheOrNull?.getDatum(url, options.expires, now) ?: return null
        if (!options.isExpired(page.prevFetchTime)) {
            pageCacheHits.incrementAndGet()
            return page
        }

        return null
    }

    private fun parseNormalizedLink(ele: Element, normalize: Boolean = false, ignoreQuery: Boolean = false): String? {
        var link = ele.attr("abs:href").takeIf { it.startsWith("http") } ?: return null
        if (normalize) {
            link = normalizeOrNull(link)?.spec ?: return null
        }

        link = link.takeUnless { ignoreQuery } ?: UrlUtils.getUrlWithoutParameters(link)
        return link.substringBeforeLast("#")
    }

    private fun loadOutPages0(portalUrl: String, options: LoadOptions): List<WebPage> {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        val selector = opts.outLinkSelectorOrNull ?: return listOf()
        val itemOpts = normURL.options.createItemOptions(portalUrl)

        require(normURL.options.rawEvent == options.rawEvent)
        require(options.rawItemEvent == itemOpts.rawEvent)

        val links = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)

        return loadAll(links, itemOpts)
    }

    private fun submitOutPages0(portalUrl: String, options: LoadOptions): AbstractPulsarSession {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        val selector = opts.outLinkSelectorOrNull ?: return this
        val itemOpts = normURL.options.createItemOptions(portalUrl)

        val outLinks = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { ListenableHyperlink("$it $itemOpts") }
            .onEach { link -> itemOpts.rawEvent?.let { link.event = it } }

        return submitAll(outLinks)
    }

    private fun loadOutPagesAsync0(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>> {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        val selector = opts.outLinkSelectorOrNull ?: return listOf()
        val itemOpts = normURL.options.createItemOptions(portalUrl)

        val outLinks = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { NormURL(it, itemOpts) }

        return loadAllAsync(outLinks)
    }

    private fun <T> ensureActive(action: () -> T): T =
        if (isActive) action() else throw IllegalApplicationContextStateException("Pulsar session is not alive")

    private fun <T> ensureActive(defaultValue: T, action: () -> T): T = defaultValue.takeIf { !isActive } ?: action()
}
