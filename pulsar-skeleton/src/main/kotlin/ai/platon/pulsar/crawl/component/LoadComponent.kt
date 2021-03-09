package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.CompletedPageFormatter
import ai.platon.pulsar.common.message.LoadCompletedPagesFormatter
import ai.platon.pulsar.common.AppStatusTracker
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.LinkOptions.Companion.parse
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.common.url.Urls.splitUrlArgs
import ai.platon.pulsar.crawl.common.FetchReason
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
    val immutableConfig: ImmutableConfig
) : AutoCloseable {
    companion object {
        private const val VAR_REFRESH = "refresh"
    }

    private val log = LoggerFactory.getLogger(LoadComponent::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }

    private val fetchMetrics get() = fetchComponent.fetchMetrics
    private val closed = AtomicBoolean()
    /**
     * TODO: only check active before blocking calls
     * */
    private val isActive get() = !closed.get()
    @Volatile
    private var numWrite = 0
    private val abnormalPage get() = WebPage.NIL.takeIf { !isActive }

    constructor(
            webDb: WebDb,
            globalCache: GlobalCache,
            fetchComponent: BatchFetchComponent,
            updateComponent: UpdateComponent,
            immutableConfig: ImmutableConfig
    ) : this(webDb, globalCache, fetchComponent, null, updateComponent, null, immutableConfig)

    /**
     * Load an url, options can be specified following the url, see [LoadOptions] for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(configuredUrl: String): WebPage {
        val (first, second) = splitUrlArgs(configuredUrl)
        val options = LoadOptions.parse(second)
        return load(first, options)
    }

    /**
     * Load an url with specified options, see [LoadOptions] for all options
     *
     * @param originalUrl The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, [WebPage.NIL] is returned
     */
    fun load(originalUrl: String, options: String): WebPage {
        return load(originalUrl, LoadOptions.parse(options))
    }

    /**
     * Load an url with specified options
     * If there is no page in local storage nor at the given remote location, [WebPage.NIL] is returned
     *
     * @param originalUrl The url to load
     * @param options The options
     * @return The WebPage.
     */
    fun load(originalUrl: String, options: LoadOptions): WebPage {
        return abnormalPage ?: loadWithRetry(NormUrl(originalUrl, options))
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
            val reason = getFetchReason(page, opt)
            tracer?.trace("Fetch reason: {} | {} {}", FetchReason.toString(reason), url, opt)
            val status = page.protocolStatus

            when (reason) {
                FetchReason.NEW_PAGE,
                FetchReason.EXPIRED,
                FetchReason.SMALL_CONTENT,
                FetchReason.RETRY,
                FetchReason.MISS_FIELD -> pendingUrls.add(url)
                FetchReason.TEMP_MOVED -> {
                    // TODO: batch redirect
                    page = redirect(page, opt)
                    if (status.isSuccess) {
                        knownPages.add(page)
                    }
                }
                FetchReason.DO_NOT_FETCH -> {
                    if (status.isSuccess) {
                        knownPages.add(page)
                    } else {
                        log.warn("Don't fetch page with unknown reason {} | {} {}", status, url, opt)
                    }
                }
                else -> log.error("Unknown fetch reason {} | {} {}", reason, url, opt)
            }
        }

        if (pendingUrls.isEmpty()) {
            return knownPages
        }

        log.debug("Fetching {} urls with options {}", pendingUrls.size, options)
        val updatedPages = try {
            globalCache.fetchingUrls.addAll(pendingUrls)
            if (options.preferParallel) {
                fetchComponent.parallelFetchAll(pendingUrls, options)
            } else {
                fetchComponent.fetchAll(pendingUrls, options)
            }
        } finally {
            globalCache.fetchingUrls.removeAll(pendingUrls)
        }.filter { it.isNotInternal }

        if (options.parse) {
            updatedPages.parallelStream().forEach { update(it, options) }
        } else {
            updatedPages.forEach { update(it, options) }
        }

        knownPages.addAll(updatedPages)
        if (log.isInfoEnabled) {
            val verbose = log.isDebugEnabled
            log.info("{}", LoadCompletedPagesFormatter(updatedPages, startTime, withSymbolicLink = verbose))
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

        beforeLoad(page, normUrl.options)

        if (page.variables.remove(VAR_REFRESH) != null) {
            try {
                beforeFetch(page, normUrl.options)
                require(page.isNotInternal) { "Internal page ${page.url}" }
                fetchComponent.fetchContent(page)
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
        if (globalCache.fetchingUrls.contains(url)) {
            log.takeIf { it.isDebugEnabled }?.debug("Page is being fetched | {}", url)
            return WebPage.NIL
        }

        // less than 1 seconds means do not check the database
        val page0 = if (options.expires.seconds > 1) {
            webDb.get(url, options.ignoreQuery)
        } else WebPage.NIL

        val reason = getFetchReason(page0, options)
        val fetchEntry = if (page0.isNil) {
//            page = fetchComponent.createFetchEntry(url, options, normUrl.hrefSpec)
            FetchEntry(url, options, normUrl.hrefSpec)
        } else {
            FetchEntry(page0, options, normUrl.hrefSpec)
        }
        // set page variables like volatileConfig here
        // fetchComponent.initFetchEntry(page, options, normUrl.hrefSpec)

        val page = fetchEntry.page
        tracer?.trace("Fetch reason: {}, url: {}, options: {}", FetchReason.toString(reason), page.url, options)
        if (reason == FetchReason.TEMP_MOVED) {
            return redirect(page, options)
        }

        val refresh = reason == FetchReason.NEW_PAGE
                || reason == FetchReason.EXPIRED
                || reason == FetchReason.SMALL_CONTENT
                || reason == FetchReason.RETRY
                || reason == FetchReason.MISS_FIELD
        if (refresh) {
            page.variables[VAR_REFRESH] = refresh
        }

        return page
    }

    private fun beforeLoad(page: WebPage, options: LoadOptions) {
        page.loadEventHandler?.onBeforeLoad?.invoke(page.url)
    }

    private fun afterLoad(page: WebPage, options: LoadOptions) {
        if (options.parse) {
            parse(page, options)
        }

        if (options.persist) {
            persist(page, options)
        }

        page.loadEventHandler?.onAfterLoad?.invoke(page)
    }

    private fun beforeFetch(page: WebPage, options: LoadOptions) {
        globalCache.fetchingUrls.add(page.url)
    }

    private fun afterFetch(page: WebPage, options: LoadOptions) {
        update(page, options)

        if (log.isInfoEnabled) {
            val verbose = log.isDebugEnabled
            log.info(CompletedPageFormatter(page, withSymbolicLink = verbose).toString())
        }

        globalCache.fetchingUrls.remove(page.url)
    }

    private fun filterUrlToNull(url: NormUrl): NormUrl? {
        return url.takeIf { filterUrlToNull(url.spec) != null }
    }

    private fun filterUrlToNull(url: String): String? {
        if (url.length <= AppConstants.SHORTEST_VALID_URL_LENGTH
                || url.contains(AppConstants.NIL_PAGE_URL)
                || url.contains(AppConstants.EXAMPLE_URL)) {
            return null
        }

        when {
            globalCache.fetchingUrls.contains(url) -> return null
            fetchMetrics?.isFailed(url) == true -> return null
            fetchMetrics?.isTimeout(url) == true -> {
            }
            // Might use UrlFilter/UrlNormalizer
        }

        // Might use UrlFilter/UrlNormalizer

        return Urls.getURLOrNull(url)?.toString()
    }

    private fun getFetchReason(page: WebPage, options: LoadOptions): Int {
        val protocolStatus = page.protocolStatus

        return when {
            closed.get() -> FetchReason.DO_NOT_FETCH
            page.isNil -> FetchReason.NEW_PAGE
            page.isInternal -> FetchReason.DO_NOT_FETCH
            protocolStatus.isNotFetched -> FetchReason.NEW_PAGE
            protocolStatus.isTempMoved -> FetchReason.TEMP_MOVED
            else -> getFetchReasonForExistPage(page, options)
        }
    }

    private fun getFetchReasonForExistPage(page: WebPage, options: LoadOptions): Int {
        val protocolStatus = page.protocolStatus
        if (protocolStatus.isRetry) {
            return FetchReason.RETRY
        }

        // Failed to fetch the page last time, it might be caused by page is gone reason
        // in such case, do not fetch it even it it's expired, unless the -retry flag is set
        if (protocolStatus.isFailed && !options.retryFailed) {
            return FetchReason.DO_NOT_FETCH
        }

        // Fetch a page already fetched before if necessary
        val now = Instant.now()
        val lastFetchTime = page.getLastFetchTime(now)
        if (lastFetchTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            statusTracker?.messageWriter?.debugIllegalLastFetchTime(page)
        }

        // if (now > lastTime + expires), it's expired
        if (options.isExpired(lastFetchTime)) {
            return FetchReason.EXPIRED
        }

        if (page.contentLength < options.requireSize) {
            return FetchReason.SMALL_CONTENT
        }

        val domStats = page.activeDomStats
        val (ni, na) = domStats["lastStat"] ?: ActiveDomStat()
        if (ni < options.requireImages) {
            return FetchReason.MISS_FIELD
        }
        if (na < options.requireAnchors) {
            return FetchReason.MISS_FIELD
        }

        return FetchReason.DO_NOT_FETCH
    }

    /**
     * TODO: not used in browser mode, redirect inside a browser instead
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
            log.warn("Invalid reprUrl, cyclic redirection, url: $reprUrl")
            return p
        }

        if (options.noRedirect) {
            log.warn("Redirect is prohibit, url: $reprUrl")
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

    private fun update(page: WebPage, options: LoadOptions) {
        if (page.isInternal) {
            return
        }

        if (page.protocolStatus.isCanceled) {
            return
        }

        val protocolStatus = page.protocolStatus
        if (protocolStatus.isFailed) {
            updateComponent.updateFetchSchedule(page)
            return
        }

        updateComponent.updateFetchSchedule(page)
    }

    private fun parse(page: WebPage, options: LoadOptions) {
        parseComponent?.takeIf { options.parse }?.also {
            val parseResult = it.parse(page, options.query, options.reparseLinks, options.noFilter)
            tracer?.trace("ParseResult: {} ParseReport: {}", parseResult, it.getTraceInfo())
        }
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
                metrics.meterContentPersists.mark()
                metrics.meterPersistMBytes.mark(bytes.toLong() / 1024 / 1024)
            }
        }
        tracer?.trace("Persisted {} | {}", Strings.readableBytes(page.contentLength), page.url)
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
    fun scrapeOutPages(portalUrl: String, loadArgs: String, linkArgs: String, start: Int, limit: Int, loadArgs2: String,
                       query: String, logLevel: Int): Map<String, Any> {
        return scrapeOutPages(portalUrl, LoadOptions.parse(loadArgs), parse(linkArgs),
                start, limit, LoadOptions.parse(loadArgs2), query, logLevel)
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
            logLevel: Int): Map<String, Any> {
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

    /**
     * Not stable
     */
    private fun buildDebugInfo(
            logLevel: Int, linkOptions: LinkOptions, options: LoadOptions, loadOptions2: LoadOptions): Map<String, String> {
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
