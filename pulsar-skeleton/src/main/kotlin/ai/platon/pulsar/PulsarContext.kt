package ai.platon.pulsar

import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_INCOGNITO
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.crawl.GlobalCacheManager
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.fetch.LazyFetchTaskManager
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 *
 * TODO: multiple pulsar context in single process
 */
class PulsarContext private constructor(): AutoCloseable {

    companion object {
        val log = LoggerFactory.getLogger(PulsarContext::class.java)
        /**
         * The spring application context
         * */
        lateinit var applicationContext: ConfigurableApplicationContext
        /**
         * TODO: Review if we need an "active context"
         * */
        val activeContext = AtomicReference<PulsarContext>()
        /**
         * Whether this pulsar object is already closed
         * */
        private val closed = AtomicBoolean()
        private val initialized = AtomicBoolean()

        /** Synchronization monitor for the "refresh" and "destroy".  */
        private val startupShutdownMonitor = Any()
        private var shutdownHook: Thread? = null

        fun initialize() {
            if (initialized.get()) {
                return
            }

            val configLocation = System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            initialize(configLocation)
        }

        fun initialize(configLocation: String) {
            if (initialized.get()) {
                return
            }

            Systems.setProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, configLocation)
            val context = ClassPathXmlApplicationContext(AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            initialize(context)
        }

        fun initialize(context: ConfigurableApplicationContext) {
            if (initialized.compareAndSet(false, true)) {
                applicationContext = context
                initEnvironment()
            }
        }

        fun shutdown() {
            activeContext.getAndSet(null)?.close()

            // shutdown application context before progress exit
            shutdownHook?.also {
                try {
                    Runtime.getRuntime().removeShutdownHook(it)
                } catch (e: IllegalStateException) { // ignore
                } catch (e: SecurityException) { // applets may not do that - ignore
                }
                shutdownHook = null
            }
        }

        /**
         * Register a shutdown hook with the JVM runtime, closing this context
         * on JVM shutdown unless it has already been closed at that time.
         *
         * Delegates to `doClose()` for the actual closing procedure.
         * @see Runtime.addShutdownHook
         */
        fun registerShutdownHook() {
            ensureAlive()
            if (shutdownHook == null) { // No shutdown hook registered yet.
                shutdownHook = Thread { synchronized(startupShutdownMonitor) {
                    activeContext.getAndSet(null)?.close() }
                }
                Runtime.getRuntime().addShutdownHook(shutdownHook)
            }
        }

        fun getOrCreate(): PulsarContext {
            ensureAlive()
            synchronized(PulsarContext::class.java) {
                if (activeContext.get() == null) {
                    activeContext.set(PulsarContext())
                }
                return activeContext.get()
            }
        }

        fun createSession(): PulsarSession {
            ensureAlive()
            return getOrCreate().createSession()
        }

        private fun initEnvironment() {
            ensureAlive()
            PulsarProperties.setAllProperties(false)
            registerShutdownHook()
            applicationContext.registerShutdownHook()
        }

        private fun ensureAlive() {
            if (closed.get()) {
                throw IllegalApplicationContextStateException("Pulsar context is closed")
            }
        }
    }

    /**
     * A immutable config is loaded from the config file at process startup, and never changes
     * */
    val unmodifiedConfig: ImmutableConfig
    /**
     * The privacy manager
     * */
    val privacyManager: PrivacyManager
    /**
     * Url normalizers
     * */
    val urlNormalizers: UrlNormalizers
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
    val parseComponent: ParseComponent
    /**
     * The fetch component
     * */
    val fetchComponent: FetchComponent

    val lazyFetchTaskManager: LazyFetchTaskManager

    val globalCacheManager: GlobalCacheManager

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()
    /**
     * Registered closeables, will be closed by Pulsar object
     * */
    private val closableObjects = ConcurrentLinkedQueue<AutoCloseable>()

    /**
     * Whether this pulsar object is already closed
     * */
    val isActive get() = !closed.get()

    /**
     * All open sessions
     * */
    val sessions = ConcurrentSkipListMap<Int, PulsarSession>()

    init {
        unmodifiedConfig = getBean()
        privacyManager = getBean()
        globalCacheManager = getBean()

        webDb = getBean()
        injectComponent = getBean()
        loadComponent = getBean()
        fetchComponent = getBean()
        parseComponent = getBean()
        urlNormalizers = getBean()
        lazyFetchTaskManager = getBean()

        log.info("PulsarContext is created")
    }

    fun createSession(): PulsarSession {
        ensureAlive()
        val session = PulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }

    fun closeSession(session: PulsarSession) {
        ensureAlive()
        sessions.remove(session.id)
    }

    @Throws(BeansException::class)
    fun <T> getBean(requiredType: Class<T>): T {
        return applicationContext.getBean(requiredType)
    }

    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T = getBean(requiredType.java)

    @Throws(BeansException::class)
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    /**
     * Close objects when sessions closes
     * */
    fun registerClosable(closable: AutoCloseable) {
        ensureAlive()
        closableObjects.add(closable)
    }

