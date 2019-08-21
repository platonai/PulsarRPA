package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.PulsarPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_MAX_URL_LENGTH
import ai.platon.pulsar.common.config.PulsarConstants.SEED_HOME_URL
import ai.platon.pulsar.common.config.PulsarConstants.URL_TRACKER_HOME_URL
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.common.collect.TreeMultiset
import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

class FetchTaskTracker(
        private val webDb: WebDb,
        private val metrics: MetricsSystem,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val groupMode = conf.getEnum(CapabilityTypes.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST)
    private val seedIndexer = WeakPageIndexer(SEED_HOME_URL, webDb)
    private val urlTrackerIndexer = WeakPageIndexer(URL_TRACKER_HOME_URL, webDb)
    /**
     * The limitation of url length
     */
    private var maxUrlLength: Int = conf.getInt(PARSE_MAX_URL_LENGTH, 1024)
    /**
     * Tracking statistics for each host
     */
    val hostStatistics = Collections.synchronizedMap(HashMap<String, FetchStatus>())
    /**
     * Tracking unreachable hosts
     */
    val unreachableHosts = Collections.synchronizedSet(HashSet<String>())
    /**
     * Tracking hosts who is failed to fetch tasks.
     * A host is considered to be a unreachable host if there are too many failure
     */
    val failedHostTracker = TreeMultiset.create<String>()
    val timeoutUrls = Collections.synchronizedSet(HashSet<CharSequence>())
    val failedUrls = Collections.synchronizedSet(HashSet<CharSequence>())
    val deadUrls = Collections.synchronizedSet(HashSet<CharSequence>())

    val totalTaskCount = AtomicInteger(0)
    val totalSuccessCount = AtomicInteger(0)

    val batchTaskCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())
    val batchSuccessCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())

    private val isClosed = AtomicBoolean()
    private val isReported = AtomicBoolean()

    init {
        timeoutUrls.addAll(urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE))
        failedUrls.addAll(urlTrackerIndexer.getAll(FAILED_URLS_PAGE))
        deadUrls.addAll(urlTrackerIndexer.getAll(DEAD_URLS_PAGE))

        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS))

        params.withLogger(LOG).info(true)
    }

    override fun getParams(): Params {
        return Params.of(
                "unreachableHosts", unreachableHosts.size,
                "maxUrlLength", maxUrlLength,
                "unreachableHostsPath", PATH_UNREACHABLE_HOSTS,
                "timeoutUrls", timeoutUrls.size,
                "failedUrls", failedUrls.size,
                "deadUrls", deadUrls.size
        )
    }

    fun isReachable(host: String): Boolean {
        return !unreachableHosts.contains(host)
    }

    fun isGone(host: String): Boolean {
        return unreachableHosts.contains(host)
    }

    fun isFailed(url: String): Boolean {
        return failedUrls.contains(url)
    }

    fun trackFailed(url: String) {
        failedUrls.add(url)
    }

    fun trackFailed(urls: Collection<String>) {
        failedUrls.addAll(urls)
    }

    fun isTimeout(url: String): Boolean {
        return timeoutUrls.contains(url)
    }

    fun trackTimeout(url: String) {
        timeoutUrls.add(url)
    }

    /**
     * @param url The url
     * @return True if the host is gone
     */
    fun trackHostGone(url: String): Boolean {
        val host = URLUtil.getHost(url, groupMode)
        if (host.isEmpty()) {
            LOG.warn("Malformed url | <{}>", url)
            return false
        }

        if (unreachableHosts.contains(host)) {
            return false
        }

        failedHostTracker.add(host)
        // Only the exception occurs for unknownHostEventCount, it's really add to the black list
        val failureHostEventCount = 3
        if (failedHostTracker.count(host) > failureHostEventCount) {
            LOG.info("Host unreachable : $host")
            unreachableHosts.add(host)
            // retune(true);
            return true
        }

        return false
    }

    /**
     * Available hosts statistics
     */
    fun trackSuccess(page: WebPage) {
        val url = page.url
        val host = URLUtil.getHost(url, groupMode)

        if (host.isEmpty()) {
            LOG.warn("Bad host in url : $url")
            return
        }

        val fetchStatus = hostStatistics.computeIfAbsent(host) { FetchStatus(it) }

        ++fetchStatus.urls

        // PageCategory pageCategory = CrawlFilter.sniffPageCategory(page);
        val pageCategory = page.pageCategory
        if (pageCategory.isIndex) {
            ++fetchStatus.indexUrls
        } else if (pageCategory.isDetail) {
            ++fetchStatus.detailUrls
        } else if (pageCategory.isMedia) {
            ++fetchStatus.mediaUrls
        } else if (pageCategory.isSearch) {
            ++fetchStatus.searchUrls
        } else if (pageCategory.isBBS) {
            ++fetchStatus.bbsUrls
        } else if (pageCategory.isTieBa) {
            ++fetchStatus.tiebaUrls
        } else if (pageCategory.isBlog) {
            ++fetchStatus.blogUrls
        } else if (pageCategory.isUnknown) {
            ++fetchStatus.unknownUrls
        }

        val depth = page.distance
        if (depth == 1) {
            ++fetchStatus.urlsFromSeed
        }

        if (url.length > maxUrlLength) {
            ++fetchStatus.urlsTooLong
            metrics.debugLongUrls(url)
        }

        ++fetchStatus.cookieView

        hostStatistics[host] = fetchStatus

        // The host is reachable
        unreachableHosts.remove(host)

        failedHostTracker.remove(host)
    }

    fun countHostTasks(host: String): Int {
        val failedTasks = failedHostTracker.count(host)
        val (_, numUrls) = hostStatistics[host] ?: return failedTasks
        return numUrls + failedTasks
    }

    fun getSeeds(mode: FetchMode, limit: Int): Set<String> {
        val pageNo = 1
        val now = Instant.now()
        return seedIndexer.getAll(pageNo)
                .asSequence()
                .mapNotNull { webDb.get(it.toString()) }
                .filter { it.fetchMode == mode && it.fetchTime.isBefore(now) }
                .take(limit).mapTo(HashSet()) { it.url }
    }

    fun commitLazyTasks(mode: FetchMode, urls: Collection<String>) {
        urls.map { WebPage.newWebPage(it) }.forEach { lazyFetch(it, mode) }

        // Urls with different fetch mode are indexed in different pages and the page number is just the enum ordinal
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        urlTrackerIndexer.indexAll(pageNo, CollectionUtils.collect(urls) { url -> url as CharSequence })
        webDb.flush()
    }

    fun getLazyTasks(mode: FetchMode): Set<CharSequence> {
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        return urlTrackerIndexer.getAll(pageNo)
    }

    fun takeLazyTasks(mode: FetchMode, n: Int): Set<CharSequence> {
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        return urlTrackerIndexer.takeN(pageNo, n)
    }

    fun commitTimeoutTasks(urls: Collection<CharSequence>, mode: FetchMode) {
        urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, urls)
    }

    fun takeTimeoutTasks(mode: FetchMode): Set<CharSequence> {
        return urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE)
    }

    private fun lazyFetch(page: WebPage, mode: FetchMode) {
        page.fetchMode = mode
        webDb.put(page)
    }

    fun report() {
        if (isReported.getAndSet(true)) {
            return
        }

        reportAndLogUnreachableHosts()
        reportAndLogAvailableHosts()
    }

    private fun reportAndLogUnreachableHosts() {
        LOG.info("There are " + unreachableHosts.size + " unreachable hosts")
        PulsarFiles.logUnreachableHosts(this.unreachableHosts)
    }

    private fun reportAndLogAvailableHosts() {
        val report = StringBuilder("# Total " + hostStatistics.size + " available hosts")
        report.append('\n')

        hostStatistics.values.sorted()
                .map { (hostName, urls, indexUrls, detailUrls, searchUrls, mediaUrls,
                               bbsUrls, blogUrls, tiebaUrls, _, urlsTooLong) ->
                    String.format("%40s -> %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s",
                            hostName,
                            "total : $urls",
                            "index : $indexUrls",
                            "detail : $detailUrls",
                            "search : $searchUrls",
                            "media : $mediaUrls",
                            "bbs : $bbsUrls",
                            "tieba : $tiebaUrls",
                            "blog : $blogUrls",
                            "long : $urlsTooLong")
                }.joinTo(report, "\n") { it }

        LOG.info(report.toString())
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        LOG.debug("[Destruction] Destructing TaskStatusTracker ...")
        LOG.info("Archive total {} timeout urls, {} failed urls and {} dead urls",
                timeoutUrls.size, failedUrls.size, deadUrls.size)

        report()

        // Trace failed tasks for further fetch as a background tasks
        if (timeoutUrls.isNotEmpty()) {
            urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, timeoutUrls)
        }

        if (failedUrls.isNotEmpty()) {
            urlTrackerIndexer.indexAll(FAILED_URLS_PAGE, failedUrls)
        }

        if (deadUrls.isNotEmpty()) {
            urlTrackerIndexer.indexAll(DEAD_URLS_PAGE, deadUrls)
        }

        urlTrackerIndexer.commit()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FetchTaskTracker::class.java)
        const val LAZY_FETCH_URLS_PAGE_BASE = 100
        const val TIMEOUT_URLS_PAGE = 1000
        const val FAILED_URLS_PAGE = 1001
        const val DEAD_URLS_PAGE = 1002
    }
}
