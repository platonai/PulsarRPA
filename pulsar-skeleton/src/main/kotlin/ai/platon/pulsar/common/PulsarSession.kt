package ai.platon.pulsar.common

import ai.platon.pulsar.common.PulsarPaths.webCacheDir
import ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
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
open class PulsarSession(applicationContext: ConfigurableApplicationContext, val config: VolatileConfig) : AutoCloseable {
    val id: Int = objectIdGenerator.incrementAndGet()
    val pulsar: Pulsar = Pulsar(applicationContext)
    private var enableCache = true
    private var pageCache: ConcurrentLRUCache<String, WebPage>
    private var documentCache: ConcurrentLRUCache<String, FeaturedDocument>
    // Session variables
    private val variables: MutableMap<String, Any> = Collections.synchronizedMap(HashMap())

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

    fun disableCache() {
        enableCache = false
    }

    fun normalize(url: String): String? {
        return pulsar.normalize(url)
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
     * @param configuredUrl The url followed by config options
     * @return The Web page
     */
    fun load(configuredUrl: String): WebPage {
        val urlAndOptions = UrlUtil.splitUrlArgs(configuredUrl)
        val options = LoadOptions.parse(urlAndOptions.value, config)

        return load(urlAndOptions.key, options)
    }

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: String, options: LoadOptions): WebPage {
        options.mutableConfig = config

        return if (enableCache) {
            getCachedOrLoad(url, options)
        } else {
            pulsar.load(url, options)
        }
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions.DEFAULT): Collection<WebPage> {
        options.mutableConfig = config

        return if (enableCache) {
            getCachedOrLoadAll(urls, options)
        } else {
            pulsar.loadAll(urls, options)
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
        options.mutableConfig = config
        options.isPreferParallel = true

        return if (enableCache) {
            getCachedOrLoadAll(urls, options)
        } else {
            pulsar.loadAll(urls, options)
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

    private fun getCachedOrGet(url: String): WebPage? {
        var page: WebPage? = pageCache.get(url)
        if (page != null) {
            return page
        }

        page = pulsar[url]
        pageCache.put(url, page)

        return page
    }

    private fun getCachedOrLoad(url: String, options: LoadOptions): WebPage {
        var page: WebPage? = pageCache.get(url)
        if (page != null) {
            return page
        }

        page = pulsar.load(url, options)
        pageCache.put(url, page)

        return page
    }

    private fun getCachedOrLoadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val pages = ArrayList<WebPage>()
        val pendingUrls = ArrayList<String>()

        for (url in urls) {
            val page = pageCache.get(url)
            if (page != null) {
                pages.add(page)
            } else {
                pendingUrls.add(url)
            }
        }

        val freshPages: Collection<WebPage>
        if (options.isPreferParallel) {
            freshPages = pulsar.parallelLoadAll(pendingUrls, options)
        } else {
            freshPages = pulsar.loadAll(pendingUrls, options)
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

    fun delete(url: String) {
        pulsar.delete(url)
    }

    fun flush() {
        pulsar.webDb.flush()
    }

    fun persist(page: WebPage) {
        pulsar.webDb.put(page.url, page)
    }

    fun export(page: WebPage, ident: String): Path {
        val path = PulsarPaths.get(webCacheDir, "export", ident, PulsarPaths.fromUri(page.url, ".htm"))
        return PulsarFiles.saveTo(page, path)
    }

    fun export(doc: FeaturedDocument): Path {
        val path = PulsarPaths.get(webCacheDir, "export", PulsarPaths.fromUri(doc.baseUri, ".htm"))
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
        log.info("Destructing pulsar session " + this)
        pulsar.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(PulsarSession::class.java)
        val SESSION_PAGE_CACHE_TTL = Duration.ofSeconds(20)
        val SESSION_PAGE_CACHE_CAPACITY = 1000

        val SESSION_DOCUMENT_CACHE_TTL = Duration.ofHours(1)
        val SESSION_DOCUMENT_CACHE_CAPACITY = 10000
        private val objectIdGenerator = AtomicInteger()
        // NOTE: can not share objects between threads
//        private lateinit var pageCache: ConcurrentLRUCache<String, WebPage>
//        private lateinit var documentCache: ConcurrentLRUCache<String, FeaturedDocument>

        fun getApplicationContext(): ClassPathXmlApplicationContext {
            return ClassPathXmlApplicationContext(
                    System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION))
        }

        fun getUnmodifiedConfig(applicationContext: ApplicationContext): ImmutableConfig {
            return applicationContext.getBean(ImmutableConfig::class.java)
        }
    }
}