    private fun initOptions(options: LoadOptions, isItemOption: Boolean = false): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = VolatileConfig(unmodifiedConfig)
        }

        options.volatileConfig?.setBoolean(BROWSER_INCOGNITO, options.incognito)

        return if (isItemOption) options.createItemOption() else options
    }

    fun clearCaches() {
        globalCacheManager.pageCache.clear()
        globalCacheManager.documentCache.clear()
    }

    fun normalize(url: String, isItemOption: Boolean = false): NormUrl {
        return normalize(url, LoadOptions.create(), isItemOption)
    }

    fun normalize(url: String, options: LoadOptions, isItemOption: Boolean = false): NormUrl {
        val parts = Urls.splitUrlArgs(url)
        var normalizedUrl = Urls.normalize(parts.first, options.shortenKey)
        if (!options.noNorm) {
            normalizedUrl = urlNormalizers.normalize(normalizedUrl)?:return NormUrl.nil
        }

        if (parts.second.isBlank()) {
            return NormUrl(normalizedUrl, initOptions(options, isItemOption))
        }

        val parsedOptions = LoadOptions.parse(parts.second)
        val options2 = LoadOptions.mergeModified(parsedOptions, options)
        return NormUrl(normalizedUrl, initOptions(options2, isItemOption))
    }

    fun normalize(urls: Iterable<String>, isItemOption: Boolean = false): List<NormUrl> {
        return urls.takeIf { isActive }?.mapNotNull { normalize(it, isItemOption).takeIf { it.isNotNil } }
                ?:listOf()
    }

    fun normalize(urls: Iterable<String>, options: LoadOptions, isItemOption: Boolean = false): List<NormUrl> {
        return urls.takeIf { isActive }?.mapNotNull { normalize(it, options, isItemOption).takeIf { it.isNotNil } }
                ?: listOf()
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage {
        return WebPage.NIL.takeUnless { isActive }?:injectComponent.inject(Urls.splitUrlArgs(url))
    }

    fun get(url: String): WebPage? {
        return webDb.takeIf { isActive }?.get(normalize(url).url, false)
    }

    fun getOrNil(url: String): WebPage {
        return webDb.takeIf { isActive }?.getOrNil(normalize(url).url, false)?: WebPage.NIL
    }

    fun scan(urlPrefix: String): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix) ?: listOf<WebPage>().iterator()
    }

    fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String): WebPage {
        val normUrl = normalize(url)
        return loadComponent.takeIf { isActive }?.load(normUrl)?: WebPage.NIL
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String, options: LoadOptions): WebPage {
        val normUrl = normalize(url, options)
        return loadComponent.takeIf { isActive }?.load(normUrl)?: WebPage.NIL
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL): WebPage {
        return loadComponent.takeIf { isActive }?.load(url, LoadOptions.create())?: WebPage.NIL
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL, options: LoadOptions): WebPage {
        return loadComponent.takeIf { isActive }?.load(url, initOptions(options))?: WebPage.NIL
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: NormUrl): WebPage {
        initOptions(url.options)
        return loadComponent.takeIf { isActive }?.load(url)?: WebPage.NIL
    }

    suspend fun loadDeferred(url: NormUrl): WebPage {
        initOptions(url.options)
        return loadComponent.takeIf { isActive }?.loadDeferred(url)?: WebPage.NIL
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
        return loadComponent.takeIf { isActive }?.loadAll(normalize(urls, options), options)?: listOf()
    }

    @JvmOverloads
    fun loadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        return loadComponent.takeIf { isActive }?.loadAll(urls, initOptions(options))?: listOf()
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
        return loadComponent.takeIf { isActive }?.parallelLoadAll(normalize(urls, options), options)?: listOf()
    }

    @JvmOverloads
    fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions = LoadOptions.create()): Collection<WebPage> {
        return loadComponent.takeIf { isActive }?.loadAll(urls, initOptions(options))?: listOf()
    }

    /**
     * Parse the WebPage using Jsoup
     */
    fun parse(page: WebPage): FeaturedDocument {
        return FeaturedDocument.NIL.takeUnless { isActive }?:JsoupParser(page, unmodifiedConfig).parse()
    }

    fun parse(page: WebPage, mutableConfig: MutableConfig): FeaturedDocument {
        return FeaturedDocument.NIL.takeUnless { isActive }?:JsoupParser(page, mutableConfig).parse()
    }

    fun persist(page: WebPage) {
        webDb.takeIf { isActive }?.put(page, false)
    }

    fun delete(url: String) {
        webDb.takeIf { isActive }?.apply { delete(url); delete(normalize(url).url) }
    }

    fun delete(page: WebPage) {
        webDb.takeIf { isActive }?.delete(page.url)
    }

    fun flush() {
        webDb.takeIf { isActive }?.flush()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                sessions.values.forEach {
                    log.trace("Closing session $it")
                    it.runCatching { it.close() }.onFailure { log.warn(it.message) }
                }
                closableObjects.forEach {
                    log.debug("Closing closeable $it")
                    it.runCatching { it.close() }.onFailure { log.warn(it.message) }
                }
            } catch (t: Throwable) {
                log.warn("Unexpected exception", t)
            }

            log.debug("PulsarContext is closed")

            runCatching { applicationContext.close() }.onFailure { log.warn(it.message) }
        }
    }
}

fun withPulsarContext(block: () -> Unit) {
    PulsarContext.initialize()
    runCatching { block() }.onFailure { System.err.println(it) }
    PulsarContext.shutdown()
}

fun withPulsarContext(contextLocation: String, block: () -> Unit) {
    PulsarContext.initialize(contextLocation)
    withPulsarContext(block)
}
