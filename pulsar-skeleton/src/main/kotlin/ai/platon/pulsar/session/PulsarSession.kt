package ai.platon.pulsar.session

import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormURL
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.common.DocumentCatch
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.PageCatch
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * [PulsarSession] defines an interface to load webpages from local storage or fetch from the Internet,
 * as well as methods for parsing, extracting, saving and exporting webpages.
 *
 * Key methods:
 *
 * * [load]: load a webpage from local storage, or fetch it from the Internet.
 * * [parse]: parse a webpage into a document.
 * * [scrape]: load a webpage, parse it into a document and then extract fields from the document.
 * * [submit]: submit a url to the URL pool, the url will be processed in the main loop later.
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
 * `Load parameters` or `load options` can be used to specify when the system will fetch a webpage
 * from the Internet:
 *
 * 1. Expiration
 * 2. Force refresh
 * 3. Page size
 * 4. Required fields
 * 5. Other conditions
 *
 * Once a page is loaded from local storage, or fetched from the Internet, we come to the next process steps:
 * 1. parse the page content into an HTML document
 * 2. extract fields from the HTML document
 * 3. write the extraction results to a destination, such as
 *    1. plain file, avro file, CSV, excel, mongodb, mysql, etc
 *    2. solr, elastic, etc
 *
 * There are many ways to fetch the content of a page from the Internet:
 * 1. through HTTP protocol
 * 2. through a real browser
 *
 * Since the webpages are becoming more and more complex, fetching webpages through
 * real browsers is the primary way nowadays.
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
 * [WebDriver] provides a complete method set for RPA, just like selenium, playwright
 * and puppeteer does, all actions and behaviors are optimized to mimic real people as closely as possible.
 * */
interface PulsarSession : AutoCloseable {

    /**
     * The session id
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

    /**
     * Close objects when the session closes
     * */
    fun registerClosable(closable: AutoCloseable): Boolean
    /**
     * Disable page cache and document cache
     * */
    fun disablePDCache()

    /**
     * Create a new [LoadOptions] object with arguments [args] and [event].
     * */
    fun options(args: String = "", event: PageEvent? = null): LoadOptions

    /**
     * Get a property from the session config.
     * */
    fun property(name: String): String?

