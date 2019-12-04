package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants.*
import ai.platon.pulsar.common.config.ReloadableParameterized
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.indexer.IndexThread
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class FetchMonitor(
        private val fetchComponent: FetchComponent,
        private val taskMonitor: TaskMonitor,
        private val taskSchedulers: TaskSchedulers,
        private val immutableConfig: ImmutableConfig
) : ReloadableParameterized, AutoCloseable {

    private val id = instanceSequence.incrementAndGet()
    private val taskScheduler = taskSchedulers.first // TODO: multiple task schedulers are not supported

    private val startTime = Instant.now()

    private var jobName: String = immutableConfig.get(PARAM_JOB_NAME, DateTimeUtil.now("MMddHHmmss"))
    private var crawlId: String = immutableConfig.get(STORAGE_CRAWL_ID)
    private var batchId: String = immutableConfig.get(BATCH_ID)
    private var fetchMode: FetchMode = immutableConfig.getEnum(FETCH_MODE, FetchMode.NATIVE)

    /**
     * Threads
     */
    private var fetchThreadCount: Int = immutableConfig.getInt(FETCH_THREADS_FETCH, 5)

    private val activeFetchThreads = ConcurrentSkipListSet<FetchThread>()
    private val retiredFetchThreads = ConcurrentSkipListSet<FetchThread>()
    private val idleFetchThreads = ConcurrentSkipListSet<FetchThread>()

    private val activeFetchThreadCount = AtomicInteger(0)
    private val idleFetchThreadCount = AtomicInteger(0)

    /**
     * Feeder threads
     */
    private val feedThreads = ConcurrentSkipListSet<FeedThread>()

    /**
     * Timing
     */
    private var fetchJobTimeout = immutableConfig.getDuration(FETCH_JOB_TIMEOUT, Duration.ofHours(1))
    private var fetchTaskTimeout = immutableConfig.getDuration(FETCH_TASK_TIMEOUT, Duration.ofMinutes(10))
    private var poolRetuneInterval = immutableConfig.getDuration(FETCH_QUEUE_RETUNE_INTERVAL, Duration.ofMinutes(8))
    private var poolPendingTimeout = immutableConfig.getDuration(FETCH_PENDING_TIMEOUT, poolRetuneInterval.multipliedBy(2))
    private var poolRetuneTime = startTime

    /**
     * Monitoring
     */
    private var checkInterval: Duration = immutableConfig.getDuration(FETCH_CHECK_INTERVAL, Duration.ofSeconds(20))

    /**
     * Throughput control
     */
    private var minPageThoRate: Int = immutableConfig.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1)
    private var maxLowThoCount: Int = immutableConfig.getInt(FETCH_THROUGHPUT_THRESHOLD_SEQENCE, 10)
    private var maxTotalLowThoCount: Int = maxLowThoCount * 10
    /*
     * Used for threshold check, holds pages and bytes processed in the last sec
     * We should keep a minimal fetch speed
     * */
    private var thoCheckInterval: Duration = immutableConfig.getDuration(FETCH_THROUGHPUT_CHECK_INTERVAL, Duration.ofSeconds(120))
    private var thoCheckTime: Instant = startTime.plus(thoCheckInterval)
    private var lowThoCount: Int = 0
    private var totalLowThoCount: Int = 0

    /**
     * Index server
     */
    private var indexServer = immutableConfig.get(INDEXER_HOSTNAME, DEFAULT_INDEX_SERVER_HOSTNAME)
    private var indexServerPort = immutableConfig.getInt(INDEXER_PORT, DEFAULT_INDEX_SERVER_PORT)

    /**
     * Scripts
     */
    private var finishScript: Path = AppPaths.get("scripts", "finish_$jobName.sh")

    var isHalt = false
        private set

    val isFeederAlive: Boolean
        get() = !feedThreads.isEmpty()

    val isMissionComplete: Boolean
        get() = !isFeederAlive && taskMonitor.readyTaskCount() == 0 && taskMonitor.pendingTaskCount() == 0

    init {
        generateFinishCommand()
        LOG.info(params.format())
    }

    override fun getConf(): ImmutableConfig? {
        return immutableConfig
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "batchId", batchId,
                "fetchMode", fetchMode,

                "taskSchedulers", taskSchedulers.name,
                "taskScheduler", taskScheduler.name,

                "fetchJobTimeout", fetchJobTimeout,
                "fetchTaskTimeout", fetchTaskTimeout,
                "poolPendingTimeout", poolPendingTimeout,
                "poolRetuneInterval", poolRetuneInterval,
                "poolRetuneTime", DateTimeUtil.format(poolRetuneTime),
                "checkInterval", checkInterval,

                "minPageThoRate", minPageThoRate,
                "maxLowThoCount", maxLowThoCount,
                "thoCheckTime", DateTimeUtil.format(thoCheckTime),

                "finishScript", finishScript
        )
    }

    private fun generateFinishCommand() {
        val cmd = "#bin\necho finish-job $jobName >> " + AppPaths.PATH_LOCAL_COMMAND

        try {
            Files.createDirectories(finishScript.parent)
            Files.write(finishScript, cmd.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            Files.setPosixFilePermissions(finishScript, PosixFilePermissions.fromString("rwxrw-r--"))
        } catch (e: IOException) {
            LOG.error(e.toString())
        }

    }

    fun registerFeedThread(feedThread: FeedThread) {
        feedThreads.add(feedThread)
    }

    fun unregisterFeedThread(feedThread: FeedThread) {
        feedThreads.remove(feedThread)
    }

    fun registerFetchThread(fetchThread: FetchThread) {
        activeFetchThreads.add(fetchThread)
        activeFetchThreadCount.incrementAndGet()
    }

    fun unregisterFetchThread(fetchThread: FetchThread) {
        activeFetchThreads.remove(fetchThread)
        activeFetchThreadCount.decrementAndGet()

        retiredFetchThreads.add(fetchThread)
    }

    fun registerIdleThread(thread: FetchThread) {
        idleFetchThreads.add(thread)
        idleFetchThreadCount.incrementAndGet()
    }

    fun unregisterIdleThread(thread: FetchThread) {
        idleFetchThreads.remove(thread)
        idleFetchThreadCount.decrementAndGet()
    }

    fun start(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFeedThread(context)

        if (FetchMode.CROWDSOURCING == fetchMode) {
            startCrowdsourcingThreads(context)
        } else {
            // Threads for native or proxy mode
            startNativeFetcherThreads(context)
        }

        if (taskScheduler.indexJIT) {
            startIndexThreads()
        }

        startCheckAndReportLoop(context)
    }

    override fun close() {
        try {
            LOG.info("[Destruction] Closing FetchMonitor #$id")

            feedThreads.forEach { it.exitAndJoin() }
            activeFetchThreads.forEach { it.exitAndJoin() }
            retiredFetchThreads.forEach { it.report() }

            Files.deleteIfExists(finishScript)
        } catch (e: Throwable) {
            LOG.error(StringUtil.stringifyException(e))
        }
    }

    fun halt() {
        isHalt = true
    }

    /**
     * Start pool feeder thread. The thread fetches webpages from the reduce result
     * and add it into the fetch pool
     * Non-Blocking
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun startFeedThread(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        val feedThread = FeedThread(this, taskScheduler, taskScheduler.tasksMonitor, context, immutableConfig);
        feedThread.start();
    }

    /**
     * Start crowd sourcing threads
     * Non-Blocking
     */
    private fun startCrowdsourcingThreads(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFetchThreads(fetchThreadCount, context)
    }

    /**
     * Start native fetcher threads
     * Non-Blocking
     */
    private fun startNativeFetcherThreads(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFetchThreads(fetchThreadCount, context)
    }

    private fun startFetchThreads(threadCount: Int, context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        for (i in 0 until threadCount) {
            val fetchThread = FetchThread(fetchComponent, this, taskScheduler, immutableConfig, context)
            fetchThread.start()
        }
    }

    /**
     * Start index threads
     * Non-Blocking
     */
    @Throws(IOException::class)
    private fun startIndexThreads() {
        val JITIndexer = taskScheduler.jitIndexer
        for (i in 0 until JITIndexer.indexThreadCount) {
            val indexThread = IndexThread(JITIndexer, immutableConfig)
            indexThread.start()
        }
    }

    @Throws(IOException::class)
    private fun startCheckAndReportLoop(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        if (checkInterval.seconds < 5) {
            LOG.warn("Check frequency is too high, it might cause a serious performance problem")
        }

        do {
            val status = taskScheduler.waitAndReport(checkInterval)

            val statusString = taskScheduler.getStatusString(status)

            /* Status string shows in yarn admin ui */
            context.status = statusString

            /* And also log it */
            LOG.info(statusString)

            val now = Instant.now()
            val jobTime = Duration.between(startTime, now)
            val idleTime = Duration.between(taskScheduler.getLastTaskFinishTime(), now)

            /*
             * Check if any fetch tasks are hung
             * */
            retuneFetchQueues(now, idleTime)

            /*
             * Dump the remainder fetch items if feeder thread is no available and fetch item is few
             * */
            if (taskMonitor.taskCount() <= FETCH_TASK_REMAINDER_NUMBER) {
                LOG.info("Totally remains only " + taskMonitor.taskCount() + " tasks")
                handleFewFetchItems()
            }

            /*
             * Check throughput(fetch speed)
             * */
            if (now.isAfter(thoCheckTime) && status.pagesThoRate < minPageThoRate) {
                checkFetchThroughput()
                thoCheckTime = thoCheckTime.plus(thoCheckInterval)
            }

            /*
             * Halt command is received
             * */
            if (isHalt) {
                LOG.info("Received halt command, exit the job ...")
                break
            }

            /*
             * Read local filesystem for control commands
             * */
            if (RuntimeUtils.hasLocalFileCommand("finish-job $jobName")) {
                handleFinishJobCommand()
                LOG.info("Find finish-job command, exit the job ...")
                halt()
                break
            }

            /*
               * All fetch tasks are finished
               * */
            if (isMissionComplete) {
                LOG.info("All done, exit the job ...")
                break
            }

            if (jobTime.seconds > fetchJobTimeout.seconds) {
                handleJobTimeout()
                LOG.info("Hit fetch job timeout " + jobTime.seconds + "s, exit the job ...")
                break
            }

            /*
             * No new tasks for too long, some requests seem to hang. We exits the job.
             * */
            if (idleTime.seconds > fetchTaskTimeout.seconds) {
                handleFetchTaskTimeout()
                LOG.info("Hit fetch task timeout " + idleTime.seconds + "s, exit the job ...")
                break
            }

            if (taskScheduler.indexJIT && !NetUtil.testHttpNetwork(indexServer, indexServerPort)) {
                LOG.warn("Lost index server, exit the job")
                break
            }
        } while (!activeFetchThreads.isEmpty())
    }

    /**
     * Handle job timeout
     */
    private fun handleJobTimeout(): Int {
        return taskMonitor.clearReadyTasks()
    }

    /**
     * Check if some threads are hung. If so, we should stop the main fetch loop
     * should we stop the main fetch loop
     */
    private fun handleFetchTaskTimeout() {
        if (activeFetchThreads.isEmpty()) {
            return
        }

        val threads = activeFetchThreads.size

        LOG.warn("Aborting with $threads hung threads")

        dumpFetchThreads()
    }

    /**
     * Dump fetch threads
     */
    fun dumpFetchThreads() {
        LOG.info("Fetch threads : active : " + activeFetchThreads.size + ", idle : " + idleFetchThreads.size)

        val report = activeFetchThreads.filter { it.isAlive }
                .map { it.stackTrace }
                .flatMap { it.asList() }
                .joinToString("\n") { it.toString() }

        LOG.info(report)
    }

    private fun handleFewFetchItems() {
        taskMonitor.dump(FETCH_TASK_REMAINDER_NUMBER)
    }

    private fun handleFinishJobCommand() {
        taskMonitor.clearReadyTasks()
    }

    /**
     * Check pools to see if something is hung
     */
    private fun retuneFetchQueues(now: Instant, idleTime: Duration) {
        if (taskMonitor.readyTaskCount() + taskMonitor.pendingTaskCount() < 20) {
            poolPendingTimeout = Duration.ofMinutes(2)
        }

        // Do not check in every report loop
        val nextCheckTime = poolRetuneTime.plus(checkInterval.multipliedBy(2))
        if (now.isAfter(nextCheckTime)) {
            val nextRetuneTime = poolRetuneTime.plus(poolRetuneInterval)
            if (now.isAfter(nextRetuneTime) || idleTime > poolPendingTimeout) {
                taskMonitor.retune(false)
                poolRetuneTime = now
            }
        }
    }

    /**
     * Check if we're dropping below the threshold (we are too slow)
     */
    @Throws(IOException::class)
    private fun checkFetchThroughput() {
        var removedSlowTasks: Int
        if (lowThoCount > maxLowThoCount) {
            // Clear slowest pools
            removedSlowTasks = taskMonitor.tryClearSlowestQueue()

            LOG.info(Params.formatAsLine(
                    "Unaccepted throughput", "clearing slowest pool, ",
                    "lowThoCount", lowThoCount,
                    "maxLowThoCount", maxLowThoCount,
                    "minPageThoRate(p/s)", minPageThoRate,
                    "removedSlowTasks", removedSlowTasks
            ))

            lowThoCount = 0
        }

        // Quit if we dropped below threshold too many times
        if (totalLowThoCount > maxTotalLowThoCount) {
            // Clear all pools
            removedSlowTasks = taskMonitor.clearReadyTasks()
            LOG.info(Params.formatAsLine(
                    "Unaccepted throughput", "all pools are cleared",
                    "lowThoCount", lowThoCount,
                    "maxLowThoCount", maxLowThoCount,
                    "minPageThoRate(p/s)", minPageThoRate,
                    "removedSlowTasks", removedSlowTasks
            ))

            totalLowThoCount = 0
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FetchMonitor::class.java)
        private val instanceSequence = AtomicInteger(0)
    }
}
