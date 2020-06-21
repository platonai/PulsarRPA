package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants.NCPU
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
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
import oshi.SystemInfo
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * This class picks items from queues and fetches the pages
 */
class FetchLoop(
        private val fetchMonitor: FetchMonitor,
        private val fetchComponent: FetchComponent,
        private val parseComponent: ParseComponent,
        private val taskScheduler: TaskScheduler,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>,
        private val immutableConfig: ImmutableConfig
): AutoCloseable, Comparable<FetchLoop> {
    companion object {
        val instanceSequencer = AtomicInteger()
        val pendingTasks = ArrayBlockingQueue<JobFetchTask>(1000)
        val numRunningTasks = AtomicInteger()
    }

    val id = instanceSequencer.incrementAndGet()
    private val log = LoggerFactory.getLogger(FetchLoop::class.java)
    private val loopConfig = immutableConfig.toVolatileConfig()
    @Volatile
    private var lastWorkingPoolId: PoolId? = null
    private val closed = AtomicBoolean(false)

    private val numPrivacyContexts = immutableConfig.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    private val concurrency = numPrivacyContexts * immutableConfig.getInt(BROWSER_MAX_ACTIVE_TABS, NCPU)

    private val isAppActive get() = !fetchMonitor.missionComplete && !closed.get()

    private val idleTimeout = Duration.ofMinutes(10)
    private var lastActiveTime = Instant.now()
    private val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    private val isIdle get() = idleTime > idleTimeout
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 500L * 1024 * 1024 // 500 MiB
    private val memoryRemaining get() = availableMemory - instanceRequiredMemory
    private val taskTimeout = Duration.ofMinutes(5)

    suspend fun start() {
        val loop = this

        supervisorScope {
            fetchMonitor.registerFetchLoop(loop)

            var j = 0
            while (isAppActive) {
                ++j

                while (isAppActive && numRunningTasks.get() > concurrency) {
                    delay(1000)
                }

                while (isAppActive && memoryRemaining < 0) {
                    if (j % 20 == 0) {
                        handleMemoryShortage(j)
                    }
                    delay(1000)
                }

                numRunningTasks.incrementAndGet()
                launch(Dispatchers.Default + CoroutineName("w")) {
                    try {
                        schedule()?.let { fetch(it) }
                    } catch (e: IllegalStateException) {
                        log.warn("Illegal state | {}", e.message)
                        return@launch
                    } catch (e: Throwable) {
                        log.warn("Unexpected exception", e)
                    } finally {
                        numRunningTasks.decrementAndGet()
                    }
                }
            }

            fetchMonitor.unregisterFetchLoop(loop)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Fetch loop #{} is closed", id)
        }
    }

    override fun compareTo(other: FetchLoop) = id - other.id

    override fun hashCode() = id

    override fun equals(other: Any?) = other is FetchLoop && id == other.id

    override fun toString() = "#$id"

    private fun schedule(): JobFetchTask? {
        if (!isAppActive) {
            return null
        }

        // find tasks from pending queue
        // TODO: pendingTasks is not used
        var fetchTask = pendingTasks.poll()
        if (fetchTask != null) {
            return fetchTask
        }

        fetchTask = taskScheduler.schedule(lastWorkingPoolId)
        log.takeIf { it.isTraceEnabled }?.trace("scheduled task from pool {} | {}", fetchTask?.poolId, fetchTask)

        // If fetchTask != null, we fetch items from the same queue the next time
        // If fetchTask == null, the current queue is empty, fetch item from top queue the next time
        lastWorkingPoolId = fetchTask?.poolId

        return fetchTask
    }

    private suspend fun fetch(task: JobFetchTask) {
        if (!isAppActive) {
            return
        }

        try {
            task.page.volatileConfig = loopConfig

            val page = try {
                withTimeoutOrNull(taskTimeout.toMillis()) {
                    fetchComponent.fetchContentDeferred(task.page)
                }
            } finally {
                taskScheduler.finish(task.poolId, task.itemId)
            }

            if (page == null) {
                log.warn("Fetch task is cancelled for timeout ({}) | {}", taskTimeout, task.urlString)
                return
            }

            if (page.isInternal) {
                log.warn("Fetch task is an internal page | {}", task.urlString)
                return
            }

            if (taskScheduler.parse) {
                val parseResult = parseComponent.parse(page, null, false, true)
                if (log.isTraceEnabled) {
                    log.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getTraceInfo())
                }
            }

            withContext(Dispatchers.IO) {
                log.takeIf { it.isInfoEnabled }?.info(CompletedPageFormatter(page).toString())
                write(page.key, page)
            }
        } catch (e: Throwable) {
            log.error("Unexpected throwable", e)
        }
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

    private fun handleMemoryShortage(j: Int) {
        log.info("$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
                numRunningTasks,
                Strings.readableBytes(availableMemory),
                Strings.readableBytes(instanceRequiredMemory),
                Strings.readableBytes(abs(memoryRemaining))
        )
        System.gc()
    }
}
