package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.data.PoolId
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This class handles FetchTasks which come from the same host ID (be it
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
    private val log = LoggerFactory.getLogger(TaskPool::class.java)

    /** Hold all tasks ready to fetch  */
    private val readyTasks = ConcurrentLinkedQueue<JobFetchTask>()
    /** Hold all tasks are fetching  */
    private val pendingTasks = ConcurrentHashMap<Int, JobFetchTask>()
    /** If a task costs more then this duration, it's a slow task  */
    private val slowTaskThreshold = Duration.ofMinutes(2)
    /** Record timing cost of slow tasks  */
    private val slowTasksRecorder = CircularFifoQueue<Duration>(RECENT_TASKS_COUNT_LIMIT)

    /** Next fetch time  */
    private var nextFetchTime = Instant.now()
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
    val slowTaskCount: Int get() = synchronized(slowTasksRecorder) { slowTasksRecorder.size }

    /**
     * Average cost in seconds
     */
    val averageTime: Double get() = totalFetchMillis.toDouble() / 1000.0 / totalFinishedTasks.toDouble()
    val averageRecentTimeCost: Double get() = recentFetchMillis.toDouble() / 1000.0 / recentFinishedTasks.toDouble()
    /**
     * Average finished tasks per second
     */
    val averageTps: Double get() = totalFinishedTasks / (totalFetchMillis / 1000.0)
    val averageRecentTps: Double get() = recentFinishedTasks / (recentFetchMillis / 1000.0)

    val timeReport: String
        get() = String.format("%1$40s -> averageTime : %2$.2fs/p, avaThoRate : %3$.2fp/s", id, averageTime, averageTps)

    enum class Status {
        ACTIVITY, INACTIVITY, RETIRED
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
                "now", DateTimes.now(),
                "nextFetchTime", DateTimes.format(nextFetchTime),
                "aveTimeCost(s)", df.format(averageTime),
                "aveThoRate(s)", df.format(averageTps),
                "readyTasks", readyCount,
                "pendingTasks", pendingCount,
                "finsihedTasks", finishedCount,
                "unreachableTasks", unreachableTasks
        )
    }

    /** Produce a task to this queue. Retired queues do not accept any tasks  */
    fun produce(task: JobFetchTask) {
        if (status != Status.ACTIVITY) {
            return
        }

        if (task.priority != id.priority || task.host != id.host) {
            log.error("Queue id mismatches with FetchTask #$task")
        }

        readyTasks.add(task)
    }

    /** Ask a task from this queue. Retired queues do not assign any tasks  */
    fun consume(): JobFetchTask? {
        if (status != Status.ACTIVITY) {
            return null
        }

        if (allowedThreads > 0 && pendingTasks.size >= allowedThreads) {
            return null
        }

        val now = Instant.now()
        if (now.isBefore(nextFetchTime)) {
            return null
        }

        val fetchTask = readyTasks.poll()
        if (fetchTask != null) {
            hangUp(fetchTask, now)
        }

        return fetchTask
    }

    fun finish(itemId: Int, asap: Boolean): Boolean {
        val item = pendingTasks[itemId]
        return item != null && finish(item, asap)
    }

    /**
     * Note : We have set response time for each page, @see {HttpBase#getProtocolOutput}
     */
    fun finish(fetchTask: JobFetchTask, asap: Boolean): Boolean {
        pendingTasks.remove(fetchTask.itemId)

        val finishTime = Instant.now()
        setNextFetchTime(finishTime, asap)

        val elapsed = Duration.between(fetchTask.pendingStart, finishTime)
        if (elapsed > slowTaskThreshold) {
            synchronized(slowTasksRecorder) {
                slowTasksRecorder.add(elapsed)
            }
        }

        ++recentFinishedTasks
        recentFetchMillis += elapsed.toMillis()
        if (recentFinishedTasks > RECENT_TASKS_COUNT_LIMIT) {
            recentFinishedTasks = 1
            recentFetchMillis = 1
        }

        ++totalFinishedTasks
        totalFetchMillis += elapsed.toMillis()

        return true
    }

    fun getPendingTask(itemID: Int): JobFetchTask? {
        return pendingTasks[itemID]
    }

    /**
     * Hang up the task and wait for completion. Move the fetch task to pending queue.
     */
    private fun hangUp(fetchTask: JobFetchTask, now: Instant) {
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
                .joinToString("\n", "Clearing slow pending items : ")
                { it.urlString + " : " + Duration.between(it.pendingStart, now) }
        log.info(report)

        pendingTasks.clear()

        return count
    }

    fun dump(drop: Boolean) {
        log.info("Dump pool | " + params.formatAsLine())

        if (drop && readyTasks.isNotEmpty()) {
            var i = 0
            val limit = 20
            var report = "Drop the following tasks : "

            var fetchTask: JobFetchTask? = readyTasks.poll()
            while (fetchTask != null && ++i <= limit) {
                report += "  " + i + ". " + fetchTask.urlString + "\t"
                fetchTask = readyTasks.poll()
            }

            log.info(report)
        }
    }

    private fun setNextFetchTime(finishTime: Instant, asap: Boolean) {
        nextFetchTime = if (!asap) {
            finishTime.plus(if (allowedThreads > 1) minCrawlDelay else crawlDelay)
        } else {
            finishTime
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is TaskPool && id == other.id
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
        const val RECENT_TASKS_COUNT_LIMIT = 100
    }
}
