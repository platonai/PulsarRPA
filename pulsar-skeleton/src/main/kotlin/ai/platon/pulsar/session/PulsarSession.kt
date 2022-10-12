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
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.common.DocumentCatch
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.PageCatch
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * [PulsarSession] defines an interface to load web pages from local storage or fetch from the Internet,
 * as well as methods for parsing, extracting, saving, indexing, and exporting web pages.
 *
 * Key methods:
 *
 * * [load]: load a webpage from local storage, or fetch it from the Internet.
 * * [parse]: parse a webpage into a document.
 * * [scrape]: load a webpage, parse it into a document and then extract fields from the document.
 * * [submit]: submit a url to the url pool, the url will be processed in the main loop later.
 *
 * And also the batch versions:
 *
 * * [loadOutPages]: load the portal page and out pages.
 * * [scrapeOutPages]: load the portal page and out pages, extract fields from out pages.
 *
 * The first thing to understand is how to load a page. Load methods like [load] first
 * check the local storage and return the local version if the required page exists and meets the
 * requirements, otherwise it will be fetched from the Internet.
 *
 * The `load parameters` or `load options` can be used to specify when the system will fetch a webpage
 * from the Internet:
 *
 * . Expiration
 * . Force refresh
 * . Page size
 * . Required fields
 * . Other conditions
 *
 * Once a webpage is loaded from local storage, or fetched from the Internet,
 * we come to the next process steps:
 * 1. parse the web content into a HTML document
 * 2. extract fields from the HTML document
 * 3. write the fields into a destination, such as
 *    1. plain file, avro file, CSV, excel, mongodb, mysql, etc.
 *    2. solr, elastic, etc.
 *
 * There are many ways to fetch the content of a page from the Internet:
 * 1. http protocol
 * 2. through a real browser
 *
 * Since the webpages are becoming more and more complex, fetching webpages through
 * real browsers is the primer way nowadays.
 *
 * When we fetch webpages using a real browser, we need to interact with pages to
 * ensure the required fields are loaded correctly and completely. Enable [PageEvent]
 * and use [WebDriver] to archive such purpose.
 *
 * ```kotlin
 * val options = session.options(args)
 * options.event.browseEvent.onDidDOMStateCheck.addLast { page, driver ->
 *   driver.scrollDown()
 * }
 * session.load(url, options)
 * ```
 *
 * Pulsar [WebDriver] provides a complete method set for RPA, just like selenium, playwright
 * and puppeteer, all actions and behaviors are optimized to mimic real people as closely as possible.
 * */
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
     * The session scope volatile config, every setting is supposed to be changed at any time
     * and any place
     * */
    val sessionConfig: VolatileConfig

    val unmodifiedConfig: ImmutableConfig

    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * */
    @Deprecated("Not used any more")
    val sessionBeanFactory: BeanFactory
    /**
     * A short descriptive display text.
     * */
    val display: String
    /**
     * The global page cache
     * */
    val pageCache: PageCatch
    /**
     * The global document cache
     * */
    val documentCache: DocumentCatch
    /**
     * The global cache
     * */
    val globalCache: GlobalCache

    @Deprecated("Factory should not be a interface property, globalCache is OK")
    val globalCacheFactory: GlobalCacheFactory
    /**
     * Close objects when the session closes
     * */
    fun registerClosable(closable: AutoCloseable): Boolean
    /**
     * Disable page cache and document cache
     * */
    fun disablePDCache()

    /**
     * Create a new options, with a new volatile config
     * */
    fun options(args: String = "", event: PageEvent? = null): LoadOptions

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
     * Inject a url as a seed to fetch. Injection is usually used in Nutch style crawls,
     * where the execution flow is like the following:
     *
     * inject -> generate -> fetch -> parse -> update
     *              ^                            ^
     *              |    <-     <-      <-       |
     *
     * @param url The url to inject, con be followed by arguments
     * @return A newly created webpage which is ready to be generated
     */
    fun inject(url: String): WebPage

    /**
     * Get a page from storage
     *
     * @param url The url
     * @return The webpage
     */
    fun get(url: String): WebPage

    /**
     * Get a page from storage.
     *
     * @param url The url
     * @return The page in storage if exists or null
     */
    fun getOrNull(url: String): WebPage?

    /**
     * Check if the page exists in the storage
     *
     * @param url The url to check
     * @return true if the page exists, false otherwise
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
     * Open a url.
     *
     * This method opens the url immediately, regardless of the previous state of the page.
     *
     * @param url The url to open
     * @return The webpage
     */
    fun open(url: String): WebPage

    /**
     * Load a url with arguments.
     *
     * This method checks the local storage first, if it exists and is good,
     * return the persisted version, otherwise, fetch it from the Internet.
     *
     * Other fetch condition can be specified in load arguments:
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     *
     * @param url The url to load
     * @param args The load arguments
     * @return The webpage
     */
    fun load(url: String, args: String): WebPage

    /**
     * Load a url with options.
     *
     * This method checks the local storage first, if it exists and is good,
     * return the persisted version, otherwise, fetch it from the Internet.
     *
     * Other fetch condition can be specified in load arguments:
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     *
     * @param url The url to load
     * @param options The load options
     * @return The webpage
     */
    fun load(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with the specified load arguments.
     *
     * @param url     The url to load
     * @param args The load arguments
     * @return The webpage loaded
     */
    fun load(url: UrlAware, args: String): WebPage

    /**
     * Load a url with options.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    fun load(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url in normalized form.
     *
     * @param normUrl The normalized url
     * @return The web page
     */
    fun load(normUrl: NormUrl): WebPage

    /**
     * Load a url with specified options.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
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
    fun loadAll(urls: Iterable<String>, options: LoadOptions = options()): List<WebPage>

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
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String, args: String? = null): PulsarSession

    /**
     * Submit a url to the url pool, the url will be processed in the main crawl loop later
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: UrlAware): PulsarSession

    /**
     * Submit the urls to the url pool, the submitted urls will be processed in the main crawl loop later
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>): PulsarSession

    /**
     * Submit the urls to the url pool, the submitted urls will be processed in the main crawl loop later
     *
     * @param urls The urls to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>, args: String): PulsarSession

    /**
     * Submit the urls to the url pool, the submitted urls will be processed in the main crawl loop later
     *
     * @param urls The urls to submit
     * @return This session
     */
    fun submitAll(urls: Collection<UrlAware>): PulsarSession

    /**
     * Load out pages linked from the portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load arguments
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
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param args The load arguments
     * @return The web page
     */
    suspend fun loadResource(url: String, referer: String, args: String): WebPage
    /**
     * Load a url as a resource without browser rendering in the browser context
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
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
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Iterable<String>): Map<String, String?>
    /**
     * Scrape a webpage
     * */
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?>
    /**
     * Scrape a webpage
     * */
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Map<String, String>): Map<String, String?>
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
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Scrape a webpage
     * */
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Scrape a webpage
     * */
    fun scrape(
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param args The load arguments
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param options The load options
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Iterable<String>): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param args The load arguments
     * @param restrictSelector The selector used to restrict all fields to be inside the DOM
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param options The load options
     * @param restrictSelector The selector used to restrict all fields to be inside the DOM
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param args The load arguments
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields with their name from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param options The load options
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields with their name from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param args The load arguments
     * @param restrictSelector The selector used to restrict all fields to be inside the DOM
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Scrape out pages using given selectors.
     *
     * @param portalUrl The portal url the scraping start from
     * @param options The load options
     * @param restrictSelector The selector used to restrict all fields to be inside the DOM
     * @param fieldSelectors The CSS selectors to extract fields from out pages
     * @return A list of extracted fields from out pages
     * */
    @ExperimentalApi
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
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
    @Deprecated("Not used any more")
    fun putSessionBean(obj: Any)

    /**
     * Delete a webpage from the backend storage
     * */
    fun delete(url: String)

    /**
     * Flush to the storage
     * */
    fun flush()

    /**
     * Persist to the storage
     * */
    fun persist(page: WebPage): Boolean

    /**
     * Export the content of a webpage.
     *
     * @param page Page to export
     * @param ident File name identifier used to distinguish from other names
     * @return The path of the exported page
     * */
    fun export(page: WebPage, ident: String = ""): Path

    /**
     * Export the outer HTML of the document.
     *
     * @param doc Document to export
     * @param ident File name identifier used to distinguish from other names
     * @return The path of the exported document
     * */
    fun export(doc: FeaturedDocument, ident: String = ""): Path

    /**
     * Export the whole HTML of the document to the given path.
     *
     * @param doc Document to export
     * @param path Path to save the exported content
     * @return The path of the exported document
     * */
    fun exportTo(doc: FeaturedDocument, path: Path): Path
}
