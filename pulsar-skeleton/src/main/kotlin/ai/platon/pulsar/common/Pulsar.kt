package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class Pulsar: AutoCloseable {
    /**
     * A immutable config is loaded from the file at program startup, and never changes
     * */
    val immutableConfig: ImmutableConfig
    /**
     * A mutable config can be changed programmatically, usually be changed at the initialization phrase
     * */
    private val defaultMutableConfig: MutableConfig
    /**
     * A volatile config is usually session scoped, and expected to be changed anywhere and anytime
     * */
    private val defaultVolatileConfig: VolatileConfig
    /**
     * Registered closeables, will be closed by Pulsar object
     * */
    private val closeables = mutableListOf<AutoCloseable>()
    /**
     * Whether this pulsar object is already closed
     * */
    private val isClosed = AtomicBoolean(false)
    /**
     * Url normalizers
     * */
    private val urlNormalizers: UrlNormalizers
    /**
     * The selenium engine
     * */
    private val seleniumEngine: SeleniumEngine
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
    val parseComponent: ParseComponent get() = loadComponent.parseComponent
    /**
     * The fetch component
     * */
    val fetchComponent: BatchFetchComponent get() = loadComponent.fetchComponent

    constructor(appConfigLocation: String) : this(ClassPathXmlApplicationContext(appConfigLocation))

    @JvmOverloads
    constructor(applicationContext: ConfigurableApplicationContext = ClassPathXmlApplicationContext(
            System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION))) {
        this.immutableConfig = applicationContext.getBean<MutableConfig>(MutableConfig::class.java)

        this.webDb = applicationContext.getBean<WebDb>(WebDb::class.java)
        this.injectComponent = applicationContext.getBean<InjectComponent>(InjectComponent::class.java)
        this.loadComponent = applicationContext.getBean<LoadComponent>(LoadComponent::class.java)
        this.urlNormalizers = applicationContext.getBean<UrlNormalizers>(UrlNormalizers::class.java)
        this.seleniumEngine = SeleniumEngine.getInstance(immutableConfig)
        this.defaultMutableConfig = MutableConfig(immutableConfig.unbox())
        this.defaultVolatileConfig = VolatileConfig(defaultMutableConfig)

        closeables.add(this.seleniumEngine)
    }

    constructor(
            injectComponent: InjectComponent,
            loadComponent: LoadComponent,
            urlNormalizers: UrlNormalizers,
            immutableConfig: ImmutableConfig) {
        this.webDb = injectComponent.webDb

        this.injectComponent = injectComponent
        this.loadComponent = loadComponent
        this.urlNormalizers = urlNormalizers
        this.immutableConfig = immutableConfig

        this.seleniumEngine = SeleniumEngine.getInstance(immutableConfig)
        this.defaultMutableConfig = MutableConfig(immutableConfig.unbox())
        this.defaultVolatileConfig = VolatileConfig(defaultMutableConfig)
    }

    fun normalize(url: String): NormUrl {
        val parts = Urls.splitUrlArgs(url)
        val options = LoadOptions.parse(parts.second, defaultVolatileConfig)
        val normalizedUrl = urlNormalizers.normalize(Urls.normalize(parts.first, options.shortenKey))
                ?: return NormUrl.nil
        return NormUrl(normalizedUrl, initOptions(options))
    }

    fun normalize(url: String, options: LoadOptions): NormUrl {
        val parts = Urls.splitUrlArgs(url)
        val normalizedUrl = urlNormalizers.normalize(Urls.normalize(parts.first, options.shortenKey))
                ?: return NormUrl.nil

        if (parts.second.isBlank()) {
            return NormUrl(normalizedUrl, options)
        }

        val options2 = LoadOptions.mergeModified(options, LoadOptions.parse(parts.second), options.volatileConfig)
        return NormUrl(normalizedUrl, initOptions(options2))
    }

    fun normalize(urls: Iterable<String>): List<NormUrl> {
        return urls.mapNotNull { normalize(it).takeIf { it.isNotNil } }
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions): List<NormUrl> {
        return urls.mapNotNull { normalize(it, options).takeIf { it.isNotNil } }
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage {
        return injectComponent.inject(Urls.splitUrlArgs(url))
    }

    fun get(url: String): WebPage? {
        return webDb.get(normalize(url).url, false)
    }

    fun getOrNil(url: String): WebPage {
        return webDb.getOrNil(normalize(url).url, false)
    }

    fun scan(urlPrefix: String): Iterator<WebPage> {
        return webDb.scan(urlPrefix)
    }

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        return webDb.scan(urlPrefix, fields)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String): WebPage {
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
        return loadComponent.load(url, initOptions(options))
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: NormUrl): WebPage {
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
        initOptions(options)
        return loadComponent.loadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun loadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
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
        initOptions(options)
        return loadComponent.parallelLoadAll(normalize(urls, options), options)
    }

    @JvmOverloads
    fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        return loadComponent.loadAll(urls, initOptions(options))
    }

    /**
     * Parse the WebPage using Jsoup
     */
    fun parse(page: WebPage): FeaturedDocument {
        val parser = JsoupParser(page, immutableConfig)
        return FeaturedDocument(parser.parse())
    }

    fun parse(page: WebPage, mutableConfig: MutableConfig): FeaturedDocument {
        val parser = JsoupParser(page, mutableConfig)
        return FeaturedDocument(parser.parse())
    }

    fun persist(page: WebPage) {
        webDb.put(page, false)
    }

    fun delete(url: String) {
        webDb.delete(url)
        webDb.delete(normalize(url).url)
    }

    fun delete(page: WebPage) {
        webDb.delete(page.url)
    }

    fun flush() {
        webDb.flush()
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        closeables.forEach { it.use { it.close() } }
    }

    private fun initOptions(options: LoadOptions): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = defaultVolatileConfig
        }
        return options
    }
}
