package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class feeds the fetchMonitor with input items, and re-fills them as
 * items are consumed by FetcherThread-s.
 */
class FeedThread(
        private val fetchMonitor: FetchMonitor,
        private val taskScheduler: TaskScheduler,
        private val tasksMonitor: TaskMonitor,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>,
        private val conf: ImmutableConfig
) : Thread(), Comparable<FeedThread>, Parameterized {
    private val LOG = LoggerFactory.getLogger(FeedThread::class.java)

    private val id: Int = instanceSequence.incrementAndGet()
    private val checkInterval: Duration = Duration.ofSeconds(2)
    private var fetchThreads: Int = conf.getUint(FETCH_THREADS_FETCH, 10)!!
    private val fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofMinutes(30))
    private var jobDeadline: Instant = Instant.now().plus(fetchJobTimeout)
    private var initBatchSize: Int = conf.getUint(FETCH_FEEDER_INIT_BATCH_SIZE, fetchThreads)!!
    private val completed = AtomicBoolean(false)

    private lateinit var currentIter: Iterator<IFetchEntry>
    private var totalTaskCount = 0

    val isCompleted: Boolean
        get() = completed.get()

    init {
        this.isDaemon = true
        this.name = javaClass.simpleName + "-" + id
        LOG.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", javaClass.simpleName,
                "fetchThreads", fetchThreads,
                "initBatchSize", initBatchSize,
                "id", id
        )
    }

    override fun run() {
        fetchMonitor.registerFeedThread(this)

        var batchSize = initBatchSize.toFloat()
        var round = 0

        try {
            var hasMore = context.nextKey()
            if (hasMore) {
                currentIter = context.values.iterator()
            }

            while (!isCompleted && Instant.now().isBefore(jobDeadline) && hasMore) {
                ++round

                var taskCount = 0
                while (taskCount < batchSize && currentIter.hasNext() && hasMore) {
                    val entry = currentIter.next()
                    val page = entry.page
                    tasksMonitor.produce(context.jobId, page)

                    ++totalTaskCount
                    ++taskCount

                    if (!currentIter.hasNext()) {
                        hasMore = context.nextKey()
                        if (hasMore) {
                            currentIter = context.values.iterator()
                        }
                    }
                }

                Params.of(
                        "Round", round,
                        "batchSize", batchSize,
                        "feedTasks", taskCount,
                        "totalTaskCount", totalTaskCount,
                        "readyTasks", tasksMonitor.readyTaskCount(),
                        "fetchThreads", fetchThreads
                ).withLogger(LOG).debug(true)

                try {
                    sleep(checkInterval.toMillis())
                } catch (ignored: Exception) {}

                batchSize = adjustFeedBatchSize(batchSize)
            }

            discardAll()

            tasksMonitor.setFeederCompleted()
        } catch (e: Throwable) {
            LOG.error("Feeder error reading input, record $totalTaskCount", e)
        } finally {
            fetchMonitor.unregisterFeedThread(this)
        }

        LOG.info("Feeder finished. Feed " + round + " rounds, Last feed batch size : "
                + batchSize + ", feed total " + totalTaskCount + " records. ")
    }

    fun exitAndJoin() {
        completed.set(true)
        try {
            join()
        } catch (e: InterruptedException) {
            LOG.error(e.toString())
        }
    }

    private fun adjustFeedBatchSize(batchSize_: Float): Float {
        var batchSize = batchSize_
        // TODO : Why readyTasks is always be very small?
        val readyTasks = tasksMonitor.readyTaskCount()
        val pagesThroughput = taskScheduler.getAveragePageThroughput()
        val recentPages = pagesThroughput * checkInterval.seconds
        // TODO : Every batch size should be greater than pages fetched during last wait interval

        if (batchSize <= 1) {
            batchSize = 1f
        }

        if (readyTasks <= fetchThreads) {
            // No ready tasks, increase batch size
            batchSize += (batchSize * 0.2).toFloat()
        } else if (readyTasks <= 2 * fetchThreads) {
            // Too many ready tasks, decrease batch size
            batchSize -= (batchSize * 0.2).toFloat()
        } else {
            // Ready task number is OK, do not feed this time
            batchSize = 0f
        }

        return batchSize
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun discardAll() {
        while (context.nextKey()) {
            currentIter = context.values.iterator()
            while (currentIter.hasNext()) {
                currentIter.next()
            }
        }
    }

    override fun compareTo(other: FeedThread): Int {
        return id - other.id
    }

    companion object {
        private val instanceSequence = AtomicInteger()
    }
}
