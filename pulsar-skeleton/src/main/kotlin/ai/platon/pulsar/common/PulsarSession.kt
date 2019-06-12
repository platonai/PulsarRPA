package ai.platon.pulsar.common

import ai.platon.pulsar.common.PulsarPaths.webCacheDir
import ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class PulsarSession(
        /**
         * The spring application context
         * */
        applicationContext: ConfigurableApplicationContext,
        /**
         * The volatile config
         * */
        val config: VolatileConfig,
        /**
         * The session id. Session id is expected to be set by the container
         * */
        val id: Int = 9000000 + idGen.incrementAndGet()
) : AutoCloseable {
    val log = LoggerFactory.getLogger(PulsarSession::class.java)
    val pulsar: Pulsar = Pulsar(applicationContext)
    val beanFactory = BeanFactory(config)
    private var enableCache = true
    private var pageCache: ConcurrentLRUCache<String, WebPage>
    private var documentCache: ConcurrentLRUCache<String, FeaturedDocument>
    // Session variables
    private val variables: MutableMap<String, Any> = Collections.synchronizedMap(HashMap())
    private val closableObjects = mutableSetOf<AutoCloseable>()
    private val closed = AtomicBoolean()

    constructor(appConfigLocation: String) : this(ClassPathXmlApplicationContext(appConfigLocation))

    @JvmOverloads
    constructor(
            applicationContext: ConfigurableApplicationContext = getApplicationContext(),
            config: ImmutableConfig = getUnmodifiedConfig(applicationContext)
    ): this(applicationContext, VolatileConfig(config))

    init {
        var capacity = config.getUint("session.page.cache.size", SESSION_PAGE_CACHE_CAPACITY)
        pageCache = ConcurrentLRUCache(SESSION_PAGE_CACHE_TTL.seconds, capacity)

        capacity = config.getUint("session.document.cache.size", SESSION_DOCUMENT_CACHE_CAPACITY)
        documentCache = ConcurrentLRUCache(SESSION_DOCUMENT_CACHE_TTL.seconds, capacity)
    }

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) {
        closableObjects.add(closable)
    }

    fun disableCache() {
        enableCache = false
    }

    fun normalize(url: String): NormUrl {
        return pulsar.normalize(url)
    }

    fun normalize(url: String, options: LoadOptions): NormUrl {
        return pulsar.normalize(url, initOptions(options))
    }

    fun normalize(urls: Iterable<String>): List<NormUrl> {
        return pulsar.normalize(urls)
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions): List<NormUrl> {
        return pulsar.normalize(urls, initOptions(options))
    }

    /**
     * Inject a url
     *
     * @param configuredUrl The url followed by config options
     * @return The web page created
     */
    fun inject(configuredUrl: String): WebPage {
        return pulsar.inject(configuredUrl)
    }

    fun getOrNil(url: String): WebPage {
        return pulsar.getOrNil(url)
    }

    /**
     * Load a url with default options
     *
     * @param url The url followed by config options
     * @return The Web page
     */
    fun load(url: String): WebPage {
        val normUrl = normalize(url)
        initOptions(normUrl.options)
        return load(normUrl)
    }

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: String, options: LoadOptions): WebPage {
        val normUrl = normalize(url, initOptions(options))
        return load(normUrl)
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions.default): Collection<WebPage> {
        initOptions(options)
        val normUrls = normalize(urls, options)

        return if (enableCache) {
            getCachedOrLoadAll(normUrls, options)
        } else {
            pulsar.loadAll(normUrls, options)
        }
    }

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        initOptions(options)
        options.preferParallel = true
        val normUrls = normalize(urls, options)

        return if (enableCache) {
            getCachedOrLoadAll(normUrls, options)
        } else {
            pulsar.loadAll(normUrls, options)
        }
    }

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    fun parse(page: WebPage): FeaturedDocument {
        val key = page.key + "\t" + page.fetchTime

        var document = documentCache.get(key)
        if (document == null) {
            document = pulsar.parse(page)
            documentCache.put(key, document)

            val prevFetchTime = page.prevFetchTime
            if (prevFetchTime.plusSeconds(3600).isAfter(Instant.now())) {
                // It might be still in the cache
                val oldKey = page.key + "\t" + prevFetchTime
                documentCache.tryRemove(oldKey)
            }
        }

        return document
    }

    fun clearCache() {
        documentCache.clear()
        pageCache.clear()
    }

    private fun getCachedOrGet(url: String): WebPage? {
        var page: WebPage? = pageCache.get(url)
        if (page != null) {
            return page
        }

        page = pulsar.get(url)
        pageCache.put(url, page)

        return page
    }

    private fun getCachedOrLoad(url: NormUrl): WebPage {
        var page: WebPage? = pageCache.get(url.url)
        if (page != null) {
            return page
        }

        page = pulsar.load(url.url, url.options)
        pageCache.put(url.url, page)

        return page
    }

    private fun getCachedOrLoadAll(urls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        initOptions(options)

        val pages = ArrayList<WebPage>()
        val pendingUrls = ArrayList<NormUrl>()

        for (url in urls) {
            val page = pageCache.get(url.url)
            if (page != null) {
                pages.add(page)
            } else {
                pendingUrls.add(url)
            }
        }

        val freshPages = if (options.preferParallel) {
            pulsar.parallelLoadAll(pendingUrls, options)
        } else {
            pulsar.loadAll(pendingUrls, options)
        }

        pages.addAll(freshPages)

        // Notice: we do not cache batch loaded pages, batch loaded pages are not used frequently
        // do not do this: sessionCachePutAll(freshPages);

        return pages
    }

    fun getVariable(name: String): Any? {
        return variables[name]
    }

    fun setVariable(name: String, value: Any) {
        variables[name] = value
    }

    fun putBean(obj: Any) {
        beanFactory.putBean(obj)
    }

    inline fun <reified T> getBean(): T? {
        return beanFactory.getBean()
    }

    fun delete(url: String) {
        pulsar.delete(url)
    }

    fun flush() {
        pulsar.webDb.flush()
    }

    fun persist(page: WebPage) {
        pulsar.webDb.put(page)
    }

    fun export(page: WebPage, ident: String = ""): Path {
        val path = PulsarPaths.get(webCacheDir, "export", ident, PulsarPaths.fromUri(page.url, ".htm"))
        return PulsarFiles.saveTo(page.contentAsString, path)
    }

    fun export(doc: FeaturedDocument, ident: String = ""): Path {
        val path = PulsarPaths.get(webCacheDir, "export", ident, PulsarPaths.fromUri(doc.location, ".htm"))
        return PulsarFiles.saveTo(doc.prettyHtml, path)
    }

    fun exportTo(doc: FeaturedDocument, path: Path): Path {
        return PulsarFiles.saveTo(doc.prettyHtml.toByteArray(), path, true)
    }

    override fun equals(other: Any?): Boolean {
        return other === this || (other is PulsarSession && other.id == id)
    }

    override fun hashCode(): Int {
        return Integer.hashCode(id)
    }

    override fun toString(): String {
        return "#$id"
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        log.info("Closing session $this ...")

        closableObjects.forEach { it.use { it.close() } }
        pulsar.use { it.close() }

        clearCache()
    }

    private fun load(url: NormUrl): WebPage {
        initOptions(url.options)

        return if (enableCache) {
            getCachedOrLoad(url)
        } else {
            pulsar.load(url)
        }
    }

    private fun initOptions(options: LoadOptions): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = config
        }
        return options
    }

    companion object {
        val SESSION_PAGE_CACHE_TTL = Duration.ofSeconds(20)
        val SESSION_PAGE_CACHE_CAPACITY = 100

        val SESSION_DOCUMENT_CACHE_TTL = Duration.ofHours(1)
        val SESSION_DOCUMENT_CACHE_CAPACITY = 100
        private val idGen = AtomicInteger()

        fun getApplicationContext(): ClassPathXmlApplicationContext {
            return ClassPathXmlApplicationContext(
                    System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION))
        }

        fun getUnmodifiedConfig(applicationContext: ApplicationContext): ImmutableConfig {
            return applicationContext.getBean(ImmutableConfig::class.java)
        }
    }
}
