package ai.platon.pulsar

import ai.platon.pulsar.common.BeanFactory
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.DocumentCatch
import ai.platon.pulsar.crawl.common.PageCatch
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.nio.file.Path

interface PulsarSession : AutoCloseable {
    /**
     * The pulsar context
     * */
    val context: AbstractPulsarContext

    /**
     * The session scope volatile config, every setting is supposed to be changed at any time and any place
     * */
    val sessionConfig: VolatileConfig

    /**
     * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
     * */
    val id: Int
    val unmodifiedConfig: ImmutableConfig

    /**
     * The scoped bean factory: for each volatileConfig object, there is a bean factory
     * TODO: session scoped?
     * */
    val sessionBeanFactory: BeanFactory
    val display: String
    val pageCache: PageCatch
    val documentCache: DocumentCatch

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable): Boolean
    fun disableCache()

    /**
     * Create a new options, with a new volatile config
     * */
    fun options(args: String = ""): LoadOptions
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
     * Inject a url
     *
     * @param url The url followed by options
     * @return The web page created
     */
    fun inject(url: String): WebPage
    fun get(url: String): WebPage
    fun getOrNull(url: String): WebPage?
    fun exists(url: String): Boolean

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param args The load args
     * @return The web page
     */
    @Throws(Exception::class)
    fun load(url: String, args: String): WebPage

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    @Throws(Exception::class)
    fun load(url: String, options: LoadOptions = options()): WebPage

    @Throws(Exception::class)
    fun load(url: UrlAware, args: String): WebPage

    @Throws(Exception::class)
    fun load(url: UrlAware, options: LoadOptions = options()): WebPage

    @Throws(Exception::class)
    fun load(normUrl: NormUrl): WebPage

    @Throws(Exception::class)
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage

    @Throws(Exception::class)
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage

    @Throws(Exception::class)
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage

    @Throws(Exception::class)
    suspend fun loadDeferred(normUrl: NormUrl): WebPage

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    fun loadAll(
        urls: Iterable<String>,
        options: LoadOptions = options(),
        areItems: Boolean = false
    ): Collection<WebPage>

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    fun parallelLoadAll(
        urls: Iterable<String>,
        options: LoadOptions = options(),
        areItems: Boolean = false
    ): Collection<WebPage>

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param args         The load args
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage>

    /**
     * Load all out pages in a portal page
     *
     * @param portalUrl    The portal url from where to load pages
     * @param options The load options
     * @return The web pages
     */
    fun loadOutPages(portalUrl: String, options: LoadOptions = options()): Collection<WebPage>

    /**
     * Parse the Web page into DOM.
     * If the Web page is not changed since last parse, use the last result if available
     */
    fun parse(page: WebPage, noCache: Boolean = false): FeaturedDocument
    fun loadDocument(url: String, args: String): FeaturedDocument
    fun loadDocument(url: String, options: LoadOptions = options()): FeaturedDocument
    fun loadDocument(normUrl: NormUrl): FeaturedDocument
    fun scrape(url: String, args: String, fieldCss: Iterable<String>): Map<String, String?>
    fun scrape(url: String, args: String, fieldCss: Map<String, String>): Map<String, String?>
    fun scrape(url: String, args: String, restrictCss: String, fieldCss: Iterable<String>): List<Map<String, String?>>
    fun scrape(
        url: String,
        args: String,
        restrictCss: String,
        fieldCss: Map<String, String>
    ): List<Map<String, String?>>

    fun scrapeOutPages(portalUrl: String, args: String, fieldsCss: Iterable<String>): List<Map<String, String?>>
    fun scrapeOutPages(
        portalUrl: String,
        args: String, restrictCss: String, fieldsCss: Iterable<String>
    ): List<Map<String, String?>>

    fun scrapeOutPages(portalUrl: String, args: String, fieldsCss: Map<String, String>): List<Map<String, String?>>
    fun scrapeOutPages(
        portalUrl: String,
        args: String, restrictCss: String, fieldsCss: Map<String, String>
    ): List<Map<String, String?>>

    fun cache(page: WebPage): WebPage
    fun disableCache(page: WebPage): WebPage?
    fun cache(doc: FeaturedDocument): FeaturedDocument
    fun disableCache(doc: FeaturedDocument): FeaturedDocument?
    fun disableCache(url: String): WebPage?
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
