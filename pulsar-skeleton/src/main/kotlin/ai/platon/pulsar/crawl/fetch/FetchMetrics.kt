package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_MAX_URL_LENGTH
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.SharedMetricRegistries
import com.google.common.collect.ConcurrentHashMultiset
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FetchMetrics(
        private val metricsManagement: MetricsManagement,
        private val messageWriter: MessageWriter,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {

    private val log = LoggerFactory.getLogger(FetchMetrics::class.java)!!
    private val groupMode = conf.getEnum(CapabilityTypes.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST)
    private val systemInfo = SystemInfo()
    private val metricRegistry = SharedMetricRegistries.getDefault()
    /**
     * The limitation of url length
     */
    private var maxUrlLength: Int = conf.getInt(PARSE_MAX_URL_LENGTH, 1024)

    val startTime = Instant.now()
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    /**
     * Tracking statistics for each host
     */
    val urlStatistics = ConcurrentHashMap<String, UrlStat>()
    /**
     * Tracking unreachable hosts
     */
    val unreachableHosts = ConcurrentSkipListSet<String>()
    /**
     * Tracking hosts who is failed to fetch tasks.
     * A host is considered to be a unreachable host if there are too many failure
     */
    val timeoutUrls = ConcurrentSkipListSet<String>()
    val movedUrls = ConcurrentSkipListSet<String>()
    val failedUrls = ConcurrentSkipListSet<String>()
    val deadUrls = ConcurrentSkipListSet<String>()
    val failedHosts = ConcurrentHashMultiset.create<String>()

    private val tasks = AtomicInteger(0)
    private val successTasks = AtomicInteger(0)
    private val totalFinishedTasks = AtomicInteger(0)
    /**
     * The total bytes of page content of all success web pages
     * */
    private val contentBytes = AtomicLong()
    private val tasks0 = metricRegistry.counter(name(javaClass, "tasks"))
    private val successTasks0 = metricRegistry.counter(name(javaClass, "successTasks"))
    private val finishedTasks0 = metricRegistry.counter(name(javaClass, "finishedTasks"))
    private val contentBytes0 = metricRegistry.counter(name(javaClass, "contentBytes"))
    private val contentBytes1 = metricRegistry.histogram(name(javaClass, "contentBytes1"))

    /**
     * The total all bytes received by the hardware at the application startup
     * */
    private var initSystemNetworkBytesRecv = 0L
    /**
     * The total all bytes received by the hardware last read from system
     * */
    @Volatile
    private var systemNetworkBytesRecv = 0L

    val successTasksPerSecond
        get() = successTasks.get() / (0.1 + elapsedTime.seconds)
    /**
     * The total bytes received by the hardware from the application startup
     * */
    val networkBytesRecv
        get() = systemNetworkBytesRecv - initSystemNetworkBytesRecv
    val networkBytesRecvPerSecond
        get() = networkBytesRecv / elapsedTime.seconds
    val networkBytesRecvPerPage
        get() = networkBytesRecv / (1 + successTasks.get())
    val contentBytesPerSecond
        get() = contentBytes.get() / elapsedTime.seconds
    val contentBytesPerTask
        get() = contentBytes.get() / (1 + successTasks.get())

    private val closed = AtomicBoolean()

    init {
        Files.readAllLines(PATH_UNREACHABLE_HOSTS).mapTo(unreachableHosts) { it }

        initSystemNetworkBytesRecv = systemInfo.hardware.networkIFs.sumBy { it.bytesRecv.toInt() }.toLong()

        params.withLogger(log).info(true)
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

    fun markTaskStart() {
        tasks.incrementAndGet()
        tasks0.inc()
    }

    fun markTaskStart(size: Int) {
        tasks.addAndGet(size)
        tasks0.inc(size.toLong())
    }

    /**
     * Available hosts statistics
     */
    fun trackSuccess(page: WebPage) {
        successTasks.incrementAndGet()
        successTasks0.inc()

        val bytes = page.contentBytes.toLong()
        contentBytes.addAndGet(bytes)
        contentBytes0.inc(bytes)
        contentBytes1.update(bytes)

        val i = totalFinishedTasks.incrementAndGet()
        finishedTasks0.inc()
        if (i % 5 == 0) {
            updateNetworkTraffic()
        }

        if (log.isInfoEnabled && i % 20 == 0) {
            log.info(formatTraffic())
        }

        val url = page.url
        val host = URLUtil.getHost(url, groupMode)

        if (host == null || host.isEmpty()) {
            log.warn("Bad host in url : $url")
            return
        }

        val fetchStatus = urlStatistics.computeIfAbsent(host) { UrlStat(it) }

        ++fetchStatus.urls

        // PageCategory pageCategory = CrawlFilter.sniffPageCategory(page);
        val pageCategory = page.pageCategory
        when {
            pageCategory.isIndex -> {
                ++fetchStatus.indexUrls
            }
            pageCategory.isDetail -> {
                ++fetchStatus.detailUrls
            }
            pageCategory.isMedia -> {
                ++fetchStatus.mediaUrls
            }
            pageCategory.isSearch -> {
                ++fetchStatus.searchUrls
            }
            pageCategory.isBBS -> {
                ++fetchStatus.bbsUrls
            }
            pageCategory.isTieBa -> {
                ++fetchStatus.tiebaUrls
            }
            pageCategory.isBlog -> {
                ++fetchStatus.blogUrls
            }
            pageCategory.isUnknown -> {
                ++fetchStatus.unknownUrls
            }

            // The host is reachable
        }

        val depth = page.distance
        if (depth == 1) {
            ++fetchStatus.urlsFromSeed
        }

        if (url.length > maxUrlLength) {
            ++fetchStatus.urlsTooLong
            messageWriter.debugLongUrls(url)
        }

        ++fetchStatus.pageViews

        urlStatistics[host] = fetchStatus

        // The host is reachable
        unreachableHosts.remove(host)

        failedHosts.remove(host)
    }

    fun trackTimeout(url: String) {
        timeoutUrls.add(url)
    }

    fun trackMoved(url: String) {
        movedUrls.add(url)
    }

    /**
     * @param url The url
     * @return True if the host is gone
     */
    fun trackHostGone(url: String): Boolean {
        val host = URLUtil.getHost(url, groupMode)
        if (host == null || host.isEmpty()) {
            log.warn("Malformed url | <{}>", url)
            return false
        }

        if (unreachableHosts.contains(host)) {
            return false
        }

        failedHosts.add(host)
        // Only the exception occurs for unknownHostEventCount, it's really add to the black list
        val failureHostEventCount = 3
        if (failedHosts.count(host) > failureHostEventCount) {
            log.info("Host unreachable : $host")
            unreachableHosts.add(host)
            return true
        }

        return false
    }

    fun countHostTasks(host: String): Int {
        val failedTasks = failedHosts.count(host)
        val (_, numUrls) = urlStatistics[host] ?: return failedTasks
        return numUrls + failedTasks
    }

    fun updateNetworkTraffic() {
        systemNetworkBytesRecv = systemInfo.hardware.networkIFs.sumBy { it.bytesRecv.toInt() }.toLong()
    }

    fun formatTraffic(): String {
        return String.format("Fetched total %d pages in %s(%.2f pages/s) successfully | content: %s, %s/s, %s/p | net recv: %s, %s/s, %s/p | total net recv: %s",
                successTasks.get(),
                DateTimes.readableDuration(elapsedTime),
                successTasksPerSecond,
                Strings.readableBytes(contentBytes.get()),
                Strings.readableBytes(contentBytesPerSecond),
                Strings.readableBytes(contentBytesPerTask),
                Strings.readableBytes(networkBytesRecv),
                Strings.readableBytes(networkBytesRecvPerSecond),
                Strings.readableBytes(networkBytesRecvPerPage),
                Strings.readableBytes(systemNetworkBytesRecv))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.info(formatTraffic())

            log.info("There are " + unreachableHosts.size + " unreachable hosts")
            AppFiles.logUnreachableHosts(this.unreachableHosts)

            logAvailableHosts()
        }
    }

    private fun logAvailableHosts() {
        val report = StringBuilder("Total " + urlStatistics.size + " available hosts")
        report.append('\n')

        urlStatistics.values.sorted()
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

        log.info(report.toString())
    }
}