    /**
     * Set a property to the session config.
     * */
    fun property(name: String, value: String)
    /**
     * Normalize a url.
     * */
    fun normalize(url: String): NormURL
    /**
     * Normalize a url with arguments.
     * */
    fun normalize(url: String, args: String): NormURL
    /**
     * Normalize a url with options.
     * */
    fun normalize(url: String, options: LoadOptions): NormURL
    /**
     * Normalize a url with options, return null if the url is not normal.
     * */
    fun normalizeOrNull(url: String?, options: LoadOptions = options()): NormURL?
    /**
     * Normalize urls.
     * */
    fun normalize(urls: Iterable<String>): List<NormURL>
    /**
     * Normalize urls with arguments.
     * */
    fun normalize(urls: Iterable<String>, args: String): List<NormURL>
    /**
     * Normalize urls with options.
     * */
    fun normalize(urls: Iterable<String>, options: LoadOptions): List<NormURL>
    /**
     * Normalize a url.
     * */
    fun normalize(url: UrlAware): NormURL
    /**
     * Normalize a url with arguments.
     * */
    fun normalize(url: UrlAware, args: String): NormURL
    /**
     * Normalize a url with options.
     * */
    fun normalize(url: UrlAware, options: LoadOptions): NormURL
    /**
     * Normalize a url with options.
     * */
    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = options()): NormURL?
    /**
     * Normalize urls.
     * */
    fun normalize(urls: Collection<UrlAware>): List<NormURL>
    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param args The arguments
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Collection<UrlAware>, args: String): List<NormURL>

    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Collection<UrlAware>, options: LoadOptions): List<NormURL>

    /**
     * Inject a url as a seed to fetch. Injection is usually used in Nutch style crawls
     * where the execution flow likes the following:
     *
     * inject -> generate -> fetch -> parse [ -> index ] -> update
     *              ^                                          ^
     *              |    <-     <-      <-         <-          |
     *
     * @param url The url to inject, con be followed by arguments
     * @return A newly created webpage record which is ready to be generated
     */
    fun inject(url: String): WebPage

    /**
     * Get a page from storage.
     *
     * @param url The url
     * @return The webpage in storage if exists, otherwise returns a NIL page
     */
    fun get(url: String): WebPage

    /**
     * Get a page from storage.
     *
     * @param url The url
     * @return The page in storage if exists, otherwise returns null
     */
    fun getOrNull(url: String): WebPage?

    /**
     * Check if the page exists in the storage.
     *
     * @param url The url to check
     * @return true if the page exists, false otherwise
     */
    fun exists(url: String): Boolean

    /**
     * Return the fetch state of the page.
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
     * @return The webpage loaded or NIL
     */
    fun open(url: String): WebPage

    /**
     * Load a url with specified arguments.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * Other fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * @param url The url to load
     * @return The webpage loaded or NIL
     */
    fun load(url: String): WebPage

    /**
     * Load a url with specified arguments.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * Other fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * @param url The url to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun load(url: String, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * Other fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * @param url The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun load(url: String, options: LoadOptions): WebPage

    /**
     * Load a url with the specified arguments.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param url  The url to load
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware): WebPage

    /**
     * Load a url with the specified arguments.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param url  The url to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware, args: String): WebPage

    /**
     * Load a url with options.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware, options: LoadOptions): WebPage

    /**
     * Load a normal url.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param normURL The normal url
     * @return The webpage loaded or NIL
     */
    fun load(normURL: NormURL): WebPage

    /**
     * Load a url with specified options.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param args The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: String, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified arguments.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url  The url to load
     * @param args The load args
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * This method first checks the url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param normURL The normal url
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(normURL: NormURL): WebPage

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Iterable<String>): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Iterable<String>, args: String): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Iterable<String>, options: LoadOptions): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Collection<UrlAware>): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Collection<UrlAware>, args: String): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun loadAll(urls: Collection<UrlAware>, options: LoadOptions): List<WebPage>

    /**
     * Load all normal urls with specified options
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param normURLs    The normal urls to load
     * @return The loaded webpages
     */
    fun loadAll(normURLs: List<NormURL>): List<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String, args: String): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String, options: LoadOptions): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware, args: String): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware, options: LoadOptions): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * @param url     The normal url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: NormURL): CompletableFuture<WebPage>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style
     *
     * This method first checks each url in the local store and return the local version if the page
     * exists and matches the requirements, otherwise fetch it from the Internet.
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: List<NormURL>): List<CompletableFuture<WebPage>>

    /**
     * Submit a url to the URL pool, the url will be processed in the crawl loop later
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be processed in a crawl loop
     *
     * @param url The url to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String, args: String): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be processed in a crawl loop
     *
     * @param url The url to submit
     * @param options The load options
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String, options: LoadOptions): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be processed in a crawl loop
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: UrlAware): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be processed in a crawl loop
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: UrlAware, args: String): PulsarSession

    // No such version, it's too complicated to handle events
    // fun submit(url: UrlAware, options: LoadOptions): PulsarSession

    /**
     * Submit the urls to the URL pool, the submitted urls will be processed in a crawl loop
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>): PulsarSession

    /**
     * Submit the urls to the URL pool, the submitted urls will be processed in a crawl loop
     *
     * @param urls The urls to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>, args: String): PulsarSession

    /**
     * Submit the urls to the URL pool, the submitted urls will be processed in a crawl loop
     *
     * @param urls The urls to submit
     * @param options The load options
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>, options: LoadOptions): PulsarSession

    /**
     * Submit the urls to the URL pool, the submitted urls will be processed in a crawl loop
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Collection<UrlAware>): PulsarSession

    /**
     * Submit the urls to the URL pool, the submitted urls will be processed in a crawl loop
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Collection<UrlAware>, args: String): PulsarSession

    // No such version, it's too complicated to handle events
    // fun submitAll(urls: Collection<UrlAware>, options: LoadOptions): PulsarSession

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load arguments
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: String, args: String): List<WebPage>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = options()): List<WebPage>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option asynchronously.
     *
     * @param portalUrl The portal url from where to load pages
     * @param args   The load arguments
     * @return The loaded out pages
     */
    fun loadOutPagesAsync(portalUrl: String, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option asynchronously.
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The loaded out pages
     */
    fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * The submitted urls will be processed in a crawl loop later.
     *
     * @param portalUrl The portal url from where to load pages
     * @param args      The load arguments
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitOutPages(portalUrl: String, args: String): PulsarSession

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * The submitted urls will be processed in a crawl loop later.
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitOutPages(portalUrl: String, options: LoadOptions = options()): PulsarSession

    /**
     * Load a url as a resource without browser rendering.
     *
     * @param url  The url to load
     * @param referrer The referrer URL
     * @param args The load arguments
     * @return The webpage containing the resource
     */
    fun loadResource(url: String, referrer: String, args: String): WebPage
    /**
     * Load a url as a resource without browser rendering.
     *
     * @param url     The url to load
     * @param referrer The referrer URL
     * @param options The load options
     * @return The webpage containing the resource
     */
    fun loadResource(url: String, referrer: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url as a resource without browser rendering.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url  The url to load
     * @param referrer The referrer URL
     * @param args The load arguments
     * @return The webpage containing the resource
     */
    suspend fun loadResourceDeferred(url: String, referrer: String, args: String): WebPage
    /**
     * Load a url as a resource without browser rendering.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * @param url     The url to load
     * @param referrer The referrer URL
     * @param options The load options
     * @return The webpage containing the resource
     */
    suspend fun loadResourceDeferred(url: String, referrer: String, options: LoadOptions = options()): WebPage
    /**
     * Parse a webpage into an HTML document.
     */
    fun parse(page: WebPage): FeaturedDocument
    /**
     * Parse a webpage into an HTML document.
     */
    fun parse(page: WebPage, noCache: Boolean = false): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     * */
    fun loadDocument(url: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     * */
    fun loadDocument(url: String, args: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     * */
    fun loadDocument(url: String, options: LoadOptions = options()): FeaturedDocument
    /**
     * Load or fetch a webpage and then parse it into an HTML document.
     * */
    fun loadDocument(normURL: NormURL): FeaturedDocument
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param args The load arguments
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their selectors
     * */
    fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?>
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param options The load options
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their selectors
     * */
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Iterable<String>): Map<String, String?>
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param args The load arguments
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their names
     * */
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?>
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param options The load options
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their names
     * */
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Map<String, String>): Map<String, String?>
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param args The load arguments
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their names
     * */
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param options The load options
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their selectors
     * */
    fun scrape(
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param args The load arguments
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their names
     * */
    fun scrape(
        url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * @param url The url to scrape
     * @param options The load options
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors The selectors to extract fields
     * @return All the extracted fields and their names
     * */
    fun scrape(
        url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param args Load arguments for both the portal page and out pages
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted
     *          with their selectors are saved in a map.
     * */
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their selectors are saved in a map.
     * */
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Iterable<String>): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param args Load arguments for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their selectors are saved in a map.
     * */
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM in the out pages
     *                         where all fields are restricted to
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their selectors are saved in a map.
     * */
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param args Load arguments for both the portal page and out pages
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their names are saved in a map.
     * */
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their names are saved in a map.
     * */
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param args Load arguments for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM in the out pages
     *                         where all fields are restricted to
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their names are saved in a map.
     * */
    fun scrapeOutPages(
        portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Load or fetch out pages specified by out link selector, and then extract fields specified by
     * field selectors from each out page.
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM in the out pages
     *                         where all fields are restricted to
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their names are saved in a map.
     * */
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Get a variable from this session
     * */
    fun getVariable(name: String): Any?

    /**
     * Set a variable into this session
     * */
    fun setVariable(name: String, value: Any)

    /**
     * Delete a page record from the storage
     * */
    fun delete(url: String)

    /**
     * Flush to the storage
     * */
    fun flush()

    /**
     * Persist a page to the storage
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
