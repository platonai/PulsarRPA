package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants.NCPU
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CONCURRENCY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlinx.coroutines.*
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class picks items from queues and fetches the pages
 */
class FetchLoop(
        private val fetchMonitor: FetchMonitor,
        private val fetchComponent: FetchComponent,
        private val taskScheduler: TaskScheduler,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>,
        conf: ImmutableConfig
): AutoCloseable {
    companion object {
        val pendingTasks = ConcurrentLinkedQueue<JobFetchTask>()
    }

    private val log = LoggerFactory.getLogger(FetchLoop::class.java)
    @Volatile
    private var lastWorkingPoolId: PoolId? = null
    private val closed = AtomicBoolean(false)
    private val concurrency = conf.getInt(FETCH_CONCURRENCY, NCPU).coerceAtLeast(2)
    private val numRunning = AtomicInteger()
    private val isAlive get() = !fetchMonitor.missionComplete && !closed.get()

    fun start() {
        val loop = this

        GlobalScope.launch {
            fetchMonitor.registerFetchLoop(loop)

            while (isAlive) {
                numRunning.incrementAndGet()
                launch(Dispatchers.Default + CoroutineName("w")) {
                    try {
                        schedule()?.let { fetch(it) }
                    } finally {
                        numRunning.decrementAndGet()
                    }
                }

                while (isAlive && numRunning.get() >= concurrency) {
                    delay(1000)
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
                withContext(Dispatchers.IO) {
                    if (log.isInfoEnabled) {
                        log.info(MessageWriter.getFetchCompleteReport(page))
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

    private fun schedule(): JobFetchTask? {
        // find tasks from pending queue
        var fetchTask: JobFetchTask? = pendingTasks.remove()
        if (fetchTask != null) {
            return fetchTask
        }

        fetchTask = taskScheduler.schedule(lastWorkingPoolId)

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
