package ai.platon.pulsar.context

import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.collect.UrlPool
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormURL
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.session.PulsarSession
import org.springframework.beans.BeansException
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * [PulsarContext] consists of a set of highly customizable components that provide
 * the core set of interfaces of the system and is used to create [PulsarSession].
 */
interface PulsarContext {

    /**
     * The context id
     * */
    val id: Int

    /**
     * An immutable config loaded from the config file at startup, and never changes
     * */
    val unmodifiedConfig: ImmutableConfig

    /**
     * The global cache
     * */
    val globalCache: GlobalCache

    /**
     * The url pool to fetch
     * */
    val crawlPool: UrlPool

    /**
     * The main loops
     * */
    val crawlLoops: CrawlLoops

    /**
     * Get a bean with the specified class, throws [BeansException] if the bean doesn't exist
     * */
    @Throws(BeansException::class)
    fun <T : Any> getBean(requiredType: KClass<T>): T

    /**
     * Get a bean with the specified class, returns null if the bean doesn't exist
     * */
    fun <T : Any> getBeanOrNull(requiredType: KClass<T>): T?

    /**
     * Create a pulsar session
     * */
    fun createSession(): PulsarSession

    /**
     * Close a pulsar session
     * */
    fun closeSession(session: PulsarSession)

    /**
     * Register close objects, the objects will be closed when the context closes
     * */
    fun registerClosable(closable: AutoCloseable)

    /**
     * Normalize an url, the url can be in one of the following forms:
     * 1. a url
     * 2. a configured url
     * 3. a base64 encoded url
     * 4. a base64 encoded configured url
     *
     * An url can be configured by appending arguments to the url, and it also can be used with a LoadOptions,
     * If both tailing arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    fun normalize(url: String, options: LoadOptions): NormURL

    fun normalizeOrNull(url: String?, options: LoadOptions): NormURL?

    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Iterable<String>, options: LoadOptions): List<NormURL>

    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    fun normalize(url: UrlAware, options: LoadOptions): NormURL

    fun normalizeOrNull(url: UrlAware?, options: LoadOptions): NormURL?

    /**
     * Normalize urls, remove invalid ones
     *
     * @param urls The urls to normalize
     * @param options The LoadOptions applied to each url
     * @return All normalized urls, all invalid input urls are removed
     * */
    fun normalize(urls: Collection<UrlAware>, options: LoadOptions): List<NormURL>

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: NormURL): WebPage

    /**
     * Get a webpage from the storage
     *
     * @param url The url of the page to retrieve
     * @return The web page, if the page doesn't exist, a NIL page is return
     */
    fun get(url: String): WebPage

    /**
     * Get a webpage from the storage
     *
     * @param url The url of the page to retrieve
     * @return The web page or null
     */
    fun getOrNull(url: String): WebPage?

    /**
     * Get a webpage from the storage
     *
     * @param url The url of the page to check
     * @return True if the page exists in the storage
     */
    fun exists(url: String): Boolean

    /**
     * Check the fetch state of a webpage
     *
     * @param page The webpage to check
     * @return The fetch state of the webpage
     */
    fun fetchState(page: WebPage, options: LoadOptions): CheckState

    /**
     * Scan webpages in the storage whose url start with [urlPrefix]
     *
     * @param urlPrefix The url prefix
     * @return The iterator of the webpages whose url start with [urlPrefix]
     */
    fun scan(urlPrefix: String): Iterator<WebPage>

    fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage>

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage>

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [MutableWebPage.NIL] is returned
     */
    fun load(url: String, options: LoadOptions): WebPage

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [MutableWebPage.NIL] is returned
     */
    fun load(url: URL, options: LoadOptions): WebPage

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [MutableWebPage.NIL] is returned
     */
    fun load(url: NormURL): WebPage

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [MutableWebPage.NIL] is returned
     */
    suspend fun loadDeferred(url: NormURL): WebPage

    /**
     * Load a batch of urls with the specified options.
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page exists neither in local storage nor at the given remote location, [MutableWebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    fun loadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage>

    /**
     * Load a batch of urls with the specified options.
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page exists neither in local storage nor at the given remote location, [MutableWebPage.NIL] is returned
     *
     * @param urls    The urls to load
     * @return Pages for all urls.
     */
    fun loadAll(urls: Iterable<NormURL>): Collection<WebPage>

    /**
     * Load a url asynchronously, the url is added to the task queue, and will be executed asynchronously
     * */
    fun loadAsync(url: NormURL): CompletableFuture<WebPage>

    /**
     * Load a batch of urls asynchronously, the urls are added to the task queue, and will be executed asynchronously
     * */
    fun loadAllAsync(urls: Iterable<NormURL>): List<CompletableFuture<WebPage>>

    /**
     * Submit a url, the url will be added to the task queue, and will be executed asynchronously.
     *
     * The url should be standard or degenerate, otherwise it will be discarded.
     *
     * @param url The url to submit, which will be added to the task queue
     * @return The [PulsarContext] itself to enabled chained operations
     * */
    fun submit(url: UrlAware): PulsarContext

    /**
     * Submit a batch of urls, the urls are added to the task queue, and will be executed asynchronously.
     *
     * The urls should be standard or degenerate, otherwise they will be discarded.
     *
     * @param urls The urls to submit, which will be added to the task queue
     * @return The [PulsarContext] itself to enabled chained operations
     * */
    fun submitAll(urls: Iterable<UrlAware>): PulsarContext

    /**
     * Parse the WebPage using ParseComponent
     */
    fun parse(page: WebPage): FeaturedDocument?

    /**
     * Persist the webpage into the storage immediately.
     * By default, the backend storage is the local file system, if mongodb is detected,
     * the mongodb will be used as the backend storage.
     */
    fun persist(page: WebPage)

    /**
     * Delete the webpage from the backend storage
     */
    fun delete(url: String)

    /**
     * Delete the webpage from the backend storage
     */
    fun delete(page: WebPage)

    /**
     * Flush the backend storage
     */
    fun flush()

    /**
     * Wait until all tasks are done.
     * */
    fun await()

    /**
     * Register shutdown hook
     * */
    fun registerShutdownHook()

    fun close()
}
