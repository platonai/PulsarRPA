package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
class FeedLoop(
        private val fetchMonitor: FetchMonitor,
        private val taskScheduler: TaskScheduler,
        private val tasksMonitor: TaskMonitor,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>,
        private val conf: ImmutableConfig
) : Comparable<FeedLoop>, Parameterized, AutoCloseable {
    private val LOG = LoggerFactory.getLogger(FeedLoop::class.java)

    private val id = instanceSequence.incrementAndGet()
    private val checkInterval = Duration.ofSeconds(2)

    private val numPrivacyContexts = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    private val maxActiveTags = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    private val fetchConcurrency = numPrivacyContexts * maxActiveTags

    private val fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofDays(2))
    private var jobDeadline = Instant.now().plus(fetchJobTimeout)
    private var initBatchSize = conf.getUint(FETCH_FEEDER_INIT_BATCH_SIZE, fetchConcurrency)!!
    private val closed = AtomicBoolean(false)

    private lateinit var currentIter: Iterator<IFetchEntry>
    private var totalFeed = 0

    val isActive get() = !closed.get()

    init {
        LOG.info(params.format())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", javaClass.simpleName,
                "fetchConcurrency", fetchConcurrency,
                "initBatchSize", initBatchSize,
                "id", id
        )
    }

    suspend fun start() {
        withContext(Dispatchers.IO) {
            doStart()
        }
    }

    private fun doStart() {
        fetchMonitor.registerFeedThread(this)

        var batchSize = initBatchSize.toFloat()
        var round = 0

        try {
            var hasMore = context.nextKey()
            if (hasMore) {
                currentIter = context.values.iterator()
            }

            while (isActive && Instant.now() < jobDeadline && hasMore) {
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
                    Thread.sleep(checkInterval.toMillis())
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

        LOG.info("Feeder finished. Feed {} rounds, last feed batch size : {}, feed total {} records",
                round, String.format("%.2f", batchSize), totalFeed)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    private fun report(round: Int, batchSize: Float, feedInRound: Int) {
        Params.of(
                "Feed round", round,
                "batchSize", String.format("%.2f", batchSize),
                "feedInRound", feedInRound,
                "totalFeed", totalFeed,
                "readyTasks", tasksMonitor.numReadyTasks,
                "pendingTasks", tasksMonitor.numPendingTasks,
                "finishedTasks", tasksMonitor.numFinishedTasks,
                "fetchConcurrency", fetchConcurrency
        ).withLogger(LOG).info(true)
    }

    private fun adjustFeedBatchSize(batchSize: Float): Float {
        var size = batchSize
        // TODO : Why readyTasks is always be very small when fetching news pages?
        val readyTasks = tasksMonitor.numReadyTasks.get()

        if (size <= 1) {
            size = 1f
        }

        if (readyTasks <= fetchConcurrency) {
            // No ready tasks, increase batch size
            size += size * 0.2f
        } else if (readyTasks <= 2 * fetchConcurrency) {
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

    override fun compareTo(other: FeedLoop) = id - other.id

    companion object {
        private val instanceSequence = AtomicInteger()
    }
}
