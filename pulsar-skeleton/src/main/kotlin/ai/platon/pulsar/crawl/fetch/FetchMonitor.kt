package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.FETCH_TASK_REMAINDER_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.Configurable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.FetchOptions
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.fetch.indexer.IndexThread
import ai.platon.pulsar.crawl.fetch.indexer.JITIndexer
import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * The fetch monitor
 */
class FetchMonitor(
        private val fetchComponent: FetchComponent,
        private val parseComponent: ParseComponent,
        private val taskMonitor: TaskMonitor,
        private val taskSchedulers: TaskSchedulers,
        private val jitIndexer: JITIndexer,
        private val conf: ImmutableConfig
) : Parameterized, Configurable, JobInitialized, AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger(0)
    }

    private val log = LoggerFactory.getLogger(FetchMonitor::class.java)
    private val id = instanceSequencer.incrementAndGet()

    private val startTime = Instant.now()

    /**
     * Timing
     */
    private var fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofHours(1))
    private var poolTuneInterval = conf.getDuration(FETCH_QUEUE_RETUNE_INTERVAL, Duration.ofMinutes(8))
    private var poolPendingTimeout = conf.getDuration(FETCH_PENDING_TIMEOUT, poolTuneInterval.multipliedBy(2))
    private var poolTuneTime = startTime

    /**
     * Monitoring
     */
    private var checkInterval: Duration = conf.getDuration(FETCH_CHECK_INTERVAL, Duration.ofSeconds(20))

    /**
     * Throughput control
     */
    private var minPageThroughputRate = conf.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1)
    private var maxLowThroughputCount = conf.getInt(FETCH_THROUGHPUT_THRESHOLD_SEQENCE, 10)
    private var maxTotalLowThroughputCount = maxLowThroughputCount * 10
    /*
     * Used for threshold check, holds pages and bytes processed in the last sec
     * We should keep a minimal fetch speed
     * */
    private var throughputCheckInterval = conf.getDuration(FETCH_THROUGHPUT_CHECK_INTERVAL, Duration.ofSeconds(120))
    private var throughputCheckTime = startTime.plus(throughputCheckInterval)
    private var lowThroughputCount = 0
    private var totalLowThroughputCount = 0

    /**
     * Task scheduler
     * */
    private val taskScheduler = taskSchedulers.first // TODO: multiple task schedulers are not supported

    /**
     * Feeder threads
     */
    private val feedThreads = ConcurrentSkipListSet<FeedThread>()
    private val fetchLoops = ConcurrentSkipListSet<FetchLoop>()

    private val isFeederAlive: Boolean get() = !feedThreads.isEmpty()

    val missionComplete: Boolean get() = !isFeederAlive &&
            taskMonitor.numReadyTasks.get() == 0 && taskMonitor.numPendingTasks.get() == 0

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
    val isActive get() = !closed.get()

    override fun setup(jobConf: ImmutableConfig) {
        jitIndexer.setup(jobConf)
        taskMonitor.setup(jobConf)
        taskScheduler.setup(jobConf)

        this.options = FetchOptions(jobConf)

        this.jobName = jobConf.get(PARAM_JOB_NAME, "UNKNOWN")
        this.finishScript = AppPaths.TMP_DIR.resolve("scripts").resolve("finish_$jobName.sh")

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
                "poolPendingTimeout", poolPendingTimeout,
                "poolRetuneInterval", poolTuneInterval,
                "poolRetuneTime", DateTimes.format(poolTuneTime),
                "checkInterval", checkInterval,

                "minPageThoRate", minPageThroughputRate,
                "maxLowThoCount", maxLowThroughputCount,
                "thoCheckTime", DateTimes.format(throughputCheckTime),

                "finishScript", finishScript
        )
    }

    override fun getConf(): ImmutableConfig {
        return conf
    }

    fun start(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFeedThread(context)

        if (jitIndexer.isEnabled) {
            startIndexThreads()
        }

        startFetchLoop(context)

        startWatcherLoop(context)
    }

    fun registerFeedThread(feedThread: FeedThread) {
        log.info("FeedThread {} is registered", feedThread)
        feedThreads.add(feedThread)
    }

    fun unregisterFeedThread(feedThread: FeedThread) {
        log.info("FeedThread {} is unregistered", feedThread)
        feedThreads.remove(feedThread)
    }

    fun registerFetchLoop(fetchLoop: FetchLoop) {
        log.info("Fetch loop {} is register", fetchLoop)
        fetchLoops.add(fetchLoop)
    }

    fun unregisterFetchLoop(fetchLoop: FetchLoop) {
        log.info("Fetch loop {} is unregistered", fetchLoop)
        fetchLoops.remove(fetchLoop)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                log.info("Closing FetchMonitor #$id")

                // cancel all tasks
                feedThreads.forEach { it.runCatching { it.close() }.onFailure { PulsarContext.log.warn(it.message) } }
                fetchLoops.forEach { it.runCatching { it.close() }.onFailure { PulsarContext.log.warn(it.message) } }

                Files.deleteIfExists(finishScript)
            } catch (e: Throwable) {
                log.error("Caught an unexpected exception when closing FetchMonitor - {}", Strings.stringifyException(e))
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
    private fun startFeedThread(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        FeedThread(this, taskScheduler, taskScheduler.tasksMonitor, context, conf).start()
    }

    private fun startFetchLoop(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        val monitor = this
        GlobalScope.launch {
            FetchLoop(monitor, fetchComponent, parseComponent, taskScheduler, context, conf).start()
        }
    }

    /**
     * Start index threads
     * Non-Blocking
     */
    private fun startIndexThreads() {
        repeat(jitIndexer.indexThreadCount) {
            IndexThread(jitIndexer, conf).start()
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

            /*
             * Dump the remainder fetch items if feeder thread is no available and fetch item is few
             * */
            if (taskMonitor.numTasks <= FETCH_TASK_REMAINDER_NUMBER) {
                log.info("Totally remains only " + taskMonitor.numTasks + " tasks")
                handleFewFetchItems()
            }

            /*
             * Check throughput(fetch speed)
             * */
            if (now.isAfter(throughputCheckTime) && status.pagesThroughputRate < minPageThroughputRate) {
                checkFetchThroughput()
                throughputCheckTime += throughputCheckInterval
            }

            /**
             * Process is closing
             * */
            if (!isActive) {
                log.info("App is closing, exit the job ...")
                break
            }

            /*
             * Read local file commands
             * */
            if (FileCommand.check("finish-job $jobName")) {
                handleFinishJobCommand()
                log.info("Found finish-job command, exit the job ...")
                close()
                break
            }

            /*
             * All fetch tasks are finished
             * */
            if (missionComplete) {
                log.info("All done, exit the job ...")
                break
            }

            if (jobTime.seconds > fetchJobTimeout.seconds) {
                handleJobTimeout()
                log.info("Hit fetch job timeout " + jobTime.seconds + "s, exit the job ...")
                break
            }

            if (jitIndexer.isEnabled && !jitIndexer.isIndexServerAvailable()) {
                log.warn("Lost index server, exit the job")
                break
            }
        } while (isActive)

        close()
    }

    /**
     * Handle job timeout
     */
    private fun handleJobTimeout(): Int {
        return taskMonitor.clearReadyTasks()
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
        if (taskMonitor.numReadyTasks.get() + taskMonitor.numPendingTasks.get() < 20) {
            poolPendingTimeout = Duration.ofMinutes(3)
        }

        // Do not check in every report loop
        val nextCheckTime = poolTuneTime + checkInterval.multipliedBy(2)
        if (now.isAfter(nextCheckTime)) {
            val nextTuneTime = poolTuneTime + poolTuneInterval
            if (now.isAfter(nextTuneTime) || idleTime > poolPendingTimeout) {
                taskMonitor.tune(false)
                poolTuneTime = now
            }
        }
    }

    /**
     * Check if we're dropping below the threshold (we are too slow)
     */
    private fun checkFetchThroughput() {
        var removedSlowTasks: Int
        if (lowThroughputCount > maxLowThroughputCount) {
            // Clear slowest pools
            removedSlowTasks = taskMonitor.tryClearSlowestQueue()

            log.info(Params.formatAsLine(
                    "Unaccepted throughput", "clearing slowest pool, ",
                    "lowThroughputCount", lowThroughputCount,
                    "maxLowThroughputCount", maxLowThroughputCount,
                    "minPageThroughputRate(p/s)", minPageThroughputRate,
                    "removedSlowTasks", removedSlowTasks
            ))

            lowThroughputCount = 0
        }

        // Quit if we dropped below threshold too many times
        if (totalLowThroughputCount > maxTotalLowThroughputCount) {
            // Clear all pools
            removedSlowTasks = taskMonitor.clearReadyTasks()
            log.info(Params.formatAsLine(
                    "Unaccepted throughput", "all pools are cleared",
                    "lowThroughputCount", lowThroughputCount,
                    "maxLowThroughputCount", maxLowThroughputCount,
                    "minPageThroughputRate(p/s)", minPageThroughputRate,
                    "removedSlowTasks", removedSlowTasks
            ))

            totalLowThroughputCount = 0
        }
    }
}
