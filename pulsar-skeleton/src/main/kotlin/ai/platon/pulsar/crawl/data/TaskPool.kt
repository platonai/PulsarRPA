package ai.platon.pulsar.crawl.data

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.fetch.FetchTask
import com.google.common.collect.Lists
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * This class handles FetchItems which come from the same host ID (be it
 * a proto/hostname or proto/IP pair).
 *
 * It also keeps track of requests in progress and elapsed time between requests.
 */
class TaskPool(val id: PoolId,
               /** Host group mode : can be by ip, by host or by domain  */
               val groupMode: URLUtil.GroupMode,
               /** Max thread count for this queue  */
               private val allowedThreads: Int,
               /** Crawl delay for the queue  */
               private val crawlDelay: Duration,
               /** Minimal crawl delay for the queue  */
               private val minCrawlDelay: Duration,
               /** Once timeout, the pending items should be put to the ready queue again  */
               private val pendingTimeout: Duration
) : Comparable<TaskPool>, Parameterized {

    /** Hold all tasks ready to fetch  */
    private val readyTasks = LinkedList<FetchTask>()
    /** Hold all tasks are fetching  */
    private val pendingTasks = TreeMap<Int, FetchTask>()
    /** If a task costs more then this duration, it's a slow task  */
    private val slowTaskThreshold = Duration.ofMillis(500)
    /** Record timing cost of slow tasks  */
    private val slowTasksRecorder = CircularFifoQueue<Duration>(RECENT_TASKS_COUNT_LIMIT)

    /** Next fetch time  */
    private var nextFetchTime: Instant? = null
    private var recentFinishedTasks = 1
    private var recentFetchMillis: Long = 1
    private var totalFinishedTasks = 1
    private var totalFetchMillis: Long = 1
    private var unreachableTasks = 0

    /**
     * If a fetch queue is inactive, the queue does not accept any tasks, nor serve any requests,
     * but still hold pending tasks, waiting to finish
     */
    var status = Status.ACTIVITY
        private set
    val priority: Int get() = id.priority
    val protocol: String get() = id.protocol
    val host: String get() = id.host
    val isSlow: Boolean get() = isSlow(Duration.ofSeconds(1))
    val isActive: Boolean get() = this.status == Status.ACTIVITY
    val isInactive: Boolean get() = this.status == Status.INACTIVITY
    val isRetired: Boolean get() = this.status == Status.RETIRED

    val readyCount: Int get() = readyTasks.size
    val pendingCount: Int get() = pendingTasks.size
    val finishedCount: Int get() = totalFinishedTasks
    val slowTaskCount: Int get() = slowTasksRecorder.size

    /**
     * Average cost in seconds
     */
    val averageTimeCost: Double get() = totalFetchMillis.toDouble() / 1000.0 / totalFinishedTasks.toDouble()
    val averageRecentTimeCost: Double get() = recentFetchMillis.toDouble() / 1000.0 / recentFinishedTasks.toDouble()
    /**
     * Throughput rate in seconds
     */
    val averageThoRate: Double get() = totalFinishedTasks / (totalFetchMillis / 1000.0)
    val averageRecentThoRate: Double get() = recentFinishedTasks / (recentFetchMillis / 1000.0)

    val costReport: String
        get() = String.format("%1$40s -> aveTimeCost : %2$.2fs/p, avaThoRate : %3$.2fp/s",
                id, averageTimeCost, averageThoRate)

    enum class Status {
        ACTIVITY, INACTIVITY, RETIRED
    }

    init {
        this.nextFetchTime = Instant.now()
    }

    override fun getParams(): Params {
        val df = DecimalFormat("###0.##")

        return Params.of(
                "className", javaClass.simpleName,
                "status", status,
                "id", id,
                "allowedThreads", allowedThreads,
                "pendingTasks", pendingTasks.size,
                "crawlDelay", crawlDelay,
                "minCrawlDelay", minCrawlDelay,
                "now", DateTimeUtil.now(),
                "nextFetchTime", DateTimeUtil.format(nextFetchTime),
                "aveTimeCost(s)", df.format(averageTimeCost),
                "aveThoRate(s)", df.format(averageThoRate),
                "readyTasks", readyCount,
                "pendingTasks", pendingCount,
                "finsihedTasks", finishedCount,
                "unreachableTasks", unreachableTasks
        )
    }

    /** Produce a task to this queue. Retired queues do not accept any tasks  */
    fun produce(task: FetchTask?) {
        if (task == null || status != Status.ACTIVITY) {
            return
        }

        if (task.priority != id.priority || task.host != id.host) {
            LOG.error("Queue id mismatches with FetchTask #$task")
        }

        readyTasks.add(task)
    }

    /** Ask a task from this queue. Retired queues do not assign any tasks  */
    fun consume(): FetchTask? {
        if (status != Status.ACTIVITY) {
            return null
        }

        // TODO : Why we need this restriction?
        if (allowedThreads > 0 && pendingTasks.size >= allowedThreads) {
            return null
        }

        val now = Instant.now()
        if (now.isBefore(nextFetchTime!!)) {
            return null
        }

        val fetchTask = readyTasks.poll()
        if (fetchTask != null) {
            hangUp(fetchTask, now)
        }

        return fetchTask
    }

    /**
     * Note : We have set response time for each page, @see {HttpBase#getProtocolOutput}
     */
    fun finish(fetchTask: FetchTask, asap: Boolean): Boolean {
        val itemId = fetchTask.itemId
        val fetchTask = pendingTasks.remove(itemId)
        if (fetchTask == null) {
            LOG.warn("Failed to remove FetchTask : $itemId")
            return false
        }

        val finishTime = Instant.now()
        setNextFetchTime(finishTime, asap)

        val timeCost = Duration.between(fetchTask.pendingStart, finishTime)
        if (timeCost.compareTo(slowTaskThreshold) > 0) {
            slowTasksRecorder.add(timeCost)
        }

        ++recentFinishedTasks
        recentFetchMillis += timeCost.toMillis()
        if (recentFinishedTasks > RECENT_TASKS_COUNT_LIMIT) {
            recentFinishedTasks = 1
            recentFetchMillis = 1
        }

        ++totalFinishedTasks
        totalFetchMillis += timeCost.toMillis()

        return true
    }

    fun finish(itemId: Int, asap: Boolean): Boolean {
        val item = pendingTasks[itemId]
        return item != null && finish(item, asap)
    }

    fun getPendingTask(itemID: Int): FetchTask? {
        return pendingTasks[itemID]
    }

    /**
     * Hang up the task and wait for completion. Move the fetch task to pending queue.
     */
    private fun hangUp(fetchTask: FetchTask?, now: Instant) {
        if (fetchTask == null) {
            return
        }

        fetchTask.pendingStart = now
        pendingTasks[fetchTask.itemId] = fetchTask
    }

    fun hasTasks(): Boolean {
        return hasReadyTasks() || hasPendingTasks()
    }

    fun hasReadyTasks(): Boolean {
        return !readyTasks.isEmpty()
    }

    fun hasPendingTasks(): Boolean {
        return !pendingTasks.isEmpty()
    }

    fun pendingTaskExists(itemId: Int): Boolean {
        return pendingTasks.containsKey(itemId)
    }

    fun isSlow(threshold: Duration): Boolean {
        return averageRecentTimeCost > threshold.seconds
    }

    fun enable() {
        this.status = Status.ACTIVITY
    }

    fun disable() {
        this.status = Status.INACTIVITY
    }

    fun retire() {
        this.status = Status.RETIRED
    }

    /**
     * Retune the queue to avoid hung tasks, pending tasks are push to ready queue so they can be re-fetched
     *
     * In crowdsourcing mode, it's a common situation to lost
     * the fetching mission and should the task should be restarted
     *
     * @param force If force is true, reload all pending fetch items immediately, otherwise, reload only exceeds pendingTimeout
     */
    fun retune(force: Boolean) {
        val now = Instant.now()

        val readyList = Lists.newArrayList<FetchTask>()
        val pendingList = HashMap<Int, FetchTask>()

        pendingTasks.values.forEach { fetchTask ->
            if (force || fetchTask.pendingStart.plus(pendingTimeout).isBefore(now)) {
                readyList.add(fetchTask)
            } else {
                pendingList[fetchTask.itemId] = fetchTask
            }
        }

        pendingTasks.clear()
        readyTasks.addAll(readyList)
        pendingTasks.putAll(pendingList)
    }

    fun clearReadyQueue(): Int {
        val count = readyTasks.size
        readyTasks.clear()
        return count
    }

    fun clearPendingQueue(): Int {
        val count = pendingTasks.size
        pendingTasks.clear()
        return count
    }

    fun clearPendingTasksIfFew(threshold: Int): Int {
        val count = pendingTasks.size

        if (count > threshold) {
            return 0
        }

        if (pendingTasks.isEmpty()) {
            return 0
        }

        val now = Instant.now()
        val report = pendingTasks.values.take(threshold)
                .joinToString("\n", "Clearing slow pending itmes : ")
                { it.url + " : " + Duration.between(it.pendingStart, now) }
        LOG.info(report)

        pendingTasks.clear()

        return count
    }

    fun dump() {
        LOG.info(params.formatAsLine())

        var i = 0
        val limit = 20
        var report = "\nDrop the following tasks : "
        var fetchTask: FetchTask? = readyTasks.poll()
        while (fetchTask != null && ++i <= limit) {
            report += "  " + i + ". " + fetchTask.url + "\t"
            fetchTask = readyTasks.poll()
        }
        LOG.info(report)
    }

    private fun setNextFetchTime(finishTime: Instant, asap: Boolean) {
        if (!asap) {
            nextFetchTime = finishTime.plus(if (allowedThreads > 1) minCrawlDelay else crawlDelay)
        } else {
            nextFetchTime = finishTime
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other == null || other !is TaskPool) {
            false
        } else id == other.id

    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun compareTo(other: TaskPool): Int {
        return id.compareTo(other.id)
    }

    override fun toString(): String {
        return id.toString()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(TaskPool::class.java)
        const val RECENT_TASKS_COUNT_LIMIT = 100
    }
}
