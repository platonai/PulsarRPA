package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.AppStatusTracker
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.PulsarParams.VAR_FETCH_STATE
import ai.platon.pulsar.common.PulsarParams.VAR_PREV_FETCH_TIME_BEFORE_UPDATE
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.measure.ByteUnitConverter
import ai.platon.pulsar.common.message.LoadStatusFormatter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.loadEvent
import ai.platon.pulsar.common.urls.NormURL
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.url.CompletableHyperlink
import ai.platon.pulsar.crawl.common.url.toCompletableListenableHyperlink
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.persist.MutableWebPage
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.model.ActiveDOMStat
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by vincent on 17-7-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Load pages from storage or fetch from the Internet if it's not fetched or expired
 */
class LoadComponent(
    val webDb: WebDb,
    val globalCacheFactory: GlobalCacheFactory,
    val fetchComponent: BatchFetchComponent,
    val parseComponent: ParseComponent,
    val updateComponent: UpdateComponent,
    val immutableConfig: ImmutableConfig,
    val statusTracker: AppStatusTracker? = null,
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

    private val coreMetrics get() = fetchComponent.coreMetrics
    private val closed = AtomicBoolean()

    private val isActive get() = !closed.get()

    @Volatile
    private var numWrite = 0
    private val abnormalPage get() = MutableWebPage.NIL.takeIf { !isActive }

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

    fun load(url: URL, options: LoadOptions): WebPage {
        return abnormalPage ?: loadWithRetry(NormURL(url, options))
    }

    fun load(normURL: NormURL): WebPage {
        return abnormalPage ?: loadWithRetry(normURL)
    }

    suspend fun loadDeferred(normURL: NormURL): WebPage {
        return abnormalPage ?: loadWithRetryDeferred(normURL)
    }

    fun loadWithRetry(normURL: NormURL): WebPage {
        if (normURL.isNil) {
            return MutableWebPage.NIL
        }

        var page = load0(normURL)
        var n = normURL.options.nJitRetry
        while (page.protocolStatus.isRetry && n-- > 0) {
            page = load0(normURL)
        }
        return page
    }

    suspend fun loadWithRetryDeferred(normURL: NormURL): WebPage {
        if (normURL.isNil) {
            return MutableWebPage.NIL
        }

        var page = loadDeferred0(normURL)
        var n = normURL.options.nJitRetry
        while (page.protocolStatus.isRetry && n-- > 0) {
            page = loadDeferred0(normURL)
        }
        return page
    }

    /**
     * Load all pages specified by [normURLs], wait until all pages are loaded or timeout
     * */
    fun loadAll(normURLs: Iterable<NormURL>): List<WebPage> {
        if (!normURLs.iterator().hasNext()) {
            return listOf()
        }

        val futures = loadAllAsync(normURLs.filter { !it.isNil })

        logger.info("Waiting for {} completable links | @{}", futures.size, futures.hashCode())

        val future = CompletableFuture.allOf(*futures.toTypedArray())
        future.join()

        val pages = futures.mapNotNull { it.get().takeIf { it.isNotNil } }

        logger.info("Finished {}/{} pages | @{}", pages.size, futures.size, futures.hashCode())

        return pages
    }

    /**
     * Load a page specified by [normURL]
     *
     * @param normURL The normalized url
     * @return A completable future of webpage
     * */
    fun loadAsync(normURL: NormURL): CompletableFuture<WebPage> {
        val link = normURL.toCompletableListenableHyperlink()
        globalCache.urlPool.add(link)
        return link
    }

    /**
     * Load all pages specified by [normURLs], wait until all pages are loaded or timeout
     * */
    fun loadAllAsync(normURLs: Iterable<NormURL>): List<CompletableFuture<WebPage>> {
        if (!normURLs.iterator().hasNext()) {
            return listOf()
        }

        val linkFutures = normURLs.asSequence().filter { !it.isNil }.distinctBy { it.spec }
            .map { it.toCompletableListenableHyperlink() }
            .toList()
        globalCache.urlPool.addAll(linkFutures)
        return linkFutures
    }

    private fun load0(normURL: NormURL): WebPage {
        val page = createPageShell(normURL)
        return load1(normURL, page)
    }

    private fun load1(normURL: NormURL, page: MutableWebPage): WebPage {
        onWillLoad(normURL, page)

        fetchContentIfNecessary(normURL, page)

        onLoaded(page, normURL)

        return page
    }

    private suspend fun loadDeferred0(normURL: NormURL): WebPage {
        val page = createPageShell(normURL)
        return loadDeferred1(normURL, page)
    }

    private suspend fun loadDeferred1(normURL: NormURL, page: MutableWebPage): WebPage {
        onWillLoad(normURL, page)

        fetchContentIfNecessaryDeferred(normURL, page)

        onLoaded(page, normURL)

        return page
    }

    private fun fetchContentIfNecessary(normURL: NormURL, page: MutableWebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContent(page, normURL)
        }
    }

    private suspend fun fetchContentIfNecessaryDeferred(normURL: NormURL, page: MutableWebPage) {
        if (page.removeVar(VAR_REFRESH) != null) {
            fetchContentDeferred(page, normURL)
        }
    }

    /**
     * Create a page shell, the page shell is the process unit for most tasks.
     * */
    private fun createPageShell(normURL: NormURL): MutableWebPage {
        val cachedPage = getCachedPageOrNull(normURL)
        var page = FetchEntry.createPageShell(normURL)

        if (cachedPage != null) {
            pageCacheHits.incrementAndGet()
            page.isCached = true
            // the cached page can be or not be persisted, but not guaranteed
            // if a page is loaded from cache, the content remains unchanged and should not persist to database
            // TODO: clone the underlying data or not?
            page.unsafeCloneGPage(cachedPage as MutableWebPage)
            page.clearPersistContent()

            page.tmpContent = cachedPage.content

            // TODO: test the dirty flag
            // do not persist this copy
            page.unbox().clearDirty()
            assert(!page.isFetched)
            assert(page.isNotInternal)
        } else {
            // get the metadata of the page from the database, this is very fast for a crawler
            // TODO: two step loading or one step loading?
//            val loadedPage = webDb.getOrNull(normURL.spec, fields = metadataFields)
            val loadedPage = webDb.getOrNull(normURL.spec)
            dbGetCount.incrementAndGet()
            if (loadedPage != null) {
                // override the old variables: args, href, etc
                FetchEntry.initWebPage(loadedPage, normURL.options, normURL.hrefSpec)
                page = loadedPage
            }

            initFetchState(normURL, page, loadedPage)
        }

        return page
    }

    private fun initFetchState(normURL: NormURL, page: MutableWebPage, loadedPage: WebPage?): CheckState {
        val options = normURL.options

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

    private fun onWillLoad(normURL: NormURL, page: WebPage) {
        val options = normURL.options

        shouldBe(options.conf, page.conf) { "Conf should be the same \n${options.conf} \n${page.conf}" }

        try {
            page.loadEvent?.onWillLoad?.invoke(page.url)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke beforeLoad | ${page.configuredUrl}", e)
        }
    }

    private fun onLoaded(page: MutableWebPage, normURL: NormURL) {
        val options = normURL.options
        val status = page.protocolStatus

        // handle page content
        if (!page.isCached) {
            // processPageContent(page, normURL)
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

        // We might use the cached page's content in parse phase
        // TODO: what if is canceled?
        if (options.parse) {
            parse(page, normURL.options)
        }

        try {
            val detail = normURL.detail
            // we might use the cached page's content in after load handler
            if (detail is CompletableHyperlink<*>) {
                require(page.loadEvent?.onLoaded?.isNotEmpty == true) {
                    "A completable link must have a onLoaded handler"
                }
            }

            page.loadEvent?.onLoaded?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onLoaded | ${page.configuredUrl}", e)
        }

        if (options.persist && !page.isCanceled && !options.readonly) {
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
    private fun processPageContent(page: MutableWebPage, normURL: NormURL) {
        val options = normURL.options

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
        if (taskLogger.isInfoEnabled) {
            val verbose = taskLogger.isDebugEnabled
            val report = LoadStatusFormatter(page, withSymbolicLink = verbose, withOptions = true).toString()
            taskLogger.info(report)
        }
    }

    private fun getCachedPageOrNull(normURL: NormURL): WebPage? {
        val (url, options) = normURL
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

            // page.conf = normURL.options.conf
            // page.args = normURL.args

            return cachedPage
        }

        return null
    }

    private fun beforeFetch(page: MutableWebPage, options: LoadOptions) {
        // require(page.options == options)
        page.setVar(VAR_PREV_FETCH_TIME_BEFORE_UPDATE, page.prevFetchTime)
        globalCache.fetchingCache.add(page.url)
        logger.takeIf { it.isDebugEnabled }?.debug("Loading url | {} {}", page.url, page.args)
    }

    private fun fetchContent(page: MutableWebPage, normURL: NormURL) {
        try {
            beforeFetch(page, normURL.options)

            require(page.conf == normURL.options.conf)
//            require(normURL.options.eventHandler != null)
//            require(page.conf.getBeanOrNull(PulsarEventHandler::class) != null)

            fetchComponent.fetchContent(page)
        } finally {
            afterFetch(page, normURL.options)
        }
    }

    private suspend fun fetchContentDeferred(page: MutableWebPage, normURL: NormURL) {
        try {
            beforeFetch(page, normURL.options)
            fetchComponent.fetchContentDeferred(page)
        } finally {
            afterFetch(page, normURL.options)
        }
    }

    private fun afterFetch(page: WebPage, options: LoadOptions) {
        // the metadata of the page is loaded from database but the content is not cached,
        // so load the content again
        updateFetchSchedule(page, options)
        globalCache.fetchingCache.remove(page.url)
    }

    /**
     * TODO: FetchSchedule.shouldFetch, crawlStatus and FetchReason should keep consistent
     * */
    private fun getFetchStateForExistPage(page: WebPage, options: LoadOptions): CheckState {
        // TODO: crawl status is better to decide the fetch reason
        val crawlStatus = page.crawlStatus
        val protocolStatus = page.protocolStatus

        if (options.refresh) {
            (page as? MutableWebPage)?.fetchRetries = 0
            return CheckState(FetchState.REFRESH, "refresh")
        }

        val ignoreFailure = options.ignoreFailure
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

        // no content
        if (page.persistedContentLength == 0L) {
            // do not enable this feature by default
            // return CheckState(FetchState.NO_CONTENT, "no content")
        }

        if (page.persistedContentLength < options.requireSize) {
            return CheckState(FetchState.SMALL_CONTENT, "small content")
        }

        val domStats = page.activeDOMStatTrace
        val (ni, na) = domStats["lastStat"] ?: ActiveDOMStat()
        if (ni < options.requireImages) {
            return CheckState(FetchState.MISS_FIELD, "miss image")
        }
        if (na < options.requireAnchors) {
            return CheckState(FetchState.MISS_FIELD, "miss anchor")
        }

        return CheckState(FetchState.DO_NOT_FETCH, "unknown")
    }

    private fun updateFetchSchedule(page: WebPage, options: LoadOptions) {
        if (page.isInternal) {
            logger.warn("Unexpected internal page [updateFetchSchedule]")
            return
        }

        // canceled or loaded from database, do not update fetch schedule
        if (page.isCanceled || !page.isFetched) {
            return
        }

        updateComponent.updateFetchSchedule(page)

        require(page.isFetched)
    }

    private fun persist(page: MutableWebPage, options: LoadOptions) {
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
        tracer?.trace("Persisted {} | {}", Strings.compactFormat(page.contentLength), page.url)
    }

    fun flush() = webDb.flush()

    override fun close() {
        closed.compareAndSet(false, true)
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
