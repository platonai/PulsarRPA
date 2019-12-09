package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls.isValidUrl
import ai.platon.pulsar.common.Urls.splitUrlArgs
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.LinkOptions.Companion.parse
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageFormatter
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.metadata.Name
import org.apache.avro.util.Utf8
import org.apache.commons.collections4.CollectionUtils
import org.apache.hadoop.classification.InterfaceStability.Evolving
import org.apache.hadoop.classification.InterfaceStability.Unstable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Created by vincent on 17-7-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 *
 * Load pages from storage or fetch from the Internet if it's not fetched or expired
 */
@Component
class LoadComponent(
        val webDb: WebDb,
        val fetchComponent: BatchFetchComponent,
        val parseComponent: ParseComponent,
        val updateComponent: UpdateComponent
) {
    companion object {
        val LOG = LoggerFactory.getLogger(LoadComponent::class.java)

        const val FETCH_REASON_DO_NOT_FETCH = 0
        const val FETCH_REASON_NEW_PAGE = 1
        const val FETCH_REASON_EXPIRED = 2
        const val FETCH_REASON_SMALL_CONTENT = 3
        const val FETCH_REASON_MISS_FIELD = 4
        const val FETCH_REASON_TEMP_MOVED = 300
        const val FETCH_REASON_RETRY_ON_FAILURE = 301

        val fetchReasonCodes = HashMap<Int, String>()
        private val globalFetchingUrls = Collections.synchronizedSet(HashSet<String>())
        fun getFetchReason(code: Int): String {
            return fetchReasonCodes.getOrDefault(code, "unknown")
        }

        init {
            fetchReasonCodes[FETCH_REASON_DO_NOT_FETCH] = "do_not_fetch"
            fetchReasonCodes[FETCH_REASON_NEW_PAGE] = "new_page"
            fetchReasonCodes[FETCH_REASON_EXPIRED] = "expired"
            fetchReasonCodes[FETCH_REASON_SMALL_CONTENT] = "small"
            fetchReasonCodes[FETCH_REASON_MISS_FIELD] = "miss_field"
            fetchReasonCodes[FETCH_REASON_TEMP_MOVED] = "temp_moved"
            fetchReasonCodes[FETCH_REASON_RETRY_ON_FAILURE] = "retry_on_failure"
        }
    }

    private val fetchTaskTracker = fetchComponent.fetchTaskTracker

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
        return loadOne(NormUrl(originalUrl, options))
    }

    fun load(url: URL, options: LoadOptions): WebPage {
        return loadOne(NormUrl(url, options))
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
        val page = load(link.url.toString(), options)
        page.setAnchor(link.anchor.toString())
        return page
    }

    fun load(normUrl: NormUrl): WebPage {
        return loadOne(normUrl)
    }

    /**
     * Load a batch of urls with the specified options.
     *
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
        val startTime = Instant.now()

//        val filteredUrls: Set<NormUrl> = HashSet()
//        CollectionUtils.collect(normUrls) { CollectionUtils.addIgnoreNull(filteredUrls, filterUrlToNull(it)) }

        val filteredUrls = normUrls.mapNotNullTo(HashSet()) { filterUrlToNull(it) }

        if (filteredUrls.isEmpty()) {
            return listOf()
        }
        val knownPages: MutableSet<WebPage> = HashSet()
        val pendingUrls: MutableSet<String> = HashSet()
        for (normUrl in filteredUrls) {
            val url = normUrl.url
            val opt = normUrl.options
            var page = webDb.getOrNil(url, opt.shortenKey)
            val reason = getFetchReason(page, opt)
            if (LOG.isTraceEnabled) {
                LOG.trace("Fetch reason: {} | {} {}", getFetchReason(reason), url, opt)
            }
            val status = page.protocolStatus
            when (reason) {
                FETCH_REASON_NEW_PAGE -> { pendingUrls.add(url) }
                FETCH_REASON_EXPIRED -> { pendingUrls.add(url) }
                FETCH_REASON_SMALL_CONTENT -> { pendingUrls.add(url) }
                FETCH_REASON_MISS_FIELD -> { pendingUrls.add(url) }
                FETCH_REASON_TEMP_MOVED -> {
                    // TODO: batch redirect
                    page = redirect(page, opt)
                    if (status.isSuccess) {
                        knownPages.add(page)
                    }
                }
                FETCH_REASON_DO_NOT_FETCH -> {
                    if (status.isSuccess) {
                        knownPages.add(page)
                    } else {
                        LOG.warn("Don't fetch page with unknown reason {} | {} {}", status, url, opt)
                    }
                }
                else -> {
                    LOG.error("Unknown fetch reason {} | {} {}", reason, url, opt)
                }
            }
        }

        if (pendingUrls.isEmpty()) {
            return knownPages
        }

        LOG.debug("Fetching {} urls with options {}", pendingUrls.size, options)
        val updatedPages: Collection<WebPage>
        updatedPages = try {
            globalFetchingUrls.addAll(pendingUrls)
            if (options.preferParallel) {
                fetchComponent.parallelFetchAll(pendingUrls, options)
            } else {
                fetchComponent.fetchAll(pendingUrls, options)
            }
        } finally {
            globalFetchingUrls.removeAll(pendingUrls)
        }

        updatedPages.forEach { update(it, options) }
        knownPages.addAll(updatedPages)
        if (LOG.isInfoEnabled) {
            logBatchComplete(updatedPages, startTime)
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

    private fun loadOne(normUrl: NormUrl): WebPage {
        if (normUrl.isInvalid) {
            LOG.warn("Malformed url | {}", normUrl)
            return WebPage.NIL
        }

        val url = normUrl.url
        val opt = normUrl.options
        if (globalFetchingUrls.contains(url)) {
            LOG.debug("Load later, it's fetching by someone else | {}", url)
            // TODO: wait for finish?
            return WebPage.NIL
        }

        val ignoreQuery = opt.shortenKey
        var page = webDb.getOrNil(url, ignoreQuery)
        val reason = getFetchReason(page, opt)
        LOG.trace("Fetch reason: {}, url: {}, options: {}", getFetchReason(reason), page.url, opt)
        if (reason == FETCH_REASON_TEMP_MOVED) {
            return redirect(page, opt)
        }

        val refresh = reason == FETCH_REASON_NEW_PAGE || reason == FETCH_REASON_EXPIRED
                || reason == FETCH_REASON_SMALL_CONTENT || reason == FETCH_REASON_MISS_FIELD
        if (refresh) {
            if (page.isNil) {
                page = WebPage.newWebPage(url, ignoreQuery)
            }
            page = fetchComponent.initFetchEntry(page, opt)
            // LOG.debug("Fetching: {} | FetchMode: {}", page.getConfiguredUrl(), page.getFetchMode());
            globalFetchingUrls.add(url)
            page = fetchComponent.fetchContent(page)
            globalFetchingUrls.remove(url)
            update(page, opt)
            if (LOG.isInfoEnabled) {
                LOG.info(getPageCompleteReport(page))
            }
        }

        return page
    }

    private fun filterUrlToNull(url: NormUrl): NormUrl? {
        val u = filterUrlToNull(url.url) ?: return null
        return NormUrl(u, url.options)
    }

    private fun filterUrlToNull(url: String): String? {
        if (url.length <= PulsarConstants.SHORTEST_VALID_URL_LENGTH
                || url.contains(PulsarConstants.NIL_PAGE_URL)
                || url.contains(PulsarConstants.EXAMPLE_URL)) {
            return null
        }
        if (globalFetchingUrls.contains(url)) {
            return null
        }
        if (fetchTaskTracker.isFailed(url)) {
            return null
        }
        if (fetchTaskTracker.isTimeout(url)) {
        }

        // Might use UrlFilter/UrlNormalizer

        return url.takeIf { isValidUrl(url) }
    }

    private fun getFetchReason(page: WebPage, options: LoadOptions): Int {
        val url = page.url
        if (page.isNil) {
            return FETCH_REASON_NEW_PAGE
        } else if (page.isInternal) {
            LOG.warn("Do not fetch, page is internal, {}", url)
            return FETCH_REASON_DO_NOT_FETCH
        }

        val protocolStatus = page.protocolStatus
        if (protocolStatus.isNotFetched) {
            return FETCH_REASON_NEW_PAGE
        } else if (protocolStatus.isTempMoved) {
            return FETCH_REASON_TEMP_MOVED
        } else if (protocolStatus.isFailed) {
            // Page is fetched last time, but failed, if retry is not allowed, just return the failed page
            if (!options.retry) {
                LOG.warn("Ignore failed page, last status: {} | {} {}", page.protocolStatus, page.url, page.options)
                return FETCH_REASON_DO_NOT_FETCH
            }
        }

        val now = Instant.now()
        val lastFetchTime = page.getLastFetchTime(now)
        if (lastFetchTime.isBefore(PulsarConstants.TCP_IP_STANDARDIZED_TIME)) {
            LOG.warn("Invalid last fetch time: {}, last status: {}", lastFetchTime, page.protocolStatus)
        }

        // if (now > lastTime + expires), it's expired
        if (now.isAfter(lastFetchTime.plus(options.expires))) {
            return FETCH_REASON_EXPIRED
        }
        if (page.contentBytes < options.requireSize) {
            return FETCH_REASON_SMALL_CONTENT
        }

        val jsData = page.browserJsData
        if (jsData != null) {
            val (ni, na) = jsData.lastStat
            if (ni < options.requireImages) {
                return FETCH_REASON_MISS_FIELD
            }
            if (na < options.requireAnchors) {
                return FETCH_REASON_MISS_FIELD
            }
        }
        return FETCH_REASON_DO_NOT_FETCH
    }

    private fun redirect(page_: WebPage, options: LoadOptions): WebPage {
        var page = page_
        val reprUrl = page.reprUrl
        if (reprUrl.equals(page.url, ignoreCase = true)) {
            LOG.warn("Invalid reprUrl, cyclic redirection, url: $reprUrl")
            return page
        }

        if (options.noRedirect) {
            LOG.warn("Redirect is prohibit, url: $reprUrl")
            return page
        }

        // do not run into a rabbit hole, never redirects here
        options.noRedirect = true
        val redirectedPage = load(reprUrl, options)
        options.noRedirect = false
        if (options.hardRedirect) { // soft redirect
            page = redirectedPage
        } else {
            page.setContent(redirectedPage.content)
        }

        return page
    }

    private fun update(page: WebPage, options: LoadOptions) {
        val protocolStatus = page.protocolStatus
        if (protocolStatus.isFailed) { // Follow redirection
            updateComponent.updateFetchSchedule(page)
            return
        }

        if (options.parse) {
            val parseResult = parseComponent.parse(page,
                    options.query,
                    options.reparseLinks,
                    options.noFilter)
            if (LOG.isTraceEnabled) {
                LOG.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getReport())
            }
        }

        updateComponent.updateFetchSchedule(page)

        if (!protocolStatus.isCanceled && options.persist) {
            webDb.put(page)
            if (!options.lazyFlush) {
                flush()
            }
            if (LOG.isTraceEnabled) {
                LOG.trace("Persisted {} | {}", StringUtil.readableByteCount(page.contentBytes.toLong()), page.url)
            }
        }
    }

    private fun logBatchComplete(pages: Collection<WebPage>, startTime: Instant) {
        val elapsed = DateTimeUtil.elapsedTime(startTime)
        val message = String.format("Fetched total %d pages in %s:\n", pages.size, DateTimeUtil.readableDuration(elapsed))
        val sb = StringBuilder(message)
        val i = AtomicInteger()
        pages.forEach { sb.append(i.incrementAndGet()).append(".\t").append(getPageCompleteReport(it)).append('\n') }
        LOG.info(sb.toString())
    }

    private fun getPageCompleteReport(page: WebPage): String {
        val bytes = page.contentBytes
        if (bytes < 0) {
            return ""
        }
        val responseTime = page.metadata[Name.RESPONSE_TIME]
        val proxy = page.metadata[Name.PROXY]
        val jsData = page.browserJsData
        var jsSate = ""
        if (jsData != null) {
            val (ni, na, nnm, nst) = jsData.lastStat
            jsSate = String.format(" i/a/nm/st:%d/%d/%d/%d", ni, na, nnm, nst)
        }
        val fmt = "Fetched%s in %8s" + (if (proxy == null) "%s" else "%26s") + ", fc:%2d %24s | %s"
        return String.format(fmt,
                StringUtil.readableByteCount(bytes.toLong(), 7, false),
                DateTimeUtil.readableDuration(responseTime),
                if (proxy == null) "" else " via $proxy",
                page.fetchCount,
                jsSate,
                page.url
        )
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