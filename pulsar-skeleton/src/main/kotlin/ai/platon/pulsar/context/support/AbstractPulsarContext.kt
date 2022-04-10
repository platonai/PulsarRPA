package ai.platon.pulsar.context.support

import ai.platon.pulsar.AbstractPulsarSession
import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.collect.UrlPool
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.CommonUrlNormalizer
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.simplify
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.support.AbstractApplicationContext
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * The main entry point for pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
abstract class AbstractPulsarContext(
        override val applicationContext: AbstractApplicationContext,
        override val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment()
): PulsarContext, AutoCloseable {

    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(AbstractPulsarContext::class.java)

    override val id = instanceSequencer.incrementAndGet()

    /**
     * An immutable config is loaded from the config file at process startup, and never changes
     * */
    override val unmodifiedConfig: ImmutableConfig get() = getBean()

    /**
     * Url normalizers
     * */
    open val urlNormalizers: CrawlUrlNormalizers get() = getBean()

    /**
     * The web db
     * */
    open val webDb: WebDb get() = getBean()

    /**
     * The global cache manager
     * */
    open val globalCacheFactory: GlobalCacheFactory get() = getBean()

    /**
     * The inject component
     * */
    open val injectComponent: InjectComponent get() = getBean()

    /**
     * The fetch component
     * */
    open val fetchComponent: BatchFetchComponent get() = getBean()

    /**
     * The load component
     * */
    open val parseComponent: ParseComponent get() = getBean()

    /**
     * The update component
     * */
    open val updateComponent: UpdateComponent get() = getBean()

    /**
     * The load component
     * */
    open val loadComponent: LoadComponent get() = getBean()

    /**
     * The url pool to fetch
     * */
    override val crawlPool: UrlPool get() = globalCacheFactory.globalCache.urlPool

    /**
     * The main loop
     * */
    override val crawlLoops: CrawlLoops get() = getBean()

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()

    // TODO: we can only check active before critical calls, for example, IO operations
    val isActive get() = !closed.get() && AppContext.isActive && applicationContext.isActive

    /**
     * All open sessions
     * */
    val sessions = ConcurrentSkipListMap<Int, PulsarSession>()

    /**
     * Registered closable objects, will be closed by Pulsar object
     * */
    private val closableObjects = ConcurrentLinkedQueue<AutoCloseable>()

    private val closed = AtomicBoolean()

    /** Synchronization monitor for the "refresh" and "destroy" */
    private val startupShutdownMonitor = Any()

    /** Reference to the JVM shutdown hook, if registered */
    private var shutdownHook: Thread? = null

    private val webDbOrNull: WebDb? get() = if (isActive) webDb else null

    private val abnormalPage get() = WebPage.NIL.takeIf { !isActive }

    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T = applicationContext.getBean(requiredType.java)

    @Throws(BeansException::class)
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    @Throws(BeansException::class)
    fun <T : Any> getBeanOrNull(requiredType: KClass<T>): T? =
        kotlin.runCatching { applicationContext.getBean(requiredType.java) }.getOrNull()

    @Throws(BeansException::class)
    inline fun <reified T : Any> getBeanOrNull(): T? = getBeanOrNull(T::class)

    abstract override fun createSession(): AbstractPulsarSession

    override fun closeSession(session: PulsarSession) {
        session.close()
        logger.info("Removing PulsarSession #{}", session.id)
        sessions.remove(session.id)
    }

    /**
     * Close objects when sessions closes
     * */
    override fun registerClosable(closable: AutoCloseable) {
        closableObjects.add(closable)
    }

    fun clearCaches() {
        if (!isActive) return

        globalCacheFactory.globalCache.pageCache.clear()
        globalCacheFactory.globalCache.documentCache.clear()
    }

    /**
     * Normalize an url, the url can be one of the following:
     * 1. a configured url
     * 2. a base64 encoded url
     * 3. a base64 encoded configured url
     *
     * A url can be configured by appending arguments to the url, and it also can be used with a LoadOptions,
     * If both tailing arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    override fun normalize(url: String, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val url0 = url.takeIf { it.contains("://") } ?: String(Base64.getUrlDecoder().decode(url))
        return normalize(PlainUrl(url0), options, toItemOption)
    }

    override fun normalizeOrNull(url: String?, options: LoadOptions, toItemOption: Boolean): NormUrl? {
        if (url == null) return null
        return kotlin.runCatching { normalize(url, options, toItemOption) }.getOrNull()
    }

    /**
     * Normalize urls, remove invalid urls
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls, all invalid input urls are removed
     * */
    override fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean): List<NormUrl> {
        return urls.mapNotNull { normalizeOrNull(it, options, toItemOption) }
    }

    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    override fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean): NormUrl {
        return CommonUrlNormalizer(urlNormalizers).normalize(url, options, toItemOption)
    }

    override fun normalizeOrNull(url: UrlAware?, options: LoadOptions, toItemOption: Boolean): NormUrl? {
        if (url == null) return null
        return kotlin.runCatching { normalize(url, options, toItemOption) }.getOrNull()
    }

    /**
     * Normalize urls, remove invalid urls
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @param toItemOption If the LoadOptions is converted to item load options
     * @return All normalized urls, all invalid input urls are removed
     * */
    override fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean): List<NormUrl> {
        return urls.mapNotNull { normalizeOrNull(it, options, toItemOption) }
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    override fun inject(url: String): WebPage {
        return abnormalPage ?: injectComponent.inject(UrlUtils.splitUrlArgs(url))
    }

    override fun inject(url: NormUrl): WebPage {
        return abnormalPage ?: injectComponent.inject(url.spec, url.args)
    }

    override fun get(url: String): WebPage {
        return webDbOrNull?.get(url, false)?: WebPage.NIL
    }

    override fun getOrNull(url: String): WebPage? {
        return webDbOrNull?.getOrNull(url, false)
    }

    override fun exists(url: String) = webDbOrNull?.exists(url) == true

    override fun fetchState(page: WebPage, options: LoadOptions) = loadComponent.fetchState(page, options)

    override fun scan(urlPrefix: String): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: String, options: LoadOptions): WebPage {
        val normUrl = normalize(url, options)
        return abnormalPage ?: loadComponent.load(normUrl)
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: URL, options: LoadOptions): WebPage {
        return abnormalPage ?: loadComponent.load(url, options)
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: NormUrl): WebPage {
        return abnormalPage ?: loadComponent.load(url)
    }

    override suspend fun loadDeferred(url: NormUrl): WebPage {
        return abnormalPage ?: loadComponent.loadDeferred(url)
    }

    /**
     * Load a batch of urls with the specified options.
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    override fun loadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.loadAll(normalize(urls, options), options) else listOf()
    }

    override fun loadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.loadAll(urls, options) else listOf()
    }

    override fun asyncLoad(url: UrlAware): AbstractPulsarContext {
        crawlPool.add(url)
        return this
    }

    override fun asyncLoadAll(urls: Collection<UrlAware>): AbstractPulsarContext {
        crawlPool.addAll(urls)
        return this
    }

    /**
     * Parse the WebPage using parseComponent
     */
    override fun parse(page: WebPage): FeaturedDocument? {
        val parser = loadComponent.parseComponent
        return parser.parse(page, noLinkFilter = true).document
    }

    override fun persist(page: WebPage) {
        webDbOrNull?.put(page, false)
    }

    override fun delete(url: String) {
        webDbOrNull?.delete(url)
    }

    override fun delete(page: WebPage) {
        webDbOrNull?.delete(page.url)
    }

    override fun flush() {
        webDbOrNull?.flush()
    }

    override fun await() {
        if (isActive) {
            crawlLoops.await()
        }
    }

    /**
     * Register a shutdown hook with the JVM runtime, closing this context
     * on JVM shutdown unless it has already been closed at that time.
     *
     * Delegates to `doClose()` for the actual closing procedure.
     * @see Runtime.addShutdownHook
     *
     * @see .close
     * @see .doClose
     */
    override fun registerShutdownHook() {
        if (this.shutdownHook == null) { // No shutdown hook registered yet.
            this.shutdownHook = Thread { synchronized(startupShutdownMonitor) { doClose() } }
            Runtime.getRuntime().addShutdownHook(this.shutdownHook)
            (applicationContext as? AbstractApplicationContext)?.registerShutdownHook()
        }
    }

    /**
     * Close this pulsar context, destroying all beans in its bean factory.
     *
     * Delegates to `doClose()` for the actual closing procedure.
     * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
     * @see .doClose
     * @see .registerShutdownHook
     */
    override fun close() {
        synchronized(startupShutdownMonitor) {
            doClose()
            // If we registered a JVM shutdown hook, we don't need it anymore now:
            // We've already explicitly closed the context.
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook)
                } catch (ex: IllegalStateException) {
                    // ignore - VM is already shutting down
                }
            }
        }
    }

    private fun doClose() {
        AppContext.beginTerminate()

        if (closed.compareAndSet(false, true)) {
            logger.info("Closing context #{}/{} | {}", id, sessions.size, this::class.java.simpleName)

            // TODO: properly cancel the fetching tasks
            globalCacheFactory.globalCache.clearCaches()

            sessions.values.forEach {
                kotlin.runCatching { it.close() }
                    .onFailure { logger.warn(it.simplify("Unexpected exception")) }
            }
            sessions.clear()

            closableObjects.forEach {
                kotlin.runCatching { it.close() }
                    .onFailure { logger.warn(it.simplify("Unexpected exception")) }
            }
            closableObjects.clear()

            if (applicationContext.isActive) {
                kotlin.runCatching { crawlLoops.stop() }
                    .onFailure { logger.warn(it.simplify("Unexpected exception")) }

                applicationContext.close()
            }
        }

        AppContext.endTerminate()
    }
}
