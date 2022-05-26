package ai.platon.pulsar.session

import ai.platon.pulsar.common.BeanFactory
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContext
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
    val context: PulsarContext

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
    /**
     * Disable page cache and document cache
     * */
    fun disablePDCache()

    /**
     * Create a new options, with a new volatile config
     * */
    fun options(args: String = "", eventHandler: PulsarEventHandler? = null): LoadOptions

    /**
     * Get a property
     * */
    fun property(name: String): String?

    /**
     * Set a session scope property
     * */
    fun property(name: String, value: String)
    /**
     * Normalize a url
     * */
    fun normalize(url: String, args: String? = null): NormUrl
    /**
     * Normalize a url
     * */
    fun normalize(url: String, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl
    /**
     * Normalize a url
     * */
    fun normalizeOrNull(url: String?, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl?
    /**
     * Normalize urls
     * */
    fun normalize(
        urls: Iterable<String>,
        options: LoadOptions = options(),
        toItemOption: Boolean = false
    ): List<NormUrl>

    /**
     * Normalize a url
     * */
    fun normalize(url: UrlAware, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl
    /**
     * Normalize a url
     * */
    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = options(), toItemOption: Boolean = false): NormUrl?
    /**
     * Normalize urls
     * */
    fun normalize(
        urls: Collection<UrlAware>,
        options: LoadOptions = options(),
        toItemOption: Boolean = false
    ): List<NormUrl>

    /**
     * Inject a url to fetch later
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
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    fun load(url: String, args: String): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    fun load(url: UrlAware, args: String): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * @param normUrl The normalized url
     * @return The web page
     */
    fun load(normUrl: NormUrl): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * @param normUrl The normalized url
     * @return The web page
     */
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

    /**
     * Load all urls with specified options
     *
     * @param normUrls    The urls to load
     * @return The web pages
     */
    fun loadAll(normUrls: Iterable<NormUrl>): List<WebPage>

    /**
     * Load a url with java async style
     *
     * @param url     The url to load
     * @return A future
     */
    fun loadAsync(url: NormUrl): CompletableFuture<WebPage>

    /**
     * Load all urls with specified options with java async style
     *
     * @param urls The urls to load
     * @return The web pages
     */
    fun loadAllAsync(urls: Iterable<NormUrl>): List<CompletableFuture<WebPage>>

    /**
     * Submit a url to the url pool, the url will be processed in the main crawl loop later
     *
     * @param url The url to submit
     * @return The web pages
     */
    fun submit(url: UrlAware): PulsarSession

    /**
     * Submit the urls to the url pool, the submitted urls will be processed in the main crawl loop later
     *
     * @param urls The urls to submit
     * @return The web pages
     */
    fun submitAll(urls: Iterable<UrlAware>): PulsarSession

    /**
     * Load out pages linked from the portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load args
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, args: String): List<WebPage>

    /**
     * Load out pages linked from the portal page
     *
     * @param portalUrl The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = options()): List<WebPage>

    /**
     * Load out pages linked from the portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Submit the urls of out pages in the portal page, the submitted urls will be processed in the main crawl loop later
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args The load arguments
     * @return The web pages
     */
    fun submitOutPages(portalUrl: String, args: String): AbstractPulsarSession

    /**
     * Submit the urls of out pages in the portal page, the submitted urls will be processed in the main crawl loop later
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun submitOutPages(portalUrl: String, options: LoadOptions = options()): PulsarSession

    /**
     * Load a url as a resource without browser rendering in the browser context
     *
     * @param url     The url to load
     * @param args The load arguments
     * @return The web page
     */
    suspend fun loadResource(url: String, referer: String, args: String): WebPage
    /**
     * Load a url as a resource without browser rendering in the browser context
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
    /**
     * Load or fetch a webpage and parse it into a document
     * */
    fun loadDocument(url: String, args: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into a document
     * */
    fun loadDocument(url: String, options: LoadOptions = options()): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into a document
     * */
    fun loadDocument(normUrl: NormUrl): FeaturedDocument
    /**
     * Scrape a webpage
     * */
    fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?>
    /**
     * Scrape a webpage
     * */
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?>
    /**
     * Scrape a webpage
     * */
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>
    /**
     * Scrape a webpage
     * */
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>

    /**
     * Scrape out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    /**
     * Scrape out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>
    /**
     * Get a variable associated with this session
     * */
    fun getVariable(name: String): Any?
    /**
     * Set a variable associated with this session
     * */
    fun setVariable(name: String, value: Any)
    /**
     * Put session scope bean
     * */
    fun putSessionBean(obj: Any)
    /**
     * Delete a webpage from the backend storage
     * */
    fun delete(url: String)
    /**
     * Flush to the backend storage
     * */
    fun flush()
    /**
     * Persist to the backend storage
     * */
    fun persist(page: WebPage): Boolean
    /**
     * Export a webpage
     * */
    fun export(page: WebPage, ident: String = ""): Path
    /**
     * Export a document
     * */
    fun export(doc: FeaturedDocument, ident: String = ""): Path
    /**
     * Export a document to the given path
     * */
    fun exportTo(doc: FeaturedDocument, path: Path): Path
}
