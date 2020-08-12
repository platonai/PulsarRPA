package ai.platon.pulsar.context.support

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
import ai.platon.pulsar.crawl.GlobalCacheManager
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.lang.Nullable
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
        override val applicationContext: ApplicationContext
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

    open val globalCacheManager: GlobalCacheManager get() = getBean()

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()

    val isActive get() = !closed.get() && AppContext.isActive

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
    @Nullable
    private var shutdownHook: Thread? = null

    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T = applicationContext.getBean(requiredType.java)

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

    override fun normalize(url: String, isItemOption: Boolean): NormUrl {
        return normalize(url, LoadOptions.create(), isItemOption)
    }

    override fun normalize(url: String, options: LoadOptions, isItemOption: Boolean): NormUrl {
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

    override fun normalize(urls: Iterable<String>, isItemOption: Boolean): List<NormUrl> {
        return urls.takeIf { isActive }?.mapNotNull { normalize(it, isItemOption).takeIf { it.isNotNil } }
                ?:listOf()
    }

    override fun normalize(urls: Iterable<String>, options: LoadOptions, isItemOption: Boolean): List<NormUrl> {
        return urls.takeIf { isActive }?.mapNotNull { normalize(it, options, isItemOption).takeIf { it.isNotNil } }
                ?: listOf()
    }

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    override fun inject(url: String): WebPage {
        return WebPage.NIL.takeUnless { isActive }?:injectComponent.inject(Urls.splitUrlArgs(url))
    }

    override fun get(url: String): WebPage? {
        return webDb.takeIf { isActive }?.get(normalize(url).url, false)
    }

    override fun getOrNil(url: String): WebPage {
        return webDb.takeIf { isActive }?.getOrNil(normalize(url).url, false)?: WebPage.NIL
    }

    override fun scan(urlPrefix: String): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        return webDb.takeIf { isActive }?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: String): WebPage {
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
    override fun load(url: String, options: LoadOptions): WebPage {
        val normUrl = normalize(url, options)
        return loadComponent.takeIf { isActive }?.load(normUrl)?: WebPage.NIL
    }

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    override fun load(url: URL): WebPage {
        return loadComponent.takeIf { isActive }?.load(url, LoadOptions.create())?: WebPage.NIL
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
    override fun parse(page: WebPage): FeaturedDocument {
        return FeaturedDocument.NIL.takeUnless { isActive }?:JsoupParser(page, unmodifiedConfig).parse()
    }

    override fun parse(page: WebPage, mutableConfig: MutableConfig): FeaturedDocument {
        return FeaturedDocument.NIL.takeUnless { isActive }?:JsoupParser(page, mutableConfig).parse()
    }

    override fun persist(page: WebPage) {
        webDb.takeIf { isActive }?.put(page, false)
    }

    override fun delete(url: String) {
        webDb.takeIf { isActive }?.apply { delete(url); delete(normalize(url).url) }
    }

    override fun delete(page: WebPage) {
        webDb.takeIf { isActive }?.delete(page.url)
    }

    override fun flush() {
        webDb.takeIf { isActive }?.flush()
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

            (applicationContext as? AutoCloseable)?.close()
        }
    }
}
