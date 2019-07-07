package ai.platon.pulsar

import ai.platon.pulsar.common.ConcurrentLRUCache
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import org.apache.log4j.LogManager
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
class PulsarContext {

    companion object {
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

    val log = LogManager.getLogger(PulsarContext::class.java)
    /**
     * The program environment
     * */
    val env = PulsarEnv.getOrCreate()
    /**
     * A immutable config is loaded from the config file at process startup, and never changes
     * */
    val unmodifiedConfig: ImmutableConfig = PulsarEnv.unmodifiedConfig
    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()
    /**
     * Whether this pulsar object is already closed
     * */
    val isStopped = AtomicBoolean()
    /**
     * Registered closeables, will be closed by Pulsar object
     * */
    private val closeables = mutableListOf<AutoCloseable>()
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

//    val taskTracker = PulsarEnv.

    val pageCache: ConcurrentLRUCache<String, WebPage>
    val documentCache: ConcurrentLRUCache<String, FeaturedDocument>

    val proxyPool = ProxyPool.getInstance(PulsarEnv.unmodifiedConfig)

    init {
        var capacity = unmodifiedConfig.getUint("session.page.cache.size", PulsarSession.SESSION_PAGE_CACHE_CAPACITY)
        pageCache = ConcurrentLRUCache(PulsarSession.SESSION_PAGE_CACHE_TTL.seconds, capacity)

        capacity = unmodifiedConfig.getUint("session.document.cache.size", PulsarSession.SESSION_DOCUMENT_CACHE_CAPACITY)
        documentCache = ConcurrentLRUCache(PulsarSession.SESSION_DOCUMENT_CACHE_TTL.seconds, capacity)

        webDb = PulsarEnv.applicationContext.getBean(WebDb::class.java)
        injectComponent = PulsarEnv.applicationContext.getBean(InjectComponent::class.java)
        loadComponent = PulsarEnv.applicationContext.getBean(LoadComponent::class.java)
        fetchComponent = PulsarEnv.applicationContext.getBean(BatchFetchComponent::class.java)
        parseComponent = PulsarEnv.applicationContext.getBean(ParseComponent::class.java)
        urlNormalizers = PulsarEnv.applicationContext.getBean(UrlNormalizers::class.java)
    }

    fun createSession(): PulsarSession {
        ensureRunning()
        return PulsarSession(this, VolatileConfig(unmodifiedConfig))
    }

    fun getBean(name: String): Any? {
        ensureRunning()
        return PulsarEnv.applicationContext.getBean(name)
    }

    fun getBean(clazz: Class<Any>): Any? {
        ensureRunning()
        return PulsarEnv.applicationContext.getBean(clazz)
    }

    fun getBean(clazz: KClass<Any>): Any? {
        ensureRunning()
        return PulsarEnv.applicationContext.getBean(clazz.java)
    }

    fun normalize(url: String): NormUrl {
        ensureRunning()
        val parts = Urls.splitUrlArgs(url)
        val options = initOptions(LoadOptions.parse(parts.second))
        var normalizedUrl = Urls.normalize(parts.first, options.shortenKey)
        if (!options.noFilter) {
            normalizedUrl = urlNormalizers.normalize(normalizedUrl)?:return NormUrl.nil
        }
        return NormUrl(normalizedUrl, initOptions(options))
    }

    fun normalize(url: String, options: LoadOptions): NormUrl {
        ensureRunning()
        val parts = Urls.splitUrlArgs(url)
        var normalizedUrl = Urls.normalize(parts.first, options.shortenKey)
        if (!options.noFilter) {
            normalizedUrl = urlNormalizers.normalize(normalizedUrl)?:return NormUrl.nil
        }

        if (parts.second.isBlank()) {
            return NormUrl(normalizedUrl, options)
        }

        val options2 = LoadOptions.mergeModified(options, LoadOptions.parse(parts.second), options.volatileConfig)
        return NormUrl(normalizedUrl, initOptions(options2))
    }

    fun normalize(urls: Iterable<String>): List<NormUrl> {
        ensureRunning()
        return urls.mapNotNull { normalize(it).takeIf { it.isNotNil } }
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions): List<NormUrl> {
        ensureRunning()
        return urls.mapNotNull { normalize(it, options).takeIf { it.isNotNil } }
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage {
        ensureRunning()
        return injectComponent.inject(Urls.splitUrlArgs(url))
    }

    fun get(url: String): WebPage? {
        ensureRunning()
        return webDb.get(normalize(url).url, false)
    }

    fun getOrNil(url: String): WebPage {
        ensureRunning()
        return webDb.getOrNil(normalize(url).url, false)
    }

    fun scan(urlPrefix: String): Iterator<WebPage> {
        ensureRunning()
        return webDb.scan(urlPrefix)
    }

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        ensureRunning()
        return webDb.scan(urlPrefix, fields)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String): WebPage {
        ensureRunning()
        val normUrl = normalize(url)
        initOptions(normUrl.options)
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
        ensureRunning()
        val normUrl = normalize(url, initOptions(options))
        return loadComponent.load(normUrl)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL): WebPage {
        ensureRunning()
        return loadComponent.load(url, initOptions(LoadOptions()))
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL, options: LoadOptions): WebPage {
        ensureRunning()
        return loadComponent.load(url, initOptions(options))
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: NormUrl): WebPage {
        ensureRunning()
        initOptions(url.options)
        return loadComponent.load(url)
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
        ensureRunning()
        initOptions(options)
        return loadComponent.loadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun loadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureRunning()
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
        ensureRunning()
        initOptions(options)
        return loadComponent.parallelLoadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        ensureRunning()
        return loadComponent.loadAll(urls, initOptions(options))
    }

    /**
     * Parse the WebPage using Jsoup
     */
    fun parse(page: WebPage): FeaturedDocument {
        ensureRunning()
        val parser = JsoupParser(page, unmodifiedConfig)
        return FeaturedDocument(parser.parse())
    }

    fun parse(page: WebPage, mutableConfig: MutableConfig): FeaturedDocument {
        ensureRunning()
        val parser = JsoupParser(page, mutableConfig)
        return FeaturedDocument(parser.parse())
    }

    fun persist(page: WebPage) {
        ensureRunning()
        webDb.put(page, false)
    }

    fun delete(url: String) {
        ensureRunning()
        webDb.delete(url)
        webDb.delete(normalize(url).url)
    }

    fun delete(page: WebPage) {
        ensureRunning()
        webDb.delete(page.url)
    }

    fun flush() {
        ensureRunning()
        webDb.flush()
    }

    fun stop() {
        if (isStopped.getAndSet(true)) {
            return
        }

        closeables.forEach { it.use { it.close() } }
    }

    private fun ensureRunning() {
        if (isStopped.get()) {
            throw IllegalStateException(
                    """Cannot call methods on a stopped PulsarContext.""")
        }
    }

    private fun initOptions(options: LoadOptions): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = VolatileConfig(PulsarEnv.unmodifiedConfig)
        }
        return options
    }
}
