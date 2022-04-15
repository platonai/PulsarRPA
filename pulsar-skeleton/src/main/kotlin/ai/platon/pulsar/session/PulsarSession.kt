package ai.platon.pulsar.session

import ai.platon.pulsar.common.BeanFactory
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.PulsarEventHandler
import ai.platon.pulsar.crawl.common.DocumentCatch
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.PageCatch
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface PulsarSession : AutoCloseable {

    /**
     * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
     * */
    val id: Int

    /**
     * The pulsar context
     * */
    val context: AbstractPulsarContext

    /**
     * The session scope volatile config, every setting is supposed to be changed at any time and any place
     * */
    val sessionConfig: VolatileConfig

    val unmodifiedConfig: ImmutableConfig

    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * TODO: session scoped?
     * */
    val sessionBeanFactory: BeanFactory
    val display: String
    val pageCache: PageCatch
    val documentCache: DocumentCatch
    val globalCacheFactory: GlobalCacheFactory

    /**
     * Close objects when sessions close
     * */
    fun registerClosable(closable: AutoCloseable): Boolean
    fun disableCache()

    /**
     * Create a new options, with a new volatile config
     * */
    fun options(args: String = "", eventHandler: PulsarEventHandler? = null): LoadOptions
    fun property(name: String): String?
    fun property(name: String, value: String)
    fun normalize(url: String, args: String? = null): NormUrl
    fun normalize(url: String, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl
    fun normalizeOrNull(url: String?, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl?
    fun normalize(
        urls: Iterable<String>,
        options: LoadOptions = options(),
        toItemOption: Boolean = false
    ): List<NormUrl>

    fun normalize(url: UrlAware, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl
    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl?
    fun normalize(
        urls: Collection<UrlAware>,
        options: LoadOptions = options(),
        toItemOption: Boolean = false
    ): List<NormUrl>

    /**
     * Inject an url to fetch later
     *
     * @param url The url followed by options
     * @return The web page created
     */
    fun inject(url: String): WebPage

    /**
     * Get a page from database if exists
     *
     * @param url The url
     * @return The webpage
     */
    fun get(url: String): WebPage

    /**
     * Get a page from database if exists
     *
     * @param url The url
     * @return The webpage
     */
    fun getOrNull(url: String): WebPage?

    /**
     * Check if a page exists in the database
     *
     * @param url The url
     * @return true if the page exists in the database
     */
    fun exists(url: String): Boolean

    /**
     * Return the fetch state of the page
     *
     * @param page The webpage
     * @param options The load options
     * @return The fetch state of the page
     */
    fun fetchState(page: WebPage, options: LoadOptions): CheckState

    /**
     * Open a page with [url]
     *
     * @param url     The url of the page to open
     * @return The web page
     */
    fun open(url: String): WebPage

    /**
     * Load an url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    fun load(url: String, args: String): WebPage

    /**
     * Load an url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: String, options: LoadOptions = options()): WebPage

    fun load(url: UrlAware, args: String): WebPage

    fun load(url: UrlAware, options: LoadOptions = options()): WebPage

    fun load(normUrl: NormUrl): WebPage

    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    suspend fun loadDeferred(normUrl: NormUrl): WebPage

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    fun loadAll(
        urls: Iterable<String>, options: LoadOptions = options(), toItemOption: Boolean = false
    ): List<WebPage>

    fun loadAll(normUrls: Iterable<NormUrl>): List<WebPage>

    fun loadAsync(url: NormUrl): CompletableFuture<WebPage>

    fun loadAllAsync(urls: Iterable<NormUrl>): List<CompletableFuture<WebPage>>

    fun submit(url: UrlAware): AbstractPulsarSession

    fun submitAll(urls: Iterable<UrlAware>): PulsarSession

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load args
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, args: String): List<WebPage>

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = options()): List<WebPage>

    fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>>

    fun submitOutPages(portalUrl: String, options: LoadOptions = options()): PulsarSession
    /**
     * Load an url as a resource without browser rendering in the browser context
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    suspend fun loadResource(url: String, referer: String, args: String): WebPage
    /**
     * Load an url as a resource without browser rendering in the browser context
     *
     * @param url     The url to load
     * @param opts The load options
     * @return The web page
     */
    suspend fun loadResource(url: String, referer: String, opts: LoadOptions = options()): WebPage

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    fun parse(page: WebPage, noCache: Boolean = false): FeaturedDocument
    fun loadDocument(url: String, args: String): FeaturedDocument
    fun loadDocument(url: String, options: LoadOptions = options()): FeaturedDocument
    fun loadDocument(normUrl: NormUrl): FeaturedDocument
    fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?>
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?>
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    fun getVariable(name: String): Any?
    fun setVariable(name: String, value: Any)
    fun putSessionBean(obj: Any)
    fun delete(url: String)
    fun flush()
    fun persist(page: WebPage): Boolean
    fun export(page: WebPage, ident: String = ""): Path
    fun export(doc: FeaturedDocument, ident: String = ""): Path
    fun exportTo(doc: FeaturedDocument, path: Path): Path
}
