package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CONCURRENCY
import ai.platon.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.silent
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.common.collect.Lists
import org.apache.commons.collections4.IteratorUtils
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The SQLContext fuses h2database and pulsar big data engine
 * So we can use SQL to do big data tasks, include but not limited:
 * <ul>
 * <li>Web spider</li>
 * <li>Web scraping</li>
 * <li>Search engine</li>
 * <li>Collect data from variable data source</li>
 * <li>Information extraction</li>
 * <li>TODO: NLP processing</li>
 * <li>TODO: knowledge graph</li>
 * <li>TODO: machine learning</li>
 * </ul>
 */
class SQLContext: AutoCloseable {

    companion object {
        private val activeContext = AtomicReference<SQLContext>()

        fun getOrCreate(): SQLContext {
            synchronized(PulsarContext::class.java) {
                if (activeContext.get() == null) {
                    activeContext.set(SQLContext())
                }
                return activeContext.get()
            }
        }
    }

    private val log = LoggerFactory.getLogger(SQLContext::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    var status: Status = Status.NOT_READY

    val unmodifiedConfig: ImmutableConfig

    val pulsarContext = PulsarContext.getOrCreate()

    private var backgroundSession = pulsarContext.createSession()

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions = ConcurrentHashMap<DbSession, QuerySession>()

    private val backgroundTaskBatchSize: Int

    private val backgroundThread: Thread

    private var lazyTaskRound = 0

    private val lazyLoadTasks = Collections.synchronizedList(LinkedList<String>())

    private val loading = AtomicBoolean()

    private var handlePeriodicalFetchTasks: Boolean

    private val closed: AtomicBoolean = AtomicBoolean()

    init {
        status = Status.INITIALIZING

        // TODO: should be closed by database engine?
        pulsarContext.registerClosable(this)

        unmodifiedConfig = pulsarContext.unmodifiedConfig
        handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, false)

        backgroundSession.disableCache()
        backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_CONCURRENCY, 20)

        // TODO: use ScheduledExecutorService
        backgroundThread = Thread { runBackgroundTasks() }
        backgroundThread.isDaemon = true
        backgroundThread.start()

        status = Status.RUNNING
    }

    fun createSession(dbSession: DbSession): QuerySession {
        ensureRunning()
        val querySession = QuerySession(pulsarContext, dbSession, SessionConfig(dbSession, unmodifiedConfig))
        sessions[dbSession] = querySession
        return querySession
    }

    fun sessionCount(): Int {
        ensureRunning()
        return sessions.size
    }

    fun getSession(sessionId: Int): QuerySession {
        ensureRunning()
        val key = DbSession(sessionId, Any())
        return sessions[key]?:throw DbException.get(ErrorCode.OBJECT_CLOSED, "Session #$sessionId is closed")
    }

    fun closeSession(sessionId: Int) {
        ensureRunning()
        val key = DbSession(sessionId, Any())
        sessions.remove(key)?.use { it.close() }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            status = Status.CLOSING

            log.info("Closing SQLContext ...")

            backgroundSession.use { it.close() }
            backgroundThread.interrupt()
            backgroundThread.join()

            // database engine will close the sessions
            sessions.clear()

            status = Status.CLOSED
        }
    }

    private fun runBackgroundTasks() {
        // start after 30 seconds
        silent { TimeUnit.SECONDS.sleep(30) }

        while (!closed.get()) {
            if (handlePeriodicalFetchTasks) {
                loadLazyTasks()
                fetchSeeds()
            }

            silent { TimeUnit.SECONDS.sleep(5) }
        }
    }

    /**
     * Get background tasks and run them
     * TODO: should handle lazy tasks in a better place
     */
    private fun loadLazyTasks() {
        ensureRunning()
        if (loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = pulsarContext.lazyFetchTaskManager.takeLazyTasks(mode, backgroundTaskBatchSize).map { it.toString() }
            if (urls.isNotEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    /**
     * Get periodical tasks and run them
     */
    private fun fetchSeeds() {
        ensureRunning()
        if (loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = pulsarContext.lazyFetchTaskManager.getSeeds(mode, 1000)
            if (urls.isNotEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    private fun loadAll(urls: Iterable<String>, batchSize: Int, mode: FetchMode) {
        ensureRunning()
        if (!urls.iterator().hasNext() || batchSize <= 0) {
            log.debug("Not loading lazy tasks")
            return
        }

        loading.set(true)

        val loadOptions = LoadOptions.create()
        loadOptions.fetchMode = mode
        loadOptions.background = true

        // TODO: lower priority
        val partitions: List<List<String>> = Lists.partition(IteratorUtils.toList(urls.iterator()), batchSize)
        partitions.forEach { loadAll(it, loadOptions) }

        loading.set(false)
    }

    private fun loadAll(urls: Collection<String>, loadOptions: LoadOptions) {
        ensureRunning()
        if (!closed.get()) {
            ++lazyTaskRound
            log.debug("Running {}th round for lazy tasks", lazyTaskRound)
            backgroundSession.parallelLoadAll(urls, loadOptions)
        }
    }

    private fun ensureRunning() {
        if (closed.get()) {
            throw IllegalStateException(
                    """Cannot call methods on a stopped SQLContext.""")
        }
    }
}
