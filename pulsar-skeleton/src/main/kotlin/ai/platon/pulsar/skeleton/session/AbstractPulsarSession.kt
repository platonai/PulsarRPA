package ai.platon.pulsar.skeleton.session

import ai.platon.pulsar.boilerpipe.extractors.DefaultExtractor
import ai.platon.pulsar.boilerpipe.sax.SAXInput
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.IllegalApplicationStateException
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.extractor.TextDocument
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.firstTextOrNull
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.FetchEntry
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
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
    
    private val logger = LoggerFactory.getLogger(AbstractPulsarSession::class.java)
    
    override val unmodifiedConfig get() = context.unmodifiedConfig
    
    override val display get() = "$id"
    
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && context.isActive
    
    private val dataCache = ConcurrentHashMap<String, Any>()
    private var enablePDCache = true
    override val globalCache get() = context.globalCache
    override val pageCache get() = globalCache.pageCache
    override val documentCache get() = globalCache.documentCache
    
    private val contextOrNull get() = if (isActive) context else null
    private val globalCacheFactoryOrNull get() = contextOrNull?.globalCacheFactory
    private val pageCacheOrNull get() = globalCacheFactoryOrNull?.globalCache?.pageCache
    private val documentCacheOrNull get() = globalCacheFactoryOrNull?.globalCache?.documentCache
    
    private val closableObjects = mutableSetOf<AutoCloseable>()
    
    fun registerClosable(closable: AutoCloseable) = ensureActive { closableObjects.add(closable) }
    
    override fun disablePDCache() = run { enablePDCache = false }
    
    override fun options(args: String) = options(args, null)
    
    override fun options(args: String, event: PageEventHandlers?): LoadOptions {
        val opts = LoadOptions.parse(args, sessionConfig.toVolatileConfig())
        if (event != null) {
            opts.rawEvent = event
        }
        return opts
    }
    
    override fun property(name: String): String? {
        return sessionConfig[name] ?: unmodifiedConfig[name]
    }
    
    override fun property(name: String, value: String) {
        sessionConfig[name] = value
    }
    
    override fun normalize(url: String) = normalize(url, "")
    
    override fun normalize(url: String, args: String, toItemOption: Boolean) =
        context.normalize(url, options(args), toItemOption)
    
    override fun normalize(url: String, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(url, options, toItemOption)
    
    override fun normalizeOrNull(url: String?, options: LoadOptions, toItemOption: Boolean) =
        context.normalizeOrNull(url, options, toItemOption)
    
    override fun normalize(urls: Iterable<String>) = normalize(urls, options(), false)
    
    override fun normalize(urls: Iterable<String>, args: String, toItemOption: Boolean) =
        normalize(urls, options(args), toItemOption)
    
    override fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(urls, options, toItemOption)
    
    override fun normalize(url: UrlAware) = normalize(url, options())
    
    override fun normalize(url: UrlAware, args: String, toItemOption: Boolean) =
        normalize(url, options(args), toItemOption)
    
    override fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(url, options, toItemOption)
    
    override fun normalizeOrNull(url: UrlAware?, options: LoadOptions, toItemOption: Boolean) =
        context.normalizeOrNull(url, options, toItemOption)
    
    override fun normalize(urls: Collection<UrlAware>) = normalize(urls, options(), false)
    
    override fun normalize(urls: Collection<UrlAware>, args: String, toItemOption: Boolean) =
        normalize(urls, options(args), toItemOption)
    
    override fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean) =
        context.normalize(urls, options, toItemOption)
    
    override fun get(url: String): WebPage = ensureActive { context.get(url) }
    
    override fun get(url: String, vararg fields: String): WebPage = ensureActive { context.get(url, *fields) }
    
    override fun getOrNull(url: String): WebPage? = contextOrNull?.getOrNull(url)
    
    override fun getOrNull(url: String, vararg fields: String): WebPage? = contextOrNull?.getOrNull(url, *fields)
    
    override fun getContent(url: String): ByteBuffer? = contextOrNull?.getContent(url)
    
    override fun getContentAsString(url: String): String? = contextOrNull?.getContentAsString(url)
    
    override fun exists(url: String): Boolean = ensureActive { context.exists(url) }
    
    override fun fetchState(page: WebPage, options: LoadOptions) = context.fetchState(page, options)

    override fun open(url: String): WebPage = load(url, "-refresh")
    
    override fun open(url: String, eventHandlers: PageEventHandlers): WebPage = load(url, options("-refresh", eventHandlers))

    override suspend fun open(url: String, driver: WebDriver): WebPage = context.open(url, driver, options("-refresh"))

    override suspend fun open(url: String, driver: WebDriver, eventHandlers: PageEventHandlers): WebPage =
        context.open(url, driver, options("-refresh", eventHandlers))

    override suspend fun connect(driver: WebDriver): WebPage = context.connect(driver, options())

    override fun load(url: String): WebPage = load(url, options())
    
    override fun load(url: String, args: String): WebPage = load(url, options(args))
    
    override fun load(url: String, options: LoadOptions): WebPage = load(normalize(url, options))
    
    override fun load(url: UrlAware): WebPage = load(normalize(url, options()))
    
    override fun load(url: UrlAware, args: String): WebPage = load(normalize(url, options(args)))
    
    override fun load(url: UrlAware, options: LoadOptions): WebPage = load(normalize(url, options))
    
    override fun load(url: NormURL): WebPage {
        if (!enablePDCache) {
            return context.load(url)
        }
        
        return createPageWithCachedCoreOrNull(url) ?: loadAndCache(url)
    }
    
    override suspend fun loadDeferred(url: String, args: String) = loadDeferred(normalize(url, options(args)))
    
    override suspend fun loadDeferred(url: String, options: LoadOptions) = loadDeferred(normalize(url, options))
    
    override suspend fun loadDeferred(url: UrlAware, args: String): WebPage =
        loadDeferred(normalize(url, options(args)))
    
    override suspend fun loadDeferred(url: UrlAware, options: LoadOptions): WebPage =
        loadDeferred(normalize(url, options))
    
    override suspend fun loadDeferred(url: NormURL): WebPage {
        if (!enablePDCache) {
            return context.loadDeferred(url)
        }
        
        return createPageWithCachedCoreOrNull(url) ?: loadAndCacheDeferred(url)
    }
    
    override fun loadAll(urls: Iterable<String>) = loadAll(urls, options())
    
    override fun loadAll(urls: Iterable<String>, args: String) = loadAll(urls, options(args))
    
    override fun loadAll(urls: Iterable<String>, options: LoadOptions) = loadAll(normalize(urls, options))
    
    override fun loadAll(urls: Collection<UrlAware>) = loadAll(urls, options())
    
    override fun loadAll(urls: Collection<UrlAware>, args: String) = loadAll(urls, options(args))
    
    override fun loadAll(urls: Collection<UrlAware>, options: LoadOptions) = loadAll(normalize(urls, options))
    
    override fun loadAll(normUrls: List<NormURL>) = context.loadAll(normUrls)
    
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
    
    override fun submit(url: UrlAware, args: String) =
        also { context.submit(url.also { it.args = LoadOptions.normalize(url.args, args) }) }
    
    override fun submitAll(urls: Iterable<String>) = submitAll(urls.map { PlainUrl(it) })
    
    override fun submitAll(urls: Iterable<String>, args: String) = submitAll(urls.map { PlainUrl(it, args) })
    
    override fun submitAll(urls: Iterable<String>, options: LoadOptions) =
        submitAll(urls.map { ListenableHyperlink(it, args = options.toString(), event = options.event) })
    
    override fun submitAll(urls: Collection<UrlAware>) = also { context.submitAll(urls) }
    
    override fun submitAll(urls: Collection<UrlAware>, args: String) =
        also { context.submitAll(urls.onEach { it.args = LoadOptions.normalize(it.args, args) }) }
    
    override fun loadOutPages(portalUrl: String, args: String) = loadOutPages(portalUrl, options(args))
    
    override fun loadOutPages(portalUrl: String, options: LoadOptions) = loadOutPages(PlainUrl(portalUrl), options)
    
    override fun loadOutPages(portalUrl: UrlAware, args: String) = loadOutPages(portalUrl, options(args))
    
    override fun loadOutPages(portalUrl: UrlAware, options: LoadOptions) = loadOutPages0(portalUrl, options)
    
    override fun submitForOutPages(portalUrl: String, args: String) = submitForOutPages(portalUrl, options(args))
    
    override fun submitForOutPages(portalUrl: String, options: LoadOptions) =
        submitForOutPages(PlainUrl(portalUrl), options)
    
    override fun submitForOutPages(portalUrl: UrlAware, args: String) = submitForOutPages(portalUrl, options(args))
    
    override fun submitForOutPages(portalUrl: UrlAware, options: LoadOptions) = submitForOutPages0(portalUrl, options)
    
    override fun loadOutPagesAsync(portalUrl: String, args: String) = loadOutPagesAsync(portalUrl, options(args))
    
    override fun loadOutPagesAsync(portalUrl: String, options: LoadOptions) = loadOutPagesAsync0(portalUrl, options)
    
    override fun loadResource(url: String, referrer: String) = loadResource(url, referrer, options())
    
    override fun loadResource(url: String, referrer: String, args: String) = loadResource(url, referrer, options(args))
    
    override fun loadResource(url: String, referrer: String, options: LoadOptions) =
        load(url, options.apply { isResource = true }.also { it.referrer = referrer })
    
    override suspend fun loadResourceDeferred(url: String, referrer: String) =
        loadResourceDeferred(url, referrer, options())
    
    override suspend fun loadResourceDeferred(url: String, referrer: String, args: String) =
        loadResourceDeferred(url, referrer, options(args))
    
    override suspend fun loadResourceDeferred(url: String, referrer: String, options: LoadOptions) =
        loadDeferred(url, options.apply { isResource = true }.also { it.referrer = referrer })
    
    override fun parse(page: WebPage) = parse0(page, false)
    
    override fun parse(page: WebPage, noCache: Boolean) = parse0(page, noCache)
    
    override fun loadDocument(url: String) = parse(load(url))
    
    override fun loadDocument(url: String, args: String) = parse(load(url, args))
    
    override fun loadDocument(url: String, options: LoadOptions) = parse(load(url, options))
    
    override fun loadDocument(url: UrlAware) = parse(load(url))
    
    override fun loadDocument(url: UrlAware, args: String) = parse(load(url, args))
    
    override fun loadDocument(url: UrlAware, options: LoadOptions) = parse(load(url, options))
    
    override fun loadDocument(url: NormURL) = parse(load(url))
    
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
    
    override fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>) =
        scrapeOutPages(portalUrl, args, ":root", fieldSelectors)
    
    override fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Iterable<String>) =
        scrapeOutPages(portalUrl, options, ":root", fieldSelectors)
    
    override fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.associateWith { ele.firstTextOrNull(it) } }
            .toList()
    }
    
    override fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, options).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.associateWith { ele.firstTextOrNull(it) } }
            .toList()
    }
    
    override fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>) =
        scrapeOutPages(portalUrl, args, ":root", fieldSelectors)
    
    override fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> = scrapeOutPages(portalUrl, options, ":root", fieldSelectors)
    
    override fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, args).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.entries.associate { it.key to ele.firstTextOrNull(it.value) } }
            .toList()
    }
    
    override fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>> {
        return loadOutPages(portalUrl, options).asSequence().map { parse(it) }
            .mapNotNull { it.selectFirstOrNull(restrictSelector) }
            .map { ele -> fieldSelectors.entries.associate { it.key to ele.firstTextOrNull(it.value) } }
            .toList()
    }
    
    override fun harvest(url: String, args: String, engine: String): TextDocument = harvest(load(url, args), engine)
    
    override fun harvest(page: WebPage, engine: String): TextDocument = harvest0(page, engine)
    
    override fun chat(prompt: String) = context.chat(prompt)
    
    override fun chat(userMessage: String, systemMessage: String) =
        context.chat(userMessage, systemMessage)
    
    override fun chat(page: WebPage, prompt: String) = chat(prompt + "\n\n" + page.contentAsString)
    
    override fun chat(document: FeaturedDocument, prompt: String) = chat(prompt + "\n\n" + document.text)
    
    override fun chat(element: Element, prompt: String) = chat(prompt + "\n\n" + element.text())
    
    override fun data(name: String): Any? = let { dataCache[name] }
    
    override fun data(name: String, value: Any) = run { dataCache[name] = value }
    
    override fun delete(url: String) = ensureActive { context.delete(url) }
    
    override fun flush() = ensureActive { context.webDb.flush() }
    
    override fun persist(page: WebPage) = ensureActive { context.webDb.put(page) }
    
    override fun export(page: WebPage) = export(page, "")
    
    override fun export(page: WebPage, ident: String): Path {
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(page.contentAsString, path, true)
    }
    
    override fun exportTo(page: WebPage, path: Path): Path {
        return AppFiles.saveTo(page.contentAsString, path, true)
    }
    
    override fun export(doc: FeaturedDocument) = export(doc, "")
    
    override fun export(doc: FeaturedDocument, ident: String): Path {
        val filename = AppPaths.fromUri(doc.baseURI, "", ".htm")
        val path = WEB_CACHE_DIR.resolve("export").resolve(ident).resolve(filename)
        return AppFiles.saveTo(doc.outerHtml, path, true)
    }
    
    override fun exportTo(doc: FeaturedDocument, path: Path): Path {
        return AppFiles.saveTo(doc.outerHtml.toByteArray(), path, true)
    }
    
    override fun equals(other: Any?) = other === this || (other is PulsarSession && other.id == id)
    
    override fun hashCode(): Int = id
    
    override fun toString(): String = "#$id"
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closableObjects.forEach {
                runCatching { it.close() }.onFailure { warnForClose(this, it) }
            }
            closableObjects.clear()
            logger.info("PulsarSession is closed | #{} | {}#{}", display, this.javaClass.name, hashCode())
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
    
    private fun harvest0(page: WebPage, engine: String) = harvest0(page.url, page.contentAsString, engine)
    
    private fun harvest0(url: String, html: String, engine: String) =
        harvest0(url, InputSource(StringReader(html)), engine)
    
    private fun harvest0(url: String, inputSource: InputSource, engine: String): TextDocument {
        if (engine != "boilerpipe") {
            throw IllegalArgumentException("Unsupported engine: $engine")
        }
        
        val d = SAXInput().parse(url, inputSource)
        val success = DefaultExtractor().process(d)
        if (!success) {
            return TextDocument(url)
        }
        
        return TextDocument(url,
            pageTitle = d.pageTitle,
            contentTitle = d.contentTitle,
            textContent = d.textContent,
            additionalFields = d.fields.takeIf { it.isNotEmpty() }
        )
    }
    
    private fun chat0() {
    
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
            page.unsafeSetGPage(cachedPage.unbox())
            
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
    
    private fun loadOutPages0(portalUrl: UrlAware, options: LoadOptions): List<WebPage> {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        
        val selector = opts.outLinkSelectorOrNull ?: return listOf()
        val itemOpts = normURL.options.createItemOptions()
        
        require(normURL.options.rawEvent == options.rawEvent)
        require(options.rawItemEvent == itemOpts.rawEvent)
        
        val links = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
        
        return loadAll(links, itemOpts)
    }
    
    private fun submitForOutPages0(portalUrl: UrlAware, options: LoadOptions): AbstractPulsarSession {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        val selector = opts.outLinkSelectorOrNull ?: return this
        val itemOpts = normURL.options.createItemOptions()
        
        val outLinks = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { ListenableHyperlink("$it $itemOpts") }
            .onEach { link -> itemOpts.rawEvent?.let { link.event = it } }
        
        submitAll(outLinks)
        
        return this
    }
    
    private fun loadOutPagesAsync0(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>> {
        val normURL = normalize(portalUrl, options)
        val opts = normURL.options
        val itemOpts = normURL.options.createItemOptions()
        val selector = opts.outLinkSelectorOrNull ?: return listOf()
        
        val outLinks = loadDocument(normURL)
            .select(selector) { parseNormalizedLink(it, !opts.noNorm, opts.ignoreUrlQuery) }
            .mapNotNullTo(mutableSetOf()) { it }
            .take(opts.topLinks)
            .map { NormURL(it, itemOpts) }
        
        return loadAllAsync(outLinks)
    }
    
    private fun <T> ensureActive(action: () -> T): T =
        if (isActive) action() else throw IllegalApplicationStateException("Pulsar session is not alive")
    
    private fun <T> ensureActive(defaultValue: T, action: () -> T): T = defaultValue.takeIf { !isActive } ?: action()
}
