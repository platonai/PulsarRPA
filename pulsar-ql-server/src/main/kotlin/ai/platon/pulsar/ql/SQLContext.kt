package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT
import ai.platon.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.common.collect.Lists
import org.apache.commons.collections4.IteratorUtils
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.util.*
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

    val env = PulsarEnv.getOrCreate()

    val pulsarContext = PulsarContext.getOrCreate()

    private var backgroundSession = pulsarContext.createSession()

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions: MutableMap<DbSession, QuerySession> = Collections.synchronizedMap(mutableMapOf())

    private val backgroundTaskBatchSize: Int

    private val backgroundThread: Thread

    private var lazyTaskRound = 0

    private val lazyLoadTasks = Collections.synchronizedList(LinkedList<String>())

    private val loading = AtomicBoolean()

    private var handlePeriodicalFetchTasks: Boolean

    private val isClosed: AtomicBoolean = AtomicBoolean()

    init {
        status = Status.INITIALIZING

        // TODO: should be closed by database engine?
        pulsarContext.registerClosable(this)

        unmodifiedConfig = pulsarContext.unmodifiedConfig
        handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, false)

        backgroundSession.disableCache()
        backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20)

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
        return sessions[key]?:throw DbException.get(ErrorCode.OBJECT_CLOSED, "Session is closed")
    }

    fun closeSession(sessionId: Int) {
        ensureRunning()
        val key = DbSession(sessionId, Any())
        sessions.remove(key)?.use { it.close() }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        status = Status.CLOSING

        log.info("[Destruction] Destructing SQLContext ...")

        backgroundSession.use { it.close() }
        backgroundThread.join()

        // database engine will close the sessions
        sessions.clear()

        status = Status.CLOSED
    }

    private fun runBackgroundTasks() {
        // start after 30 seconds
        TimeUnit.SECONDS.sleep(30)

        while (!isClosed.get()) {
            if (handlePeriodicalFetchTasks) {
                loadLazyTasks()
                fetchSeeds()
            }

            try {
                TimeUnit.SECONDS.sleep(10)
            } catch (e: InterruptedException) {}
        }
    }

    /**
     * Get background tasks and run them
     */
    private fun loadLazyTasks() {
        ensureRunning()
        if (loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = pulsarContext.taskStatusTracker.takeLazyTasks(mode, backgroundTaskBatchSize).map { it.toString() }
            if (!urls.isEmpty()) {
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
            val urls = pulsarContext.taskStatusTracker.getSeeds(mode, 1000)
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
        if (!isClosed.get()) {
            ++lazyTaskRound
            log.debug("Running {}th round for lazy tasks", lazyTaskRound)
            backgroundSession.parallelLoadAll(urls, loadOptions)
        }
    }

    private fun ensureRunning() {
        if (isClosed.get()) {
            throw IllegalStateException(
                    """Cannot call methods on a stopped SQLContext.""")
        }
    }
}
