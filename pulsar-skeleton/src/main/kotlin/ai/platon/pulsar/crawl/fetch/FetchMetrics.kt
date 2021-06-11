package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.AbstractPulsarSession
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import com.google.common.collect.ConcurrentHashMultiset
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.net.MalformedURLException
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class FetchMetrics(
        private val messageWriter: MiscMessageWriter,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {

    companion object {
        var runningChromeProcesses = 0
        var usedMemory = 0L

        init {
            mapOf(
                "runningChromeProcesses" to Gauge { runningChromeProcesses },
                "usedMemory" to Gauge { Strings.readableBytes(usedMemory) },

                "pulsarSessionPageCacheHits" to Gauge { AbstractPulsarSession.pageCacheHits },

                "loadCompPageCacheHits" to Gauge { LoadComponent.pageCacheHits },
                "loadCompDbGets" to Gauge { LoadComponent.dbGetCount },
                "loadCompDbGets/s" to Gauge { LoadComponent.dbGetPerSec },

                "dbGets" to Gauge { WebDb.dbGetCount },
                "dbGetAveMillis" to Gauge { WebDb.dbGetAveMillis },
                "dbPuts" to Gauge { WebDb.dbPutCount },
                "dbPutAveMillis" to Gauge { WebDb.dbPutAveMillis },
            ).forEach { AppMetrics.reg.register(this, it.key, it.value) }
        }
    }

    private val log = LoggerFactory.getLogger(FetchMetrics::class.java)!!
    val groupMode = conf.getEnum(PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST)
    val maxHostFailureEvents = conf.getInt(FETCH_MAX_HOST_FAILURES, 20)
    private val systemInfo = SystemInfo()
    private val processor = systemInfo.hardware.processor
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

    private val registry = AppMetrics.reg

    val meterTotalNetworkIFsRecvMBytes = registry.meter(this, "totalNetworkIFsRecvMBytes")

    /**
     * The total bytes of page content of all success web pages
     * */
    val tasks = registry.meter(this, "tasks")
    val successTasks = registry.meter(this, "successTasks")
    val finishedTasks = registry.meter(this, "finishedTasks")
    val persists = registry.multiMetric(this, "persists")
    val contentPersists = registry.multiMetric(this, "contentPersists")
    val meterContentMBytes = registry.multiMetric(this, "contentBytes")
    val persistContentMBytes = registry.multiMetric(this, "persistContentMBytes")
    val meterContentBytes = registry.meter(this, "contentBytes")

    val histogramContentBytes = registry.histogram(this, "contentBytes")
    val pageImages = registry.histogram(this, "pageImages")
    val pageAnchors = registry.histogram(this, "pageAnchors")
    val pageNumbers = registry.histogram(this, "pageNumbers")
    val pageSmallTexts = registry.histogram(this, "pageSmallTexts")
    val pageHeights = registry.histogram(this, "pageHeights")

    val realTimeNetworkIFsRecvBytes get() = systemInfo.hardware.networkIFs.sumBy { it.bytesRecv.toInt() }
            .toLong().coerceAtLeast(0)
    /**
     * The total all bytes received by the hardware at the application startup
     * */
    val initNetworkIFsRecvBytes by lazy { realTimeNetworkIFsRecvBytes }
    /**
     * The total all bytes received by the hardware last read from system
     * */
    @Volatile
    var totalNetworkIFsRecvBytes = 0L

    /**
     * The total bytes received by the hardware from the application startup
     * */
    val networkIFsRecvBytes
        get() = (totalNetworkIFsRecvBytes - initNetworkIFsRecvBytes).coerceAtLeast(0L)
    val networkIFsRecvBytesPerSecond
        get() = networkIFsRecvBytes / elapsedTime.seconds.coerceAtLeast(1)
    val networkIFsRecvBytesPerPage
        get() = networkIFsRecvBytes / successTasks.count.coerceAtLeast(1)

    val successTasksPerSecond
        get() = successTasks.count / elapsedTime.seconds.coerceAtLeast(1)

    private var lastSystemInfoRefreshTime = 0L
    private val closed = AtomicBoolean()

    init {
        Files.readAllLines(PATH_UNREACHABLE_HOSTS).mapTo(unreachableHosts) { it }

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

    fun isReachable(url: String) = !isUnreachable(url)

    fun isUnreachable(url: String): Boolean {
        val host = URLUtil.getHost(url, groupMode) ?: return true
        return unreachableHosts.contains(host)
    }

    fun isFailed(url: String) = failedUrls.contains(url)

    fun trackFailedUrl(url: String) {
        failedUrls.add(url)
    }

    fun trackFailedUrls(urls: Collection<String>) {
        failedUrls.addAll(urls)
    }

    fun isTimeout(url: String): Boolean {
        return timeoutUrls.contains(url)
    }

    fun markTaskStart() {
        tasks.mark()
    }

    fun markTaskStart(size: Int) {
        tasks.mark(size.toLong())
    }

    /**
     * Available hosts statistics
     */
    fun trackSuccess(page: WebPage) {
        val url = page.url
        val host = URLUtil.getHost(url, groupMode) ?: throw MalformedURLException(url)

        // The host is reachable
        unreachableHosts.remove(host)
        failedHosts.remove(host)

        successTasks.mark()
        finishedTasks.mark()

        val bytes = page.contentLength
        histogramContentBytes.update(bytes)
        meterContentBytes.mark(bytes)
        meterContentMBytes.inc(ByteUnit.convert(bytes, "M").toLong())

        page.activeDomStats["lastStat"]?.apply {
            pageAnchors.update(na)
            pageImages.update(ni)
            pageNumbers.update(nnm)
            pageSmallTexts.update(nst)
            pageHeights.update(h)
        }

        val i = finishedTasks.count

        val period = when {
            i < 100 -> 20
            i < 10000 -> 30
            else -> 60 + 30 * (i % 3 - 1) // generate: 30, 60, 90, 30, 60, 90, ...
        }
        if (log.isInfoEnabled && tasks.count > 0L && i % period == 0L) {
            log.info(formatStatus())
        }

        val urlStats = urlStatistics.computeIfAbsent(host) { UrlStat(it) }

        ++urlStats.urls
        ++urlStats.pageViews

        // PageCategory pageCategory = CrawlFilter.sniffPageCategory(page);
        val pageCategory = page.pageCategory
        when {
            pageCategory.isIndex -> ++urlStats.indexUrls
            pageCategory.isDetail -> ++urlStats.detailUrls
            pageCategory.isMedia -> ++urlStats.mediaUrls
            pageCategory.isSearch -> ++urlStats.searchUrls
            pageCategory.isBBS -> ++urlStats.bbsUrls
            pageCategory.isTieBa -> ++urlStats.tiebaUrls
            pageCategory.isBlog -> ++urlStats.blogUrls
            pageCategory.isUnknown -> ++urlStats.unknownUrls
        }

        val depth = page.distance
        if (depth == 1) {
            ++urlStats.urlsFromSeed
        }

        if (url.length > maxUrlLength) {
            ++urlStats.urlsTooLong
            messageWriter.debugLongUrls(url)
        }

        urlStatistics[host] = urlStats
    }

    fun trackTimeout(url: String) {
        timeoutUrls.add(url)
    }

    fun trackMoved(url: String) {
        movedUrls.add(url)
    }

    /**
     * @param url The url
     * @return true if the host is unreachable
     */
    fun trackHostUnreachable(url: String, occurrences: Int = 1): Boolean {
        val host = URLUtil.getHost(url, groupMode)
        if (host == null || host.isEmpty()) {
            log.warn("Malformed url identified as gone | <{}>", url)
            return false
        }

        // special hosts
        if (host.contains("amazon.com")) {
            return false
        }

        if (unreachableHosts.contains(host)) {
            return false
        }

        failedHosts.add(host, occurrences)
        val failures = failedHosts.count(host)
        // Only the exception occurs for unknownHostEventCount, it's really add to the black list
        if (failures > maxHostFailureEvents) {
            unreachableHosts.add(host)
            log.info("Host unreachable | {} failures | {}", failures, host)
            return true
        }

        return false
    }

    fun countHostTasks(host: String): Int {
        val failedTasks = failedHosts.count(host)
        val (_, numUrls) = urlStatistics[host] ?: return failedTasks
        return numUrls + failedTasks
    }

    fun formatStatus(): String {
        if (tasks.count == 0L) return ""

        val seconds = elapsedTime.seconds.coerceAtLeast(1)
        val count = successTasks.count.coerceAtLeast(1)
        val bytes = meterContentBytes.count
        return String.format("Fetched total %d pages in %s(%.2f pages/s) successfully | content: %s, %s/s, %s/p" +
                " | net recv: %s, %s/s, %s/p | total net recv: %s",
                count,
                elapsedTime.readable(),
                successTasks.meanRate,
                Strings.readableBytes(bytes),
                Strings.readableBytes(bytes / seconds),
                Strings.readableBytes(bytes / count),
                Strings.readableBytes(networkIFsRecvBytes),
                Strings.readableBytes(networkIFsRecvBytesPerSecond),
                Strings.readableBytes(networkIFsRecvBytesPerPage),
                Strings.readableBytes(totalNetworkIFsRecvBytes))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (tasks.count > 0) {
                log.info(formatStatus())

                log.info("There are " + unreachableHosts.size + " unreachable hosts")
                AppFiles.logUnreachableHosts(this.unreachableHosts)

                logAvailableHosts()
            }
        }
    }

    fun updateSystemInfo() {
        kotlin.runCatching { updateSystemInfo0() }.onFailure { log.warn("Unexpected exception", it) }
    }

    @Throws(Exception::class)
    private fun updateSystemInfo0() {
        val currentTimeMillis = System.currentTimeMillis()
        if (Duration.ofMillis(currentTimeMillis - lastSystemInfoRefreshTime).seconds < 60) {
            return
        }
        lastSystemInfoRefreshTime = currentTimeMillis

        totalNetworkIFsRecvBytes = systemInfo.hardware.networkIFs.sumBy { it.bytesRecv.toInt() }.toLong()
                .coerceAtLeast(totalNetworkIFsRecvBytes)
        meterTotalNetworkIFsRecvMBytes.mark(totalNetworkIFsRecvBytes / 1024 / 1024)

        runningChromeProcesses = Runtimes.countSystemProcess("chrome")
        usedMemory = systemInfo.hardware.memory.total - systemInfo.hardware.memory.available
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
