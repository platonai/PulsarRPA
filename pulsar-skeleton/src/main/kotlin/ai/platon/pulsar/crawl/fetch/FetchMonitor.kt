package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.FETCH_TASK_REMAINDER_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.*
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
import java.util.concurrent.TimeUnit
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
) : Parameterized, JobInitialized, AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger(0)
    }

    private val log = LoggerFactory.getLogger(FetchMonitor::class.java)
    private val id = instanceSequencer.incrementAndGet()

    private val startTime = Instant.now()
    private val coreMetrics = fetchComponent.coreMetrics

    private val numPrivacyContexts = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    private val fetchConcurrency = numPrivacyContexts * conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    /**
     * Timing
     */
    private val poolTuneInterval = conf.getDuration(FETCH_QUEUE_RETUNE_INTERVAL, Duration.ofMinutes(5))
    private var poolPendingTimeout = taskMonitor.poolPendingTimeout
    private var poolTuneTime = startTime

    private var fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofHours(1))
    private var jobIdleTimeout = poolTuneInterval.multipliedBy(3)
    private val isFetchJobTimeout get() = Duration.between(startTime, Instant.now()) > fetchJobTimeout

    /**
     * Monitoring
     */
    private var checkInterval: Duration = conf.getDuration(FETCH_CHECK_INTERVAL, Duration.ofSeconds(10))

    /**
     * Throughput control
     */
    private var minSuccessPagesPerSecond = conf.getInt(FETCH_THROUGHPUT_PAGES_PER_SECOND, -1)
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
    private val taskScheduler = taskSchedulers.first

    /**
     * Feeder threads
     */
    private val feedLoops = ConcurrentSkipListSet<FeedLoop>()
    private val fetchLoops = ConcurrentSkipListSet<FetchLoop>()

    private val isFeederAlive: Boolean get() = !feedLoops.isEmpty()

    val isMissionComplete: Boolean get() = !isFeederAlive && taskMonitor.numTasks == 0

    private val initialized = AtomicBoolean()
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
        if (initialized.compareAndSet(false, true)) {
            jitIndexer.setup(jobConf)
            taskMonitor.setup(jobConf)
            taskScheduler.setup(jobConf)

            this.options = FetchOptions(jobConf)

            this.jobName = jobConf.get(PARAM_JOB_NAME, "UNNAMED JOB")
            this.finishScript = AppPaths.TMP_DIR.resolve("scripts").resolve("finish_$jobName.sh")

            generateFinishCommand()
        }

        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "crawlId", options.crawlId,
                "batchId", options.batchId,
                "fetchMode", options.fetchMode,

                "taskSchedulers", taskSchedulers.name,
                "taskScheduler", taskScheduler.name,

                "numPrivacyContexts", numPrivacyContexts,
                "fetchConcurrency", fetchConcurrency,

                "numPoolThreads", options.numPoolThreads,
                "fetchJobTimeout", fetchJobTimeout,
                "jobIdleTimeout", jobIdleTimeout,
                "poolTuneInterval", poolTuneInterval,
                "poolTuneTime", DateTimes.format(poolTuneTime),
                "checkInterval", checkInterval,

                "minSuccessPagesPerSecond", minSuccessPagesPerSecond,
                "maxLowThroughputCount", maxLowThroughputCount,
                "throughputCheckTime", DateTimes.format(throughputCheckTime),

                "finishScript", "file:$finishScript"
        )
    }

    fun start(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        startFeedLoop(context)

        if (jitIndexer.isEnabled) {
            startIndexThreads()
        }

        while (feedLoops.isEmpty()) {
            Thread.sleep(1000)
        }

        startFetchLoop(context)

        startWatcherLoop(context)
    }

    fun registerFeedThread(feedLoop: FeedLoop) {
        log.info("FeedThread {} is registered", feedLoop)
        feedLoops.add(feedLoop)
    }

    fun unregisterFeedThread(feedLoop: FeedLoop) {
        log.info("FeedThread {} is unregistered", feedLoop)
        feedLoops.remove(feedLoop)
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
                log.info("Closing fetch monitor #$id")

                // cancel all tasks
                feedLoops.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }
                fetchLoops.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }

                Files.deleteIfExists(finishScript)
            } catch (e: Throwable) {
                log.error("Unexpected exception", e)
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
     * Start pool feeder thread. The thread fetches web pages from the reduce result
     * and add it into the fetch pool
     * Non-Blocking
     */
    private fun startFeedLoop(context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>) {
        val monitor = this
        GlobalScope.launch {
            FeedLoop(monitor, taskScheduler, taskScheduler.tasksMonitor, context, conf).start()
        }
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
            try {
                TimeUnit.MILLISECONDS.sleep(checkInterval.toMillis())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            taskScheduler.updateCounters()

            val now = Instant.now()
            val jobTime = Duration.between(startTime, now)
            val idleTime = Duration.between(taskScheduler.lastTaskFinishTime, now)

            if (idleTime > jobIdleTimeout) {
                log.info("Idle timeout for {}, exit the job", jobIdleTimeout.readable())
                break
            }

            /*
             * Check if any fetch tasks are hung
             * */
            tuneFetchQueues(now, idleTime)

            /*
             * Dump the remainder fetch items if feeder thread is no available and fetch item is few
             * */
            if (taskMonitor.numTasks <= FETCH_TASK_REMAINDER_NUMBER) {
                log.info("Totally remains only {} tasks", taskMonitor.numTasks)
                handleFewFetchItems()
            }

            /**
             * Check throughput(fetch speed)
             * */
            if (coreMetrics != null && now > throughputCheckTime
                    && coreMetrics.successTasksPerSecond < minSuccessPagesPerSecond) {
                lowThroughputCount++
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

            /**
             * Read local file commands
             * */
            if (FileCommand.check("finish-job $jobName")) {
                handleFinishJobCommand()
                log.info("Found finish-job command, exit the job ...")
                break
            }

            /**
             * All fetch tasks are finished
             * */
            if (isMissionComplete) {
                log.info("All done, exit the job ...")
                break
            }

            if (isFetchJobTimeout) {
                handleJobTimeout()
                log.info("Hit fetch job timeout {}, exit the job ...", jobTime.readable())
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
        if (now > nextCheckTime) {
            val nextTuneTime = poolTuneTime + poolTuneInterval
            if (now > nextTuneTime || idleTime > poolPendingTimeout) {
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
                    "minPageThroughputRate(p/s)", minSuccessPagesPerSecond,
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
                    "minPageThroughputRate(p/s)", minSuccessPagesPerSecond,
                    "removedSlowTasks", removedSlowTasks
            ))

            totalLowThroughputCount = 0
        }
    }
}
