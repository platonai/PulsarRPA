package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.MetricsSystem
import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.common.Urls
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
import ai.platon.pulsar.persist.metadata.FetchMode
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
        private val taskTracker: FetchTaskTracker,
        private val metrics: MetricsSystem,
        immutableConfig: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {
    private val log = LoggerFactory.getLogger(TaskMonitor::class.java)

    private lateinit var options: FetchOptions
    private val id = instanceSequence.incrementAndGet()
    private val feederCompleted = AtomicBoolean(false)
    private val fetchPools = PoolQueue()
    private var lastTaskPriority = Integer.MIN_VALUE

    /**
     * Tracking time cost of each pool
     */
    private val poolTimeCosts = DualTreeBidiMap<PoolId, Double>()
    /**
     * Tracking access thread for each each pool
     */
    private val poolServedThreads = TreeMultimap.create<String, String>()
    /**
     * Task counters
     */
    private val readyTaskCount = AtomicInteger(0)
    private val pendingTaskCount = AtomicInteger(0)
    private val finishedTaskCount = AtomicInteger(0)

    private var groupMode = immutableConfig.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST)
    /**
     * The minimal page throughout rate
     */
    private var minPageThoRate = immutableConfig.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1)
    /**
     * Delay before crawl
     */
    private var crawlDelay = immutableConfig.getDuration(FETCH_QUEUE_DELAY, Duration.ofSeconds(5))
    private var minCrawlDelay = immutableConfig.getDuration(FETCH_QUEUE_MIN_DELAY, Duration.ofSeconds(0))

    /**
     * The maximal number of threads allowed to access a task pool
     */
    private var poolThreads: Int = 1

    /**
     * Once timeout, the pending items should be put to the ready pool again.
     */
    private var poolPendingTimeout = immutableConfig.getDuration(FETCH_PENDING_TIMEOUT, Duration.ofMinutes(3))
    /**
     * @return Return true if the feeder is completed
     */
    val isFeederCompleted get() = feederCompleted.get()

    val queueCount get() = fetchPools.size

    private val slowestQueue: TaskPool?
        get() {
            var pool: TaskPool? = null

            while (!fetchPools.isEmpty() && pool == null) {
                val maxCost = poolTimeCosts.inverseBidiMap().lastKey()
                val id = poolTimeCosts.inverseBidiMap()[maxCost]
                if (id != null) {
                    poolTimeCosts.remove(id)
                    pool = fetchPools.find(id)
                }
            }

            return pool
        }

    override fun setup(jobConf: ImmutableConfig) {
        this.options = FetchOptions(jobConf)

        poolThreads = if (options.fetchMode == FetchMode.CROWDSOURCING) Integer.MAX_VALUE
        else jobConf.getInt(FETCH_THREADS_PER_QUEUE, 1)

        log.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "poolThreads", poolThreads,
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

        val task = FetchTask.create(jobID, page.fetchPriority, page.url, page, groupMode)

        if (task != null) {
            produce(task)
        } else {
            log.warn("Failed to create FetchTask | {}", page.url)
        }
    }

    @Synchronized
    fun produce(task: FetchTask) {
        doProduce(task)
    }

    @Synchronized
    fun consume(poolId: PoolId? = null): FetchTask? {
        if (poolId == null) {
            return consumeInternal()
        }

        val pool = fetchPools.find(poolId) ?: return null

        return if (isConsumable(pool)) {
            consumeUnchecked(pool)
        } else null
    }

    @Synchronized
    fun finish(priority: Int, protocol: String, host: String, itemId: Int, asap: Boolean) {
        doFinish(PoolId(priority, protocol, host), itemId, asap)
    }

    @Synchronized
    fun finish(item: FetchTask) {
        finish(item.priority, item.protocol, item.host, item.itemId, false)
    }

    @Synchronized
    fun finishAsap(item: FetchTask) {
        finish(item.priority, item.protocol, item.host, item.itemId, true)
    }

    fun maintain() {
        fetchPools.forEach { maintain(it) }
    }

    private fun isConsumable(pool: TaskPool): Boolean {
        return pool.isActive && pool.hasReadyTasks() && taskTracker.isReachable(pool.host)
    }

    /** Maintain pool life time, return true if the life time status is changed, false otherwise  */
    private fun maintain(pool: TaskPool): TaskPool {
        val lastStatus = pool.status
        if (taskTracker.isGone(pool.host)) {
            retire(pool)
            log.info("Retire pool with unreachable host " + pool.id)
        } else if (isFeederCompleted && !pool.hasTasks()) {
            // All tasks are finished, including pending tasks, we can remove the pool from the pools safely
            fetchPools.disable(pool)
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
    @Synchronized
    private fun consumeInternal(): FetchTask? {
        var pool = fetchPools.peek() ?: return null

        val nextPriority = pool.priority
        val priorityChanged = nextPriority < lastTaskPriority
        if (priorityChanged && fetchPools.hasPriorPendingTasks(nextPriority)) {
            // Waiting for all pending tasks with higher priority to be finished
            return null
        }

        if (priorityChanged) {
            log.info("Fetch priority changed : $lastTaskPriority -> $nextPriority")
        }

        pool = fetchPools.find { isConsumable(it) } ?: return null

        return consumeUnchecked(pool)
    }

    private fun consumeUnchecked(pool: TaskPool): FetchTask? {
        val item = pool.consume()

        if (item != null) {
            readyTaskCount.decrementAndGet()
            pendingTaskCount.incrementAndGet()
            lastTaskPriority = pool.priority
        }

        return item
    }

    private fun doProduce(task: FetchTask) {
        if (taskTracker.isGone(task.host)) {
            return
        }

        val url = task.urlString
        if (taskTracker.isGone(URLUtil.getHostName(url)) || taskTracker.isGone(url)) {
            log.warn("Ignore unreachable url (indicate task.getHost() failed) | {}", url)
            return
        }

        val poolId = task.poolId
        var pool = fetchPools.find(poolId)

        if (pool == null) {
            pool = fetchPools.findExtend(poolId)
            if (pool != null) {
                fetchPools.enable(pool)
            } else {
                pool = createFetchQueue(poolId)
                fetchPools.add(pool)
            }
        }
        pool.produce(task)

        readyTaskCount.incrementAndGet()
        poolTimeCosts[pool.id] = 0.0
    }

    private fun doFinish(poolId: PoolId, itemId: Int, asap: Boolean) {
        val pool = fetchPools.findExtend(poolId)

        if (pool == null) {
            log.warn("Attempt to finish item from unknown pool $poolId")
            return
        }

        if (!pool.pendingTaskExists(itemId)) {
            if (!fetchPools.isEmpty()) {
                log.warn("Attempt to finish unknown item: <{}, {}>", poolId, itemId)
            }

            return
        }

        pool.finish(itemId, asap)

        pendingTaskCount.decrementAndGet()
        finishedTaskCount.incrementAndGet()

        poolTimeCosts[poolId] = pool.averageRecentTimeCost
        poolServedThreads.put(poolId.host, Thread.currentThread().name.substring(THREAD_SEQUENCE_POS))
    }

    private fun retire(pool: TaskPool) {
        pool.retire()
        fetchPools.remove(pool)
    }

    @Synchronized
    fun report() {
        dump(FETCH_TASK_REMAINDER_NUMBER, false)

        taskTracker.report()

        reportCost()

        reportServedThreads()
    }

    fun trackSuccess(page: WebPage) {
        taskTracker.trackSuccess(page)
    }

    fun trackHostGone(url: String) {
        val isGone = taskTracker.trackHostGone(url)
        if (isGone) {
            retune(true)
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
    internal fun retune(force: Boolean) {
        val unreachablePools = fetchPools.filter { taskTracker.isGone(it.host) }

        unreachablePools.forEach { retire(it) }
        fetchPools.forEach { it.retune(force) }

        if (!unreachablePools.isEmpty()) {
            val report = unreachablePools
                    .joinToString (", ", "Retired unavailable pools : ") { it.id.toString() }
            log.info(report)
        }

        calculateTaskCounter()
    }

    @Synchronized
    fun findPendingTask(priority: Int, url: URL, itemID: Int): FetchTask? {
        val pool = fetchPools.findExtend(PoolId(priority, url))
        return pool?.getPendingTask(itemID)
    }

    /** Get a pending task, the task can be in working pools or in detached pools  */
    @Synchronized
    fun findPendingTask(poolId: PoolId, itemID: Int): FetchTask? {
        val pool = fetchPools.findExtend(poolId)
        return pool?.getPendingTask(itemID)
    }

    @Synchronized
    internal fun dump(limit: Int, drop: Boolean) {
        fetchPools.dump(limit, drop)
        calculateTaskCounter()
    }

    @Synchronized
    internal fun tryClearSlowestQueue(): Int {
        val pool = slowestQueue ?: return 0

        val df = DecimalFormat("0.##")

        if (pool.averageThoRate >= minPageThoRate) {
            Params.of(
                    "EfficientQueue", pool.id,
                    "ReadyTasks", pool.readyCount,
                    "PendingTasks", pool.pendingCount,
                    "FinishedTasks", pool.finishedCount,
                    "SlowTasks", pool.slowTaskCount,
                    "Throughput, ", df.format(pool.averageTimeCost) + "s/p" + ", " + df.format(pool.averageThoRate) + "p/s"
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
                "Throughput, ", df.format(pool.averageTimeCost) + "s/p" + ", " + df.format(pool.averageThoRate) + "p/s",
                "Deleted", deleted).withLogger(log).info(true)

        return deleted
    }

    @Synchronized
    @Throws(Exception::class)
    override fun close() {
        log.info("[Destruction] Closing TasksMonitor #$id")

        report()

        fetchPools.clear()
        readyTaskCount.set(0)
    }

    @Synchronized
    fun clearReadyTasks(): Int {
        var count = 0

        val costRecorder = TreeMap<Double, String>(Comparator.reverseOrder())
        for (pool in fetchPools) {
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
        pendingTaskCount.addAndGet(-deleted)
        return deleted
    }

    private fun clearReadyTasks(pool: TaskPool): Int {
        val deleted = pool.clearReadyQueue()

        readyTaskCount.addAndGet(-deleted)
        if (readyTaskCount.get() <= 0 && fetchPools.size == 0) {
            readyTaskCount.set(0)
        }

        return deleted
    }

    fun taskCount(): Int {
        return readyTaskCount.get() + pendingTaskCount()
    }

    fun readyTaskCount(): Int {
        return readyTaskCount.get()
    }

    fun pendingTaskCount(): Int {
        return pendingTaskCount.get()
    }

    fun getFinishedTaskCount(): Int {
        return finishedTaskCount.get()
    }

    private fun calculateTaskCounter() {
        var readyCount = 0
        var pendingCount = 0
        fetchPools.forEach {
            readyCount += it.readyCount
            pendingCount += it.pendingCount
        }
        readyTaskCount.set(readyCount)
        pendingTaskCount.set(pendingCount)
    }

    private fun createFetchQueue(poolId: PoolId): TaskPool {
        val pool = TaskPool(poolId,
                groupMode,
                poolThreads,
                crawlDelay!!,
                minCrawlDelay!!,
                poolPendingTimeout)
        log.info("FetchQueue created : $pool")
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
        var report = "Top slow hosts : \n" + fetchPools.costReport
        report += "\n"
        log.info(report)
    }

    companion object {
        private val instanceSequence = AtomicInteger(0)
        private const val THREAD_SEQUENCE_POS = "FetchThread-".length
    }
}
