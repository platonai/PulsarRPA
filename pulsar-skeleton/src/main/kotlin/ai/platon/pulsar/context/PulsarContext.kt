package ai.platon.pulsar.context

import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.collect.UrlPool
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.springframework.context.ApplicationContext
import java.net.URL

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
interface PulsarContext: AutoCloseable {
    val id: Int

    val pulsarEnvironment: PulsarEnvironment

    val applicationContext: ApplicationContext

    val unmodifiedConfig: ImmutableConfig

    val crawlPool: UrlPool

    val crawlLoops: CrawlLoops

    fun createSession(): PulsarSession

    fun closeSession(session: PulsarSession)

    /**
     * Close objects when sessions close
     * */
    fun registerClosable(closable: AutoCloseable)

    fun normalize(url: String, options: LoadOptions, toItemOption: Boolean = false): NormUrl

    fun normalizeOrNull(url: String?, options: LoadOptions, toItemOption: Boolean): NormUrl?

    fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean = false): List<NormUrl>

    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean = false): NormUrl

    fun normalizeOrNull(url: UrlAware?, options: LoadOptions, toItemOption: Boolean): NormUrl?

    fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean = false): List<NormUrl>

    /**
     * Inject an url
     *
     * @param url The url followed by config options
     * @return The web page created
     */
    fun inject(url: String): WebPage

    fun inject(url: NormUrl): WebPage

    fun get(url: String): WebPage

    fun getOrNull(url: String): WebPage?

    fun exists(url: String): Boolean

    fun fetchState(page: WebPage, options: LoadOptions): CheckState

    fun scan(urlPrefix: String): Iterator<WebPage>

    fun scan(urlPrefix: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage>

    fun scan(urlPrefix: String, fields: Array<String>): Iterator<WebPage>

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String, options: LoadOptions): WebPage

    /**
     * Load a url with specified options, see [LoadOptions] for all options
     *
     * @param url     The url followed by options
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: URL, options: LoadOptions): WebPage

    /**
     * Load a url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param url The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: NormUrl): WebPage

    suspend fun loadDeferred(url: NormUrl): WebPage

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
    fun loadAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage>

    fun loadAll(urls: Collection<NormUrl>, options: LoadOptions): Collection<WebPage>

    fun asyncLoad(url: UrlAware): PulsarContext

    fun asyncLoadAll(urls: Collection<UrlAware>): PulsarContext

    /**
     * Parse the WebPage using Jsoup
     */
    fun parse(page: WebPage): FeaturedDocument?

    fun persist(page: WebPage)

    fun delete(url: String)

    fun delete(page: WebPage)

    fun flush()

    /**
     * Wait until all tasks are done.
     * */
    fun await()

    fun registerShutdownHook()
}
