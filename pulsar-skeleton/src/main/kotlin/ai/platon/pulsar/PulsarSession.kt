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
        val context: PulsarContext,
        /**
         * The session scope volatile volatileConfig, every item is supposed to be changed at any time and any place
         * */
        val volatileConfig: VolatileConfig,
        /**
         * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
         * */
        val id: Int = 9000000 + idGen.incrementAndGet()
) : AutoCloseable {
    val log = LoggerFactory.getLogger(PulsarSession::class.java)
    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * TODO: session scoped?
     * */
    val beanFactory = BeanFactory(volatileConfig)
    private val variables: MutableMap<String, Any> = ConcurrentHashMap()
    private var enableCache = true
    // Session variables
    private val closableObjects = mutableSetOf<AutoCloseable>()
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && PulsarEnv.isActive

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) {
        ensureAlive()
        closableObjects.add(closable)
    }

    fun disableCache() {
        ensureAlive()
        enableCache = false
    }

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
    fun inject(configuredUrl: String): WebPage {
        ensureAlive()
        return context.inject(configuredUrl)
    }

    fun getOrNil(url: String): WebPage {
        ensureAlive()
        return context.getOrNil(url)
    }

    /**
     * Load a url with default options
     *
     * @param url The url followed by volatileConfig options
     * @return The Web page
     */
    fun load(url: String): WebPage {
        ensureAlive()
        return load(normalize(url))
    }

    fun load(url: NormUrl): WebPage {
        ensureAlive()

        initOptions(url.options)

        return if (enableCache) {
            getCachedOrLoad(url)
        } else {
            context.load(url)
        }
    }

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: String, options: LoadOptions): WebPage {
        ensureAlive()
        val normUrl = normalize(url, options)
        return load(normUrl)
    }

    suspend fun loadDeferred(url: String): WebPage {
        ensureAlive()
        return loadDeferred(normalize(url))
    }

    suspend fun loadDeferred(url: NormUrl): WebPage {
        ensureAlive()

        initOptions(url.options)
        val page = if (enableCache) getCachedOrLoad(url) else null
        return page?:context.loadDeferred(url)
    }

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
        val opt = normUrls.firstOrNull()?.options?:return listOf()

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
        val opt = normUrls.firstOrNull()?.options?:return listOf()

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
     *
     * TODO: harvest task can not use the previous parsed document
     */
    fun parse(page: WebPage): FeaturedDocument {
        ensureAlive()
        val key = page.key + "\t" + page.fetchTime
        if (!enableCache) {
            return context.parse(page)
        }

        var document = context.documentCache.get(key)
        if (document == null) {
            document = context.parse(page)
            context.documentCache.put(key, document)

            val prevFetchTime = page.prevFetchTime
            if (prevFetchTime.plusSeconds(3600).isAfter(Instant.now())) {
                // It might be still in the cache
                val oldKey = page.key + "\t" + prevFetchTime
                context.documentCache.tryRemove(oldKey)
            }
        }

        return document
    }

    fun loadAndParse(url: String, options: LoadOptions = LoadOptions.create()): FeaturedDocument {
        ensureAlive()
        val normUrl = normalize(url, options)
        return parse(load(normUrl))
    }

    private fun getCachedOrGet(url: String): WebPage? {
        ensureAlive()
        var page: WebPage? = context.pageCache.get(url)
        if (page != null) {
            return page
        }

        page = context.get(url)
        context.pageCache.put(url, page)

        return page
    }

    private fun getCachedOrLoad(url: NormUrl): WebPage {
        ensureAlive()
        var page = context.pageCache.get(url.url)
        if (page != null) {
            return page
        }

        page = context.load(url.url, url.options)
        context.pageCache.put(url.url, page)

        return page
    }

    private fun getCachedOrLoadAll(urls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        ensureAlive()
        urls.forEach { initOptions(it.options) }
        initOptions(options)

        val pages = ArrayList<WebPage>()
        val pendingUrls = ArrayList<NormUrl>()

        for (url in urls) {
            val page = context.pageCache.get(url.url)
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

    fun getVariable(name: String): Any? {
        ensureAlive()
        return variables[name]
    }

    fun setVariable(name: String, value: Any) {
        ensureAlive()
        variables[name] = value
    }

    fun putBean(obj: Any) {
        ensureAlive()
        beanFactory.putBean(obj)
    }

    inline fun <reified T> getBean(): T? {
        return beanFactory.getBean()
    }

    fun delete(url: String) {
        ensureAlive()
        context.delete(url)
    }

    fun flush() {
        ensureAlive()
        context.webDb.flush()
    }

    fun persist(page: WebPage) {
        ensureAlive()
        context.webDb.put(page)
    }

    fun export(page: WebPage, ident: String = ""): Path {
        ensureAlive()
        val path = AppPaths.get(WEB_CACHE_DIR, "export", ident, AppPaths.fromUri(page.url, "", ".htm"))
        return AppFiles.saveTo(page.contentAsString, path, true)
    }

    fun export(doc: FeaturedDocument, ident: String = ""): Path {
        ensureAlive()
        val path = AppPaths.get(WEB_CACHE_DIR, "export", ident, AppPaths.fromUri(doc.location, "", ".htm"))
        return AppFiles.saveTo(doc.prettyHtml, path, true)
    }

    fun exportTo(doc: FeaturedDocument, path: Path): Path {
        ensureAlive()
        return AppFiles.saveTo(doc.prettyHtml.toByteArray(), path, true)
    }

    override fun equals(other: Any?): Boolean {
        return other === this || (other is PulsarSession && other.id == id)
    }

    override fun hashCode(): Int {
        // return just id itself
        return Integer.hashCode(id)
    }

    override fun toString(): String {
        return "#$id"
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            context.webDb.flush()
            closableObjects.forEach { o -> o.use { it.close() } }
            context.closeSession(this)

            log.info("Pulsar session #{} is closed. Used memory: {}, free memory: {}",
                    id,
                    Strings.readableBytes(Systems.getMemoryUsed()),
                    Strings.readableBytes(Systems.getMemoryFree()))
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
            options.volatileConfig = volatileConfig
        }
        return options
    }

    private fun ensureAlive(): Boolean {
        if (!PulsarEnv.isActive) {
            return false
        }

        if (closed.get()) {
            return false
        }

        return true
    }

    companion object {
        val SESSION_PAGE_CACHE_TTL = Duration.ofSeconds(20)!!
        const val SESSION_PAGE_CACHE_CAPACITY = 100

        val SESSION_DOCUMENT_CACHE_TTL = Duration.ofHours(1)!!
        const val SESSION_DOCUMENT_CACHE_CAPACITY = 100
        private val idGen = AtomicInteger()
    }
}
