package ai.platon.pulsar.ql

import ai.platon.pulsar.common.PulsarContext
import ai.platon.pulsar.common.PulsarContext.applicationContext
import ai.platon.pulsar.common.PulsarContext.unmodifiedConfig
import ai.platon.pulsar.common.PulsarSession
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT
import ai.platon.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.dom.data.BrowserControl
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.common.collect.Lists
import org.apache.commons.collections4.IteratorUtils
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The QueryEngine fuses h2database and pulsar big data engine
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
object QueryEngine: AutoCloseable {
    private val log = LoggerFactory.getLogger(QueryEngine::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    var status: Status = Status.NOT_READY

    private var backgroundSession: PulsarSession = PulsarContext.createSession()

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions: MutableMap<DbSession, QuerySession> = mutableMapOf()

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

        SeleniumEngine.CLIENT_JS = BrowserControl(unmodifiedConfig).getJs()
        proxyPool = ProxyPool.getInstance(unmodifiedConfig)
        handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, true)
        taskStatusTracker = applicationContext.getBean(TaskStatusTracker::class.java)

        backgroundSession.disableCache()
        backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20)

        backgroundThread = Thread { runBackgroundTasks() }
        backgroundThread.isDaemon = true
        backgroundThread.start()

        status = Status.RUNNING
    }

    @Synchronized
    fun createQuerySession(dbSession: DbSession): QuerySession {
        val querySession = QuerySession(dbSession, SessionConfig(dbSession, unmodifiedConfig))
        sessions[dbSession] = querySession
        return querySession
    }

    @Synchronized
    fun sessionCount(): Int {
        return sessions.size
    }

    @Synchronized
    fun getSession(sessionId: Int): QuerySession {
        return sessions.entries.firstOrNull { it.key.id == sessionId }?.value
                ?:throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, "Failed to find session in cache")
    }

    @Synchronized
    fun closeSession(sessionId: Int) {
        val removal = sessions.filter { it.key.id == sessionId }
        removal.forEach {
            sessions.remove(it.key)
            it.value.use { it.close() }
        }
    }

    @Synchronized
    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        status = Status.CLOSING

        log.info("[Destruction] Destructing QueryEngine ...")

        backgroundSession.close()
        backgroundThread.join()

        sessions.values.forEach { it.close() }
        sessions.clear()

        taskStatusTracker.close()

        proxyPool.close()

        status = Status.CLOSED
    }

    private fun runBackgroundTasks() {
        while (!isClosed.get()) {
            Thread.sleep(10000)

            loadLazyTasks()
            fetchSeeds()
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
            if (!urls.isEmpty()) {
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
        loadOptions.isBackground = true

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
