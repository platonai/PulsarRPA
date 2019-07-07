package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv.applicationContext
import ai.platon.pulsar.PulsarEnv.unmodifiedConfig
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT
import ai.platon.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.common.collect.Lists
import org.apache.commons.collections4.IteratorUtils
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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
object SQLContext: AutoCloseable {
    private val log = LoggerFactory.getLogger(SQLContext::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    var status: Status = Status.NOT_READY

    private val pulsarContext = PulsarContext.create()

    private var backgroundSession = pulsarContext.createSession()

    /**pulsarContext
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions: MutableMap<DbSession, QuerySession> = Collections.synchronizedMap(mutableMapOf())

    private val taskStatusTracker: TaskStatusTracker

    private val proxyPool: ProxyPool

    private val backgroundTaskBatchSize: Int

    private val backgroundThread: Thread

    private var lazyTaskRound = 0

    private val lazyLoadTasks = Collections.synchronizedList(LinkedList<String>())

    private val loading = AtomicBoolean()

    private var handlePeriodicalFetchTasks: Boolean

    private val isClosed: AtomicBoolean = AtomicBoolean()

    init {
        status = Status.INITIALIZING

        Runtime.getRuntime().addShutdownHook(Thread(this::close))

        proxyPool = ProxyPool.getInstance(unmodifiedConfig)
        handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, false)
        taskStatusTracker = applicationContext.getBean(TaskStatusTracker::class.java)

        backgroundSession.disableCache()
        backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20)

        backgroundThread = Thread { runBackgroundTasks() }
        backgroundThread.isDaemon = true
        backgroundThread.start()

        status = Status.RUNNING
    }

    fun createQuerySession(dbSession: DbSession): QuerySession {
        val querySession = QuerySession(pulsarContext, dbSession, SessionConfig(dbSession, unmodifiedConfig))
        sessions[dbSession] = querySession
        return querySession
    }

    fun sessionCount(): Int {
        return sessions.size
    }

    fun getSession(sessionId: Int): QuerySession {
        return sessions.entries.firstOrNull { it.key.id == sessionId }?.value
                ?:throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, "Failed to find session in cache")
    }

    fun closeSession(sessionId: Int) {
        val removal = sessions.filter { it.key.id == sessionId }
        removal.forEach {
            sessions.remove(it.key)
            it.value.use { it.close() }
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        status = Status.CLOSING

        log.info("[Destruction] Destructing SQLContext ...")

        backgroundSession.use { it.close() }
        backgroundThread.join()

        sessions.values.forEach { it.use { it.close() } }
        sessions.clear()

        taskStatusTracker.use { it.close() }

        proxyPool.use { it.close() }

        status = Status.CLOSED
    }

    private fun runBackgroundTasks() {
        while (!isClosed.get()) {
            Thread.sleep(10000)

            if (handlePeriodicalFetchTasks) {
                loadLazyTasks()
                fetchSeeds()
            }

            maintainProxyPool()
        }
    }

    /**
     * Get background tasks and run them
     */
    private fun loadLazyTasks() {
        if (loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = taskStatusTracker.takeLazyTasks(mode, backgroundTaskBatchSize).map { it.toString() }
            if (!urls.isEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    /**
     * Get periodical tasks and run them
     */
    private fun fetchSeeds() {
        if (loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = taskStatusTracker.getSeeds(mode, 1000)
            if (urls.isNotEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    private fun maintainProxyPool() {
        proxyPool.recover(100)
    }

    private fun loadAll(urls: Iterable<String>, batchSize: Int, mode: FetchMode) {
        if (!urls.iterator().hasNext() || batchSize <= 0) {
            log.debug("Not loading lazy tasks")
            return
        }

        loading.set(true)

        val loadOptions = LoadOptions()
        loadOptions.fetchMode = mode
        loadOptions.background = true

        // TODO: lower priority
        val partitions: List<List<String>> = Lists.partition(IteratorUtils.toList(urls.iterator()), batchSize)
        partitions.forEach { loadAll(it, loadOptions) }

        loading.set(false)
    }

    private fun loadAll(urls: Collection<String>, loadOptions: LoadOptions) {
        ++lazyTaskRound
        log.debug("Running {}th round for lazy tasks", lazyTaskRound)
        backgroundSession.parallelLoadAll(urls, loadOptions)
    }
}
