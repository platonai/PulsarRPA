package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.MetricsSystem
import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

/**
 * This class picks items from queues and fetches the pages.
 */
class FetchThread(
        private val fetchMonitor: FetchMonitor,
        private val fetchComponent: FetchComponent,
        private val taskScheduler: TaskScheduler,
        immutableConfig: ImmutableConfig,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>
): Thread(), Comparable<FetchThread> {
    companion object {
        private val instanceSequence = AtomicInteger(0)
        val pendingTasks = ConcurrentHashMap<Int, WebPage>()
    }

    private val log = LoggerFactory.getLogger(FetchThread::class.java)

    private val id = instanceSequence.incrementAndGet()
    /**
     * Fix the thread to a specified queue as possible as we can
     */
    private var currPoolId: PoolId? = null
    private val servedPoolIds = TreeSet<PoolId>()
    private var taskCount = 0
    private val closed = AtomicBoolean(false)
    private val tasksMonitor = taskScheduler.tasksMonitor

    init {
        this.isDaemon = true
        this.name = "w$id" // w is short for worker
    }

    fun exit() {
        if (closed.compareAndSet(false, true)) {
            interrupt()
            join()
        }
    }

    override fun run() {
        fetchMonitor.registerFetchThread(this)


        var task: FetchTask? = null

        try {
            while (!fetchMonitor.isMissionComplete && !closed.get() && !interrupted()) {
                task = schedule()

                // TODO: block in schedule
                if (task == null) {
                    sleep()
                    continue
                }

                if (log.isTraceEnabled) {
                    log.trace("Ready to fetch task from pool {}", task.poolId)
                }

                val page = fetchComponent.fetchContent(task.page)
                taskScheduler.finish(task.poolId, task.itemId)

                if (page.protocolStatus.isRetry(RetryScope.PRIVACY_CONTEXT)) {
                    // 1. cancel all running tasks
                    // 2. push back to fetch queue
                    // 3. lock down fetch queue
                    // 4. reset fetch context
                    //
                    // tasksMonitor.produce(context.jobId, page)
                    pendingTasks[context.jobId] = page
                } else if (page.isNotNil) {
                    if (log.isInfoEnabled) {
                        log.info(MetricsSystem.getFetchCompleteReport(page))
                    }

                    write(page.key, page)
                }

                ++taskCount
            } // while
        } catch (e: Throwable) {
            log.error("Unexpected throwable : " + StringUtil.stringifyException(e))
        } finally {
            if (task != null) {
                taskScheduler.finishUnchecked(task)
            }

            fetchMonitor.unregisterFetchThread(this)

            log.info("Thread #{} finished", getId())
        }
    }

    fun report() {
        if (servedPoolIds.isEmpty()) {
            return
        }

        val report = StringBuilder()
        report.appendln(String.format("Thread #%d served %d tasks for %d hosts : \n", getId(), taskCount, servedPoolIds.size))

        servedPoolIds.map {
            Urls.reverseHost("${it.protocol}://${it.host}")
        }.sorted().map { Urls.unreverseHost(it) }.joinTo(report, "\n") { String.format("%1$40s", it) }
        report.appendln()

        log.info(report.toString())
    }

    private fun waitUntilAllFree() {

    }

    private fun sleep() {
        fetchMonitor.registerIdleThread(this)

        try {
            TimeUnit.SECONDS.sleep(1)
        } catch (ignored: InterruptedException) {}

        fetchMonitor.unregisterIdleThread(this)
    }

    private fun schedule(): FetchTask? {
        var fetchTask: FetchTask? = null

        val fetchMode = fetchMonitor.options.fetchMode
        if (fetchMode == FetchMode.CROWD_SOURCING) {
            val response = taskScheduler.pollForwardResponse()

            if (response != null) {
                val url = Urls.getURLOrNull(response.queueId)
                if (url != null) {
                    fetchTask = taskScheduler.tasksMonitor.findPendingTask(response.priority, url, response.itemId)
                }

                if (fetchTask == null) {
                    log.warn("Bad fetch item id {}-{}", response.queueId, response.itemId)
                }
            }
        } else {
            fetchTask = if (currPoolId == null) {
                taskScheduler.schedule()
            } else {
                taskScheduler.schedule(currPoolId)
            }

            if (fetchTask != null) {
                val poolId = fetchTask.poolId
                servedPoolIds.add(poolId)

                // the next time, we fetch items from the same queue as this time
                currPoolId = poolId
            } else {
                // The current queue is empty, fetch item from top queue the next time
                currPoolId = null
            }
        }

        if (fetchTask != null) {
            if (fetchMode != fetchTask.page.fetchMode) {
                log.warn("FetchTask.page.fetchMode {} is not expected {}", fetchMode, fetchTask.page.fetchMode)
            }
        }

        return fetchTask
    }

    private fun write(key: String, page: WebPage) {
        try {
            // the page is fetched and status are updated, write to the file system
            context.write(key, page.unbox())
        } catch (e: IOException) {
            log.error("Failed to write to hdfs - {}", StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            log.error("Interrupted - {}", StringUtil.stringifyException(e))
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
        }
    }

    override fun compareTo(other: FetchThread): Int {
        return id - other.id
    }
}
