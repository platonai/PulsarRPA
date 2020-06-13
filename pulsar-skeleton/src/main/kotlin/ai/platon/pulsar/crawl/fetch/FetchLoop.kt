package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants.NCPU
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CONCURRENCY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.CompletedPageFormatter
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlinx.coroutines.*
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class picks items from queues and fetches the pages
 */
class FetchLoop(
        private val fetchMonitor: FetchMonitor,
        private val fetchComponent: FetchComponent,
        private val parseComponent: ParseComponent,
        private val taskScheduler: TaskScheduler,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>,
        conf: ImmutableConfig
): AutoCloseable, Comparable<FetchLoop> {
    companion object {
        val instanceSequencer = AtomicInteger()
        val pendingTasks = ArrayBlockingQueue<JobFetchTask>(1000)
    }

    private val log = LoggerFactory.getLogger(FetchLoop::class.java)
    val id = instanceSequencer.incrementAndGet()
    @Volatile
    private var lastWorkingPoolId: PoolId? = null
    private val closed = AtomicBoolean(false)
    private val concurrency = conf.getInt(FETCH_CONCURRENCY, NCPU).coerceAtLeast(2)
    private val numRunning = AtomicInteger()
    private val isAlive get() = !fetchMonitor.missionComplete && !closed.get()

    suspend fun start() {
        val loop = this

        supervisorScope {
            fetchMonitor.registerFetchLoop(loop)

            while (isAlive) {
                while (isAlive && numRunning.get() > concurrency) {
                    delay(1000)
                }

                numRunning.incrementAndGet()
                launch(Dispatchers.Default + CoroutineName("w")) {
                    try {
                        schedule()?.let { fetch(it) }
                    } catch (e: Throwable) {
                        log.warn("Unexpected exception", e)
                    } finally {
                        numRunning.decrementAndGet()
                    }
                }
            }

            fetchMonitor.unregisterFetchLoop(loop)
        }
    }

    private suspend fun fetch(task: JobFetchTask) {
        try {
            val page = fetchComponent.fetchContentDeferred(task.page)
            taskScheduler.finish(task.poolId, task.itemId)
            if (page.isNotInternal) {
                if (taskScheduler.parse) {
                    val parseResult = parseComponent.parse(page, null, false, true)
                    if (log.isTraceEnabled) {
                        log.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getTraceInfo())
                    }
                }

                withContext(Dispatchers.IO) {
                    if (log.isInfoEnabled) {
                        log.info(CompletedPageFormatter(page).toString())
                    }

                    write(page.key, page)
                }
            }
        } catch (e: Throwable) {
            log.error("Unexpected throwable", e)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    override fun compareTo(other: FetchLoop) = id - other.id

    override fun hashCode() = id

    override fun equals(other: Any?): Boolean {
        return other is FetchLoop && id == other.id
    }

    override fun toString() = "loop#$id"

    private fun schedule(): JobFetchTask? {
        // find tasks from pending queue
        var fetchTask = pendingTasks.poll()
        if (fetchTask != null) {
            return fetchTask
        }

        fetchTask = taskScheduler.schedule(lastWorkingPoolId)
        log.debug("scheduled task from pool {} | {}", fetchTask?.poolId, fetchTask)

        // If fetchTask != null, we fetch items from the same queue the next time
        // If fetchTask == null, the current queue is empty, fetch item from top queue the next time
        lastWorkingPoolId = fetchTask?.poolId

        return fetchTask
    }

    private fun write(key: String, page: WebPage) {
        try {
            // the page is fetched and status are updated, write to the file system
            context.write(key, page.unbox())
        } catch (e: IOException) {
            log.error("Failed to write to hdfs - {}", Strings.stringifyException(e))
        } catch (e: InterruptedException) {
            log.error("Interrupted - {}", Strings.stringifyException(e))
        } catch (e: Throwable) {
            log.error(Strings.stringifyException(e))
        }
    }
}
