package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.options.FetchOptions
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.indexer.IndexThread
import ai.platon.pulsar.crawl.fetch.indexer.JITIndexer
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * The fetch monitor
 */
class FetchMonitor(
        private val fetchComponent: FetchComponent,
        private val taskMonitor: TaskMonitor,
        private val taskSchedulers: TaskSchedulers,
        private val jitIndexer: JITIndexer,
        private val immutableConfig: ImmutableConfig
) : Parameterized, Configurable, JobInitialized, AutoCloseable {
    companion object {
        private val instanceSequence = AtomicInteger(0)
    }

    private val log = LoggerFactory.getLogger(FetchMonitor::class.java)
    private val id = instanceSequence.incrementAndGet()

    private val startTime = Instant.now()

    private val activeFetchThreadCount = AtomicInteger(0)
    private val idleFetchThreadCount = AtomicInteger(0)

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
     * Task scheduler
     * */
    private val taskScheduler = taskSchedulers.first // TODO: multiple task schedulers are not supported

    /**
     * Index server, solr or elastic search
     */
    private var indexServerHost = immutableConfig.get(INDEXER_HOSTNAME, DEFAULT_INDEX_SERVER_HOSTNAME)
    private var indexServerPort = immutableConfig.getInt(INDEXER_PORT, DEFAULT_INDEX_SERVER_PORT)

    /**
     * Thread tracking
     * */
    private val activeFetchThreads = ConcurrentSkipListSet<FetchThread>()
    private val retiredFetchThreads = ConcurrentSkipListSet<FetchThread>()
    private val idleFetchThreads = ConcurrentSkipListSet<FetchThread>()

    /**
     * Feeder threads
     */
    private val feedThreads = ConcurrentSkipListSet<FeedThread>()

    private val isFeederAlive: Boolean
        get() = !feedThreads.isEmpty()

    val isMissionComplete: Boolean
        get() = !isFeederAlive && taskMonitor.readyTaskCount() == 0 && taskMonitor.pendingTaskCount() == 0

    /**
     * Initialize in setup using job conf
     * */
    lateinit var options: FetchOptions
    /**
     * Initialize in setup using job conf
     * */
    lateinit var jobName: String
    /**
     * The path of finish scripts, run the finish script to kill this job
     * Initialize in setup using job conf
     */
    private lateinit var finishScript: Path

    private val closed = AtomicBoolean()

    override fun setup(jobConf: ImmutableConfig) {
        jitIndexer.setup(jobConf)
        taskMonitor.setup(jobConf)
        taskScheduler.setup(jobConf)

        this.options = FetchOptions(jobConf)

        this.jobName = jobConf.get(PARAM_JOB_NAME, "UNKNOWN")
        this.finishScript = AppPaths.get("scripts", "finish_$jobName.sh")

        generateFinishCommand()

        log.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "crawlId", options.crawlId,
                "batchId", options.batchId,
                "fetchMode", options.fetchMode,

                "taskSchedulers", taskSchedulers.name,
                "taskScheduler", taskScheduler.name,

                "fetchThreads", options.numFetchThreads,
                "poolThreads", options.numPoolThreads,
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

    override fun getConf(): ImmutableConfig {
        return immutableConfig
    }

    fun start(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFeedThread(context)

        if (FetchMode.CROWD_SOURCING == options.fetchMode) {
            startCrowdSourcingThreads(context)
        } else {
            // Threads for SELENIUM or proxy mode
            startLocalFetcherThreads(context)
        }

        if (jitIndexer.isEnabled) {
            startIndexThreads()
        }

        startWatcherLoop(context)
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

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                log.info("[Destruction] Closing FetchMonitor #$id")

                // cancel all tasks
                feedThreads.forEach { it.exitAndJoin() }
                activeFetchThreads.forEach { it.exit() }
                retiredFetchThreads.forEach { it.report() }

                Files.deleteIfExists(finishScript)
            } catch (e: Throwable) {
                log.error("Caught an unexpected exception when closing FetchMonitor - {}", StringUtil.stringifyException(e))
            }
        }
    }

    private fun generateFinishCommand() {
        val cmd = "#bin\necho finish-job $jobName >> " + AppPaths.PATH_LOCAL_COMMAND

        try {
            Files.write(finishScript, cmd.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            Files.setPosixFilePermissions(finishScript, PosixFilePermissions.fromString("rwxrw-r--"))
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }

    /**
     * Start pool feeder thread. The thread fetches webpages from the reduce result
     * and add it into the fetch pool
     * Non-Blocking
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun startFeedThread(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        FeedThread(this, taskScheduler, taskScheduler.tasksMonitor, context, immutableConfig).start()
    }

    /**
     * Start crowd sourcing threads
     * Non-Blocking
     * TODO: not implemented
     */
    private fun startCrowdSourcingThreads(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFetchThreads(options.numFetchThreads, context)
    }

    /**
     * Start SELENIUM fetcher threads
     * Non-Blocking
     */
    private fun startLocalFetcherThreads(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFetchThreads(options.numFetchThreads, context)
    }

    private fun startFetchThreads(threadCount: Int, context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        val nThreads = if (threadCount > 0) threadCount else AppConstants.FETCH_THREADS
        repeat(nThreads) {
            FetchThread(this, fetchComponent, taskScheduler, immutableConfig, context).start()
        }
    }

    /**
     * Start index threads
     * Non-Blocking
     */
    private fun startIndexThreads() {
        repeat(jitIndexer.indexThreadCount) {
            IndexThread(jitIndexer, immutableConfig).start()
        }
    }

    @Throws(IOException::class)
    private fun startWatcherLoop(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        if (checkInterval.seconds < 5) {
            log.warn("Check frequency is too high, it might cause a serious performance problem")
        }

        do {
            val status = taskScheduler.lap(checkInterval)

            val statusString = taskScheduler.format(status)

            /* Status string shows in yarn admin ui */
            context.status = statusString

            /* And also log it */
            log.info(statusString)

            val now = Instant.now()
            val jobTime = Duration.between(startTime, now)
            val idleTime = Duration.between(taskScheduler.lastTaskFinishTime, now)

            /*
             * Check if any fetch tasks are hung
             * */
            tuneFetchQueues(now, idleTime)

            // TODO: handle browse context reset
            handleContextReset()
            // TODO: merge report from AppMonitor and ProxyManager

            /*
             * Dump the remainder fetch items if feeder thread is no available and fetch item is few
             * */
            if (taskMonitor.taskCount() <= FETCH_TASK_REMAINDER_NUMBER) {
                log.info("Totally remains only " + taskMonitor.taskCount() + " tasks")
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
            if (closed.get()) {
                log.info("Received halt command, exit the job ...")
                break
            }

            /*
             * Read local file commands
             * */
            if (RuntimeUtils.hasLocalFileCommand("finish-job $jobName")) {
                handleFinishJobCommand()
                log.info("Found finish-job command, exit the job ...")
                close()
                break
            }

            /*
             * All fetch tasks are finished
             * */
            if (isMissionComplete) {
                log.info("All done, exit the job ...")
                break
            }

            if (jobTime.seconds > fetchJobTimeout.seconds) {
                handleJobTimeout()
                log.info("Hit fetch job timeout " + jobTime.seconds + "s, exit the job ...")
                break
            }

            /*
             * No new tasks for too long, some requests seem to hang. We exits the job.
             * */
            if (idleTime.seconds > fetchTaskTimeout.seconds) {
                handleFetchTaskTimeout()
                log.info("Hit fetch task timeout " + idleTime.seconds + "s, exit the job ...")
                break
            }

            if (jitIndexer.isEnabled && !jitIndexer.isIndexServerAvailable()) {
                log.warn("Lost index server, exit the job")
                break
            }
        } while (!closed.get() && activeFetchThreads.isNotEmpty())
    }

    /**
     * Handle job timeout
     */
    private fun handleJobTimeout(): Int {
        return taskMonitor.clearReadyTasks()
    }

    /**
     * Check if some threads are hung. If so, we should stop the main fetch loop
     */
    private fun handleFetchTaskTimeout() {
        if (activeFetchThreads.isEmpty()) {
            return
        }

        val threads = activeFetchThreads.size

        log.warn("Aborting with $threads hung threads")

        dumpFetchThreads()
    }

    /**
     * Dump fetch threads
     */
    private fun dumpFetchThreads() {
        log.info("Fetch threads : active : " + activeFetchThreads.size + ", idle : " + idleFetchThreads.size)

        val report = activeFetchThreads.filter { it.isAlive }
                .map { it.stackTrace }
                .flatMap { it.asList() }
                .joinToString("\n") { it.toString() }

        log.info(report)
    }

    private fun handleContextReset() {
        val pendingTasks = FetchThread.pendingTasks
        synchronized(FetchThread::class.java) {

            while (activeFetchThreads.isNotEmpty()) {
                // do not
                // taskScheduler.freeze()
            }

            if (pendingTasks.isNotEmpty()) {
                // collect all protocols need to reset
                val protocols = pendingTasks.mapNotNullTo(HashSet()) {
                    fetchComponent.protocolFactory.getProtocol(it.value)
                }
                protocols.forEach { it.reset() }

                // wait until the context is ok

                // re-fetch all pending tasks again
                pendingTasks.forEach {
                    taskScheduler.tasksMonitor.produce(it.key, it.value)
                }
                pendingTasks.clear()
            }
        }
    }

    private fun handleFewFetchItems() {
        taskMonitor.dump(FETCH_TASK_REMAINDER_NUMBER, false)
    }

    private fun handleFinishJobCommand() {
        taskMonitor.clearReadyTasks()
    }

    /**
     * Check pools to see if something is hung
     */
    private fun tuneFetchQueues(now: Instant, idleTime: Duration) {
        if (taskMonitor.readyTaskCount() + taskMonitor.pendingTaskCount() < 20) {
            poolPendingTimeout = Duration.ofMinutes(3)
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

            log.info(Params.formatAsLine(
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
            log.info(Params.formatAsLine(
                    "Unaccepted throughput", "all pools are cleared",
                    "lowThoCount", lowThoCount,
                    "maxLowThoCount", maxLowThoCount,
                    "minPageThoRate(p/s)", minPageThoRate,
                    "removedSlowTasks", removedSlowTasks
            ))

            totalLowThoCount = 0
        }
    }
}
