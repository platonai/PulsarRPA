package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.FETCH_TASK_REMAINDER_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.FetchOptions
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.crawl.fetch.data.PoolQueue
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.TreeMultimap
import org.apache.commons.collections4.bidimap.DualTreeBidiMap
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.net.URL
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tasks Monitor
 * TODO: review locks(synchronization)
 */
class TaskMonitor(
        private val fetchMetrics: FetchMetrics,
        private val metrics: MiscMessageWriter,
        conf: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {
    private val log = LoggerFactory.getLogger(TaskMonitor::class.java)

    private lateinit var options: FetchOptions
    private val id = instanceSequencer.incrementAndGet()
    private val feederCompleted = AtomicBoolean(false)
    private val taskPools = PoolQueue()
    private var lastTaskPriority = Integer.MIN_VALUE

    /**
     * Tracking time cost of each pool
     */
    private val poolTimeCosts = DualTreeBidiMap<PoolId, Double>()
    /**
     * Tracking access thread for each each pool
     */
    private val poolServedThreads = TreeMultimap.create<String, String>()

    private var groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST)
    /**
     * The minimal page throughout rate
     */
    private var minPageThoRate = conf.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1)
    /**
     * Delay before crawl
     */
    private var crawlDelay = conf.getDuration(FETCH_QUEUE_DELAY, Duration.ofSeconds(5))
    private var minCrawlDelay = conf.getDuration(FETCH_QUEUE_MIN_DELAY, Duration.ofSeconds(0))

    /**
     * The maximal number of threads allowed to access a task pool
     */
    private var numPoolThreads: Int = 5
    /**
     * Fetch threads
     */
    private var initFetchThreadCount: Int = conf.getInt(FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)

    /**
     * Once timeout, the pending items should be put to the ready pool again.
     */
    private var poolPendingTimeout = conf.getDuration(FETCH_PENDING_TIMEOUT, Duration.ofMinutes(3))

    private val closed = AtomicBoolean()

    val numTaskPools get() = taskPools.size
    /**
     * Task counters
     */
    val numReadyTasks = AtomicInteger(0)
    val numPendingTasks = AtomicInteger(0)
    val numFinishedTasks = AtomicInteger(0)
    val numTasks get() = numReadyTasks.get() + numPendingTasks.get()

    override fun setup(jobConf: ImmutableConfig) {
        // TODO: just parse from command line
        this.options = FetchOptions(jobConf)
        numPoolThreads = options.numPoolThreads
        log.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "numPoolThreads", numPoolThreads,
                "initFetchThreadCount", initFetchThreadCount,
                "groupMode", groupMode,
                "crawlDelay", crawlDelay,
                "minCrawlDelay", minCrawlDelay,
                "poolPendingTimeout", poolPendingTimeout
        )
    }

    fun setFeederCompleted() {
        feederCompleted.set(true)
    }

    @Synchronized
    fun produce(jobID: Int, page: WebPage) {
        page.fetchMode = options.fetchMode

        val task = JobFetchTask.create(jobID, page.fetchPriority, page.url, page, groupMode)

        if (task != null) {
            produce(task)
        } else {
            log.warn("Failed to create FetchTask | {}", page.url)
        }
    }

    @Synchronized
    fun produce(task: JobFetchTask) {
        doProduce(task)
    }

    @Synchronized
    fun consume(poolId: PoolId? = null): JobFetchTask? {
        if (poolId == null) {
            return consumeFromAnyPool()
        }

        val pool = taskPools.find(poolId) ?: return null

        return if (isConsumable(pool)) {
            consumeUnchecked(pool)
        } else null
    }

    @Synchronized
    fun finish(item: JobFetchTask) {
        doFinish(PoolId(item.priority, item.protocol, item.host), item.itemId, false)
    }

    @Synchronized
    fun finishAsap(item: JobFetchTask) {
        doFinish(PoolId(item.priority, item.protocol, item.host), item.itemId, true)
    }

    fun maintain() {
        taskPools.forEach { maintain(it) }
    }

    private fun isConsumable(pool: TaskPool): Boolean {
        return pool.isActive && pool.hasReadyTasks() && fetchMetrics.isReachable(pool.host)
    }

    /** Maintain pool life time, return true if the life time status is changed, false otherwise  */
    private fun maintain(pool: TaskPool): TaskPool {
        val lastStatus = pool.status
        if (fetchMetrics.isGone(pool.host)) {
            retire(pool)
            log.info("Retire pool with unreachable host " + pool.id)
        } else if (feederCompleted.get() && !pool.hasTasks()) {
            // All tasks are finished, including pending tasks, we can remove the pool from the pool list safely
            taskPools.disable(pool)
        }

        val status = pool.status
        if (status !== lastStatus) {
            Params.of(
                    "FetchQueue", pool.id,
                    "status", lastStatus.toString() + " -> " + pool.status,
                    "ready", pool.readyCount,
                    "pending", pool.pendingCount,
                    "finished", pool.finishedCount
            ).withLogger(log).info(true)
        }

        return pool
    }

    /**
     * Find out the FetchQueue with top priority,
     * wait for all pending tasks with higher priority are finished
     */
    private fun consumeFromAnyPool(): JobFetchTask? {
        var pool = taskPools.peek() ?: return null

        val nextPriority = pool.priority
        val priorityChanged = nextPriority < lastTaskPriority
        if (priorityChanged && taskPools.hasPriorPendingTasks(nextPriority)) {
            // Waiting for all pending tasks with higher priority to be finished
            return null
        }

        if (priorityChanged) {
            log.info("Fetch priority changed : $lastTaskPriority -> $nextPriority")
        }

        pool = taskPools.find { isConsumable(it) } ?: return null

        return consumeUnchecked(pool)
    }

    private fun consumeUnchecked(pool: TaskPool): JobFetchTask? {
        val item = pool.consume()

        if (item != null) {
            numReadyTasks.decrementAndGet()
            numPendingTasks.incrementAndGet()
            lastTaskPriority = pool.priority
        }

        return item
    }

    private fun doProduce(task: JobFetchTask) {
        if (fetchMetrics.isGone(task.host)) {
            return
        }

        val url = task.urlString
        val host = URLUtil.getHostName(url)
        if (host == null || fetchMetrics.isGone(host) || fetchMetrics.isGone(url)) {
            log.warn("Ignore unreachable url (indicate task.getHost() failed) | {}", url)
            return
        }

        val poolId = task.poolId
        var pool = taskPools.find(poolId)

        if (pool == null) {
            pool = taskPools.findExtend(poolId)
            if (pool != null) {
                taskPools.enable(pool)
            } else {
                pool = createFetchQueue(poolId)
                taskPools.add(pool)
            }
        }
        pool.produce(task)

        numReadyTasks.incrementAndGet()
        poolTimeCosts[pool.id] = 0.0
    }

    private fun doFinish(poolId: PoolId, itemId: Int, asap: Boolean) {
        val pool = taskPools.findExtend(poolId)

        if (pool == null) {
            log.warn("Attempt to finish item from unknown pool $poolId")
            return
        }

        if (!pool.pendingTaskExists(itemId)) {
            if (!taskPools.isEmpty()) {
                log.warn("Attempt to finish unknown item: <{}, {}>", poolId, itemId)
            }

            return
        }

        pool.finish(itemId, asap)

        numPendingTasks.decrementAndGet()
        numFinishedTasks.incrementAndGet()

        poolTimeCosts[poolId] = pool.averageRecentTimeCost
        poolServedThreads.put(poolId.host, Thread.currentThread().name.substring(THREAD_SEQUENCE_POS))
    }

    private fun retire(pool: TaskPool) {
        pool.retire()
        taskPools.remove(pool)
    }

    @Synchronized
    fun report() {
        dump(FETCH_TASK_REMAINDER_NUMBER, false)

        reportCost()

        reportServedThreads()
    }

    fun trackHostGone(url: String) {
        val isGone = fetchMetrics.trackHostGone(url)
        if (isGone) {
            tune(true)
        }
    }

    /**
     * Reload pending fetch items so that the items can be re-fetched
     *
     *
     * In crowdsourcing mode, it's a common situation to lost
     * the fetching mission and should restart the task
     *
     * @param force reload all pending fetch items immediately
     */
    @Synchronized
    internal fun tune(force: Boolean) {
        taskPools.filter { fetchMetrics.isGone(it.host) }.onEach { retire(it) }.takeIf { it.isNotEmpty() }?.let { pool ->
            pool.joinToString (", ", "Unavailable pools : ") { it.id.toString() }.let { log.info(it) }
        }
        calculateTaskCounter()
    }

    @Synchronized
    fun findPendingTask(priority: Int, url: URL, itemID: Int): JobFetchTask? {
        val pool = taskPools.findExtend(PoolId(priority, url))
        return pool?.getPendingTask(itemID)
    }

    /** Get a pending task, the task can be in working pools or in detached pools  */
    @Synchronized
    fun findPendingTask(poolId: PoolId, itemID: Int): JobFetchTask? {
        val pool = taskPools.findExtend(poolId)
        return pool?.getPendingTask(itemID)
    }

    @Synchronized
    internal fun dump(limit: Int, drop: Boolean) {
        taskPools.dump(limit, drop)
        calculateTaskCounter()
    }

    @Synchronized
    internal fun tryClearSlowestQueue(): Int {
        val pool = getSlowestPool() ?: return 0

        val df = DecimalFormat("0.##")

        if (pool.averageTps >= minPageThoRate) {
            Params.of(
                    "EfficientQueue", pool.id,
                    "ReadyTasks", pool.readyCount,
                    "PendingTasks", pool.pendingCount,
                    "FinishedTasks", pool.finishedCount,
                    "SlowTasks", pool.slowTaskCount,
                    "Throughput, ", df.format(pool.averageTime) + "s/p" + ", " + df.format(pool.averageTps) + "p/s"
            ).withLogger(log).info(true)

            return 0
        }

        // slowest pools should retires as soon as possible
        retire(pool)

        val minPendingSlowTasks = 2
        clearPendingTasksIfFew(pool, minPendingSlowTasks)

        val deleted = clearReadyTasks(pool)

        Params.of(
                "SlowestQueue", pool.id,
                "ReadyTasks", pool.readyCount,
                "PendingTasks", pool.pendingCount,
                "FinishedTasks", pool.finishedCount,
                "SlowTasks", pool.slowTaskCount,
                "Throughput, ", df.format(pool.averageTime) + "s/p" + ", " + df.format(pool.averageTps) + "p/s",
                "Deleted", deleted).withLogger(log).info(true)

        return deleted
    }

    @Synchronized
    @Throws(Exception::class)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Closing TasksMonitor #$id")

            report()

            taskPools.clear()
            numReadyTasks.set(0)
        }
    }

    @Synchronized
    fun clearReadyTasks(): Int {
        var count = 0

        val costRecorder = TreeMap<Double, String>(Comparator.reverseOrder())
        for (pool in taskPools) {
            costRecorder[pool.averageRecentTimeCost] = pool.id.host

            if (pool.readyCount == 0) {
                continue
            }

            count += clearReadyTasks(pool)
        }

        reportCost(costRecorder)

        return count
    }

    private fun clearPendingTasksIfFew(pool: TaskPool, limit: Int): Int {
        val deleted = pool.clearPendingTasksIfFew(limit)
        numPendingTasks.addAndGet(-deleted)
        return deleted
    }

    private fun clearReadyTasks(pool: TaskPool): Int {
        val deleted = pool.clearReadyQueue()

        numReadyTasks.addAndGet(-deleted)
        if (numReadyTasks.get() <= 0 && taskPools.size == 0) {
            numReadyTasks.set(0)
        }

        return deleted
    }

    private fun calculateTaskCounter() {
        var readyCount = 0
        var pendingCount = 0
        taskPools.forEach {
            readyCount += it.readyCount
            pendingCount += it.pendingCount
        }
        numReadyTasks.set(readyCount)
        numPendingTasks.set(pendingCount)
    }

    private fun createFetchQueue(poolId: PoolId): TaskPool {
        val pool = TaskPool(poolId,
                groupMode,
                numPoolThreads,
                crawlDelay,
                minCrawlDelay,
                poolPendingTimeout)

        log.info("FetchQueue created : $pool")

        return pool
    }

    private fun getSlowestPool(): TaskPool? {
        var pool: TaskPool? = null

        while (!taskPools.isEmpty() && pool == null) {
            val maxCost = poolTimeCosts.inverseBidiMap().lastKey()
            val id = poolTimeCosts.inverseBidiMap()[maxCost]
            if (id != null) {
                poolTimeCosts.remove(id)
                pool = taskPools.find(id)
            }
        }

        return pool
    }

    private fun reportServedThreads() {
        val report = StringBuilder()
        poolServedThreads.keySet()
                .map { Urls.reverseHost(it) }
                .sorted()
                .map { Urls.unreverseHost(it) }
                .forEach { poolId ->
                    val threads = "#" + StringUtils.join(poolServedThreads.get(poolId), ", #")
                    val line = String.format("%1$40s -> %2\$s\n", poolId, threads)
                    report.append(line)
                }

        log.info("Served threads : \n$report")
    }

    private fun reportCost(costRecorder: Map<Double, String>) {
        val sb = StringBuilder()

        sb.append(String.format("\n%s\n", "---------------Queue Cost Report--------------"))
        sb.append(String.format("%25s %s\n", "Ava Time(s)", "Queue Id"))
        val i = intArrayOf(0)
        costRecorder.entries.stream().limit(100).forEach { entry ->
            sb.append(String.format("%1$,4d.%2$,20.2f", ++i[0], entry.key))
            sb.append(" <- ")
            sb.append(entry.value)
            sb.append("\n")
        }

        log.info(sb.toString())
    }

    private fun reportCost() {
        var report = "Top slow hosts : \n" + taskPools.timeReport
        report += "\n"
        log.info(report)
    }

    companion object {
        private val instanceSequencer = AtomicInteger(0)
        private const val THREAD_SEQUENCE_POS = "FetchThread-".length
    }
}
