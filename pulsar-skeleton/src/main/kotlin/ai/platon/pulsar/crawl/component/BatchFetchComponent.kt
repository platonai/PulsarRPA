package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.fetch.FetchMetrics
import ai.platon.pulsar.crawl.fetch.LazyFetchTaskManager
import ai.platon.pulsar.crawl.protocol.Protocol
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.MoreExecutors
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*

class BatchFetchComponent(
        val webDb: WebDb,
        fetchMetrics: FetchMetrics? = null,
        val lazyFetchTaskManager: LazyFetchTaskManager? = null,
        protocolFactory: ProtocolFactory,
        immutableConfig: ImmutableConfig
) : FetchComponent(fetchMetrics, protocolFactory, immutableConfig) {
    private final val logger = LoggerFactory.getLogger(BatchFetchComponent::class.java)

    constructor(webDb: WebDb, immutableConfig: ImmutableConfig)
            : this(webDb, null, null, ProtocolFactory(immutableConfig), immutableConfig)

    val fetchTaskExecutor by lazy { MoreExecutors.newDirectExecutorService() }

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
     * Group all urls by URL schema, and parallel fetch each group
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
                fetchMetrics?.trackFailedUrls(gUrls)
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
     * Parallel fetch all urls
     * If the protocol supports native parallel, use the protocol's native parallel fetch method,
     * Or else parallel fetch pages in a ExecutorService
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
        fetchMetrics?.markTaskStart(Iterables.size(urls))
        return urls.map { FetchEntry(it, options).page }
                .let { protocol.getResponses(it, options.conf?:immutableConfig.toVolatileConfig()) }
                .map { getProtocolOutput(protocol, it, it.page) }
    }

    private fun manualParallelFetchAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val size = Iterables.size(urls)
        fetchMetrics?.markTaskStart(size)
        return urls.map { fetchTaskExecutor.submit(Callable { fetch(it, options) }) }.map { getResponse(it) }
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
            // TODO: save url with options
            lazyFetchTaskManager?.commitLazyTasks(mode, lazyTasks, conf)
            if (logger.isDebugEnabled) {
                logger.debug("Committed {} lazy tasks in mode {}", lazyTasks.size, mode)
            }
        }
        return eagerTasks
    }

    private fun getResponse(future: Future<WebPage>): WebPage {
        try {
            return future.get(35, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.warn("Interrupted when fetch resource", e)
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            logger.warn(e.toString())
        } catch (e: TimeoutException) {
            logger.warn("Fetch resource timeout", e)
        }
        return WebPage.NIL
    }
}
