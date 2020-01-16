package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.Protocol
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.concurrent.*
import java.util.stream.Collectors

class BatchFetchComponent(
        val webDb: WebDb,
        statusTracker: FetchTaskTracker,
        protocolFactory: ProtocolFactory,
        immutableConfig: ImmutableConfig
) : FetchComponent(statusTracker, protocolFactory, immutableConfig) {
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
        return parallelFetchAllInternal(urls, protocol, options)
    }

    /**
     * Group all urls by URL schema, and parallel fetch each group
     *
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    fun parallelFetchAllGroupedBySchema(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        val pages: MutableList<WebPage> = ArrayList()
//        val groupedUrls = optimizeBatchSize(Lists.newArrayList(urls), options).stream()
//                .collect(Collectors.groupingBy { url: String? -> StringUtils.substringBefore(url, "://") })
        val groupedUrls = optimizeBatchSize(urls, options).groupBy { StringUtils.substringBefore(it, "://") }
        groupedUrls.forEach { (key, gUrls) ->
            val protocol = protocolFactory.getProtocol(key)
            if (protocol != null) {
                pages.addAll(parallelFetchAllInternal(gUrls, protocol, options))
            } else {
                fetchTaskTracker.trackFailed(gUrls)
            }
        }
        return pages
    }

    /**
     * Fetch all urls, if allowParallel is true and the config suggests parallel is preferred, parallel fetch all items
     *
     *
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     *
     * If the protocol supports native parallel, use the protocol's batch fetch method,
     * Or else parallel fetch pages in a ExecutorService
     */
    private fun fetchAllInternal(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        Objects.requireNonNull(options)
        return if (options.preferParallel) {
            parallelFetchAll(urls, options)
        } else {
            optimizeBatchSize(urls, options).stream()
                    .map { url: String? -> fetch(url!!, options) }
                    .collect(Collectors.toList())
        }
    }

    /**
     * Parallel fetch all urls
     * If the protocol supports native parallel, use the protocol's native batch fetch method,
     * Or else parallel fetch pages in a ExecutorService
     */
    private fun parallelFetchAllInternal(urls: Iterable<String>, protocol: Protocol, options: LoadOptions): Collection<WebPage> {
        val optimizedUrls = optimizeBatchSize(urls, options)
        return if (protocol.supportParallel()) {
            protocolParallelFetchAll(optimizedUrls, protocol, options)
        } else {
            manualParallelFetchAll(optimizedUrls, options)
        }
    }

    private fun protocolParallelFetchAll(urls: Iterable<String>, protocol: Protocol, options: LoadOptions): Collection<WebPage> {
        // TODO: avoid searching a page from the map, carry it inside response
        val pages = urls.associateWith { createFetchEntry(it, options) }
        return protocol.getResponses(pages.values, options.volatileConfig)
                .mapNotNull { response -> pages[response.url]?.let { forwardResponse(protocol, response, it) } }
    }

    private fun manualParallelFetchAll(urls: Iterable<String>, options: LoadOptions): Collection<WebPage> {
        if (LOG.isDebugEnabled) {
            LOG.debug("Manual parallel fetch urls")
        }
        // TODO: use GlobalExecutor
        val executor = Executors.newWorkStealingPool()
        return urls.map { executor.submit(Callable { fetch(it, options) }) }.map { getResponse(it) }
    }

    /**
     * Forward previous fetched response to protocol for further process: retry, status processing, etc
     */
    private fun forwardResponse(protocol: Protocol, response: Response, page: WebPage): WebPage {
        protocol.setResponse(response)
        // run protocol.getProtocolOutput so the page have a chance to perform FETCH_PROTOCOL retry if necessary
        return processProtocolOutput(page, protocol.getProtocolOutput(page))
    }

    /**
     * TODO: can be optimized in when the case the input is not a Collection
     * */
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
        var config: ImmutableConfig? = options.volatileConfig
        if (config == null) {
            config = immutableConfig
        }
        val eagerFetchLimit = config.getUint(CapabilityTypes.FETCH_EAGER_FETCH_LIMIT, 20)
        if (urls.size <= eagerFetchLimit) {
            return urls
        }
        val eagerTasks: MutableList<String> = ArrayList(eagerFetchLimit)
        val lazyTasks: MutableList<String> = ArrayList(0.coerceAtLeast(urls.size - eagerFetchLimit))
        for ((i, url) in urls.withIndex()) {
            if (i < eagerFetchLimit) {
                eagerTasks.add(url)
            } else {
                lazyTasks.add(url)
            }
        }
        if (lazyTasks.isNotEmpty()) {
            val mode = options.fetchMode
            // TODO: save url with options
            fetchTaskTracker.commitLazyTasks(mode, lazyTasks)
            if (LOG.isDebugEnabled) {
                LOG.debug("Committed {} lazy tasks in mode {}", lazyTasks.size, mode)
            }
        }
        return eagerTasks
    }

    private fun getResponse(future: Future<WebPage>): WebPage {
        try {
            return future.get(35, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            LOG.warn("Interrupted when fetch resource $e")
        } catch (e: ExecutionException) {
            LOG.warn(e.toString())
        } catch (e: TimeoutException) {
            LOG.warn("Fetch resource timeout, $e")
        }
        return WebPage.NIL
    }
}
