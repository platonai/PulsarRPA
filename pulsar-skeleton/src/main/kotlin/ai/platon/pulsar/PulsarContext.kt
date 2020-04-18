package ai.platon.pulsar

import ai.platon.pulsar.common.ConcurrentLRUCache
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_INCOGNITO
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.fetch.LazyFetchTaskManager
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
class PulsarContext private constructor(): AutoCloseable {

    companion object {
        init {
            PulsarEnv.initialize()
        }

        private val activeContext = AtomicReference<PulsarContext>()

        fun getOrCreate(): PulsarContext {
            synchronized(PulsarContext::class.java) {
                if (activeContext.get() == null) {
                    activeContext.set(PulsarContext())
                }
                return activeContext.get()
            }
        }

        fun createSession(): PulsarSession {
            return getOrCreate().createSession()
        }
    }

    val log = LoggerFactory.getLogger(PulsarContext::class.java)
    /**
     * The spring application context
     * */
    val applicationContext = PulsarEnv.applicationContext
    /**
     * A immutable config is loaded from the config file at process startup, and never changes
     * */
    val unmodifiedConfig = applicationContext.getBean(ImmutableConfig::class.java)
    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()
    /**
     * Whether this pulsar object is already closed
     * */
    private val closed = AtomicBoolean()
    /**
     * Whether this pulsar object is already closed
     * */
    val isClosed = closed.get()
    /**
     * Registered closeables, will be closed by Pulsar object
     * */
    private val closableObjects = ConcurrentLinkedQueue<AutoCloseable>()
    /**
     * All open sessions
     * */
    val sessions = ConcurrentSkipListMap<Int, PulsarSession>()
    /**
     * Url normalizers
     * */
    val urlNormalizers: UrlNormalizers
    /**
     * The web db
     * */
    val webDb: WebDb
    /**
     * The inject component
     * */
    val injectComponent: InjectComponent
    /**
     * The load component
     * */
    val loadComponent: LoadComponent
    /**
     * The parse component
     * */
    val parseComponent: ParseComponent
    /**
     * The fetch component
     * */
    val fetchComponent: FetchComponent

    val lazyFetchTaskManager: LazyFetchTaskManager

    val pageCache: ConcurrentLRUCache<String, WebPage>
    val documentCache: ConcurrentLRUCache<String, FeaturedDocument>

    init {
        var capacity = unmodifiedConfig.getUint("session.page.cache.size", PulsarSession.SESSION_PAGE_CACHE_CAPACITY)
        pageCache = ConcurrentLRUCache(PulsarSession.SESSION_PAGE_CACHE_TTL.seconds, capacity)

        capacity = unmodifiedConfig.getUint("session.document.cache.size", PulsarSession.SESSION_DOCUMENT_CACHE_CAPACITY)
        documentCache = ConcurrentLRUCache(PulsarSession.SESSION_DOCUMENT_CACHE_TTL.seconds, capacity)

        webDb = applicationContext.getBean(WebDb::class.java)
        injectComponent = applicationContext.getBean(InjectComponent::class.java)
        loadComponent = applicationContext.getBean(LoadComponent::class.java)
        fetchComponent = applicationContext.getBean(BatchFetchComponent::class.java)
        parseComponent = applicationContext.getBean(ParseComponent::class.java)
        urlNormalizers = applicationContext.getBean(UrlNormalizers::class.java)
        lazyFetchTaskManager = applicationContext.getBean(LazyFetchTaskManager::class.java)
    }

    fun createSession(): PulsarSession {
        ensureAlive()
        val session = PulsarSession(this, VolatileConfig(unmodifiedConfig))
        sessions[session.id] = session
        return session
    }

    fun closeSession(session: PulsarSession) {
        sessions.remove(session.id)
    }

