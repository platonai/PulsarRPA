package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.ReducerContext
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class picks items from queues and fetches the pages.
 */
class FetchThread(
        private val fetchComponent: FetchComponent,
        private val fetchMonitor: FetchMonitor,
        private val taskScheduler: TaskScheduler,
        immutableConfig: ImmutableConfig,
        private val context: ReducerContext<IntWritable, out IFetchEntry, String, GWebPage>
): Thread(), Comparable<FetchThread> {

    private val LOG = LoggerFactory.getLogger(FetchThread::class.java)

    private val id: Int

    /**
     * Fix the thread to a specified queue as possible as we can
     */
    private val currPriority = -1
    private var currQueueId: PoolId? = null
    private val halted = AtomicBoolean(false)
    private val servedHosts = TreeSet<PoolId>()
    private var taskCount = 0

    val isHalted: Boolean
        get() = halted.get()

    init {
        this.id = instanceSequence.incrementAndGet()

        this.isDaemon = true
        this.name = javaClass.simpleName + "-" + id
    }

    fun halt() {
        halted.set(true)
    }

    fun exitAndJoin() {
        halted.set(true)
        try {
            join()
        } catch (e: InterruptedException) {
            LOG.error(e.toString())
        }
    }

    override fun run() {
        fetchMonitor.registerFetchThread(this)

        var task: FetchTask? = null

        try {
            while (!fetchMonitor.isMissionComplete && !isHalted) {
                task = schedule()

                if (task == null) {
                    sleepAndRecord()
                    continue
                }

                // TODO: is there a better place to set fetch mode?
                task.page?.fetchMode = fetchMonitor.options.fetchMode
                val page = fetchOne(task)
                if (page.isNotNil) {
                    write(page.key, page)
                }

                ++taskCount
            } // while
        } catch (e: Throwable) {
            LOG.error("Unexpected throwable : " + StringUtil.stringifyException(e))
        } finally {
            if (task != null) {
                taskScheduler.finishUnchecked(task)
            }

            fetchMonitor.unregisterFetchThread(this)

            LOG.info("Thread #{} finished", getId())
        }
    }

    fun report() {
        if (servedHosts.isEmpty()) {
            return
        }

        val report = StringBuilder()
        report.appendln(String.format("Thread #%d served %d tasks for %d hosts : \n", getId(), taskCount, servedHosts.size))

        servedHosts.map { Urls.reverseHost(it.url) }.sorted().map { Urls.unreverseHost(it) }
                .joinTo(report, "\n") { String.format("%1$40s", it) }
        report.appendln()

        LOG.info(report.toString())
    }

    private fun sleepAndRecord() {
        fetchMonitor.registerIdleThread(this)

        try {
            sleep(1000)
        } catch (ignored: InterruptedException) {}

        fetchMonitor.unregisterIdleThread(this)
    }

    private fun schedule(): FetchTask? {
        var fetchTask: FetchTask? = null

        val fetchMode = fetchMonitor.options.fetchMode
        if (fetchMode == FetchMode.CROWDSOURCING) {
            val response = taskScheduler.pollFetchResult()

            if (response != null) {
                val url = Urls.getURLOrNull(response.queueId)
                if (url != null) {
                    fetchTask = taskScheduler.tasksMonitor.findPendingTask(response.priority, url, response.itemId)
                }

                if (fetchTask == null) {
                    LOG.warn("Bad fetch item id {}-{}", response.queueId, response.itemId)
                }
            }
        } else {
            fetchTask = if (currQueueId == null) {
                taskScheduler.schedule()
            } else {
                taskScheduler.schedule(currQueueId)
            }

            if (fetchTask != null) {
                // the next time, we fetch items from the same queue as this time
                currQueueId = PoolId(fetchTask.priority, fetchTask.protocol, fetchTask.host)
                servedHosts.add(currQueueId!!)
            } else {
                // The current queue is empty, fetch item from top queue the next time
                currQueueId = null
            }
        }

        return fetchTask
    }

    private fun fetchOne(task: FetchTask): WebPage {
        val page = task.page?:return WebPage.NIL

        fetchComponent.fetchContent(page)

        val queueId = PoolId(task.priority, task.protocol, task.host)
        taskScheduler.finish(queueId, task.itemId)

        return page
    }

    private fun write(key: String, page: WebPage) {
        try {
            // the page is fetched and status are updated, write to the file system
            context.write(key, page.unbox())
        } catch (e: IOException) {
            LOG.error("Failed to write to hdfs - {}", StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            LOG.error("Interrupted - {}", StringUtil.stringifyException(e))
        } catch (e: Throwable) {
            LOG.error(StringUtil.stringifyException(e))
        }
    }

    override fun compareTo(other: FetchThread): Int {
        return id - other.id
    }

    companion object {

        private val instanceSequence = AtomicInteger(0)
    }
}
