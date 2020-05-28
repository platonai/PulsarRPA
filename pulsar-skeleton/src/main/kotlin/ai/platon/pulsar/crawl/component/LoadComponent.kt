package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.Urls.splitUrlArgs
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.message.CompletedPageFormatter
import ai.platon.pulsar.common.message.LoadCompletedPagesFormatter
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.LinkOptions.Companion.parse
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.crawl.common.FetchReason
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.model.ActiveDomStat
import ai.platon.pulsar.persist.model.WebPageFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.avro.util.Utf8
import org.apache.hadoop.classification.InterfaceStability.Evolving
import org.apache.hadoop.classification.InterfaceStability.Unstable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
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
        val fetchComponent: BatchFetchComponent,
        val parseComponent: ParseComponent,
        val updateComponent: UpdateComponent,
        val messageWriter: MiscMessageWriter
): AutoCloseable {
    companion object {
        private const val VAR_REFRESH = "refresh"
        private val globalFetchingUrls = ConcurrentSkipListSet<String>()
        // TODO: a better way to manage page cache
        private val pageCache get() = PulsarContext.pageCache
    }

    private val log = LoggerFactory.getLogger(LoadComponent::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }

    private val fetchTaskTracker get() = fetchComponent.fetchMetrics

    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get() && PulsarEnv.isActive

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
        return WebPage.NIL.takeIf { !isActive } ?: load0(NormUrl(originalUrl, options))
    }

    fun load(url: URL, options: LoadOptions): WebPage {
        return WebPage.NIL.takeIf { !isActive } ?: load0(NormUrl(url, options))
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
        return WebPage.NIL.takeIf { !isActive } ?: load(link.url.toString(), options)
                .also { it.anchor = link.anchor.toString() }
    }

    fun load(normUrl: NormUrl): WebPage {
        return WebPage.NIL.takeIf { !isActive } ?: load0(normUrl)
    }

    suspend fun loadDeferred(normUrl: NormUrl): WebPage {
        return WebPage.NIL.takeIf { !isActive } ?: loadDeferred0(normUrl)
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
            val url = normUrl.url
            val opt = normUrl.options
            var page = webDb.getOrNil(url, opt.shortenKey)
            val reason = getFetchReason(page, opt)
            tracer?.trace("Fetch reason: {} | {} {}", FetchReason.toString(reason), url, opt)
            val status = page.protocolStatus

            when (reason) {
                FetchReason.NEW_PAGE,
                FetchReason.EXPIRED,
                FetchReason.SMALL_CONTENT,
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
            globalFetchingUrls.addAll(pendingUrls)
            if (options.preferParallel) {
                fetchComponent.parallelFetchAll(pendingUrls, options)
            } else {
                fetchComponent.fetchAll(pendingUrls, options)
            }
        } finally {
            globalFetchingUrls.removeAll(pendingUrls)
        }.filter { it.isNotInternal }

        if (options.parse) {
            updatedPages.parallelStream().forEach { update(it, options) }
        } else {
            updatedPages.forEach { update(it, options) }
        }

        knownPages.addAll(updatedPages)
        if (log.isInfoEnabled) {
            val verbose = log.isDebugEnabled
            log.info(LoadCompletedPagesFormatter(updatedPages, startTime, verbose).toString())
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

    private fun load0(normUrl: NormUrl): WebPage {
        val page = createLoadEntry(normUrl)
        if (page.variables.remove(VAR_REFRESH) != null) {
            try {
                beforeFetch(page, normUrl.options)
                require(page.isNotInternal) { "Internal page ${page.url}" }
                fetchComponent.fetchContent(page)
            } finally {
                afterFetch(page, normUrl.options)
            }
        }
        return page
    }

    private suspend fun loadDeferred0(normUrl: NormUrl): WebPage {
        val page = createLoadEntry(normUrl)
        if (page.variables.remove(VAR_REFRESH) != null) {
            try {
                beforeFetch(page, normUrl.options)
                require(page.isNotInternal) { "Internal page ${page.url}" }
                fetchComponent.fetchContentDeferred(page)
            } finally {
                afterFetch(page, normUrl.options)
            }
        }
        return page
    }

    private fun createLoadEntry(normUrl: NormUrl): WebPage {
        if (normUrl.isInvalid) {
            log.warn("Malformed url | {}", normUrl)
            return WebPage.NIL
        }
        if (normUrl.url == AppConstants.NIL_PAGE_URL) {
            return WebPage.NIL
        }

        val url = normUrl.url
        val options = normUrl.options
        if (globalFetchingUrls.contains(url)) {
            log.takeIf { it.isDebugEnabled }?.debug("Page is being fetched | {}", url)
            return WebPage.NIL
        }

        var page = webDb.getOrNil(url, options.ignoreQuery)
        val reason = getFetchReason(page, options)
        if (page.isNil) {
            page = fetchComponent.createFetchEntry(url, options)
        }

        tracer?.trace("Fetch reason: {}, url: {}, options: {}", FetchReason.toString(reason), page.url, options)
        if (reason == FetchReason.TEMP_MOVED) {
            return redirect(page, options)
        }

        val refresh = reason == FetchReason.NEW_PAGE || reason == FetchReason.EXPIRED
                || reason == FetchReason.SMALL_CONTENT || reason == FetchReason.MISS_FIELD
        if (refresh) {
            page.variables[VAR_REFRESH] = refresh
        }

        return page
    }

    private fun beforeFetch(page: WebPage, options: LoadOptions) {
        globalFetchingUrls.add(page.url)
        fetchComponent.initFetchEntry(page, options)
    }

    private fun afterFetch(page: WebPage, options: LoadOptions) {
        update(page, options)

        if (log.isInfoEnabled && isActive) {
            val verbose = log.isDebugEnabled
            log.info(CompletedPageFormatter(page, verbose).toString())
        }

        globalFetchingUrls.remove(page.url)
    }

    private fun filterUrlToNull(url: NormUrl): NormUrl? {
        val u = filterUrlToNull(url.url) ?: return null
        return NormUrl(u, url.options)
    }

    private fun filterUrlToNull(url: String): String? {
        if (url.length <= AppConstants.SHORTEST_VALID_URL_LENGTH
                || url.contains(AppConstants.NIL_PAGE_URL)
                || url.contains(AppConstants.EXAMPLE_URL)) {
            return null
        }

        when {
            globalFetchingUrls.contains(url) -> return null
            fetchTaskTracker.isFailed(url) -> return null
            fetchTaskTracker.isTimeout(url) -> {
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
            protocolStatus.isFailed && !options.retryFailed -> FetchReason.DO_NOT_FETCH
            else -> getFetchReasonForExistPage(page, options)
        }
    }

    private fun getFetchReasonForExistPage(page: WebPage, options: LoadOptions): Int {
        // Fetch a page already fetched before if necessary
        val now = Instant.now()
        val lastFetchTime = page.getLastFetchTime(now)
        if (lastFetchTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            messageWriter.debugIllegalLastFetchTime(page)
        }

        // if (now > lastTime + expires), it's expired
        if (now.isAfter(lastFetchTime.plus(options.expires))) {
            return FetchReason.EXPIRED
        }

        if (page.contentBytes < options.requireSize) {
            return FetchReason.SMALL_CONTENT
        }

        val domStatus = page.activeDomMultiStatus
        if (domStatus != null) {
            val (ni, na) = domStatus.lastStat ?: ActiveDomStat()
            if (ni < options.requireImages) {
                return FetchReason.MISS_FIELD
            }
            if (na < options.requireAnchors) {
                return FetchReason.MISS_FIELD
            }
        }

        return FetchReason.DO_NOT_FETCH
    }

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

        pageCache.put(page.url, page)

        val protocolStatus = page.protocolStatus
        if (protocolStatus.isFailed) {
            updateComponent.updateFetchSchedule(page)
            return
        }

        if (options.parse) {
            val parseResult = parseComponent.parse(page,
                    options.query, options.reparseLinks, options.noFilter)
            if (log.isTraceEnabled) {
                log.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getReport())
            }
        }

        updateComponent.updateFetchSchedule(page)

        if (options.persist) {
            webDb.put(page)
            if (!options.lazyFlush) {
                flush()
            }
            if (log.isTraceEnabled) {
                log.trace("Persisted {} | {}", Strings.readableBytes(page.contentBytes.toLong()), page.url)
            }
        }
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @Evolving
    fun loadOutPages(url: String, loadArgs: String, linkArgs: String, start: Int, limit: Int, loadArgs2: String,
            query: String, logLevel: Int): Map<String, Any> {
        return loadOutPages(url, LoadOptions.parse(loadArgs), parse(linkArgs),
                start, limit, LoadOptions.parse(loadArgs2), query, logLevel)
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @Evolving
    fun loadOutPages(
            url: String, options: LoadOptions,
            linkOptions: LinkOptions,
            start: Int, limit: Int, loadOptions2: LoadOptions,
            query: String,
            logLevel: Int): Map<String, Any> {
        if (!isActive) {
            return mapOf()
        }

        val persist = options.persist
        options.persist = false
        val persist2 = loadOptions2.persist
        loadOptions2.persist = false
        val page = load(url, options)
        var filteredLinks: List<GHypeLink> = emptyList()
        var outPages: List<WebPage> = emptyList()
        var outDocs = emptyList<Map<String, Any?>>()
        val counters = intArrayOf(0, 0, 0)
        if (page.protocolStatus.isSuccess) {
            filteredLinks = page.liveLinks.values
                    .filter { l -> l.url.toString() != url }
                    .filter { l -> !page.deadLinks.contains(Utf8(l.url.toString())) }
                    .filter { linkOptions.asGHypeLinkPredicate().test(it) }
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

    fun loadOutPages(links: List<GHypeLink>, start: Int, limit: Int, options: LoadOptions): List<WebPage> {
        val pages = links.stream()
                .skip(if (start > 1) (start - 1).toLong() else 0.toLong())
                .limit(limit.toLong())
                .map { l: GHypeLink -> load(l, options) }
                .filter { p: WebPage -> p.protocolStatus.isSuccess }
                .collect(Collectors.toList())
        if (!options.lazyFlush) {
            flush()
        }
        return pages
    }

    fun flush() {
        webDb.flush()
    }

    override fun close() {
        closed.set(true)
    }

    /**
     * Not stable
     */
    @Unstable
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
