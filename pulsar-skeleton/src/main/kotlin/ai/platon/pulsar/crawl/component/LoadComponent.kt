package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.PulsarParams.VAR_FETCH_STATE
import ai.platon.pulsar.common.PulsarParams.VAR_PREV_FETCH_TIME_BEFORE_UPDATE
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.measure.ByteUnitConverter
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlUtils.splitUrlArgs
import ai.platon.pulsar.crawl.CrawlLoop
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.url.CompletableListenableHyperlink
import ai.platon.pulsar.crawl.parse.ParseResult
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by vincent on 17-7-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Load pages from storage or fetch from the Internet if it's not fetched or expired
 */
@Component
class LoadComponent(
    val webDb: WebDb,
    val globalCacheFactory: GlobalCacheFactory,
    val fetchComponent: BatchFetchComponent,
    val parseComponent: ParseComponent,
    val updateComponent: UpdateComponent,
    val statusTracker: AppStatusTracker? = null,
    val crawlLoop: CrawlLoop? = null,
    val immutableConfig: ImmutableConfig,
) : AutoCloseable {
    companion object {
        private const val VAR_REFRESH = "refresh"
        val pageCacheHits = AtomicLong()
        val dbGetCount = AtomicLong()
    }

    private val logger = LoggerFactory.getLogger(LoadComponent::class.java)
    private val taskLogger = LoggerFactory.getLogger(LoadComponent::class.java.name + ".Task")
    private val tracer = logger.takeIf { it.isTraceEnabled }

    val globalCache get() = globalCacheFactory.globalCache
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

    private val coreMetrics get() = fetchComponent.coreMetrics
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
        globalCacheFactory: GlobalCacheFactory,
        fetchComponent: BatchFetchComponent,
        parseComponent: ParseComponent,
        updateComponent: UpdateComponent,
        immutableConfig: ImmutableConfig,
    ) : this(webDb, globalCacheFactory, fetchComponent, parseComponent, updateComponent, null, null, immutableConfig)

    fun fetchState(page: WebPage, options: LoadOptions): CheckState {
        val protocolStatus = page.protocolStatus

        return when {
            closed.get() -> CheckState(FetchState.DO_NOT_FETCH, "closed")
            page.isNil -> CheckState(FetchState.NEW_PAGE, "nil")
            page.isInternal -> CheckState(FetchState.DO_NOT_FETCH, "internal")
            protocolStatus.isNotFetched -> CheckState(FetchState.NEW_PAGE, "not fetched")
            protocolStatus.isTempMoved -> CheckState(FetchState.TEMP_MOVED, "temp moved")
            else -> getFetchStateForExistPage(page, options)
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
        val queue = globalCache.fetchCaches.higher3Cache.reentrantQueue
        val timeoutSeconds = options.pageLoadTimeout.seconds + 1
        val links = normUrls
            .asSequence()
            .map { CompletableListenableHyperlink<WebPage>(it.spec, args = it.args, href = it.hrefSpec) }
            .onEach { it.maxRetry = 0 }
            .onEach {
                it.crawlEventHandler.onAfterLoadPipeline.addFirst { url, page ->
                    (url as? CompletableListenableHyperlink<WebPage>)?.complete(page)
                }
            }
            .onEach { it.completeOnTimeout(WebPage.NIL, timeoutSeconds, TimeUnit.SECONDS) }
            .toList()

        queue.addAll(links)
        logger.info("Waiting for {} completable hyperlinks, {}@{}, {}", links.size,
            globalCache.javaClass, globalCache.hashCode(), globalCache.fetchCaches.hashCode())

        var i = 90
        val pendingLinks = links.toMutableList()
        while (i-- > 0 && pendingLinks.isNotEmpty()) {
            val finishedLinks = pendingLinks.filter { it.isDone }
            if (finishedLinks.isNotEmpty()) {
                logger.debug("Has finished {} links", finishedLinks.size)
            }

            if (i % 30 == 0) {
                logger.debug("Still {} pending links", pendingLinks.size)
            }

            pendingLinks.removeIf { it.isDone }
            sleepSeconds(1)
        }

        // timeout process?
//        val future = CompletableFuture.allOf(*links.toTypedArray())
//        future.join()

        return links.filter { it.isDone }.mapNotNull { it.get() }.filter { it.isNotInternal }
    }

    private fun load0(normUrl: NormUrl): WebPage {
        val page = createPageShell(normUrl)

        beforeLoad(normUrl, page)

        fetchContentIfNecessary(normUrl, page)

        afterLoad(page, normUrl)

        return page
    }

    private suspend fun loadDeferred0(normUrl: NormUrl): WebPage {
        val page = createPageShell(normUrl)

        beforeLoad(normUrl, page)

        fetchContentIfNecessaryDeferred(normUrl, page)

        afterLoad(page, normUrl)

        return page
    }

    private fun fetchContentIfNecessary(normUrl: NormUrl, page: WebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContent(page, normUrl)
        }
    }

    private suspend fun fetchContentIfNecessaryDeferred(normUrl: NormUrl, page: WebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContentDeferred(page, normUrl)
        }
    }

    /**
     * Create a page shell, the page shell is the process unit for most tasks
     * */
    private fun createPageShell(normUrl: NormUrl): WebPage {
        val cachedPage = getCachedPageOrNull(normUrl)
        var page = FetchEntry.createPageShell(normUrl)

        if (cachedPage != null) {
            pageCacheHits.incrementAndGet()
            page.isCached = true
            // the cached page can be or not be persisted, but not guaranteed
            // if a page is loaded from cache, the content remains unchanged and should not persist to database
            // TODO: clone the underlying data or not?
            page.unsafeCloneGPage(cachedPage)
            page.clearPersistContent()

            page.args = normUrl.args
            page.tmpContent = cachedPage.content

            // TODO: test the dirty flag
            // do not persist this copy
            page.unbox().clearDirty()
            assert(!page.isFetched)
            assert(page.isNotInternal)
        } else {
            // get the metadata of the page from the database, this is very fast for a crawler
            // TODO: two step loading or one step loading?
//            val loadedPage = webDb.getOrNull(normUrl.spec, fields = metadataFields)
            val loadedPage = webDb.getOrNull(normUrl.spec)
            dbGetCount.incrementAndGet()
            if (loadedPage != null) {
                // override the old variables: args, href, etc
                FetchEntry.initWebPage(loadedPage, normUrl.options, normUrl.hrefSpec)
                page = loadedPage
            }

            initFetchState(normUrl, page, loadedPage)
        }

        return page
    }

    private fun initFetchState(normUrl: NormUrl, page: WebPage, loadedPage: WebPage?): CheckState {
        val options = normUrl.options

        val state = when {
            loadedPage == null -> CheckState(FetchState.NEW_PAGE, "nil 1")
            loadedPage.isNil -> CheckState(FetchState.NEW_PAGE, "nil 2")
            loadedPage.isInternal -> CheckState(FetchState.DO_NOT_FETCH, "internal 1")
            else -> fetchState(page, options)
        }

        page.setVar(VAR_FETCH_STATE, state)
        val refresh = state.code in FetchState.refreshCodes
        if (refresh) {
            page.setVar(VAR_REFRESH, state)
        }

        return state
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

        // handle page content
        if (!page.isCached) {
            // processPageContent(page, normUrl)
        }

        // handle cache
        if (!options.readonly) {
            if (page.isFetched && page.protocolStatus.isSuccess) {
                documentCache.remove(page.url)
            }
            pageCache.putDatum(page.url, page)
        }

        if (!page.isCached) {
            report(page)
        }

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
        // TODO: do not persist content if it's not changed, we can add a contentPage inside a WebPage
        // TODO: do not we persist if it's loaded from cache or no fields change
        if (!page.isCached && !options.readonly && options.persist) {
            persist(page, options)
        }
    }

    private fun parse(page: WebPage, options: LoadOptions): ParseResult? {
        val parser = parseComponent.takeIf { options.parse } ?: return null
        val parseResult = parser.parse(page, options.reparseLinks, options.noFilter)
        tracer?.trace("ParseResult: {} ParseReport: {}", parseResult, parser.getTraceInfo())

        return parseResult
    }

    /**
     * Because the content is large, a general webpage is up to 2M, so we do not load it from the database unless have to
     *
     * if the page is fetched, the content is set by the fetch component, so we do not load it from the database
     * if the protocol status is not success, the content is useless and not loaded
     * */
    private fun processPageContent(page: WebPage, normUrl: NormUrl) {
        val options = normUrl.options

        if (page.protocolStatus.isSuccess && page.content == null) {
            shouldBe(false, page.isFetched) { "Page should not be fetched | ${page.configuredUrl}" }
            // load the content of the page
            val contentPage = webDb.getOrNull(page.url, GWebPage.Field.CONTENT)
            if (contentPage != null) {
                page.content = contentPage.content
                // TODO: test the dirty flag
                page.unbox().clearDirty(GWebPage.Field.CONTENT.index)
            }
        }

        shouldBe(options.conf, page.conf) { "Conf should be the same \n${options.conf} \n${page.conf}" }
    }

    private fun report(page: WebPage) {
        if (logger.isInfoEnabled) {
            val verbose = logger.isDebugEnabled
            val report = LoadedPageFormatter(page, withSymbolicLink = verbose, withOptions = true).toString()
            taskLogger.info(report)
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
        globalCache.fetchingUrlQueue.add(page.url)
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
        globalCache.fetchingUrlQueue.remove(page.url)
    }

    /**
     * TODO: FetchSchedule.shouldFetch, crawlStatus and FetchReason should keep consistent
     * */
    private fun getFetchStateForExistPage(page: WebPage, options: LoadOptions): CheckState {
        // TODO: crawl status is better to decide the fetch reason
        val crawlStatus = page.crawlStatus
        val protocolStatus = page.protocolStatus

        if (options.refresh) {
            page.fetchRetries = 0
            return CheckState(FetchState.REFRESH, "refresh")
        }

        val ignoreFailure = options.ignoreFailure || options.retryFailed
        if (protocolStatus.isRetry) {
            return CheckState(FetchState.RETRY, "retry")
        } else if (protocolStatus.isFailed && !ignoreFailure) {
            // Failed to fetch the page last time, it might be caused by page is gone
            // in such case, do not fetch it even it it's expired, unless the retryFailed flag is set
            return CheckState(FetchState.DO_NOT_FETCH, "failed")
        }

        val now = Instant.now()

        // Fetch a page already fetched before if it's expired
        val prevFetchTime = page.prevFetchTime
        if (prevFetchTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            statusTracker?.messageWriter?.debugIllegalLastFetchTime(page)
        }

        // if (expireAt in prevFetchTime..now || now > prevFetchTime + expires), it's expired
        if (options.isExpired(prevFetchTime)) {
            return CheckState(FetchState.EXPIRED, "expired 1")
        }

        val duration = Duration.between(page.fetchTime, now)
        val days = duration.toDays()
        if (duration.toMillis() > 0 && days < 3) {
            return CheckState(FetchState.SCHEDULED, "scheduled")
        }

        if (page.persistContentLength == 0L) {
            // do not enable this feature by default
            // return CheckState(FetchState.NO_CONTENT, "no content")
        }

        if (page.persistContentLength < options.requireSize) {
            return CheckState(FetchState.SMALL_CONTENT, "small content")
        }

        val domStats = page.activeDomStats
        val (ni, na) = domStats["lastStat"] ?: ActiveDomStat()
        if (ni < options.requireImages) {
            return CheckState(FetchState.MISS_FIELD, "miss image")
        }
        if (na < options.requireAnchors) {
            return CheckState(FetchState.MISS_FIELD, "miss anchor")
        }

        return CheckState(FetchState.DO_NOT_FETCH, "unknown")
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

        require(page.isFetched)
    }

    private fun persist(page: WebPage, options: LoadOptions) {
        // Remove content if storingContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (!options.storeContent && page.content != null) {
            page.clearPersistContent()
        }

        // the content is loaded from cache, the content remains unchanged, do not persist it
        if (page.isCached) {
            page.unbox().clearDirty(GWebPage.Field.CONTENT.index)
            assert(!page.unbox().isContentDirty)
        }

        webDb.put(page)
        ++numWrite

        collectPersistMetrics(page)

        if (numWrite < 200) {
            flush()
        } else if (!options.lazyFlush || numWrite % 20 == 0) {
            flush()
        }
    }

    private fun collectPersistMetrics(page: WebPage) {
        val metrics = coreMetrics
        if (metrics != null) {
            metrics.persists.mark()
            val bytes = page.content?.array()?.size ?: 0
            if (bytes > 0) {
                metrics.contentPersists.mark()
                metrics.persistContentMBytes.inc(ByteUnitConverter.convert(bytes, "M").toLong())
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
