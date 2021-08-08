package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.metrics.CommonCounter
import ai.platon.pulsar.common.metrics.EnumCounterUtils
import ai.platon.pulsar.common.measure.ByteUnitConverter
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.crawl.fetch.indexer.JITIndexer
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.metadata.Mark
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class TaskScheduler(
    val tasksMonitor: TaskMonitor,
    val pageParser: PageParser,
    val jitIndexer: JITIndexer,
    val coreMetrics: CoreMetrics,
    val messageWriter: MiscMessageWriter,
    val immutableConfig: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {
    data class Status(
            var pagesThroughputRate: Double,
            var bytesThoRate: Double,
            var readyFetchItems: Int,
            var pendingFetchItems: Int
    )

    private val log = LoggerFactory.getLogger(TaskScheduler::class.java)
    val id: Int = instanceSequence.incrementAndGet()
    private val enumCounters = AppMetrics.reg.enumCounterRegistry

    /**
     * Our own Hardware bandwidth in mbytes, if exceed the limit, slows down the task scheduling.
     * TODO: auto detect bandwidth
     */
    private var bandwidth = 1024 * 1024 * immutableConfig.getInt(FETCH_NET_BANDWIDTH_M, BANDWIDTH_INFINITE_M)
    var skipTruncated = immutableConfig.getBoolean(PARSE_SKIP_TRUNCATED, true)
    var storeContent = immutableConfig.getBoolean(FETCH_STORE_CONTENT, true)

    // Indexer
    var indexJIT: Boolean = false
    // Parser setting
    var parse: Boolean = false

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

    /**
     * The reprUrl is the representative url of a redirect, we save a reprUrl for each thread
     * We use a concurrent skip list map to gain the best concurrency
     *
     * TODO : check why we store a reprUrl for each thread?
     */
    private val reprUrls = ConcurrentSkipListMap<Long, String>()

    val name: String get() = javaClass.simpleName + "-" + id

    override fun setup(jobConf: ImmutableConfig) {
        indexJIT = jobConf.getBoolean(INDEXER_JIT, false)
        // Parser setting
        parse = indexJIT || jobConf.getBoolean(PARSE_PARSE, false)

        log.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "id", id,
                "bandwidth", Strings.readableBytes(bandwidth),
                "skipTruncated", skipTruncated,
                "parse", parse,
                "storeContent", storeContent,
                "indexJIT", indexJIT
        )
    }

    /**
     * Schedule a queue with the given priority and given poolId
     */
    fun schedule(): JobFetchTask? {
        val fetchTask = tasksMonitor.consume()
        if (fetchTask == null) {
            tasksMonitor.maintain()
        } else {
            lastTaskStartTime = Instant.now()
        }

        return fetchTask
    }

    /**
     * Schedule the queues with top priority
     */
    fun schedule(number: Int): List<JobFetchTask> {
        return schedule(null, number)
    }

    /**
     * Null queue id means the queue with top priority
     * Consume a fetch item and try to download the target web page
     */
    fun schedule(poolId: PoolId?, number: Int): List<JobFetchTask> {
        var num = number
        if (num <= 0) {
            log.warn("The number must be positive to schedule fetch items")
            return listOf()
        }

        val fifteenMinutePageSizeRate = coreMetrics.meterContentBytes.fifteenMinuteRate
        if (tasksMonitor.numPendingTasks.get() * fifteenMinutePageSizeRate * 8.0 > 30 * this.bandwidth) {
            log.warn("Bandwidth exhausted, slows down the scheduling")
            return listOf()
        }

        val fetchTasks = ArrayList<JobFetchTask>(num)
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
    fun finishUnchecked(fetchTask: JobFetchTask) {
        tasksMonitor.finish(fetchTask)
        lastTaskFinishTime = Instant.now()
        enumCounters.inc(Counter.rFinishedTasks)
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

            if (protocolStatus.isRetry(RetryScope.JOB)) {
                // NOTE: retries should be delayed
                tasksMonitor.produce(fetchTask)
            }
        } catch (e: Throwable) {
            log.error("Unexpected error - {} | {}", e, fetchTask.urlString)

            tasksMonitor.finishAsap(fetchTask)
            fetchErrors.incrementAndGet()
            enumCounters.inc(CommonCounter.errors)
        } finally {
            lastTaskFinishTime = Instant.now()
        }
    }

    /**
     * Wait for a while and report task status
     *
     * TODO: use metrics system instead
     *
     * @return Status
     */
    fun updateCounters() {
        val readyFetchTasks = tasksMonitor.numReadyTasks.get()
        val pendingFetchTasks = tasksMonitor.numPendingTasks.get()

        enumCounters.setValue(Counter.rReadyTasks, readyFetchTasks)
        enumCounters.setValue(Counter.rPendingTasks, pendingFetchTasks)

        if (indexJIT) {
            enumCounters.setValue(Counter.rIndexed, jitIndexer.indexedPageCount)
            enumCounters.setValue(Counter.rNotIndexed, jitIndexer.ignoredPageCount)
        }
    }

    override fun close() {
        log.info("Closing TaskScheduler #$id")

        val border = StringUtils.repeat('.', 40)
        log.info(border)
        log.info("[Final Report - " + DateTimes.now() + "]")

        report()

        log.info("[End Report]")
        log.info(border)
    }

    private fun handleResult(fetchTask: JobFetchTask, crawlStatus: CrawlStatus) {
        val page = fetchTask.page

        messageWriter.debugFetchHistory(page)

        if (parse && crawlStatus.isFetched) {
            val parseResult = pageParser.parse(page)

            if (!parseResult.isSuccess) {
                enumCounters.inc(Counter.rParseFailed)
                page.pageCounters.increase(PageCounters.Self.parseErr)
            }

            // Double check success
            if (!page.hasMark(Mark.PARSE)) {
                enumCounters.inc(Counter.rNoParse)
            }

            if (parseResult.minorCode != ParseStatus.SUCCESS_OK) {
                messageWriter.reportFlawParsedPage(page, false)
            }

            if (jitIndexer.isEnabled && parseResult.isSuccess) {
                // JIT Index
                jitIndexer.produce(fetchTask)
            }
        }

        // Remove content if storeContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (page.content != null && !storeContent) {
            if (!page.isSeed) {
                page.setContent(ByteArray(0))
            } else if (page.fetchCount > 2) {
                page.setContent(ByteArray(0))
            }
        }

        updateStatus(page)
    }

    /**
     * Do not redirect too many times
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
        enumCounters.inc(CommonCounter.rPersist)
        enumCounters.inc(CommonCounter.rLinks, page.impreciseLinkCount)

        totalPages.incrementAndGet()
        totalBytes.addAndGet(page.contentLength)

        if (page.isSeed) {
            enumCounters.inc(Counter.rSeeds)
        }

        EnumCounterUtils.increaseRDepth(page.distance, enumCounters)

        enumCounters.inc(Counter.rMbytes, ByteUnitConverter.convert(page.contentLength, "M").toInt())
    }

    private fun logFetchFailure(message: String) {
        if (message.isNotEmpty()) {
            log.warn("Fetch failed, $message")
        }

        fetchErrors.incrementAndGet()
        enumCounters.inc(CommonCounter.errors)
    }

    private fun report() {
        if (pageParser.unparsableTypes.isNotEmpty()) {
            var report = ""
            val hosts = pageParser.unparsableTypes.sortedBy { it.toString() }.joinToString("\n") { it }
            report += hosts
            report += "\n"
            log.info("Un-parsable types : \n$report")
        }
    }

    companion object {
        enum class Counter {
            rMbytes, unknowHosts,
            rReadyTasks, rPendingTasks, rFinishedTasks,
            rPgps, rMbps, rRedirect,
            rSeeds,
            rParseFailed, rNoParse,
            rIndexed, rNotIndexed
        }

        init { AppMetrics.reg.register(Counter::class.java) }

        private val instanceSequence = AtomicInteger(0)
    }
}
