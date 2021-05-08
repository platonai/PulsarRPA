package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.message.LoadedPagesFormatter
import ai.platon.pulsar.common.AppStatusTracker
import ai.platon.pulsar.common.PulsarParams.VAR_FETCH_REASON
import ai.platon.pulsar.common.PulsarParams.VAR_PREV_FETCH_TIME_BEFORE_UPDATE
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.LinkOptions.Companion.parse
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.common.urls.Urls.splitUrlArgs
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.model.ActiveDomStat
import ai.platon.pulsar.persist.model.WebPageFormatter
import org.apache.avro.util.Utf8
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

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
    val immutableConfig: ImmutableConfig,
) : AutoCloseable {
    companion object {
        private const val VAR_REFRESH = "refresh"
    }

    private val logger = LoggerFactory.getLogger(LoadComponent::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }

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
    ) : this(webDb, globalCache, fetchComponent, null, updateComponent, null, immutableConfig)

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
        var page = doLoad(normUrl)
        var n = normUrl.options.nJitRetry
        while (page.protocolStatus.isRetry && n-- > 0) {
            page = doLoad(normUrl)
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

    /**
     * Load a batch of urls with the specified options.
     *
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     *
     * If a page does not exists neither in local storage nor at the given remote location, [WebPage.NIL] is
     * returned
     *
     * @param normUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    fun loadAll(normUrls: Iterable<NormUrl>, options: LoadOptions): Collection<WebPage> {
        if (!isActive) {
            return listOf()
        }

        val startTime = Instant.now()

        val filteredUrls = normUrls.mapNotNullTo(HashSet()) { filterUrlToNull(it) }
        if (filteredUrls.isEmpty()) {
            return listOf()
        }

        val knownPages: MutableSet<WebPage> = mutableSetOf()
        val pendingUrls: MutableSet<String> = mutableSetOf()
        for (normUrl in filteredUrls) {
            val url = normUrl.spec
            val opt = normUrl.options
            var page = webDb.get(url, opt.shortenKey)
            val reason = fetchState(page, opt)
            tracer?.trace("Fetch reason: {} | {} {}", FetchState.toString(reason), url, opt)
            val status = page.protocolStatus

            when (reason) {
                FetchState.TEMP_MOVED -> {
                    // TODO: batch redirect
                    page = redirect(page, opt)
                    if (status.isSuccess) {
                        knownPages.add(page)
                    }
                }
                FetchState.DO_NOT_FETCH -> {
                    if (status.isSuccess) {
                        knownPages.add(page)
                    } else {
                        logger.warn("Don't fetch page with unknown reason {} | {} {}", status, url, opt)
                    }
                }
                in FetchState.refreshCodes -> pendingUrls.add(url)
                else -> logger.error("Unknown fetch reason {} | {} {}", reason, url, opt)
            }
        }

        if (pendingUrls.isEmpty()) {
            return knownPages
        }

        logger.debug("Fetching {} urls with options {}", pendingUrls.size, options)
        val updatedPages = try {
            globalCache.fetchingCache.addAll(pendingUrls)
            if (options.preferParallel) {
                fetchComponent.parallelFetchAll(pendingUrls, options)
            } else {
                fetchComponent.fetchAll(pendingUrls, options)
            }
        } finally {
            globalCache.fetchingCache.removeAll(pendingUrls)
        }.filter { it.isNotInternal }

        if (options.parse) {
            updatedPages.parallelStream().forEach { update(it, options) }
        } else {
            updatedPages.forEach { update(it, options) }
        }

        knownPages.addAll(updatedPages)

        val verbose = logger.isDebugEnabled
        lastPageReport = LoadedPagesFormatter(updatedPages, startTime, withSymbolicLink = verbose).toString()
        if (logger.isInfoEnabled) {
            logger.info(lastPageReport)
        }

        return knownPages
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

    private fun doLoad(normUrl: NormUrl): WebPage {
        val page = createLoadEntry(normUrl)

        require(page.isNotNil)
        assertSame(page.conf, normUrl.options.conf) { "Volatile config should be the same" }

        beforeLoad(page, normUrl.options)

        if (page.variables.remove(VAR_REFRESH) != null) {
            try {
                beforeFetch(page, normUrl.options)
                require(page.isNotInternal) { "Internal page ${page.url}" }

                fetchComponent.fetchContent(page)
                // require(page.args == normUrl.options.toString())
                // require(page.volatileConfig == normUrl.options.volatileConfig)
            } finally {
                afterFetch(page, normUrl.options)
            }
        }

        afterLoad(page, normUrl.options)

        return page
    }

    private suspend fun loadDeferred0(normUrl: NormUrl): WebPage {
        val page = createLoadEntry(normUrl)

        beforeLoad(page, normUrl.options)

        if (page.variables.remove(VAR_REFRESH) != null) {
            try {
                beforeFetch(page, normUrl.options)
                fetchComponent.fetchContentDeferred(page)
            } finally {
                afterFetch(page, normUrl.options)
            }
        }

        afterLoad(page, normUrl.options)

        return page
    }

    private fun createLoadEntry(normUrl: NormUrl): WebPage {
        if (normUrl.spec == AppConstants.NIL_PAGE_URL) {
            return WebPage.NIL
        }

        val url = normUrl.spec
        val options = normUrl.options
        if (globalCache.fetchingCache.contains(url)) {
            logger.takeIf { it.isDebugEnabled }?.debug("Page is being fetched | {}", url)
            return WebPage.NIL
        }

        // less than 1 seconds means do not check the database
        // NOTE: if we do not check the database, necessary information is lost
//        val page0 = if (options.expires.seconds > 1) {
//            webDb.get(url, options.ignoreQuery)
//        } else WebPage.NIL

        val page0 = webDb.get(url)

        val reason = fetchState(page0, options)
        val fetchEntry = if (page0.isNil) {
            FetchEntry(url, options, normUrl.hrefSpec)
        } else {
            FetchEntry(page0, options, normUrl.hrefSpec)
        }

        val page = fetchEntry.page

        tracer?.trace("Fetch reason: {}, url: {}, options: {}", FetchState.toString(reason), page.url, options)
        if (reason == FetchState.TEMP_MOVED) {
            return redirect(page, options)
        }

        val refresh = reason in FetchState.refreshCodes
        if (refresh) {
            page.variables[VAR_REFRESH] = reason
            page.variables[VAR_FETCH_REASON] = reason
        }

        return page
    }

    private fun beforeLoad(page: WebPage, options: LoadOptions) {
        try {
            page.loadEventHandler?.onBeforeLoad?.invoke(page.url)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke beforeLoad | ${page.configuredUrl}", e)
        }
    }

    private fun afterLoad(page: WebPage, options: LoadOptions) {
        if (options.parse) {
            parse(page, options)
        }

        if (options.persist) {
            persist(page, options)
        }

        try {
            page.loadEventHandler?.onAfterLoad?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke afterLoad | ${page.configuredUrl}", e)
        }
    }

    private fun beforeFetch(page: WebPage, options: LoadOptions) {
        // require(page.options == options)
        page.setVar(VAR_PREV_FETCH_TIME_BEFORE_UPDATE, page.prevFetchTime)
        globalCache.fetchingCache.add(page.url)
        logger.takeIf { it.isDebugEnabled }?.debug("Loading url | {} {}", page.url, page.args)
    }

    private fun afterFetch(page: WebPage, options: LoadOptions) {
        update(page, options)
        globalCache.fetchingCache.remove(page.url)

        if (logger.isInfoEnabled) {
            val verbose = logger.isDebugEnabled
            val report = LoadedPageFormatter(page, withSymbolicLink = verbose, withOptions = true).toString()
            logger.info(report)
        }
    }

    private fun filterUrlToNull(url: NormUrl): NormUrl? {
        return url.takeIf { filterUrlToNull(url.spec) != null }
    }

    private fun filterUrlToNull(url: String): String? {
        if (url.length <= AppConstants.SHORTEST_VALID_URL_LENGTH
            || url.contains(AppConstants.NIL_PAGE_URL)
            || url.contains(AppConstants.EXAMPLE_URL)
        ) {
            return null
        }

        when {
            globalCache.fetchingCache.contains(url) -> return null
            fetchMetrics?.isFailed(url) == true -> return null
            fetchMetrics?.isTimeout(url) == true -> {
            }
            // Might use UrlFilter/UrlNormalizer
        }

        // Might use UrlFilter/UrlNormalizer

        return Urls.getURLOrNull(url)?.toString()
    }

    /**
     * TODO: FetchSchedule.shouldFetch, crawlStatus and FetchReason should keep consistent
     *
     * 1. 1601 Retry -> 系统正在重试，最大重试次数为 3
     * 2. 410 Gone -> 系统重试失败，应用层继续判断是否需要强制重新采集。在出现 410 Gone 后，
     *    0. 加 -i 0s 告诉系统网页已经过期
     *    1. 如果不加其他参数，应用层每发送一次请求，强制重新采集一次，retry 计数 +1
     *    2. 如果需要激活内部重试，加参数 -fresh, -fresh 清除 retry 计数并忽略错误
     *    3. 如果 pageStatusCode 为 1601 和 410 之外的其他错误，需要加 -retryFailed 强制重新采集
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

    /**
     * NOTE: not used in browser mode, redirect inside a browser instead
     * */
    private fun redirect(page: WebPage, options: LoadOptions): WebPage {
        if (!isActive) {
            page.protocolStatus = ProtocolStatus.STATUS_CANCELED
            return page
        }

        if (page.protocolStatus.isCanceled) {
            return page
        }

        var p = page
        val reprUrl = p.reprUrl
        if (reprUrl.equals(p.url, ignoreCase = true)) {
            logger.warn("Invalid reprUrl, cyclic redirection, url: $reprUrl")
            return p
        }

        if (options.noRedirect) {
            logger.warn("Redirect is prohibit, url: $reprUrl")
            return p
        }

        // do not run into a rabbit hole, never redirects here
        options.noRedirect = true
        val redirectedPage = load(reprUrl, options)
        options.noRedirect = false
        if (options.hardRedirect) {
            p = redirectedPage
        } else {
            // soft redirect
            p.content = redirectedPage.content
        }

        return p
    }

    fun loadOutPages(links: List<GHypeLink>, start: Int, limit: Int, options: LoadOptions): List<WebPage> {
        if (start < 0) throw IllegalArgumentException("Argument start must be greater than 0")
        if (limit < 0) throw IllegalArgumentException("Argument limit must be greater than 0")

        val pages = links.asSequence().drop(start).take(limit).map { load(it, options) }
            .filter { it.protocolStatus.isSuccess }.toList()
        if (!options.lazyFlush) {
            flush()
        }
        return pages
    }

    /**
     * We load everything from the internet, our storage is just a cache
     * @param portalUrl The portal url we are scraping out pages from
     * @param loadArgs The load args of portal page
     * @param linkArgs The link args of portal page
     * @param start zero based start link to load
     * @param limit The limit of links to load
     * @param loadArgs2 The args of item pages
     * @param query The query on item pages
     * @param logLevel The log level
     * @return The scrape result
     */
    fun scrapeOutPages(
        portalUrl: String, loadArgs: String, linkArgs: String, start: Int, limit: Int, loadArgs2: String,
        query: String, logLevel: Int,
    ): Map<String, Any> {
        val conf = immutableConfig.toVolatileConfig()
        return scrapeOutPages(portalUrl, LoadOptions.parse(loadArgs, conf), parse(linkArgs),
            start, limit, LoadOptions.parse(loadArgs2, conf), query, logLevel)
    }

    /**
     * We load everything from the internet, our storage is just a cache
     * @param portalUrl The portal url we are scraping out pages from
     * @param loadArgs The load args of portal page
     * @param linkArgs The link args of portal page
     * @param start zero based start link to load
     * @param limit The limit of links to load
     * @param loadArgs2 The args of item pages
     * @param query The query on item pages
     * @param logLevel The log level
     * @return The scrape result
     */
    fun scrapeOutPages(
        portalUrl: String, options: LoadOptions,
        linkOptions: LinkOptions,
        start: Int, limit: Int, loadOptions2: LoadOptions,
        query: String,
        logLevel: Int,
    ): Map<String, Any> {
        if (!isActive) {
            return mapOf()
        }

        if (start < 0) throw IllegalArgumentException("Argument start must be greater than 0")
        if (limit < 0) throw IllegalArgumentException("Argument limit must be greater than 0")

        val persist = options.persist
        options.persist = false
        val persist2 = loadOptions2.persist
        loadOptions2.persist = false
        val page = load(portalUrl, options)
        var filteredLinks: List<GHypeLink> = emptyList()
        var outPages: List<WebPage> = emptyList()
        var outDocs = emptyList<Map<String, Any?>>()
        val counters = intArrayOf(0, 0, 0)
        if (page.protocolStatus.isSuccess) {
            filteredLinks = page.liveLinks.values.asSequence()
                .filter { l -> l.url.toString() != portalUrl }
                .filter { l -> !page.deadLinks.contains(Utf8(l.url.toString())) }
                .filter { linkOptions.asGHypeLinkPredicate().test(it) }
                .toList()
            loadOptions2.query = query
            outPages = loadOutPages(filteredLinks, start, limit, loadOptions2)
            outPages.map { it.pageCounters }.forEach {
                counters[0] += it.get(Self.missingFields)
                counters[1] += if (counters[0] > 0) 1 else 0
                counters[2] += it.get(Self.brokenSubEntity)
            }

            updateComponent.updateByOutgoingPages(page, outPages)

            if (persist) {
                webDb.put(page)
            }

            if (persist2) {
                outPages.forEach { webDb.put(it) }
            }

            // log.debug(page.getPageCounters().asStringMap().toString());
            outDocs = outPages.map {
                WebPageFormatter(it).withLinks(loadOptions2.withLinks).withText(loadOptions2.withText)
                    .withEntities(loadOptions2.withModel).toMap()
            }
        }

        // Metadata
        val response: MutableMap<String, Any> = LinkedHashMap()
        response["totalCount"] = filteredLinks.size
        response["count"] = outPages.size
        // Counters
        val refCounter: MutableMap<String, Int> = LinkedHashMap()
        refCounter["missingFields"] = counters[0]
        refCounter["brokenEntity"] = counters[1]
        refCounter["brokenSubEntity"] = counters[2]
        response["refCounter"] = refCounter
        // Main document
        response["doc"] = WebPageFormatter(page)
            .withLinks(options.withLinks)
            .withText(options.withText)
            .withEntities(options.withModel)
            .toMap()
        // Outgoing document
        response["docs"] = outDocs
        if (logLevel > 0) {
            options.persist = persist
            loadOptions2.persist = persist2
            response["debug"] = buildDebugInfo(logLevel, linkOptions, options, loadOptions2)
        }
        if (!options.lazyFlush) {
            flush()
        }
        return response
    }

    fun flush() = webDb.flush()

    override fun close() {
        closed.set(true)
    }

    private fun assertSame(a: Any?, b: Any?, lazyMessage: () -> String) {
        require(a === b) { lazyMessage() }
    }

    /**
     * Not stable
     */
    private fun buildDebugInfo(
        logLevel: Int, linkOptions: LinkOptions, options: LoadOptions, loadOptions2: LoadOptions,
    ): Map<String, String> {
        val debugInfo: MutableMap<String, String> = HashMap()
        val counter = intArrayOf(0)
        val linkReport = linkOptions.getReport().stream()
            .map { r: String -> (++counter[0]).toString() + ".\t" + r }.collect(Collectors.joining("\n"))
        debugInfo["logLevel"] = logLevel.toString()
        debugInfo["options"] = options.toString()
        debugInfo["loadOptions2"] = loadOptions2.toString()
        debugInfo["linkOptions"] = linkOptions.toString()
        debugInfo["linkReport"] = linkReport
        return debugInfo
    }
}
