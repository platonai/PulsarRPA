package ai.platon.pulsar.skeleton.session

import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.extractor.TextDocument
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.ai.tta.InstructionResult
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.skeleton.context.PulsarContext
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.DocumentCatch
import ai.platon.pulsar.skeleton.crawl.common.GlobalCache
import ai.platon.pulsar.skeleton.crawl.common.PageCatch
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.common.annotations.Beta
import org.jsoup.nodes.Element
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * The [PulsarSession] interface is designed to provide methods for loading pages from either
 * local storage or from the internet. Additionally, it offers functionality for parsing
 * these pages, extracting relevant data, and saving or exporting the web content.
 *
 * Key methods:
 *
 * * [load]: load a page from local storage or the Internet.
 * * [parse]: parse a page into a document.
 * * [submit]: submit a url to the URL pool, the url will be processed in the main loop later.
 * * [scrape]: load a page, parse it into a document and then extract fields from the document.
 *
 * Basic examples:
 *
 * ```kotlin
 * val url = "http://example.com"
 * val page = session.load(url)
 * val document = session.parse(page)
 *
 * val title = document.selectFirstTextOrNull(".title")
 * val content = document.selectFirstTextOrNull(".content")
 *
 * val fields = session.scrape("http://example.com", "-expire 1d", listOf(".title", ".content"))
 *
 * val document2 = session.loadDocument(url)
 *
 * val url2 = "http://example.com/1"
 * session.submit(url2)
 * ```
 *
 * And also the batch versions:
 *
 * * [loadOutPages]: load the portal page and out pages.
 * * [submitForOutPages]: load the portal page and submit out pages.
 * * [scrapeOutPages]: load the portal page and out pages, extract fields from out pages.
 *
 * ```kotlin
 * val urls = listOf("http://example.com", "http://example.com/1")
 * val pages = session.loadAll(urls)
 *
 * val pages2 = session.loadOutPages("http://example.com", "-outLink a[rel='next']")
 * val fields = session.scrapeOutPages("http://example.com", "-outLink a", listOf("title", "content"))
 *
 * session.submitAll(urls)
 * session.submitForOutPages("http://example.com", "-outLink a[rel='next']")
 * ```
 *
 * The primary consideration is understanding how to load a page efficiently. Load methods, such as [load],
 * initially check local storage for the required page. If the page exists locally and meets the necessary criteria,
 * it is retrieved from the local storage; otherwise, the page is fetched from the internet.
 *
 * To load a page, PulsarRPA follows these steps:
 *
 * 1. Check the local storage for the required page.
 * 2. If the page exists in local storage and meets the specified criteria (such as being up-to-date or valid),
 *    return the local version.
 * 3. If the page does not exist in local storage or does not meet the criteria, fetch the page from the internet.
 * 4. Handle any errors that occur during the process, such as issues with local storage access or problems with
 *    the network fetch.
 *
 * To optimize the retrieval of webpages, `load parameters` or `load options` are used to establish the conditions
 * under which a page should be fetched from the internet.
 *
 * These conditions may encompass:
 *
 * * Expiration timestamps to determine if the local page is still valid.
 * * A directive for a forced refresh to ensure the latest content is retrieved.
 * * Specifications regarding the minimal acceptable page size.
 * * Identifiers for essential fields that must be present in the page.
 * * Additional custom criteria that align with the application’s needs.
 *
 * For example:
 *
 * ```kotlin
 * val url = "http://example.com"
 * val page = session.load(url, "-expire 1d")
 *
 * val url2 = "http://example.com/1"
 * val page2 = session.load(url2, "-refresh")
 *
 * val url3 = "http://example.com/2"
 * val page3 = session.load(url3, "-requireSize 100000")
 * ```
 *
 * Once a page has been retrieved from either local storage or the internet, the subsequent processing steps include:
 *
 * * Parsing the page content to construct an HTML document.
 * * Extracting relevant data fields from the parsed HTML document.
 * * Recording the extracted data to a designated destination.
 *
 * For example:
 *
 * ```kotlin
 * val page = session.load("http://example.com")
 * val document = session.parse(page)
 *
 * val title = document.selectFirstTextOrNull(".title")
 * val content = document.selectFirstTextOrNull(".content")
 *
 * val path = session.exportTo(page, Paths.get("/tmp/example.html"))
 * ```
 *
 * There are many ways to fetch the content of a page from the Internet, but the two primary methods are:
 * 1. through HTTP protocol
 * 2. through a real browser
 *
 * Since the webpages are becoming more and more complex, fetching webpages through real browsers is the
 * primary way nowadays.
 *
 * When we fetch webpages using a real browser, sometimes we need to interact with pages to ensure the required
 * fields are loaded correctly and completely. Enable [PageEventHandlers] and use [WebDriver] to archive such purpose.
 *
 * ```kotlin
 * val options = session.options(args)
 * options.eventHandlers.browseEventHandlers.onDocumentSteady.addLast { page, driver ->
 *   driver.fill("input[name='search']", "geek")
 *   driver.click("button[type='submit']")
 * }
 * session.load(url, options)
 * ```
 *
 * [WebDriver] offers a comprehensive method set for browser automation, meticulously designed to replicate
 * real human actions and behaviors with precision.
 *
 * @see UrlAware Encapsulates a URL along with additional specifications defining its loading behavior.
 * @see LoadOptions A set of parameters that define how a page should be loaded.
 * @see WebPage Store the content, metadata, and other data of a page.
 * @see FeaturedDocument The HTML document.
 * @see PageEventHandlers  Specifies all event handlers that are triggered at various stages of a webpage’s lifecycle.
 * @see WebDriver Offers a comprehensive method set for browser automation.
 * */
