package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.crawl.fetch.indexer.JITIndexer
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

class TaskScheduler(
        val tasksMonitor: TaskMonitor,
        val metricsSystem: MetricsSystem,
        val pageParser: PageParser,
        val jitIndexer: JITIndexer,
        val conf: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {
    data class Status(
            var pagesThoRate: Double,
            var bytesThoRate: Double,
            var readyFetchItems: Int,
            var pendingFetchItems: Int
    )

    private val log = LoggerFactory.getLogger(FetchMonitor::class.java)
    val id: Int = objectSequence.getAndIncrement()
    private val metricsCounters = MetricsCounters()

    private val fetchResultQueue = ConcurrentLinkedQueue<FetchJobForwardingResponse>()

    /**
     * Our own Hardware bandwidth in mbytes, if exceed the limit, slows down the task scheduling.
     */
    private var bandwidth = 1024 * 1024 * conf.getInt(FETCH_NET_BANDWIDTH_M, BANDWIDTH_INFINITE_M)
    private var skipTruncated = conf.getBoolean(PARSE_SKIP_TRUNCATED, true)
    private var storingContent = conf.getBoolean(FETCH_STORE_CONTENT, true)

    // Indexer
    private var indexJIT: Boolean = false
    // Parser setting
    private var parse: Boolean = false

    // Timer
    private val startTime = Instant.now()!! // Start time of fetcher run
    var lastTaskStartTime = startTime
        private set
    var lastTaskFinishTime = startTime
        private set

    // Statistics
    private val totalBytes = AtomicLong(0)     // total fetched bytes
    private val totalPages = AtomicInteger(0)  // total fetched pages
    private val fetchErrors = AtomicInteger(0) // total fetch fetchErrors

    var averagePageThroughput = 0.01
        private set
    var averageBytesThroughput = 0.01
        private set
    var averagePageSize = 0.0
        private set

    /**
     * Output
     */
    private var outputDir = AppPaths.REPORT_DIR

    /**
     * The reprUrl is the representative url of a redirect, we save a reprUrl for each thread
     * We use a concurrent skip list map to gain the best concurrency
     *
     * TODO : check why we store a reprUrl for each thread?
     */
    private val reprUrls = ConcurrentSkipListMap<Long, String>()

    override fun setup(jobConf: ImmutableConfig) {
        indexJIT = jobConf.getBoolean(INDEXER_JIT, false)
        // Parser setting
        parse = indexJIT || conf.getBoolean(PARSE_PARSE, true)

        log.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "id", id,
                "bandwidth", StringUtil.readableByteCount(bandwidth.toLong()),
                "skipTruncated", skipTruncated,
                "parse", parse,
                "storingContent", storingContent,
                "indexJIT", indexJIT,
                "outputDir", outputDir
        )
    }

    val name: String get() = javaClass.simpleName + "-" + id

    fun produce(result: FetchJobForwardingResponse) {
        fetchResultQueue.add(result)
    }

    /**
     * Schedule a queue with the given priority and given poolId
     */
    fun schedule(poolId: PoolId? = null): FetchTask? {
        val fetchTasks = schedule(poolId, 1)
        return fetchTasks.firstOrNull()
    }

    /**
     * Schedule the queues with top priority
     */
    fun schedule(number: Int): List<FetchTask> {
        return schedule(null, number)
    }

    /**
     * Null queue id means the queue with top priority
     * Consume a fetch item and try to download the target web page
     */
    fun schedule(poolId: PoolId?, number: Int): List<FetchTask> {
        var num = number
        if (num <= 0) {
            log.warn("Required no fetch item")
            return ArrayList()
        }

        if (tasksMonitor.pendingTaskCount() * averagePageSize * 8.0 > 30 * this.bandwidth) {
            log.info("Bandwidth exhausted, slows down the scheduling")
            return listOf()
        }

        val fetchTasks = ArrayList<FetchTask>(num)
        while (num-- > 0) {
            val fetchTask = tasksMonitor.consume(poolId)
            if (fetchTask != null) {
                fetchTasks.add(fetchTask)
            } else {
                tasksMonitor.maintain()
            }
        }

        if (fetchTasks.isNotEmpty()) {
            lastTaskStartTime = Instant.now()
        }

        return fetchTasks
    }

    /**
     * Finish the fetch item anyway, even if it's failed to download the target page
     */
    fun finishUnchecked(fetchTask: FetchTask) {
        tasksMonitor.finish(fetchTask)
        lastTaskFinishTime = Instant.now()
        metricsCounters.increase(Counter.rFinishedTasks)
    }

    /**
     * Finished downloading the web page
     *
     * Multiple threaded, non-synchronized class member variables are not allowed inside this method.
     */
    fun finish(poolId: PoolId, itemId: Int) {
        val fetchTask = tasksMonitor.findPendingTask(poolId, itemId)

        if (fetchTask == null) {
            // Can not find task to finish, The queue might be retuned or cleared up
            log.warn("Failed to finish fetch task <{}, {}>", poolId, itemId)
            return
        }

        val page = fetchTask.page
        val protocolStatus = page.protocolStatus
        try {
            // un-block queue
            tasksMonitor.finish(fetchTask)

            handleResult(fetchTask, CrawlStatus.STATUS_FETCHED)

            if (protocolStatus.isRetry(RetryScope.FETCH_PROTOCOL)) {
                tasksMonitor.produce(fetchTask)
            }
        } catch (e: Throwable) {
            log.error("Unexpected error - {} | {}", e, fetchTask.urlString)

            tasksMonitor.finishAsap(fetchTask)
            fetchErrors.incrementAndGet()
            metricsCounters.increase(CommonCounter.errors)
        } finally {
            lastTaskFinishTime = Instant.now()
        }
    }

    /**
     * Multiple threaded
     */
    fun pollFetchResult(): FetchJobForwardingResponse? {
        return fetchResultQueue.remove()
    }

    override fun close() {
        log.info("[Destruction] Closing TaskScheduler #$id")

        val border = StringUtils.repeat('.', 40)
        log.info(border)
        log.info("[Final Report - " + DateTimeUtil.now() + "]")

        report()

        log.info("[End Report]")
        log.info(border)
    }

    /**
     * Wait for a while and report task status
     *
     * @param reportInterval Report interval
     * @return Status
     */
    fun waitAndReport(reportInterval: Duration): Status {
        val pagesLastSec = totalPages.get().toFloat()
        val bytesLastSec = totalBytes.get()

        try {
            Thread.sleep(reportInterval.toMillis())
        } catch (ignored: InterruptedException) {
        }

        val reportIntervalSec = reportInterval.seconds.toDouble()
        val pagesThoRate = (totalPages.get() - pagesLastSec) / reportIntervalSec
        val bytesThoRate = (totalBytes.get() - bytesLastSec) / reportIntervalSec

        val readyFetchItems = tasksMonitor.readyTaskCount()
        val pendingFetchItems = tasksMonitor.pendingTaskCount()

        metricsCounters.setValue(Counter.rReadyTasks, readyFetchItems)
        metricsCounters.setValue(Counter.rPendingTasks, pendingFetchItems)
        metricsCounters.setValue(Counter.rPagesTho, pagesThoRate.roundToInt())
        metricsCounters.setValue(Counter.rMbTho, (bytesThoRate / 1000).roundToInt())

        if (indexJIT) {
            metricsCounters.setValue(Counter.rIndexed, jitIndexer.indexedPageCount)
            metricsCounters.setValue(Counter.rNotIndexed, jitIndexer.ignoredPageCount)
        }

        return Status(pagesThoRate, bytesThoRate, readyFetchItems, pendingFetchItems)
    }

    fun format(status: Status): String {
        return format(status.pagesThoRate, status.bytesThoRate, status.readyFetchItems, status.pendingFetchItems)
    }

    private fun format(pagesThroughput: Double, bytesThroughput: Double, readyFetchItems: Int, pendingFetchItems: Int): String {
        val df = DecimalFormat("0.0")

        averagePageSize = 1.0 * bytesThroughput / pagesThroughput

        val status = StringBuilder()
        val elapsed = Duration.between(startTime, Instant.now()).seconds

        status.append(totalPages).append(" pages, ").append(fetchErrors).append(" errors, ")

        // average speed
        averagePageThroughput = 1.0 * totalPages.get() / elapsed
        status.append(df.format(averagePageThroughput)).append(" ")
        // instantaneous speed
        status.append(df.format(pagesThroughput)).append(" pages/s, ")

        // average speed
        averageBytesThroughput = 1.0 * totalBytes.get() / elapsed
        status.append(df.format(averageBytesThroughput * 8.0 / 1024)).append(" ")
        // instantaneous speed
        status.append(df.format(bytesThroughput * 8.0 / 1024)).append(" Kb/s, ")

        status.append(readyFetchItems).append(" ready ")
        status.append(pendingFetchItems).append(" pending ")
        status.append("URLs in ").append(tasksMonitor.poolCount).append(" queues")

        return status.toString()
    }

    private fun handleResult(fetchTask: FetchTask, crawlStatus: CrawlStatus) {
        val page = fetchTask.page

        metricsSystem.debugFetchHistory(page)

        if (parse && crawlStatus.isFetched) {
            val parseResult = pageParser.parse(page)

            if (!parseResult.isSuccess) {
                metricsCounters.increase(Counter.rParseFailed)
                page.pageCounters.increase<PageCounters.Self>(PageCounters.Self.parseErr)
            }

            // Double check success
            if (!page.hasMark(Mark.PARSE)) {
                metricsCounters.increase(Counter.rNoParse)
            }

            if (parseResult.minorCode != ParseStatus.SUCCESS_OK) {
                metricsSystem.reportFlawParsedPage(page, false)
            }

            if (jitIndexer.isEnabled && parseResult.isSuccess) {
                // JIT Index
                jitIndexer.produce(fetchTask)
            }
        }

        // Remove content if storingContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (page.content != null && !storingContent) {
            if (!page.isSeed) {
                page.setContent(ByteArray(0))
            } else if (page.fetchCount > 2) {
                page.setContent(ByteArray(0))
            }
        }

        updateStatus(page)
    }

    private fun handleRedirect(url: String, newUrl_: String, temp: Boolean, redirType: String, page: WebPage) {
        var newUrl = newUrl_
        newUrl = pageParser.crawlFilters.normalizeToEmpty(newUrl, UrlNormalizers.SCOPE_FETCHER)
        if (newUrl.isEmpty() || newUrl == url) {
            return
        }

        page.addLiveLink(HypeLink(newUrl))
        page.metadata.set(Name.REDIRECT_DISCOVERED, YES_STRING)

        val threadId = Thread.currentThread().id
        var reprUrl = reprUrls.getOrDefault(threadId, url)
        reprUrl = URLUtil.chooseRepr(reprUrl, newUrl, temp)

        if (reprUrl.length < SHORTEST_VALID_URL_LENGTH) {
            log.warn("reprUrl is too short")
            return
        }

        page.reprUrl = reprUrl
        reprUrls[threadId] = reprUrl
        metricsSystem.reportRedirects(String.format("[%s] - %100s -> %s\n", redirType, url, reprUrl))
        metricsCounters.increase(Counter.rRedirect)
    }

    /**
     * Do not redirect too much
     * TODO : Check why we need to save reprUrl for each thread
     */
    private fun handleRedirectUrl(page: WebPage, url: String, newUrl: String, temp: Boolean): String {
        val threadId = Thread.currentThread().id
        var reprUrl = reprUrls.getOrDefault(threadId, url)
        reprUrl = URLUtil.chooseRepr(reprUrl, newUrl, temp)

        if (reprUrl.length < SHORTEST_VALID_URL_LENGTH) {
            log.warn("reprUrl is too short")
            return reprUrl
        }

        page.reprUrl = reprUrl
        reprUrls[threadId] = reprUrl

        return reprUrl
    }

    private fun updateStatus(page: WebPage) {
        metricsCounters.increase(CommonCounter.rPersist)
        metricsCounters.increase(CommonCounter.rLinks, page.impreciseLinkCount)

        totalPages.incrementAndGet()
        totalBytes.addAndGet(page.contentBytes.toLong())

        if (page.isSeed) {
            metricsCounters.increase(Counter.rSeeds)
        }

        CounterUtils.increaseRDepth(page.distance, metricsCounters)

        metricsCounters.increase(Counter.rMbytes, (page.contentBytes / 1024.0f).roundToInt())
    }

    private fun logFetchFailure(message: String) {
        if (message.isNotEmpty()) {
            log.warn("Fetch failed, $message")
        }

        fetchErrors.incrementAndGet()
        metricsCounters.increase(CommonCounter.errors)
    }

    private fun report() {
        if (pageParser.unparsableTypes.isNotEmpty()) {
            var report = ""
            val hosts = pageParser.unparsableTypes.sortedBy { it.toString() }.joinToString("\n") { it }
            report += hosts
            report += "\n"
            log.info("# Un-parsable types : \n$report")
        }
    }

    companion object {
        enum class Counter {
            rMbytes, unknowHosts,
            rReadyTasks, rPendingTasks, rFinishedTasks,
            rPagesTho, rMbTho, rRedirect,
            rSeeds,
            rParseFailed, rNoParse,
            rIndexed, rNotIndexed
        }

        init { MetricsCounters.register(Counter::class.java) }

        private val objectSequence = AtomicInteger(0)
    }
}
