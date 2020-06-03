package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.config.AppConstants
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
) : Thread(), Comparable<FeedThread>, Parameterized, AutoCloseable {
    private val LOG = LoggerFactory.getLogger(FeedThread::class.java)

    private val id = instanceSequence.incrementAndGet()
    private val checkInterval = Duration.ofSeconds(2)
    private var fetchThreads = conf.getUint(FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)!!
    private val fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofMinutes(30))
    private var jobDeadline = Instant.now().plus(fetchJobTimeout)
    private var initBatchSize = conf.getUint(FETCH_FEEDER_INIT_BATCH_SIZE, fetchThreads)!!
    private val closed = AtomicBoolean(false)

    private lateinit var currentIter: Iterator<IFetchEntry>
    private var totalFeed = 0

    val isClosed: Boolean
        get() = closed.get()

    init {
        this.isDaemon = true
        this.name = "feeder-$id"
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

            while (!isClosed && Instant.now().isBefore(jobDeadline) && hasMore) {
                ++round

                var feedInRound = 0
                while (feedInRound < batchSize && currentIter.hasNext() && hasMore) {
                    val entry = currentIter.next()
                    val page = entry.page ?: continue
                    tasksMonitor.produce(context.jobId, page)

                    ++totalFeed
                    ++feedInRound

                    if (!currentIter.hasNext()) {
                        hasMore = context.nextKey()
                        if (hasMore) {
                            currentIter = context.values.iterator()
                        }
                    }
                }

                if (round % 5 == 0 && LOG.isInfoEnabled) {
                    report(round, batchSize, feedInRound)
                }

                try {
                    sleep(checkInterval.toMillis())
                } catch (ignored: Exception) {}

                batchSize = adjustFeedBatchSize(batchSize)
            }

            discardAll()

            tasksMonitor.setFeederCompleted()
        } catch (e: Throwable) {
            LOG.error("Feeder error reading input, record $totalFeed", e)
        } finally {
            fetchMonitor.unregisterFeedThread(this)
        }

        LOG.info("Feeder finished. Feed " + round + " rounds, Last feed batch size : "
                + batchSize + ", feed total " + totalFeed + " records. ")
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                currentThread().interrupt()
                join()
            } catch (ignored: InterruptedException) {}
        }
    }

    private fun report(round: Int, batchSize: Float, feedInRound: Int) {
        Params.of(
                "Feed round", round,
                "batchSize", batchSize,
                "feedInRound", feedInRound,
                "totalFeed", totalFeed,
                "readyTasks", tasksMonitor.numReadyTasks,
                "pendingTasks", tasksMonitor.numPendingTasks,
                "finishedTasks", tasksMonitor.numFinishedTasks,
                "fetchThreads", fetchThreads
        ).withLogger(LOG).info(true)
    }

    private fun adjustFeedBatchSize(batchSize: Float): Float {
        var size = batchSize
        // TODO : Why readyTasks is always be very small when fetching news pages?
        val readyTasks = tasksMonitor.numReadyTasks.get()
        val pagesThroughput = taskScheduler.averagePageThroughput
        val recentPages = pagesThroughput * checkInterval.seconds
        // TODO : Every batch size should be greater than pages fetched during last wait interval

        if (size <= 1) {
            size = 1f
        }

        if (readyTasks <= fetchThreads) {
            // No ready tasks, increase batch size
            size += size * 0.2f
        } else if (readyTasks <= 2 * fetchThreads) {
            // Too many ready tasks, decrease batch size
            size -= size * 0.2f
        } else {
            // Ready task number is OK, do not feed this time
            size = 0f
        }

        return size
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
