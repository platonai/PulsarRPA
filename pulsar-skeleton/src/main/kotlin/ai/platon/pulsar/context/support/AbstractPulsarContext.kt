package ai.platon.pulsar.context.support

import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.LoadOptionsNormalizer
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.url.PlainUrl
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
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
import kotlin.reflect.KClass

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
abstract class AbstractPulsarContext(
        override val applicationContext: AbstractApplicationContext,
        override val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment()
): PulsarContext, AutoCloseable {
    private val log = LoggerFactory.getLogger(AbstractPulsarContext::class.java)

    /**
     * A immutable config is loaded from the config file at process startup, and never changes
     * */
    override val unmodifiedConfig: ImmutableConfig get() = getBean()

    /**
     * Url normalizers
     * */
    open val urlNormalizers: UrlNormalizers get() = getBean()

    /**
     * The web db
     * */
    open val webDb: WebDb get() = getBean()

    /**
     * The global cache manager
     * */
    open val globalCache: GlobalCache get() = getBean()

    /**
     * The inject component
     * */
    open val injectComponent: InjectComponent get() = getBean()

    /**
     * The fetch component
     * */
    open val fetchComponent: BatchFetchComponent get() = getBean()

    /**
     * The update component
     * */
    open val updateComponent: UpdateComponent get() = getBean()

    /**
     * The load component
     * */
    open val loadComponent: LoadComponent get() = getBean()

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()

    val isActive get() = !closed.get() && AppContext.isActive && applicationContext.isActive

    /**
     * All open sessions
     * */
    val sessions = ConcurrentSkipListMap<Int, PulsarSession>()

    /**
     * Registered closeables, will be closed by Pulsar object
     * */
    private val closableObjects = ConcurrentLinkedQueue<AutoCloseable>()

    private val closed = AtomicBoolean()

    /** Synchronization monitor for the "refresh" and "destroy".  */
    private val startupShutdownMonitor = Any()

    /** Reference to the JVM shutdown hook, if registered.  */
    private var shutdownHook: Thread? = null

    private val webDbOrNull: WebDb? get() = if (isActive) webDb else null

    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T {
        return applicationContext.getBean(requiredType.java)
    }

    @Throws(BeansException::class)
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    override fun createSession(): PulsarSession {
        val session = PulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }

    override fun closeSession(session: PulsarSession) {
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

        globalCache.pageCache.clear()
        globalCache.documentCache.clear()
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
        return LoadOptionsNormalizer(unmodifiedConfig, urlNormalizers).normalize(url, options, toItemOption)
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
        return WebPage.NIL.takeIf { !isActive } ?: injectComponent.inject(Urls.splitUrlArgs(url))
    }

    override fun inject(url: NormUrl): WebPage {
        return WebPage.NIL.takeIf { !isActive } ?: injectComponent.inject(url.spec, url.args)
    }

    override fun get(url: String): WebPage {
        return webDbOrNull?.get(normalize(url).spec, false)?: WebPage.NIL
    }

    override fun getOrNull(url: String): WebPage? {
        return webDbOrNull?.getOrNull(normalize(url).spec, false)
    }

    override fun exists(url: String) = null != getOrNull(url)

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
        return WebPage.NIL.takeIf { !isActive } ?: loadComponent.load(normUrl)
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: URL, options: LoadOptions): WebPage {
        return WebPage.NIL.takeIf { !isActive } ?: loadComponent.load(url, initOptions(options))
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: NormUrl): WebPage {
        initOptions(url.options)
        return WebPage.NIL.takeIf { !isActive } ?: loadComponent.load(url)
    }

    override suspend fun loadDeferred(url: NormUrl): WebPage {
        initOptions(url.options)
        return WebPage.NIL.takeIf { !isActive } ?: loadComponent.loadDeferred(url)
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
    override fun loadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.loadAll(normalize(urls, options), options) else listOf()
    }

    override fun loadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.loadAll(urls, initOptions(options)) else listOf()
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
    override fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.parallelLoadAll(normalize(urls, options), options) else listOf()
    }

    override fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage> {
        return if (isActive) loadComponent.parallelLoadAll(urls, initOptions(options)) else listOf()
    }

    /**
     * Parse the WebPage using Jsoup
     */
    override fun parse(page: WebPage) = JsoupParser(page, unmodifiedConfig).parse()

    override fun parse(page: WebPage, mutableConfig: MutableConfig) = JsoupParser(page, mutableConfig).parse()

    override fun persist(page: WebPage) {
        webDbOrNull?.put(page, false)
    }

    override fun delete(url: String) {
        webDbOrNull?.delete(url)
        webDbOrNull?.delete(normalize(url).spec)
    }

    override fun delete(page: WebPage) {
        webDbOrNull?.delete(page.url)
    }

    override fun flush() {
        webDbOrNull?.flush()
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
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { webDbOrNull?.flush() }.onFailure { log.warn(it.message) }

            sessions.values.forEach {
                it.runCatching { it.close() }.onFailure { log.warn(it.message) }
            }

            closableObjects.forEach {
                it.runCatching { it.close() }.onFailure { log.warn(it.message) }
            }
        }
    }

    private fun initOptions(options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = unmodifiedConfig.toVolatileConfig()
        }

        options.apply(options.volatileConfig)

        return if (toItemOption) options.createItemOptions() else options
    }
}
