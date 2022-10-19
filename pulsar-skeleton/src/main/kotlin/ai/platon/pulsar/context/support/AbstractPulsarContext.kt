package ai.platon.pulsar.context.support

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.collect.UrlPool
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.*
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.ChainedUrlNormalizer
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.session.AbstractPulsarSession
import ai.platon.pulsar.session.PulsarEnvironment
import ai.platon.pulsar.session.PulsarSession
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.support.AbstractApplicationContext
import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

abstract class AbstractPulsarContext(
        val applicationContext: AbstractApplicationContext,
        val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment()
): PulsarContext, AutoCloseable {

    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(AbstractPulsarContext::class.java)

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

    private val loadComponentOrNull: LoadComponent? get() = if (isActive) loadComponent else null

    /**
     * Return null if everything is OK, or return NIL if something wrong
     * */
    private val abnormalPage get() = if (isActive) null else WebPage.NIL

    /**
     * Return null if everything is OK, or return a empty list if something wrong
     * */
    private val abnormalPages: List<WebPage>? get() = if (isActive) null else listOf()

    /**
     * Check if the context is active
     * */
    val isActive get() = !closed.get() && AppContext.isActive && applicationContext.isActive

    /**
     * The context id
     * */
    override val id = instanceSequencer.incrementAndGet()

    /**
     * An immutable config is loaded from the config file at process startup, and never changes
     * */
    override val unmodifiedConfig: ImmutableConfig get() = getBean()

    /**
     * Url normalizers
     * */
    open val urlNormalizers: ChainedUrlNormalizer get() = getBean()

    /**
     * The web db
     * */
    open val webDb: WebDb get() = getBean()

    open val globalCacheFactory: GlobalCacheFactory get() = getBean()

    open val injectComponent: InjectComponent get() = getBean()

    open val fetchComponent: BatchFetchComponent get() = getBean()

    open val parseComponent: ParseComponent get() = getBean()

    open val updateComponent: UpdateComponent get() = getBean()

    open val loadComponent: LoadComponent get() = getBean()

    override val globalCache: GlobalCache get() = globalCacheFactory.globalCache

    override val crawlPool: UrlPool get() = globalCache.urlPool

    override val crawlLoops: CrawlLoops get() = getBean()

    /**
     * The start time
     * */
    val startTime = System.currentTimeMillis()

    /**
     * All open sessions
     * */
    val sessions = ConcurrentSkipListMap<Int, PulsarSession>()

    /**
     * Get a bean with the specified class, throws [BeansException] if the bean doesn't exist
     * */
    @Throws(BeansException::class)
    override fun <T : Any> getBean(requiredType: KClass<T>): T = applicationContext.getBean(requiredType.java)

    /**
     * Get a bean with the specified class, returns null if the bean doesn't exist
     * */
    override fun <T : Any> getBeanOrNull(requiredType: KClass<T>): T? =
        kotlin.runCatching { applicationContext.getBean(requiredType.java) }.getOrNull()

    /**
     * Get a bean with the specified class, throws [BeansException] if the bean doesn't exist
     * */
    @Throws(BeansException::class)
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    /**
     * Get a bean with the specified class, returns null if the bean doesn't exist
     * */
    inline fun <reified T : Any> getBeanOrNull(): T? = getBeanOrNull(T::class)

    /**
     * Create a session
     * */
    abstract override fun createSession(): AbstractPulsarSession

    /**
     * Close the given session
     * */
    override fun closeSession(session: PulsarSession) {
        session.close()
        logger.info("Removing PulsarSession #{}", session.id)
        sessions.remove(session.id)
    }

    /**
     * Register close objects, the objects will be closed when the context closes
     * */
    override fun registerClosable(closable: AutoCloseable) {
        closableObjects.add(closable)
    }

    override fun normalize(url: String, options: LoadOptions): NormURL {
        val url0 = url.takeIf { it.contains("://") } ?: String(Base64.getUrlDecoder().decode(url))
        return normalize(PlainUrl(url0), options)
    }

    override fun normalizeOrNull(url: String?, options: LoadOptions): NormURL? {
        return if (url == null) return null else kotlin.runCatching { normalize(url, options) }.getOrNull()
    }

    override fun normalize(urls: Iterable<String>, options: LoadOptions) =
        urls.mapNotNull { normalizeOrNull(it, options) }

    override fun normalize(url: UrlAware, options: LoadOptions) =
        CombinedUrlNormalizer(urlNormalizers).normalize(url, options)

    override fun normalizeOrNull(url: UrlAware?, options: LoadOptions): NormURL? {
        return if (url == null) return null else kotlin.runCatching { normalize(url, options) }.getOrNull()
    }

    override fun normalize(urls: Collection<UrlAware>, options: LoadOptions) =
        urls.mapNotNull { normalizeOrNull(it, options) }

    override fun inject(url: String) = abnormalPage ?: injectComponent.inject(UrlUtils.splitUrlArgs(url))

    override fun inject(url: NormURL) = abnormalPage ?: injectComponent.inject(url.spec, url.args)

    override fun get(url: String): WebPage = webDbOrNull?.get(url, false) ?: WebPage.NIL

    override fun getOrNull(url: String) = webDbOrNull?.getOrNull(url, false)

    override fun exists(url: String) = webDbOrNull?.exists(url) == true

    override fun fetchState(page: WebPage, options: LoadOptions) =
        loadComponentOrNull?.fetchState(page, options) ?: CheckState(FetchState.DO_NOT_FETCH, "closed")

    override fun scan(urlPrefix: String): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    override fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage> {
        return webDbOrNull?.scan(urlPrefix, fields) ?: listOf<WebPage>().iterator()
    }

    override fun load(url: String, options: LoadOptions) =
        abnormalPage ?: loadComponent.load(normalize(url, options))

    override fun load(url: URL, options: LoadOptions) = abnormalPage ?: loadComponent.load(url, options)

    override fun load(url: NormURL) = abnormalPage ?: loadComponent.load(url)

    override suspend fun loadDeferred(url: NormURL) = abnormalPage ?: loadComponent.loadDeferred(url)

    override fun loadAll(urls: Iterable<String>, options: LoadOptions): List<WebPage> {
        startLoopIfNecessary()
        return abnormalPages ?: loadComponent.loadAll(normalize(urls, options))
    }

    override fun loadAll(urls: Iterable<NormURL>): List<WebPage> {
        startLoopIfNecessary()
        return abnormalPages ?: loadComponent.loadAll(urls)
    }

    override fun loadAsync(url: NormURL): CompletableFuture<WebPage> {
        startLoopIfNecessary()
        return loadComponentOrNull?.loadAsync(url) ?: CompletableFuture.completedFuture(WebPage.NIL)
    }

    override fun loadAllAsync(urls: Iterable<NormURL>): List<CompletableFuture<WebPage>> {
        startLoopIfNecessary()
        return loadComponentOrNull?.loadAllAsync(urls) ?: listOf()
    }

    override fun submit(url: UrlAware): AbstractPulsarContext {
        startLoopIfNecessary()
        if (url.isStandard || url is DegenerateUrl) {
            crawlPool.add(url)
        }
        return this
    }

    override fun submitAll(urls: Iterable<UrlAware>): AbstractPulsarContext {
        startLoopIfNecessary()
        crawlPool.addAll(urls.filter { it.isStandard || it is DegenerateUrl })
        return this
    }

    override fun parse(page: WebPage): FeaturedDocument? {
        val parser = loadComponentOrNull?.parseComponent
        return parser?.parse(page, noLinkFilter = true)?.document
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

    @Throws(IllegalStateException::class)
    override fun registerShutdownHook() {
        if (this.shutdownHook == null) { // No shutdown hook registered yet.
            this.shutdownHook = Thread { synchronized(startupShutdownMonitor) { doClose() } }
            Runtime.getRuntime().addShutdownHook(this.shutdownHook)
        }
    }

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

    protected open fun doClose() {
        AppContext.beginTermination()

        if (closed.compareAndSet(false, true)) {
            try {
                doClose0()
            } catch (t: Throwable) {
                System.err.println("Unexpected exception:")
                t.printStackTrace(System.err)
            }
        }

        AppContext.endTermination()
    }

    protected open fun doClose0() {
        logger.info("Closing context #{}/{} | {}", id, sessions.size, this::class.java.simpleName)

        getBeanOrNull<GlobalCacheFactory>()?.globalCache?.clearCaches()

        val nonSyncSessions = sessions.values.toList().also { sessions.clear() }
        nonSyncSessions.parallelStream().forEach { session ->
            session.runCatching { close() }.onFailure { logger.warn(it.brief("[Unexpected]")) }
        }

        val nonSyncObjects = closableObjects.toList().also { closableObjects.clear() }
        nonSyncObjects.parallelStream().forEach { closable ->
            closable.runCatching { close() }.onFailure { logger.warn(it.brief("[Unexpected]")) }
        }
    }

    private fun startLoopIfNecessary() {
        if (isActive && !crawlLoops.isStarted) {
            crawlLoops.start()
        }
    }
}
