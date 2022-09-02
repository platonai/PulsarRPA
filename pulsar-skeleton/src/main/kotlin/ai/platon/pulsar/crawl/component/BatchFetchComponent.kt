package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.protocol.Protocol
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class BatchFetchComponent(
    val webDb: WebDb,
    val globalCacheFactory: GlobalCacheFactory,
    coreMetrics: CoreMetrics? = null,
    protocolFactory: ProtocolFactory,
    immutableConfig: ImmutableConfig
) : FetchComponent(coreMetrics, protocolFactory, immutableConfig) {
    private val logger = LoggerFactory.getLogger(BatchFetchComponent::class.java)

    constructor(webDb: WebDb, immutableConfig: ImmutableConfig) : this(
        webDb, GlobalCacheFactory(immutableConfig), null, ProtocolFactory(immutableConfig), immutableConfig)

    val globalCache get() = globalCacheFactory.globalCache

    /**
     * Fetch all the urls, config property 'fetch.concurrency' controls the concurrency level.
     * If concurrency level is not great than 1, fetch all urls in the caller thread
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    fun fetchAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return fetchAllInternal(urls, options)
    }

    /**
     * Parallel fetch all the urls
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    fun parallelFetchAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val protocol = protocolFactory.getProtocol(options.fetchMode)
                ?: return parallelFetchAllGroupedBySchema(urls, options)
        return parallelFetchAll0(urls, protocol, options)
    }

    /**
     * Group all urls by URL schema, and parallel fetch each group.
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    fun parallelFetchAllGroupedBySchema(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val pages: MutableList<WebPage> = ArrayList()
        val groupedUrls = optimizeBatchSize(urls, options).groupBy { it.substringBefore("://") }
        groupedUrls.forEach { (key, gUrls) ->
            val protocol = protocolFactory.getProtocol(key)
            if (protocol != null) {
                pages.addAll(parallelFetchAll0(gUrls, protocol, options))
            } else {
                coreMetrics?.trackFailedUrls(gUrls)
            }
        }
        return pages
    }

    /**
     * Fetch all urls, if allowParallel is true and the config suggests parallel is preferred, parallel fetch all items
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * If the protocol supports native parallel, use the protocol's batch fetch method,
     * Or else parallel fetch pages in a ExecutorService
     */
    private fun fetchAllInternal(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        return if (options.preferParallel) {
            parallelFetchAll(urls, options)
        } else {
            optimizeBatchSize(urls, options).map { fetch(it, options) }
        }
    }

    /**
     * Parallel fetch all urls.
     * If the protocol supports native parallel, use the protocol's native parallel fetch method,
     * Or else parallel fetch pages in a ExecutorService.
     */
    private fun parallelFetchAll0(urls: Iterable<String>, protocol: Protocol, options: LoadOptions): Collection<WebPage> {
        val optimizedUrls = optimizeBatchSize(urls, options)
        return if (protocol.supportParallel) {
            protocolParallelFetchAll(optimizedUrls, protocol, options)
        } else {
            manualParallelFetchAll(optimizedUrls, options)
        }
    }

    private fun protocolParallelFetchAll(urls: Iterable<String>, protocol: Protocol, options: LoadOptions): Collection<WebPage> {
        coreMetrics?.markFetchTaskStart(Iterables.size(urls))
        return urls.map { FetchEntry(it, options).page }
                .let { protocol.getResponses(it, options.conf) }
                .map { getProtocolOutput(protocol, it, it.page) }
    }

    /**
     * TODO: add to fetch queue instead of invoke new threads
     * */
    private fun manualParallelFetchAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val size = Iterables.size(urls)
        coreMetrics?.markFetchTaskStart(size)
        return runBlocking { urls.asFlow().map { fetch(it, options) }.toList(mutableListOf()) }
    }

    /**
     * Forward previous fetched response to protocol for further process: retry, status processing, etc
     */
    private fun getProtocolOutput(protocol: Protocol, response: Response, page: WebPage): WebPage {
        // forward a response
        protocol.setResponse(response)
        // run protocol.getProtocolOutput so the page have a chance to perform PROTOCOL scope retry if necessary
        // TODO: RetryScope.PROTOCOL does not work since the response is forwarded
        return processProtocolOutput(page, protocol.getProtocolOutput(page))
    }

    private fun optimizeBatchSize(urls: Iterable<String>, options: LoadOptions): Collection<String> {
        return if (urls is Collection<*>) {
            optimizeBatchSize(urls as Collection<String>, options)
        } else optimizeBatchSize(urls.toList(), options)
    }

    /**
     * If there are too many urls to fetch, just fetch some of them in the foreground and
     * fetch the rest in the background
     */
    private fun optimizeBatchSize(urls: Collection<String>, options: LoadOptions): Collection<String> {
        val conf = options.conf
        val parallelLevel = conf.getUint(CapabilityTypes.FETCH_CONCURRENCY, AppContext.NCPU)
        if (urls.size <= parallelLevel) {
            return urls
        }

        val eagerTasks: MutableList<String> = ArrayList(parallelLevel)
        val lazyTasks: MutableList<String> = ArrayList(0.coerceAtLeast(urls.size - parallelLevel))
        for ((i, url) in urls.withIndex()) {
            if (i < parallelLevel) {
                eagerTasks.add(url)
            } else {
                lazyTasks.add(url)
            }
        }

        if (lazyTasks.isNotEmpty()) {
            val mode = options.fetchMode
            val links = lazyTasks.map { NormUrl(it, options) }.map { Hyperlink(it.spec, args = it.args) }
            globalCache.urlPool.normalCache.nReentrantQueue.addAll(links)
            if (logger.isDebugEnabled) {
                logger.debug("Committed {} lazy tasks in mode {}", lazyTasks.size, mode)
            }
        }

        return eagerTasks
    }
}