interface PulsarSession : AutoCloseable {
    /**
     * The session id.
     * */
    val id: Int
    /**
     * The pulsar context which is used to create this session.
     * */
    val context: PulsarContext
    /**
     * This is an immutable configuration, loaded from the configuration file during process startup.
     * Once the process has started, this configuration remains unchangeable.
     * */
    val unmodifiedConfig: ImmutableConfig

    /**
     * The session-specific volatile configuration, which allows dynamic adjustments to settings at any point during the session.
     * Unlike the immutable configuration loaded at startup, this configuration is designed to be modified on-the-fly to adapt to runtime requirements.
     * */
    val sessionConfig: VolatileConfig

    /**
     * A short descriptive display text.
     * */
    val display: String
    /**
     * The global page cache.
     * */
    val pageCache: PageCatch
    /**
     * The global document cache.
     * */
    val documentCache: DocumentCatch
    /**
     * The global cache.
     * */
    val globalCache: GlobalCache
    /**
     * Disable page cache and document cache
     * */
    fun disablePDCache()

    /**
     * Get a variable which is stored in this session
     *
     * @param name The name of the variable
     * @return The value of the variable
     * */
    fun data(name: String): Any?
    /**
     * Store a variable in this session
     *
     * @param name The name of the variable
     * @param value The value of the variable
     * */
    fun data(name: String, value: Any)
    /**
     * Get a property from the session scope.
     * */
    fun property(name: String): String?
    /**
     * Set a session scope property.
     * */
    fun property(name: String, value: String)
    /**
     * Create a new [LoadOptions] object with [args].
     * */
    fun options(args: String = ""): LoadOptions
    /**
     * Create a new [LoadOptions] object with [args] and [event].
     * */
    fun options(args: String = "", event: PageEventHandlers?): LoadOptions
    /**
     * Create a new [LoadOptions] object with [options].
     * */
    fun normalize(options: LoadOptions): LoadOptions
    /**
     * Normalize a url.
     *
     * @param url The url to normalize
     * @return The normalized url
     * */
    fun normalize(url: String): NormURL
    /**
     * Normalize a url.
     *
     * @param url The url to normalize
     * @param args The arguments
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url
     * */
    fun normalize(url: String, args: String, toItemOption: Boolean = false): NormURL
    /**
     * Normalize a url.
     *
     * @param url The url to normalize
     * @param options The LoadOptions applied to the url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url
     * */
    fun normalize(url: String, options: LoadOptions, toItemOption: Boolean = false): NormURL
    /**
     * Normalize a url, return null if the url is invalid.
     *
     * @param url The url to normalize
     * @param options The LoadOptions applied to the url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url or null
     * */
    fun normalizeOrNull(url: String?, options: LoadOptions = options(), toItemOption: Boolean = false): NormURL?
    /**
     * Normalize urls, remove invalid ones.
     *
     * @param urls The urls to normalize
     * @return All normalized urls
     * */
    fun normalize(urls: Iterable<String>): List<NormURL>
    /**
     * Normalize urls, remove invalid ones.
     *
     * @param urls The urls to normalize
     * @param args The arguments
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls
     * */
    fun normalize(urls: Iterable<String>, args: String, toItemOption: Boolean = false): List<NormURL>
    /**
     * Normalize urls, remove invalid ones.
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls
     * */
    fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean = false): List<NormURL>
    /**
     * Normalize a url, return `NormURL.NIL` if the url is invalid.
     *
     * @param url The url to normalize
     * @return The normalized url
     * */
    fun normalize(url: UrlAware): NormURL
    /**
     * Normalize a url, return `NormURL.NIL` if the url is invalid.
     *
     * @param url The url to normalize
     * @param args The arguments
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url
     * */
    fun normalize(url: UrlAware, args: String, toItemOption: Boolean = false): NormURL
    /**
     * Normalize a url, return `NormURL.NIL` if the url is invalid.
     *
     * @param url The url to normalize
     * @param options The LoadOptions applied to the url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url
     * */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean = false): NormURL
    /**
     * Normalize a url, return null if the url is invalid.
     *
     * @param url The url to normalize
     * @param options The LoadOptions applied to the url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return The normalized url or null
     * */
    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = options(), toItemOption: Boolean = false): NormURL?
    /**
     * Normalize urls, remove invalid ones.
     *
     * @param urls The urls to normalize
     * @return All normalized urls
     * */
    fun normalize(urls: Collection<UrlAware>): List<NormURL>
    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param args The arguments
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Collection<UrlAware>, args: String, toItemOption: Boolean = false): List<NormURL>

    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean = false): List<NormURL>

    /**
     * Get a page from local storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.get(url)
     * ```
     *
     * @param url The url
     * @return The webpage in storage if exists, otherwise returns a NIL page
     */
    fun get(url: String): WebPage

    /**
     * Get a page from local storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.get(url, "content", "metadata")
     * ```
     *
     * @param url The url
     * @param fields The fields to load from local storage
     * @return The webpage in storage if exists, otherwise returns a NIL page
     */
    fun get(url: String, vararg fields: String): WebPage

    /**
     * Get a page from local storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.getOrNull(url)
     * ```
     *
     * @param url The url
     * @return The page in storage if exists, otherwise returns null
     */
    fun getOrNull(url: String): WebPage?

    /**
     * Get a page from local storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.getOrNull(url, "content", "metadata")
     * ```
     *
     * @param url The url
     * @param fields The fields to load from local storage
     * @return The page in storage if exists, otherwise returns null
     */
    fun getOrNull(url: String, vararg fields: String): WebPage?

    /**
     * Get the content of the page from the storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val content = session.getContent(url)
     * ```
     *
     * @param url The url of the page to retrieve
     * @return The page content or null
     */
    fun getContent(url: String): ByteBuffer?

    /**
     * Get the content of the page from the storage
     *
     * ```kotlin
     * val url = "http://example.com"
     * val content = session.getContentAsString(url)
     * ```
     *
     * @param url The url of the page to retrieve
     * @return The page content in string format or null
     */
    @Beta
    fun getContentAsString(url: String): String?

    /**
     * Check if the page exists in the storage.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val exists = session.exists(url)
     * ```
     *
     * @param url The url to check
     * @return true if the page exists, false otherwise
     */
    fun exists(url: String): Boolean

    /**
     * Return the fetch state of the page.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val state = session.fetchState(url)
     * ```
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
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.open(url)
     * ```
     *
     * @param url The url to open
     * @return The webpage loaded or NIL
     */
    fun open(url: String): WebPage

    /**
     * Open a url with page events.
     *
     * This method opens the url immediately, regardless of the previous state of the page.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val event = PrintFlowEvent()
     * val page = session.open(url, event)
     * ```
     *
     * @param url The url to open
     * @return The webpage loaded or NIL
     */
    fun open(url: String, eventHandlers: PageEventHandlers): WebPage
    /**
     * Open a url with webdriver.
     *
     * This method opens the url immediately, regardless of the previous state of the page.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.open(url, driver)
     * ```
     *
     * @param url The url to open
     * @return The webpage loaded or NIL
     */
    suspend fun open(url: String, driver: WebDriver): WebPage
    /**
     * Open a url with page events and webdriver.
     *
     * This method opens the url immediately, regardless of the previous state of the page.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val event = PrintFlowEvent()
     * val page = session.open(url, event)
     * ```
     *
     * @param url The url to open
     * @return The webpage loaded or NIL
     */
    suspend fun open(url: String, driver: WebDriver, eventHandlers: PageEventHandlers): WebPage
    /**
     * Connect a webpage to a webdriver. Once connected, this webdriver is the only driver to automate web pages.
     *
     * ```kotlin
     * val driver = DefaultBrowserFactory().launchSystemDefaultBrowser().newDriver()
     * val page = session.connect(driver)
     * ```
     *
     * @param driver The webdriver to connect
     */
    fun connect(driver: WebDriver)

    /**
     * Connect a webpage to a browser. Once connected, the browser is the only browser to automate web pages.
     *
     * ```kotlin
     * val browser = DefaultBrowserFactory().launchSystemDefaultBrowser()
     * val page = session.connect(driver)
     * ```
     *
     * @param browser The browser to connect
     * */
    fun connect(browser: Browser)

    /**
     * Load an url.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * Other fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * ```kotlin
     * val url = "http://example.com"
     * val page = session.load(url)
     * ```
     *
     * @param url The url to load
     * @return The webpage loaded or NIL
     */
    fun load(url: String): WebPage

    /**
     * Load a url with specified arguments.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * Fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * ```kotlin
     * val url = "http://example.com"
     * val args = "-expire 1d"
     * val page = session.load(url, args)
     * ```
     *
     * @param url The url to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun load(url: String, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * Fetch conditions can be specified by load arguments:
     *
     * 1. expiration
     * 2. page size requirement
     * 3. fields requirement
     * 4. other
     *
     * ```kotlin
     * val url = "http://example.com"
     * val options = session.options("-expire 1d")
     * val page = session.load(url, options)
     * ```
     *
     * @param url The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun load(url: String, options: LoadOptions): WebPage

    /**
     * Load a url with the specified arguments.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val url = Hyperlink("http://example.com")
     * val page = session.load(url)
     * ```
     *
     * @param url  The url to load
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware): WebPage

    /**
     * Load a url with the specified arguments.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val url = Hyperlink("http://example.com")
     * val page = session.load(url, "-expire 1d")
     * ```
     *
     * @param url  The url to load
     * @param args The load arguments
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware, args: String): WebPage

    /**
     * Load a url with options.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val url = Hyperlink("http://example.com")
     * val options = session.options("-expire 1d")
     * val page = session.load(url, options)
     * ```
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    fun load(url: UrlAware, options: LoadOptions): WebPage

    /**
     * Load a normal url.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val url = session.normalize("http://example.com")
     * val page = session.load(url)
     * ```
     *
     * * @param url The normal url
     * @return The webpage loaded or NIL
     */
    fun load(url: NormURL): WebPage

    /**
     * Load a url with specified options.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val args = "-expire 1d"
     * val page = session.loadDeferred(url, args)
     * ```
     *
     * @param url     The url to load
     * @param args The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: String, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val url = "http://example.com"
     * val options = session.options("-expire 1d")
     * val page = session.loadDeferred(url, options)
     * ```
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified arguments.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val url = Hyperlink("http://example.com")
     * val page = session.loadDeferred(url, "-expire 1d")
     * ```
     *
     * @param url  The url to load
     * @param args The load args
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    /**
     * Load a url with specified options.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val url = Hyperlink("http://example.com")
     * val options = session.options("-expire 1d")
     * val page = session.loadDeferred(url, options)
     * ```
     *
     * @param url     The url to load
     * @param options The load options
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    /**
     * Load a url with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val url = session.normalize("http://example.com")
     * val page = session.loadDeferred(url)
     * ```
     *
     * @param url The normal url
     * @return The webpage loaded or NIL
     */
    suspend fun loadDeferred(url: NormURL): WebPage

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1")
     * val pages = session.loadAll(urls)
     * ```
     *
     * @param urls    The urls to load
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Iterable<String>): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1")
     * val pages = session.loadAll(urls, "-expire 1d")
     * ```
     *
     * @param urls    The urls to load
     * @param args The load arguments
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Iterable<String>, args: String): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1")
     * val pages = session.loadAll(urls, session.options("-expire 1d"))
     * ```
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Iterable<String>, options: LoadOptions): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf(Hyperlink("http://example.com"), Hyperlink("http://example.com/1"))
     * val pages = session.loadAll(urls)
     * ```
     *
     * @param urls    The urls to load
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Collection<UrlAware>): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf(Hyperlink("http://example.com"), Hyperlink("http://example.com/1"))
     * val pages = session.loadAll(urls, "-expire 1d")
     * ```
     *
     * @param urls    The urls to load
     * @param args The load arguments
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Collection<UrlAware>, args: String): List<WebPage>

    /**
     * Load all urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf(Hyperlink("http://example.com"), Hyperlink("http://example.com/1"))
     * val pages = session.loadAll(urls, session.options("-expire 1d"))
     * ```
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(urls: Collection<UrlAware>, options: LoadOptions): List<WebPage>

    /**
     * Load all normal urls with specified options
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1").map { session.normalize(it) }
     * val pages = session.loadAll(urls)
     * ```
     *
     * @param normUrls    The normal urls to load
     * @return The successfully loaded webpages, all failed urls are ignored
     */
    fun loadAll(normUrls: List<NormURL>): List<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * String url = "http://example.com";
     * WebPage page = session.loadAsync(url).join();
     * ```
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String): CompletableFuture<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * String url = "http://example.com";
     * WebPage page = session.loadAsync(url, "-expire 1d").join();
     * ```
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String, args: String): CompletableFuture<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * String url = "http://example.com";
     * WebPage page = session.loadAsync(url, session.options("-expire 1d")).join();
     * ```
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: String, options: LoadOptions): CompletableFuture<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * Hyperlink url = new Hyperlink("http://example.com");
     * WebPage page = session.loadAsync(url).join();
     * ```
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware): CompletableFuture<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * Hyperlink url = new Hyperlink("http://example.com");
     * WebPage page = session.loadAsync(url, "-expire 1d").join();
     * ```
     *
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware, args: String): CompletableFuture<WebPage>

    /**
     * Load a url in java async style
     *
     * ```java
     * Hyperlink url = new Hyperlink("http://example.com");
     * WebPage page = session.loadAsync(url, session.options("-expire 1d")).join();
     * ```
     * @param url     The url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: UrlAware, options: LoadOptions): CompletableFuture<WebPage>

    /**
     * Load a normal url in java async style
     *
     * ```java
     * NormURL url = session.normalize("http://example.com");
     * WebPage page = session.loadAsync(url).join();
     * ```
     *
     * @param url     The normal url to load
     * @return A completable future of webpage
     */
    fun loadAsync(url: NormURL): CompletableFuture<WebPage>

    /**
     * Load all urls in java async style
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<String> urls = Arrays.asList("http://example.com", "http://example.com/1");
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls).toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>): List<CompletableFuture<WebPage>>

    /**
     * Load all urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<String> urls = Arrays.asList("http://example.com", "http://example.com/1");
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls, "-expire 1d").toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<String> urls = Arrays.asList("http://example.com", "http://example.com/1");
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls, session.options("-expire 1d")).toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Iterable<String>, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load all urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<UrlAware> urls = Arrays.asList(new Hyperlink("http://example.com"), new Hyperlink("http://example.com/1"));
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls).toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<UrlAware> urls = Arrays.asList(new Hyperlink("http://example.com"), new Hyperlink("http://example.com/1"));
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls, "-expire 1d").toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<UrlAware> urls = Arrays.asList(new Hyperlink("http://example.com"), new Hyperlink("http://example.com/1"));
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls, session.options("-expire 1d")).toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: Collection<UrlAware>, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load all normal urls in java async style.
     *
     * This method initially verifies the presence of the page in the local store. If the page exists and meets the
     * specified requirements, it returns the local version. Otherwise, it fetches the page from the Internet.
     *
     * ```java
     * List<NormURL> urls = Arrays.asList(session.normalize("http://example.com"), session.normalize("http://example.com/1"));
     * CompletableFuture<?>[] pages = session.loadAllAsync(urls).toArray(CompletableFuture<?>[]::new);
     * CompletableFuture.allOf(futures).join();
     * ```
     *
     * @param urls The normal urls to load
     * @return The completable futures of webpages
     */
    fun loadAllAsync(urls: List<NormURL>): List<CompletableFuture<WebPage>>

    /**
     * Submit a url to the URL pool, and it will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submit("http://example.com")
     * PulsarContexts.await()
     * ```
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submit("http://example.com", "-expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param url The url to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submit(url: String, args: String): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submit("http://example.com", session.options("-expire 1d"))
     * PulsarContexts.await()
     * ```
     *
     * Submit can be used with event listeners to handle the page events.
     *
     * The code snippet below shows how to submit a url with an attached load event handler.
     * This handler is invoked once the page is loaded, and it prints the URL of the page.
     *
     * ```kotlin
     * val options = session.options("-expire 1d")
     * options.eventHandlers.loadEvent.onLoaded.addLast { println(it.url) }
     * session.submit("http://example.com", options)
     * PulsarContexts.await()
     * ```
     *
     * The code snippet below shows how to submit a hyperlink with every supported event handlers attached.
     * Each event handler will print a message when its associated event is triggered.
     *
     * ```kotlin
     * val event = PrintFlowEvent()
     * val options = session.options("-expire 1d", event)
     * session.submit("http://example.com", options)
     * PulsarContexts.await()
     * ```
     *
     * @param url The url to submit
     * @param options The load options
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see submit(UrlAware) to learn more.
     */
    fun submit(url: String, options: LoadOptions): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * Basic usage:
     *
     * ```kotlin
     * session.submit(Hyperlink("http://example.com"))
     * PulsarContexts.await()
     * ```
     *
     * Submit methods are commonly be used in conjunction with event listeners to handle page events.
     *
     * The code snippet below shows how to submit a hyperlink with an attached load event handler.
     * This handler is invoked once the page is loaded, and it prints the URL of the page.
     *
     * ```kotlin
     * val hyperlink = ListenableHyperlink("http://example.com")
     * hyperlink.eventHandlers.loadEvent.onLoaded.addLast { page -> println(page.url) }
     * session.submit(hyperlink)
     * PulsarContexts.await()
     * ```
     *
     * The code snippet below shows how to submit a hyperlink with a event handler attached that processes
     * the parsed document once the relevant event is triggered.
     *
     * ```kotlin
     * val hyperlink = ListenableHyperlink("http://example.com", args = "-parse", event = PrintFlowEvent())
     * hyperlink.eventHandlers.loadEvent.onHTMLDocumentParsed.addLast { page, document ->
     *      val title = document.selectFirstOrNull(".title")?.text() ?: "Not found"
     *      println(title)
     * }
     * session.submit(hyperlink)
     * PulsarContexts.await()
     * ```
     *
     * ParsableHyperlink can be used to simplify the load-and-parse tasks.
     *
     * ```kotlin
     * val hyperlink = ParsableHyperlink("http://example.com") { page, document ->
     * val title = document.selectFirstOrNull(".title")?.text() ?: ""
     *      println(title)
     * }
     * session.submit(hyperlink)
     * PulsarContexts.await()
     * ```
     *
     * The code snippet below shows how to submit a hyperlink with every supported event handlers attached.
     * Each event handler will print a message when its associated event is triggered.
     *
     * ```kotlin
     * val event = PrintFlowEvent()
     * val options = session.options("-expire 1d", event)
     * session.submit("http://example.com", options)
     * PulsarContexts.await()
     * ```
     *
     * @param url The url to submit
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see PageEventHandlers
     * @see ai.platon.pulsar.common.urls.Hyperlink
     * @see ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink
     * @see ai.platon.pulsar.skeleton.crawl.common.url.ParsableHyperlink
     */
    fun submit(url: UrlAware): PulsarSession

    /**
     * Submit a url to the URL pool, and it will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submit(Hyperlink("http://example.com"), "-expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param url The url to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see submit(UrlAware) to learn more.
     */
    fun submit(url: UrlAware, args: String): PulsarSession

    /**
     * No such version, it's too complicated to handle events
     * */
    fun submit(url: UrlAware, options: LoadOptions): PulsarSession = throw NotImplementedError(
        "The signature submit(UrlAware, LoadOptions) is a confusing version, " +
            "it's too complicated to handle events and should not be implemented.")

    /**
     * Submit urls to the URL pool, and they will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submitAll(listOf("http://example.com", "http://example.com/1"))
     * PulsarContexts.await()
     * ```
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>): PulsarSession

    /**
     * Submit urls to the URL pool, and they will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submitAll(listOf("http://example.com", "http://example.com/1"), "-expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param urls The urls to submit
     * @param args The load arguments
     * @return The [PulsarSession] itself to enabled chained operations
     */
    fun submitAll(urls: Iterable<String>, args: String): PulsarSession

    /**
     * Submit urls to the URL pool, and they will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session.submitAll(listOf("http://example.com", "http://example.com/1"), session.options("-expire 1d"))
     * PulsarContexts.await()
     * ```
     *
     * @param urls The urls to submit
     * @param options The load options
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see submit(UrlAware) to learn more.
     */
    fun submitAll(urls: Iterable<String>, options: LoadOptions): PulsarSession

    /**
     * Submit urls to the URL pool, and they will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1").map { Hyperlink(it) }
     * session.submitAll(urls)
     * PulsarContexts.await()
     * ```
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see submit(UrlAware) to learn more.
     */
    fun submitAll(urls: Collection<UrlAware>): PulsarSession

    /**
     * Submit urls to the URL pool, and they will be subsequently processed in the crawl loop.
     *
     * A submit operation is non-blocking, meaning it returns immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * val urls = listOf("http://example.com", "http://example.com/1").map { Hyperlink(it) }
     * session.submitAll(urls, "-expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param urls The urls to submit
     * @return The [PulsarSession] itself to enabled chained operations
     *
     * @see submit(UrlAware) to learn more.
     */
    fun submitAll(urls: Collection<UrlAware>, args: String): PulsarSession

    /**
     * No such version, it's too complicated to handle events
     * */
    fun submitAll(urls: Collection<UrlAware>, options: LoadOptions): PulsarSession =
        throw NotImplementedError("The signature submitAll(Collection<UrlAware>, LoadOptions) is a confusing version, " +
            "it's too complicated to handle events and should not be implemented.")

    /**
     * No such confusing version
     * */
    fun loadOutPages(portalUrl: String): List<WebPage> =
        throw NotImplementedError("The signature loadOutPages(String) is a confusing version and should not be " +
            "implemented.")

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPages("http://example.com", "-outLink a")
     * val pages2 = session.loadOutPages("http://example.com", "-outLink a -expire 1d")
     * ```
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load arguments
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: String, args: String): List<WebPage>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPages("http://example.com", session.options("-outLink a"))
     * val pages2 = session.loadOutPages("http://example.com", session.options("-outLink a -expire 1d"))
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions): List<WebPage>

    /**
     * A confusing version, it's too complicated to handle events and should not be implemented.
     */
    fun loadOutPages(portalUrl: UrlAware): List<WebPage> =
        throw NotImplementedError("The signature loadOutPages(UrlAware) is a confusing version, it's too complicated to " +
            "handle events and should not be implemented.")

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPages("http://example.com", "-outLink a")
     * val pages2 = session.loadOutPages("http://example.com", "-outLink a -expire 1d")
     * ```
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load arguments
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: UrlAware, args: String): List<WebPage>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPages("http://example.com", session.options("-outLink a"))
     * val pages2 = session.loadOutPages("http://example.com", session.options("-outLink a -expire 1d"))
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The loaded out pages
     */
    fun loadOutPages(portalUrl: UrlAware, options: LoadOptions): List<WebPage>

    /**
     * A confusing version, it's too complicated to handle events and should not be implemented.
     */
    fun loadOutPages(portalUrl: NormURL): List<WebPage> =
        throw NotImplementedError("The signature loadOutPages(NormURL) is " +
            "a confusing version, it's too complicated to handle events and should not be implemented.")

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option asynchronously.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPagesAsync("http://example.com", "-outLink a")
     * val pages2 = session.loadOutPagesAsync("http://example.com", "-outLink a -expire 1d")
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param args   The load arguments
     * @return The loaded out pages
     */
    fun loadOutPagesAsync(portalUrl: String, args: String): List<CompletableFuture<WebPage>>

    /**
     * Load or fetch the portal page, and then load or fetch the out links selected by `-outLink` option asynchronously.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * ```kotlin
     * val pages = session.loadOutPagesAsync("http://example.com", session.options("-outLink a"))
     * val pages2 = session.loadOutPagesAsync("http://example.com", session.options("-outLink a -expire 1d"))
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The loaded out pages
     */
    fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>>

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * The submitted urls will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session
     *   .submitForOutPages("http://example.com", "-outLink a[href*=review]")
     *   .submitForOutPages("http://example.com", "-outLink a[href*=item] -expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param args      The load arguments
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitForOutPages(portalUrl: String, args: String): PulsarSession

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * The submitted urls will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session
     *   .submitForOutPages("http://example.com", session.options("-outLink a[href*=review]"))
     *   .submitForOutPages("http://example.com", session.options("-outLink a[href*=item] -expire 1d"))
     * PulsarContexts.await()
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitForOutPages(portalUrl: String, options: LoadOptions): PulsarSession

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * The submitted urls will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session
     *   .submitForOutPages(Hyperlink("http://example.com"), "-outLink a[href*=review]")
     *   .submitForOutPages(Hyperlink("http://example.com"), "-outLink a[href*=item] -expire 1d")
     * PulsarContexts.await()
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param args      The load arguments
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitForOutPages(portalUrl: UrlAware, args: String): PulsarSession

    /**
     * Load the portal page and submit the out links specified by the `-outLink` option to the URL pool.
     *
     * Option `-outLink` specifies the cssSelector for links in the portal page to load.
     *
     * The submitted urls will be subsequently processed in the crawl loop.
     *
     * Submit operations are non-blocking, meaning they return immediately without blocking the current thread or
     * suspending the current coroutine.
     *
     * ```kotlin
     * session
     *   .submitForOutPages(Hyperlink("http://example.com"), session.options("-outLink a[href*=review]"))
     *   .submitForOutPages(Hyperlink("http://example.com"), session.options("-outLink a[href*=item] -expire 1d"))
     * PulsarContexts.await()
     * ```
     *
     * @param portalUrl The portal url from where to load pages
     * @param options   The load options
     * @return The [PulsarSession] itself to enable chained operation
     */
    fun submitForOutPages(portalUrl: UrlAware, options: LoadOptions): PulsarSession

    /**
     * Load a url as a resource without browser rendering.
     *
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * ```kotlin
     * val page = session.loadResource("http://example.com/robots.txt", "http://example.com")
     * ```
     *
     * @param url  The url to load
     * @param referrer The referrer URL
     * @return The webpage containing the resource
     */
    fun loadResource(url: String, referrer: String): WebPage
    /**
     * Load a url as a resource without browser rendering.
     *
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * ```kotlin
     * val page = session.loadResource("http://example.com/robots.txt", "http://example.com", "-expire 1d")
     * ```
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
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * ```kotlin
     * val page = session.loadResource("http://example.com/robots.txt", "http://example.com", session.options("-expire 1d"))
     * ```
     *
     * @param url     The url to load
     * @param referrer The referrer URL
     * @param options The load options
     * @return The webpage containing the resource
     */
    fun loadResource(url: String, referrer: String, options: LoadOptions): WebPage

    /**
     * Load a url as a resource without browser rendering.
     *
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val page = session.loadResourceDeferred("http://example.com/robots.txt", "http://example.com")
     * ```
     *
     * @param url  The url to load
     * @param referrer The referrer URL
     * @return The webpage containing the resource
     */
    suspend fun loadResourceDeferred(url: String, referrer: String): WebPage
    /**
     * Load a url as a resource without browser rendering.
     *
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val page = session.loadResourceDeferred("http://example.com/robots.txt", "http://example.com", "-expire 1d")
     * ```
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
     * Referrer will be opened by a browser first to obtain browsing environment,
     * such as headers and cookies, and then the browsing environment will be applied to the later resource fetching.
     *
     * This function is a kotlin suspend function, which could be started, paused, and resume.
     * Suspend functions are only allowed to be called from a coroutine or another suspend function.
     *
     * ```kotlin
     * val page = session.loadResourceDeferred("http://example.com/robots.txt", "http://example.com", session.options("-expire 1d"))
     * ```
     *
     * @param url     The url to load
     * @param referrer The referrer URL
     * @param options The load options
     * @return The webpage containing the resource
     */
    suspend fun loadResourceDeferred(url: String, referrer: String, options: LoadOptions): WebPage
    /**
     * Parse a webpage into an HTML document.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val document = session.parse(page)
     * ```
     *
     * @param page The webpage to parse
     * @return The parsed HTML document
     */
    fun parse(page: WebPage): FeaturedDocument
    /**
     * Parse a webpage into an HTML document.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val document = session.parse(page, true)
     * ```
     *
     * @param page The webpage to parse
     * @param noCache Whether to skip the cache
     * @return The parsed HTML document
     */
    fun parse(page: WebPage, noCache: Boolean): FeaturedDocument
    /**
     * Parse a HTML string into an HTML document.
     *
     * @param html The HTML string to parse
     * @param baseURI The base URI to resolve relative URLs
     * */
    fun parse(html: String, baseURI: String = ""): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com")
     * ```
     *
     * @param url The url to load
     * @return The parsed HTML document
     * */
    fun loadDocument(url: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com", "-expire 1d")
     * ```
     *
     * @param url The url to load
     * @param args The load arguments
     * @return The parsed HTML document
     * */
    fun loadDocument(url: String, args: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com", session.options("-expire 1d"))
     * ```
     *
     * @param url The url to load
     * @param options The load options
     * @return The parsed HTML document
     * */
    fun loadDocument(url: String, options: LoadOptions): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument(Hyperlink("http://example.com"))
     * ```
     *
     * @param url The url to load
     * @return The parsed HTML document
     * */
    fun loadDocument(url: UrlAware): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument(Hyperlink("http://example.com"), "-expire 1d")
     * ```
     *
     * @param url The url to load
     * @param args The load arguments
     * @return The parsed HTML document
     * */
    fun loadDocument(url: UrlAware, args: String): FeaturedDocument
    /**
     * Load or fetch a webpage and parse it into an HTML document
     *
     * ```kotlin
     * val document = session.loadDocument(Hyperlink("http://example.com"), session.options("-expire 1d"))
     * ```
     *
     * @param url The url to load
     * @param options The load options
     * @return The parsed HTML document
     * */
    fun loadDocument(url: UrlAware, options: LoadOptions): FeaturedDocument
    /**
     * Load or fetch a webpage and then parse it into an HTML document.
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com")
     * ```
     *
     * @param url The url to load
     * @return The parsed HTML document
     * */
    fun loadDocument(url: NormURL): FeaturedDocument
    /**
     * Load or fetch a webpage located by the given url, and then extract fields specified by
     * field selectors.
     *
     * ```kotlin
     * val fields = session.scrape("http://example.com", "-expire 1d", listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", session.options("-expire 1d"), listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", "-expire 1d", ".title", ".content")
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", session.options("-expire 1d"), ".title", ".content")
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", "-expire 1d", ".container", listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", session.options("-expire 1d"), ".container",
     *      listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", "-expire 1d", ".container",
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrape("http://example.com", session.options("-expire 1d"), ".container",
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", "-outLink a", listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", session.options("-outLink a"), listOf(".title", ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", "-outLink a", mapOf("title" to ".title", "content" to ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", session.options("-outLink a"),
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", "-outLink a", mapOf("title" to ".title", "content" to ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", session.options("-outLink a"),
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", "-outLink a", ".container",
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
     *
     * @param portalUrl The portal url to start scraping
     * @param args Load arguments for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
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
     * ```kotlin
     * val fields = session.scrapeOutPages("http://example.com", session.options("-outLink a"), ".container",
     *      mapOf("title" to ".title", "content" to ".content"))
     * ```
     *
     * @param portalUrl The portal url to start scraping
     * @param options Load options for both the portal page and out pages
     * @param restrictSelector A CSS selector to locate a DOM where all fields are restricted to
     * @param fieldSelectors CSS selectors to extract fields from out pages
     * @return All extracted fields. For each out page, fields extracted with their names are saved in a map.
     * */
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>
    ): List<Map<String, String?>>

    /**
     * Harvest the content of a webpage using a web content extractor engine.
     *
     * Available engines are "boilerpipe".
     *
     * ```kotlin
     * val document = session.harvest("http://example.com", "-expire 1d")
     * println(document.textContent)
     * ```
     *
     * @param url The url to harvest
     * @param args The load arguments
     * @param engine The content extractor engine, default is "boilerpipe".
     * @return The harvested text document
     *
     * @see [boilerpipe ](https://github.com/kohlschutter/boilerpipe)
     * @see [boilerpipe-web](https://boilerpipe-web.appspot.com/)
     * */
    fun harvest(url: String, args: String = "", engine: String = "boilerpipe"): TextDocument
    /**
     * Harvest the content of a webpage using a web content extractor engine.
     *
     * Available engines are "boilerpipe".
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val document = session.harvest(page, "boilerpipe")
     * println(document.textContent)
     * ```
     *
     * @param page The webpage to harvest
     * @param engine The content extractor engine, default is "boilerpipe".
     * @return The harvested text document
     *
     * @see [boilerpipe ](https://github.com/kohlschutter/boilerpipe)
     * @see [boilerpipe-web](https://boilerpipe-web.appspot.com/)
     * */
    fun harvest(page: WebPage, engine: String = "boilerpipe"): TextDocument
    
    /**
     * Chat with the AI model.
     *
     * @param prompt The prompt to chat with
     * @return The response from the model
     */
    fun chat(prompt: String): ModelResponse

    /**
     * Chat with the AI model.
     *
     * @param userMessage Represents a message from a user, typically an end user of the application.
     * @param systemMessage Represents a system message, typically defined by a developer. This type of message usually
     *  provides instructions regarding the AI's actions, such as its behavior or response style.
     * @return The response from the model.
     */
    fun chat(userMessage: String, systemMessage: String): ModelResponse

    /**
     * Chat with the AI model about the specified webpage.
     *
     * @param prompt The prompt to chat with
     * @param page The page to chat with
     * @return The response from the model
     */
    fun chat(page: WebPage, prompt: String): ModelResponse

    /**
     * Chat with the AI model about the specified webpage.
     *
     * @param prompt The prompt to chat with
     * @param page The page to chat with
     * @return The response from the model
     */
    fun chat(prompt: String, page: WebPage): ModelResponse

    /**
     * Chat with the AI model about the specified document.
     *
     * @param document The document to chat with
     * @param prompt The prompt to chat with
     * @return The response from the model
     */
    fun chat(document: FeaturedDocument, prompt: String): ModelResponse

    /**
     * Chat with the AI model about the specified document.
     *
     * @param document The document to chat with
     * @param prompt The prompt to chat with
     * @return The response from the model
     */
    fun chat(prompt: String, document: FeaturedDocument): ModelResponse

    /**
     * Chat with the AI model about the specified element.
     *
     * @param element The element to chat with
     * @param prompt The prompt to chat with
     * @return The response from the model
     */
    fun chat(element: Element, prompt: String): ModelResponse

    /**
     * Chat with the AI model about the specified element.
     *
     * @param prompt The prompt to chat with
     * @param element The element to chat with
     * @return The response from the model
     */
    fun chat(prompt: String, element: Element): ModelResponse
    /**
     * Instructs the webdriver to perform a series of actions based on the given prompt.
     * This function converts the prompt into a sequence of webdriver actions, which are then executed.
     *
     * @param prompt The textual prompt that describes the actions to be performed by the webdriver.
     * @param driver The webdriver instance that will execute the actions.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    suspend fun instruct(prompt: String, driver: WebDriver): InstructionResult

    /**
     * Export the content of a webpage.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val path = session.export(page)
     * ```
     *
     * @param page Page to export
     * @return The path of the exported page
     * */
    fun export(page: WebPage): Path

    /**
     * Export the content of a webpage.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val path = session.export(page, "example")
     * ```
     *
     * @param page Page to export
     * @param ident File name identifier used to distinguish from other files
     * @return The path of the exported page
     * */
    fun export(page: WebPage, ident: String = ""): Path

    /**
     * Export the content of a webpage.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * val path = session.exportTo(page, Paths.get("/tmp/example.html"))
     * ```
     *
     * @param page Webpage to export
     * @param path Path to save the exported content
     * @return The path of the exported document
     * */
    fun exportTo(page: WebPage, path: Path): Path

    /**
     * Export the outer HTML of the document.
     *
     * @param doc Document to export
     * @return The path of the exported document
     * */
    fun export(doc: FeaturedDocument): Path

    /**
     * Export the outer HTML of the document.
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com")
     * val path = session.export(document, "example")
     * ```
     *
     * @param doc Document to export
     * @param ident File name identifier used to distinguish from other files
     * @return The path of the exported document
     * */
    fun export(doc: FeaturedDocument, ident: String = ""): Path

    /**
     * Export the outer HTML of the document.
     *
     * ```kotlin
     * val document = session.loadDocument("http://example.com")
     * val path = session.exportTo(document, Paths.get("/tmp/example.html"))
     * ```
     *
     * @param doc Document to export
     * @param path Path to save the exported content
     * @return The path of the exported document
     * */
    fun exportTo(doc: FeaturedDocument, path: Path): Path

    /**
     * Persist the webpage.
     *
     * ```kotlin
     * val page = session.load("http://example.com")
     * session.persist(page)
     * ```
     *
     * @param page Page to persist
     * @return Whether the page is persisted successfully
     * */
    fun persist(page: WebPage): Boolean
    /**
     * Delete a webpage from the storage
     *
     * ```kotlin
     * session.delete("http://example.com")
     * ```
     *
     * @param url The url to delete
     * */
    fun delete(url: String)
    /**
     * Flush to the storage
     * */
    fun flush()
}
