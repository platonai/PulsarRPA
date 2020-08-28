package ai.platon.pulsar.context.support

import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_INCOGNITO
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.GlobalCache
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import java.net.URL
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
        override val applicationContext: ApplicationContext,
        override val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment().apply { initialize() }
): PulsarContext, AutoCloseable {

    /**
     * A immutable config is loaded from the config file at process startup, and never changes
     * */
    open val unmodifiedConfig: ImmutableConfig get() = getBean()

    /**
     * Url normalizers
     * */
    open val urlNormalizers: UrlNormalizers get() = getBean()

    /**
     * The web db
     * */
    open val webDb: WebDb get() = getBean()

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
     * The global cache manager
     * */
    open val globalCache: GlobalCache get() = getBean()

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()

    val isActive get() = !closed.get() && AppContext.isActive
            && (applicationContext as AbstractApplicationContext).isActive

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

    private val webDbOrNull: WebDb? get() = webDb.takeIf { isActive }

    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T {
        return applicationContext.getBean(requiredType.java)
//        return applicationContext.takeIf { isActive }?.getBean(requiredType.java)
//                ?: throw IllegalApplicationContextStateException("Pulsar context is not active")
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
        globalCache.pageCache.clear()
        globalCache.documentCache.clear()
    }

    override fun normalize(url: String, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val (spec, args) = Urls.splitUrlArgs(url)
        var normalizedUrl = Urls.normalize(spec, options.shortenKey)
        if (!options.noNorm) {
            normalizedUrl = urlNormalizers.normalize(normalizedUrl) ?: return NormUrl.NIL
        }

        if (args.isBlank()) {
            return NormUrl(normalizedUrl, initOptions(options, toItemOption))
        }

        val parsedOptions = LoadOptions.parse(args)
        val options2 = LoadOptions.mergeModified(parsedOptions, options)
        return NormUrl(normalizedUrl, initOptions(options2, toItemOption))
    }

    override fun normalizeOrNull(url: String?, options: LoadOptions, toItemOption: Boolean): NormUrl? {
        if (url == null) return null
        return kotlin.runCatching { normalize(url, options, toItemOption) }.getOrNull()
    }

    override fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean): List<NormUrl> {
        return urls.mapNotNull { normalizeOrNull(it, options, toItemOption) }
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    override fun inject(url: String): WebPage {
        return WebPage.NIL.takeIf { !isActive }?:injectComponent.inject(Urls.splitUrlArgs(url))
    }

    override fun inject(url: NormUrl): WebPage {
        return WebPage.NIL.takeIf { !isActive }?:injectComponent.inject(url.spec, url.args)
    }

    override fun getOrNull(url: String): WebPage? {
        return webDbOrNull?.getOrNull(normalize(url).spec, false)
    }

    override fun get(url: String): WebPage {
        return webDbOrNull?.get(normalize(url).spec, false)?: WebPage.NIL
    }

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
        return loadComponent.takeIf { isActive }?.load(normUrl)?: WebPage.NIL
    }

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: URL, options: LoadOptions): WebPage {
        return loadComponent.takeIf { isActive }?.load(url, initOptions(options))?: WebPage.NIL
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: NormUrl): WebPage {
        initOptions(url.options)
        return loadComponent.takeIf { isActive }?.load(url)?: WebPage.NIL
    }

    override suspend fun loadDeferred(url: NormUrl): WebPage {
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
    override fun loadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return loadComponent.takeIf { isActive }?.loadAll(normalize(urls, options), options)?: listOf()
    }

    override fun loadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage> {
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
    override fun parallelLoadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return loadComponent.takeIf { isActive }?.parallelLoadAll(normalize(urls, options), options)?: listOf()
    }

    override fun parallelLoadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage> {
        return loadComponent.takeIf { isActive }?.loadAll(urls, initOptions(options))?: listOf()
    }

    /**
     * Parse the WebPage using Jsoup
     */
    override fun parse(page: WebPage) = JsoupParser(page, unmodifiedConfig).parse()

    override fun parse(page: WebPage, mutableConfig: MutableConfig) =
            JsoupParser(page, mutableConfig).parse()

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
            this.shutdownHook = object : Thread() {
                override fun run() {
                    synchronized(startupShutdownMonitor) { doClose() }
                }
            }
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
                } catch (ex: IllegalStateException) { // ignore - VM is already shutting down
                }
            }
        }
    }

    private fun doClose() {
        if (closed.compareAndSet(false, true)) {
            sessions.values.forEach {
                it.runCatching { it.close() }.onFailure { it.printStackTrace() }
            }

            closableObjects.forEach {
                it.runCatching { it.close() }.onFailure { it.printStackTrace() }
            }
        }
    }

    private fun initOptions(options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = VolatileConfig(unmodifiedConfig)
        }

        options.volatileConfig?.setBoolean(BROWSER_INCOGNITO, options.incognito)

        return if (toItemOption) options.createItemOptions() else options
    }
}
