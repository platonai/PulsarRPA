package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.AppStatusTracker
import ai.platon.pulsar.common.PulsarParams.VAR_FETCH_REASON
import ai.platon.pulsar.common.PulsarParams.VAR_PREV_FETCH_TIME_BEFORE_UPDATE
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.Urls.splitUrlArgs
import ai.platon.pulsar.crawl.CrawlLoop
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.url.CompletableListenableHyperlink
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.model.ActiveDomStat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 17-7-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Load pages from storage or fetch from the Internet if it's not fetched or expired
 */
@Component
class LoadComponent(
    val webDb: WebDb,
    val globalCache: GlobalCache,
    val fetchComponent: BatchFetchComponent,
    val parseComponent: ParseComponent? = null,
    val updateComponent: UpdateComponent,
    val statusTracker: AppStatusTracker? = null,
    val crawlLoop: CrawlLoop? = null,
    val immutableConfig: ImmutableConfig,
) : AutoCloseable {
    companion object {
        private const val VAR_REFRESH = "refresh"
    }

    private val logger = LoggerFactory.getLogger(LoadComponent::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }

    val pageCache get() = globalCache.pageCache
    val documentCache get() = globalCache.documentCache

    private val nonMetadataFields = listOf(
        GWebPage.Field.CONTENT,
        GWebPage.Field.PAGE_TEXT,
        GWebPage.Field.PAGE_MODEL
    )
    val metadataFields = GWebPage.Field.values()
        .filterNot { it in nonMetadataFields }
        .map { it.getName() }
        .toTypedArray()

    private val fetchMetrics get() = fetchComponent.fetchMetrics
    private val closed = AtomicBoolean()

    /**
     * TODO: only check active before blocking calls
     * */
    private val isActive get() = !closed.get()

    @Volatile
    private var numWrite = 0
    private val abnormalPage get() = WebPage.NIL.takeIf { !isActive }

    @Volatile
    var lastPageReport: String? = null
        protected set

    constructor(
        webDb: WebDb,
        globalCache: GlobalCache,
        fetchComponent: BatchFetchComponent,
        updateComponent: UpdateComponent,
        immutableConfig: ImmutableConfig,
    ) : this(webDb, globalCache, fetchComponent, null, updateComponent, null, null, immutableConfig)

    fun fetchState(page: WebPage, options: LoadOptions): Int {
        val protocolStatus = page.protocolStatus

        return when {
            closed.get() -> FetchState.DO_NOT_FETCH
            page.isNil -> FetchState.NEW_PAGE
            page.isInternal -> FetchState.DO_NOT_FETCH
            protocolStatus.isNotFetched -> FetchState.NEW_PAGE
            protocolStatus.isTempMoved -> FetchState.TEMP_MOVED
            else -> getFetchReasonForExistPage(page, options)
        }
    }

    /**
     * Load an url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(configuredUrl: String): WebPage {
        val (first, second) = splitUrlArgs(configuredUrl)
        val options = LoadOptions.parse(second, immutableConfig.toVolatileConfig())
        return load(first, options)
    }

    /**
     * Load an url with specified options, see [LoadOptions] for all options
     *
     * @param url The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(url: String, args: String): WebPage {
        return load(url, LoadOptions.parse(args, immutableConfig.toVolatileConfig()))
    }

    /**
     * Load an url with specified options
     * If there is no page in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param url The url to load
     * @param options The options
     * @return The WebPage.
     */
    fun load(url: String, options: LoadOptions): WebPage {
        return abnormalPage ?: loadWithRetry(NormUrl(url, options))
    }

    fun load(url: URL, options: LoadOptions): WebPage {
        return abnormalPage ?: loadWithRetry(NormUrl(url, options))
    }

    /**
     * Load an url in [GHypeLink] format
     * If there is no page in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param link    The url in [GHypeLink] format to load
     * @param options The options
     * @return The WebPage.
     */
    fun load(link: GHypeLink, options: LoadOptions): WebPage {
        return abnormalPage ?: load(link.url.toString(), options).also { it.anchor = link.anchor.toString() }
    }

    fun load(normUrl: NormUrl): WebPage {
        return abnormalPage ?: loadWithRetry(normUrl)
    }

    suspend fun loadDeferred(normUrl: NormUrl): WebPage {
        return abnormalPage ?: loadWithRetryDeferred(normUrl)
    }

    fun loadWithRetry(normUrl: NormUrl): WebPage {
        var page = load0(normUrl)
        var n = normUrl.options.nJitRetry
        while (page.protocolStatus.isRetry && n-- > 0) {
            page = load0(normUrl)
        }
        return page
    }

    suspend fun loadWithRetryDeferred(normUrl: NormUrl): WebPage {
        var page = loadDeferred0(normUrl)
        var n = normUrl.options.nJitRetry
        while (page.protocolStatus.isRetry && n-- > 0) {
            page = loadDeferred0(normUrl)
        }
        return page
    }

    fun loadAll(normUrls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        val queue = globalCache.fetchCaches.highestCache.nReentrantQueue
        val estimatedWaitTime = 120L
        val links = normUrls
            .asSequence()
            .map { CompletableListenableHyperlink<WebPage>(it.spec, args = it.args, href = it.hrefSpec) }
            .onEach { it.maxRetry = 0 }
            .onEach {
                it.crawlEventHandler.onAfterLoadPipeline.addFirst { url, page ->
                    (url as? CompletableListenableHyperlink<WebPage>)?.complete(page)
                }
            }
            .onEach { it.completeOnTimeout(WebPage.NIL, estimatedWaitTime, TimeUnit.SECONDS) }
            .toList()
        queue.addAll(links)
        logger.debug("Waiting for {} completable hyperlinks", links.size)
        // timeout process?
        val future = CompletableFuture.allOf(*links.toTypedArray())
        future.join()
        return links.mapNotNull { it.get() }.filter { it.isNotInternal }
    }

    /**
     * Load a batch of urls with the specified options.
     *
     *
     * Urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param normUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    fun parallelLoadAll(normUrls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        options.preferParallel = true
        return loadAll(normUrls, options)
    }

    private fun load0(normUrl: NormUrl): WebPage {
        val page = createWebPageShell(normUrl)

        beforeLoad(normUrl, page)

        fetchContentIfNecessary(normUrl, page)

        afterLoad(page, normUrl)

        return page
    }

    private suspend fun loadDeferred0(normUrl: NormUrl): WebPage {
        val page = createWebPageShell(normUrl)

        beforeLoad(normUrl, page)

        fetchContentIfNecessaryDeferred(normUrl, page)

        afterLoad(page, normUrl)

        return page
    }

    private fun fetchContentIfNecessary(normUrl: NormUrl, page: WebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContent(page, normUrl)
            if (page.protocolStatus.isSuccess) {
                documentCache.remove(page.url)
            }
            pageCache.putDatum(page.url, page)
        }
    }

    private suspend fun fetchContentIfNecessaryDeferred(normUrl: NormUrl, page: WebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContentDeferred(page, normUrl)
            if (page.protocolStatus.isSuccess) {
                documentCache.remove(page.url)
            }
            pageCache.putDatum(page.url, page)
        }
    }

    private fun createWebPageShell(normUrl: NormUrl): WebPage {
        // load the metadata of the page from the database, this is very fast for a crawler
        val page = createWebPageShell0(normUrl)
        val cachedPage = getCachedPageOrNull(normUrl)
        if (cachedPage != null) {
            page.isCached = true
            page.content = cachedPage.content
            page.removeVar(VAR_REFRESH)
            assert(!page.isFetched)
        }
        return page
    }

    private fun createWebPageShell0(normUrl: NormUrl): WebPage {
        val url = normUrl.spec
        val options = normUrl.options
        if (globalCache.fetchingCache.contains(url)) {
            logger.takeIf { it.isDebugEnabled }?.debug("Page is being fetched | {}", url)
            // return WebPage.NIL
        }

        // get metadata only
        val page0 = webDb.get(url, fields = metadataFields)

        val reason = fetchState(page0, options)
        val fetchEntry = if (page0.isNil) {
            FetchEntry(url, options, normUrl.hrefSpec)
        } else {
            FetchEntry(page0, options, normUrl.hrefSpec)
        }

        val page = fetchEntry.page

        page.variables[VAR_FETCH_REASON] = reason
        val refresh = reason in FetchState.refreshCodes
        if (refresh) {
            page.variables[VAR_REFRESH] = reason
        }

        return page
    }

    private fun beforeLoad(normUrl: NormUrl, page: WebPage) {
        val options = normUrl.options

        shouldBe(options.conf, page.conf) { "Conf should be the same \n${options.conf} \n${page.conf}" }

        try {
            page.loadEventHandler?.onBeforeLoad?.invoke(page.url)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke beforeLoad | ${page.configuredUrl}", e)
        }
    }

    private fun afterLoad(page: WebPage, normUrl: NormUrl) {
        val options = normUrl.options

        ensurePageContent(page, normUrl)

        shouldBe(options.conf, page.conf) { "Conf should be the same \n${options.conf} \n${page.conf}" }

        // we might use the cached page's content in parse phrase
        if (options.parse) {
            parse(page, normUrl.options)
        }

        try {
            // we might use the cached page's content in after load handler
            page.loadEventHandler?.onAfterLoad?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke afterLoad | ${page.configuredUrl}", e)
        }

        // persist if it's not loaded from the cache so it's not updated
        // we might persist only when it's fetched
        if (!page.isCached && options.persist) {
            persist(page, options)
        }
    }

    private fun ensurePageContent(page: WebPage, normUrl: NormUrl) {
        if (page.persistContent == null) {
            shouldBe(false, page.isFetched) { "Page should not be fetched | ${page.configuredUrl}" }
            // load the content of the page
            // TODO: what if the page's status is failed?
            val contentPage = webDb.getOrNull(page.url, GWebPage.Field.CONTENT)
            if (contentPage != null) {
                page.content = contentPage.persistContent
            }
        }

        pageCache.putDatum(page.url, page)

        if (logger.isInfoEnabled) {
            val verbose = logger.isDebugEnabled
            val report = LoadedPageFormatter(page, withSymbolicLink = verbose, withOptions = true).toString()
            logger.info(report)
        }
    }

    private fun getCachedPageOrNull(normUrl: NormUrl): WebPage? {
        val (url, options) = normUrl
        if (options.refresh) {
            // refresh the page, do not take cached version
            return null
        }

        val now = Instant.now()
        val cachedPage = pageCache.getDatum(url, options.expires, now)
        if (cachedPage != null && !options.isExpired(cachedPage.prevFetchTime)) {
            // TODO: properly handle page conf, a page might work in different context which have different conf
            // TODO: properly handle ListenableHyperlink
            // here is a complex logic for a ScrapingHyperlink: the page have an event handlers, and the page can
            // also be loaded inside an event handler. We must handle such situation very carefully

            // page.conf = normUrl.options.conf
            // page.args = normUrl.args

            return cachedPage
        }

        return null
    }

    private fun beforeFetch(page: WebPage, options: LoadOptions) {
        // require(page.options == options)
        page.setVar(VAR_PREV_FETCH_TIME_BEFORE_UPDATE, page.prevFetchTime)
        globalCache.fetchingCache.add(page.url)
        logger.takeIf { it.isDebugEnabled }?.debug("Loading url | {} {}", page.url, page.args)
    }

    private fun fetchContent(page: WebPage, normUrl: NormUrl) {
        try {
            beforeFetch(page, normUrl.options)
            fetchComponent.fetchContent(page)
        } finally {
            afterFetch(page, normUrl.options)
        }
    }

    private suspend fun fetchContentDeferred(page: WebPage, normUrl: NormUrl) {
        try {
            beforeFetch(page, normUrl.options)
            fetchComponent.fetchContentDeferred(page)
        } finally {
            afterFetch(page, normUrl.options)
        }
    }

    private fun afterFetch(page: WebPage, options: LoadOptions) {
        // the metadata of the page is loaded from database but the content is not cached,
        // so load the content again
        update(page, options)
        globalCache.fetchingCache.remove(page.url)
    }

    /**
     * TODO: FetchSchedule.shouldFetch, crawlStatus and FetchReason should keep consistent
     * */
    private fun getFetchReasonForExistPage(page: WebPage, options: LoadOptions): Int {
        // TODO: crawl status is better to decide the fetch reason
        val crawlStatus = page.crawlStatus
        val protocolStatus = page.protocolStatus

        if (options.refresh) {
            page.fetchRetries = 0
            return FetchState.REFRESH
        }

        val ignoreFailure = options.ignoreFailure || options.retryFailed
        if (protocolStatus.isRetry) {
            return FetchState.RETRY
        } else if (protocolStatus.isFailed && !ignoreFailure) {
            // Failed to fetch the page last time, it might be caused by page is gone
            // in such case, do not fetch it even it it's expired, unless the retryFailed flag is set
            return FetchState.DO_NOT_FETCH
        }

        val now = Instant.now()

        // Fetch a page already fetched before if it's expired
        val prevFetchTime = page.prevFetchTime
        if (prevFetchTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            statusTracker?.messageWriter?.debugIllegalLastFetchTime(page)
        }

        // if (expireAt in prevFetchTime..now || now > prevFetchTime + expires), it's expired
        if (options.isExpired(prevFetchTime)) {
            return FetchState.EXPIRED
        }

        val duration = Duration.between(page.fetchTime, now)
        val days = duration.toDays()
        if (duration.toMillis() > 0 && days < 3) {
            return FetchState.SCHEDULED
        }

        if (page.persistContentLength == 0L) {
            return FetchState.NO_CONTENT
        }

        if (page.persistContentLength < options.requireSize) {
            return FetchState.SMALL_CONTENT
        }

        val domStats = page.activeDomStats
        val (ni, na) = domStats["lastStat"] ?: ActiveDomStat()
        if (ni < options.requireImages) {
            return FetchState.MISS_FIELD
        }
        if (na < options.requireAnchors) {
            return FetchState.MISS_FIELD
        }

        return FetchState.DO_NOT_FETCH
    }

    private fun update(page: WebPage, options: LoadOptions) {
        if (page.isInternal) {
            return
        }

        // canceled or loaded from database, do not update fetch schedule
        if (!page.isFetched || page.protocolStatus.isCanceled) {
            return
        }

        updateComponent.updateFetchSchedule(page)
    }

    private fun parse(page: WebPage, options: LoadOptions) {
        val parser = parseComponent?.takeIf { options.parse }
        if (parser == null) {
            logger.info("Parser is null")
            return
        }

        val parseResult = parser.parse(page, options.query, options.reparseLinks, options.noFilter)
        tracer?.trace("ParseResult: {} ParseReport: {}", parseResult, parser.getTraceInfo())
    }

    private fun persist(page: WebPage, options: LoadOptions) {
        // Remove content if storingContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (!options.storeContent && page.content != null) {
            page.clearPersistContent()
        }

        if (!page.isFetched) {
            page.unbox().clearDirty(GWebPage.Field.CONTENT.index)
            assert(!page.unbox().isContentDirty)
        }

        webDb.put(page)
        ++numWrite

        collectPersistMetrics(page)

        if (!options.lazyFlush || numWrite % 20 == 0) {
            flush()
        }
    }

    private fun collectPersistMetrics(page: WebPage) {
        val metrics = fetchMetrics
        if (metrics != null) {
            metrics.persists.mark()
            val bytes = page.content?.array()?.size ?: 0
            if (bytes > 0) {
                metrics.contentPersists.mark()
                metrics.persistContentMBytes.inc(ByteUnit.convert(bytes, "M").toLong())
            }
        }
        tracer?.trace("Persisted {} | {}", Strings.readableBytes(page.contentLength), page.url)
    }

    fun flush() = webDb.flush()

    override fun close() {
        closed.set(true)
    }

    private fun assertSame(a: Any?, b: Any?, lazyMessage: () -> String) {
        require(a === b) { lazyMessage() }
    }

    private fun shouldBe(expected: Any?, actual: Any?, lazyMessage: () -> String) {
        if (actual != expected) {
            logger.warn(lazyMessage())
        }
    }
}
