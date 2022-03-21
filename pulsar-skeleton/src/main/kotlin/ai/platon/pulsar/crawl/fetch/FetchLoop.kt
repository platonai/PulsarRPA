package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.metrics.CommonCounter
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlinx.coroutines.*
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.io.IOException
import java.time.Duration
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
        val illegalState = AtomicBoolean()
    }

    private val enumCounters = AppMetrics.reg.enumCounterRegistry

    val id = instanceSequencer.incrementAndGet()
    private val log = LoggerFactory.getLogger(FetchLoop::class.java)
    private val loopConfig = immutableConfig.toVolatileConfig()
    private val closed = AtomicBoolean(false)

    private val numPrivacyContexts = immutableConfig.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    private val fetchConcurrency = numPrivacyContexts * immutableConfig.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    private val isAppActive get() = !fetchMonitor.isMissionComplete && !closed.get() && !illegalState.get()

    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 500L * 1024 * 1024 // 500 MiB
    private val memoryRemaining get() = availableMemory - instanceRequiredMemory
    private val fetchTaskTimeout = Duration.ofMinutes(5)

    suspend fun start() {
        val loop = this

        supervisorScope {
            fetchMonitor.registerFetchLoop(loop)

            var j = 0
            while (isAppActive) {
                ++j

                // to prevent CPU overload
                delay(1000)

                while (isAppActive && numRunningTasks.get() > fetchConcurrency) {
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
                    } catch (e: IllegalApplicationContextStateException) {
                        AppContext.beginTerminate()
                        illegalState.set(true)
                        log.warn("Illegal context state | {}", e.message)
                    } catch (e: TimeoutCancellationException) {
                        log.warn("Coroutine timeout canceled | {}", e.message)
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
        // find tasks from pending queue
        // TODO: pendingTasks is not used
        var fetchTask = pendingTasks.poll()
        if (fetchTask != null) {
            return fetchTask
        }

        fetchTask = taskScheduler.schedule()
        log.takeIf { it.isTraceEnabled }?.trace("Scheduled task from pool {} | {}", fetchTask?.poolId, fetchTask)

        return fetchTask
    }

    private suspend fun fetch(task: JobFetchTask) {
        if (!isAppActive) {
            taskScheduler.finish(task.poolId, task.itemId)
            return
        }

        try {
            val page = try {
                fetchComponent.fetchContentDeferred(task.page)
            } finally {
                taskScheduler.finish(task.poolId, task.itemId)
            }

            if (page.isInternal) {
                log.warn("Fetch task is an internal page | {}", task.urlString)
                return
            }

            val isCanceled = page.protocolStatus.isCanceled
            if (!isCanceled && taskScheduler.parse) {
                val parseResult = parseComponent.parse(page, false, true)
                if (log.isTraceEnabled) {
                    log.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getTraceInfo())
                }
            }

            withContext(Dispatchers.IO) {
                log.takeIf { it.isInfoEnabled }?.info(LoadedPageFormatter(page).toString())
                if (!isCanceled) {
                    write(page.key, page)
                    enumCounters.inc(CommonCounter.rPersist)
                }
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
            log.error("Failed to write to hdfs - {}", e.stringify())
        } catch (e: InterruptedException) {
            log.error("Interrupted - {}", e.stringify())
            Thread.currentThread().interrupt()
        } catch (e: Throwable) {
            log.error(e.stringify())
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
