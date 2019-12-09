package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants.*
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.data.PoolId
import ai.platon.pulsar.crawl.fetch.indexer.JITIndexer
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import com.google.common.util.concurrent.AtomicDouble
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
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
        var jitIndexer: JITIndexer,
        val conf: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {
    data class Status(
            var pagesThoRate: Float,
            var bytesThoRate: Float,
            var readyFetchItems: Int,
            var pendingFetchItems: Int
    )


    val id: Int = objectSequence.getAndIncrement()
    private val metricsCounters = MetricsCounters()

    private val fetchResultQueue = ConcurrentLinkedQueue<FetchJobForwardingResponse>()

    /**
     * Our own Hardware bandwidth in mbytes, if exceed the limit, slows down the task scheduling.
     */
    private var bandwidth = 1024 * 1024 * conf.getInt("fetcher.net.bandwidth.m", BANDWIDTH_INFINITE)
    private var skipTruncated = conf.getBoolean(PARSE_SKIP_TRUNCATED, true)
    private var storingContent = conf.getBoolean(FETCH_STORE_CONTENT, false)

    // Indexer
    private var indexJIT: Boolean = false
    // Parser setting
    private var parse: Boolean = false

    /**
     * Fetch threads
     */
    private var initFetchThreadCount: Int = conf.getInt(FETCH_THREADS_FETCH, 10)
    private var threadsPerQueue: Int = conf.getInt(FETCH_THREADS_PER_QUEUE, 1)

    // Timer
    private val startTime = System.currentTimeMillis() // Start time of fetcher run
    private val lastTaskStartTime = AtomicLong(startTime)
    private val lastTaskFinishTime = AtomicLong(startTime)

    // Statistics
    private val totalBytes = AtomicLong(0)        // total fetched bytes
    private val totalPages = AtomicInteger(0)  // total fetched pages
    private val fetchErrors = AtomicInteger(0) // total fetch fetchErrors

    private val averagePageThroughput = AtomicDouble(0.01)
    private val averageBytesThroughput = AtomicDouble(0.01)
    private val averagePageSize = AtomicDouble(0.0)

    /**
     * Output
     */
    private var outputDir: Path = AppPaths.REPORT_DIR

    /**
     * The reprUrl is the representative url of a redirect, we save a reprUrl for each thread
     * We use a concurrent skip list map to gain the best concurrency
     *
     * TODO : check why we store a reprUrl for each thread?
     */
    private val reprUrls = ConcurrentSkipListMap<Long, String>()

    val unparsableTypes: Set<CharSequence>
        get() = pageParser.unparsableTypes

    override fun setup(jobConf: ImmutableConfig) {
        indexJIT = jobConf.getBoolean(INDEXER_JIT, false)
        // Parser setting
        parse = indexJIT || conf.getBoolean(PARSE_PARSE, true)

        LOG.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,

                "id", id,

                "bandwidth", StringUtil.readableByteCount(bandwidth.toLong()),
                "initFetchThreadCount", initFetchThreadCount,
                "threadsPerQueue", threadsPerQueue,

                "skipTruncated", skipTruncated,
                "parse", parse,
                "storingContent", storingContent,

                "indexJIT", indexJIT,
                "outputDir", outputDir
        )
    }

    val name: String get() = javaClass.simpleName + "-" + id

    fun getAveragePageThroughput(): Double {
        return averagePageThroughput.get()
    }

    fun getAverageBytesThroughput(): Double {
        return averageBytesThroughput.get()
    }

    fun getLastTaskFinishTime(): Instant {
        return Instant.ofEpochMilli(lastTaskFinishTime.get())
    }

    fun produce(result: FetchJobForwardingResponse) {
        fetchResultQueue.add(result)
    }

    /**
     * Schedule a queue with the given priority and given queueId
     */
    @JvmOverloads
    fun schedule(queueId: PoolId? = null): FetchTask? {
        val fetchTasks = schedule(queueId, 1)
        return if (fetchTasks.isEmpty()) null else fetchTasks.iterator().next()
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
    fun schedule(queueId: PoolId?, number: Int): List<FetchTask> {
        var num = number
        if (num <= 0) {
            LOG.warn("Required no fetch item")
            return ArrayList()
        }

        val fetchTasks = ArrayList<FetchTask>(num)
        if (tasksMonitor.pendingTaskCount().toDouble() * averagePageSize.get() * 8.0 > 30 * this.bandwidth) {
            LOG.info("Bandwidth exhausted, slows down the scheduling")
            return fetchTasks
        }

        while (num-- > 0) {
            val fetchTask = if (queueId == null) tasksMonitor.consume() else tasksMonitor.consume(queueId)
            if (fetchTask != null) {
                fetchTasks.add(fetchTask)
            }
        }

        if (fetchTasks.isNotEmpty()) {
            lastTaskStartTime.set(System.currentTimeMillis())
        }

        return fetchTasks
    }

    /**
     * Finish the fetch item anyway, even if it's failed to download the target page
     */
    fun finishUnchecked(fetchTask: FetchTask) {
        tasksMonitor.finish(fetchTask)
        lastTaskFinishTime.set(System.currentTimeMillis())
        metricsCounters.increase(Counter.rFinishedTasks)
    }

    /**
     * Finished downloading the web page
     *
     * Multiple threaded, non-synchronized class member variables are not allowed inside this method.
     */
    fun finish(queueId: PoolId, itemId: Int) {
        val fetchTask = tasksMonitor.findPendingTask(queueId, itemId)

        if (fetchTask == null) {
            // Can not find task to finish, The queue might be retuned or cleared up
            LOG.info("Can not find task to finish <{}, {}>", queueId, itemId)
            return
        }

        val page = fetchTask.page?:return
        val protocolStatus = page.protocolStatus
        try {
            // un-block queue
            tasksMonitor.finish(fetchTask)

            handleResult(fetchTask, CrawlStatus.STATUS_FETCHED)

            if (protocolStatus === ProtocolStatus.STATUS_RETRY) {
                tasksMonitor.produce(fetchTask)
            }
        } catch (t: Throwable) {
            LOG.error("Unexpected error for " + fetchTask.url + StringUtil.stringifyException(t))

            tasksMonitor.finish(fetchTask)
            fetchErrors.incrementAndGet()
            metricsCounters.increase(CommonCounter.errors)

            try {
                handleResult(fetchTask, CrawlStatus.STATUS_RETRY)
            } catch (e: IOException) {
                LOG.error("Unexpected fetcher exception, " + StringUtil.stringifyException(e))
            } finally {
                tasksMonitor.finish(fetchTask)
            }
        } finally {
            lastTaskFinishTime.set(System.currentTimeMillis())
        }
    }

    /**
     * Multiple threaded
     */
    fun pollFetchResult(): FetchJobForwardingResponse? {
        return fetchResultQueue.remove()
    }

    override fun close() {
        LOG.info("[Destruction] Closing TaskScheduler #$id")

        val border = StringUtils.repeat('.', 40)
        LOG.info(border)
        LOG.info("[Final Report - " + DateTimeUtil.now() + "]")

        report()

        LOG.info("[End Report]")
        LOG.info(border)
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

        val reportIntervalSec = reportInterval.seconds.toFloat()
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

    fun getStatusString(status: Status): String {
        return getStatusString(status.pagesThoRate, status.bytesThoRate, status.readyFetchItems, status.pendingFetchItems)
    }

    private fun getStatusString(pagesThroughput: Float, bytesThroughput: Float, readyFetchItems: Int, pendingFetchItems: Int): String {
        val df = DecimalFormat("0.0")

        this.averagePageSize.set((bytesThroughput / pagesThroughput).toDouble())

        val status = StringBuilder()
        val elapsed = (System.currentTimeMillis() - startTime) / 1000

        // status.append(idleFetchThreadCount).append("/").append(activeFetchThreadCount).append(" idle/active threads, ");
        status.append(totalPages).append(" pages, ").append(fetchErrors).append(" errors, ")

        // average speed
        averagePageThroughput.set(1.0 * totalPages.get() / elapsed)
        status.append(df.format(averagePageThroughput.get())).append(" ")
        // instantaneous speed
        status.append(df.format(pagesThroughput.toDouble())).append(" pages/s, ")

        // average speed
        averageBytesThroughput.set(1.0 * totalBytes.get() / elapsed)
        status.append(df.format(averageBytesThroughput.get() * 8.0 / 1024)).append(" ")
        // instantaneous speed
        status.append(df.format(bytesThroughput * 8.0 / 1024)).append(" kb/s, ")

        status.append(readyFetchItems).append(" ready ")
        status.append(pendingFetchItems).append(" pending ")
        status.append("URLs in ").append(tasksMonitor.queueCount).append(" queues")

        return status.toString()
    }

    @Throws(IOException::class)
    private fun handleResult(fetchTask: FetchTask, crawlStatus: CrawlStatus) {
        val url = fetchTask.url
        val page = fetchTask.page

        metricsSystem.debugFetchHistory(page!!, crawlStatus)

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
                metricsSystem.reportFlawyParsedPage(page, true)
            }

            if (jitIndexer.isEnabled && parseResult.isSuccess) {
                // JIT Index
                jitIndexer.produce(fetchTask)
            }
        }

        // Remove content if storingContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (page.content != null) {
            if (!storingContent) {
                if (!page.isSeed) {
                    page.setContent(ByteArray(0))
                } else if (page.fetchCount > 2) {
                    page.setContent(ByteArray(0))
                }
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
            LOG.warn("reprUrl is too short")
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
            LOG.warn("reprUrl is too short")
            return reprUrl
        }

        page.setReprUrl(reprUrl)
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
        if (!message.isEmpty()) {
            LOG.warn("Fetch failed, $message")
        }

        fetchErrors.incrementAndGet()
        metricsCounters.increase(CommonCounter.errors)
    }

    private fun report() {
        if (unparsableTypes.isNotEmpty()) {
            var report = ""
            val hosts = unparsableTypes.sortedBy { it.toString() }.joinToString("\n") { it }
            report += hosts
            report += "\n"
            LOG.info("# UnparsableTypes : \n$report")
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FetchMonitor::class.java)
        val PROTOCOL_REDIR = "protocol"

        enum class Counter {
            rMbytes, unknowHosts, rowsInjected,
            rReadyTasks, rPendingTasks, rFinishedTasks,
            rPagesTho, rMbTho, rRedirect,
            rSeeds,
            rParseFailed, rNoParse,
            rIndexed, rNotIndexed,
            rDepthUp
        }

        init { MetricsCounters.register(Counter::class.java) }

        private val objectSequence = AtomicInteger(0)
    }
}