    @Throws(BeansException::class)
    fun <T> getBean(requiredType: Class<T>): T {
        ensureAlive()
        return PulsarEnv.getBean(requiredType)
    }

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) {
        ensureAlive()
        closableObjects.add(closable)
    }

    private fun initOptions(options: LoadOptions, isItemOption: Boolean = false): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = VolatileConfig(unmodifiedConfig)
        }

        options.volatileConfig?.setBoolean(BROWSER_INCOGNITO, options.incognito)

        return if (isItemOption) options.createItemOption() else options
    }

    fun normalize(url: String, isItemOption: Boolean = false): NormUrl {
        ensureAlive()
        return normalize(url, LoadOptions.create(), isItemOption)
    }

    fun normalize(url: String, options: LoadOptions, isItemOption: Boolean = false): NormUrl {
        ensureAlive()
        val parts = Urls.splitUrlArgs(url)
        var normalizedUrl = Urls.normalize(parts.first, options.shortenKey)
        if (!options.noNorm) {
            normalizedUrl = urlNormalizers.normalize(normalizedUrl)?:return NormUrl.nil
        }

        if (parts.second.isBlank()) {
            return NormUrl(normalizedUrl, initOptions(options, isItemOption))
        }

        val parsedOptions = LoadOptions.parse(parts.second)
        if (parsedOptions.toString() != parts.second) {
            log.error("Options parsing error: {}", parts.second)
        }
        val options2 = LoadOptions.mergeModified(parsedOptions, options)
        return NormUrl(normalizedUrl, initOptions(options2, isItemOption))
    }

    fun normalize(urls: Iterable<String>, isItemOption: Boolean = false): List<NormUrl> {
        ensureAlive()
        return urls.mapNotNull { normalize(it, isItemOption).takeIf { it.isNotNil } }
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions, isItemOption: Boolean = false): List<NormUrl> {
        ensureAlive()
        return urls.mapNotNull { normalize(it, options, isItemOption).takeIf { it.isNotNil } }
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage {
        ensureAlive()
        return injectComponent.inject(Urls.splitUrlArgs(url))
    }

    fun get(url: String): WebPage? {
        ensureAlive()
        return webDb.get(normalize(url).url, false)
    }

    fun getOrNil(url: String): WebPage {
        ensureAlive()
        return webDb.getOrNil(normalize(url).url, false)
    }

    fun scan(urlPrefix: String): Iterator<WebPage> {
        ensureAlive()
        return webDb.scan(urlPrefix)
    }

    fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        ensureAlive()
        return webDb.scan(urlPrefix, fields)
    }

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        ensureAlive()
        return webDb.scan(urlPrefix, fields)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String): WebPage {
        ensureAlive()
        val normUrl = normalize(url)
        return loadComponent.load(normUrl)
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String, options: LoadOptions): WebPage {
        ensureAlive()
        val normUrl = normalize(url, options)
        return loadComponent.load(normUrl)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL): WebPage {
        ensureAlive()
        return loadComponent.load(url, LoadOptions.create())
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL, options: LoadOptions): WebPage {
        ensureAlive()
        return loadComponent.load(url, initOptions(options))
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: NormUrl): WebPage {
        ensureAlive()
        initOptions(url.options)
        return loadComponent.load(url)
    }

    suspend fun loadDeferred(url: NormUrl): WebPage {
        ensureAlive()
        initOptions(url.options)
        return loadComponent.loadDeferred(url)
    }

    /**
     * Load a batch of urls with the specified options.
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureAlive()
        return loadComponent.loadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun loadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureAlive()
        return loadComponent.loadAll(urls, initOptions(options))
    }

    /**
     * Load a batch of urls with the specified options.
     *
     * Urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    @JvmOverloads
    fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureAlive()
        return loadComponent.parallelLoadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureAlive()
        return loadComponent.loadAll(urls, initOptions(options))
    }

    /**
     * Parse the WebPage using Jsoup
     */
    fun parse(page: WebPage): FeaturedDocument {
        ensureAlive()
        val parser = JsoupParser(page, unmodifiedConfig)
        return parser.parse()
    }

    fun parse(page: WebPage, mutableConfig: MutableConfig): FeaturedDocument {
        ensureAlive()
        val parser = JsoupParser(page, mutableConfig)
        return parser.parse()
    }

    fun persist(page: WebPage) {
        ensureAlive()
        webDb.put(page, false)
    }

    fun delete(url: String) {
        ensureAlive()
        webDb.delete(url)
        webDb.delete(normalize(url).url)
    }

    fun delete(page: WebPage) {
        ensureAlive()
        webDb.delete(page.url)
    }

    fun flush() {
        ensureAlive()
        webDb.flush()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            sessions.values.forEach { it.use { it.close() } }
            closableObjects.forEach { it.use { it.close() } }
        }
    }

    private fun ensureAlive() {
        if (closed.get()) {
//            throw IllegalStateException(
//                    """Cannot call methods on a stopped PulsarContext.""")
        }
    }
}
