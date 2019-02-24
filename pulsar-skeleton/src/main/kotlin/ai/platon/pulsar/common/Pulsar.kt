package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.options.LoadOptions
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
import java.util.concurrent.atomic.AtomicBoolean

class Pulsar: AutoCloseable {

    val immutableConfig: ImmutableConfig
    val webDb: WebDb
    val injectComponent: InjectComponent
    val loadComponent: LoadComponent
    private val urlNormalizers: UrlNormalizers
    private val defaultMutableConfig: MutableConfig
    private val isClosed = AtomicBoolean(false)

    val parseComponent: ParseComponent get() = loadComponent.parseComponent

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

        this.defaultMutableConfig = MutableConfig(immutableConfig.unbox())
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

        this.defaultMutableConfig = MutableConfig(immutableConfig.unbox())
    }

    fun normalize(url: String): String? {
        return urlNormalizers.normalize(url)
    }

    /**
     * Inject a url
     *
     * @param configuredUrl The url followed by config options
     * @return The web page created
     */
    fun inject(configuredUrl: String): WebPage {
        return injectComponent.inject(UrlUtil.splitUrlArgs(configuredUrl))
    }

    operator fun get(url: String): WebPage? {
        return webDb.get(url)
    }

    fun getOrNil(url: String): WebPage {
        return webDb.getOrNil(url)
    }

    fun scan(urlBase: String): Iterator<WebPage> {
        return webDb.scan(urlBase)
    }

    fun scan(urlBase: String, fields: Array<String>): Iterator<WebPage> {
        return webDb.scan(urlBase, fields)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(configuredUrl: String): WebPage {
        val urlAndOptions = UrlUtil.splitUrlArgs(configuredUrl)

        val options = LoadOptions.parse(urlAndOptions.value, defaultMutableConfig)
        options.mutableConfig = defaultMutableConfig

        return loadComponent.load(urlAndOptions.key, options)
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String, options: LoadOptions): WebPage {
        if (options.mutableConfig == null) {
            options.mutableConfig = defaultMutableConfig
        }
        return loadComponent.load(url, options)
    }

    /**
     * Load a batch of urls with the specified options.
     *
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    @JvmOverloads
    fun loadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions()): Collection<WebPage> {
        if (options.mutableConfig == null) {
            options.mutableConfig = defaultMutableConfig
        }
        return loadComponent.loadAll(urls, options)
    }

    /**
     * Load a batch of urls with the specified options.
     *
     *
     * Urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    @JvmOverloads
    fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions = LoadOptions()): Collection<WebPage> {
        if (options.mutableConfig == null) {
            options.mutableConfig = defaultMutableConfig
        }
        return loadComponent.parallelLoadAll(urls, options)
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
        webDb.put(page.url, page)
    }

    fun delete(url: String) {
        webDb.delete(url)
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

        SeleniumEngine.getInstance(immutableConfig).close()
        injectComponent.close()
        webDb.close()
    }
}
