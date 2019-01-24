package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.common.PulsarContext
import `fun`.platonic.pulsar.common.PulsarContext.applicationContext
import `fun`.platonic.pulsar.common.PulsarContext.unmodifiedConfig
import `fun`.platonic.pulsar.common.PulsarSession
import `fun`.platonic.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT
import `fun`.platonic.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import `fun`.platonic.pulsar.common.options.LoadOptions
import `fun`.platonic.pulsar.common.proxy.ProxyPool
import `fun`.platonic.pulsar.crawl.fetch.TaskStatusTracker
import `fun`.platonic.pulsar.dom.data.BrowserControl
import `fun`.platonic.pulsar.net.SeleniumEngine
import `fun`.platonic.pulsar.persist.metadata.FetchMode
import com.google.common.cache.*
import com.google.common.collect.Lists
import org.apache.commons.collections4.IteratorUtils
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
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
object QueryEngine {
    private val log = LoggerFactory.getLogger(QueryEngine::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    var status: Status = Status.NOT_READY

    private var backgroundSession: PulsarSession = PulsarContext.createSession()

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions: LoadingCache<DbSession, QuerySession>

    private val taskStatusTracker: TaskStatusTracker

    private val proxyPool: ProxyPool

    private val backgroundExecutor: ScheduledExecutorService

    private val backgroundTaskBatchSize: Int

    private var lazyTaskRound = 0

    private val loading = AtomicBoolean()

    private var handlePeriodicalFetchTasks: Boolean

    private val isClosed: AtomicBoolean = AtomicBoolean()

    init {
        status = Status.INITIALIZING

        Runtime.getRuntime().addShutdownHook(Thread(this::close))

        SeleniumEngine.CLIENT_JS = BrowserControl(unmodifiedConfig).getJs()
        sessions = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(SessionRemovalListener())
                .build(SessionCacheLoader(this))
        proxyPool = ProxyPool.getInstance(unmodifiedConfig)
        handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, true)
        taskStatusTracker = applicationContext.getBean(TaskStatusTracker::class.java)

        backgroundSession.disableCache()
        backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20)
        backgroundExecutor = Executors.newScheduledThreadPool(5)
        registerBackgroundTasks()

        status = Status.RUNNING
    }

    fun createQuerySession(dbSession: DbSession): QuerySession {
        val querySession = QuerySession(dbSession, SessionConfig(dbSession, unmodifiedConfig))

        sessions.put(dbSession, querySession)

        return querySession
    }

    /**
     * Get a query session from h2 session
     */
    fun getSession(dbSession: DbSession): QuerySession {
        try {
            return sessions.get(dbSession)
        } catch (e: ExecutionException) {
            throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, e)
        }
    }

    fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
        status = Status.CLOSING

        log.info("[Destruction] Destructing QueryEngine ...")

        backgroundExecutor.shutdownNow()

        sessions.asMap().values.forEach { it.close() }
        sessions.cleanUp()

        taskStatusTracker.close()

        proxyPool.close()

        status = Status.CLOSED
    }

    private fun registerBackgroundTasks() {
        val r = { runSilently { loadLazyTasks() } }
        backgroundExecutor.scheduleAtFixedRate(r, 10, 30, TimeUnit.SECONDS)

        if (handlePeriodicalFetchTasks) {
            val r2 = { runSilently { fetchSeeds() } }
            backgroundExecutor.scheduleAtFixedRate(r2, 30, 120, TimeUnit.SECONDS)
        }

        val r3 = { runSilently { maintainProxyPool() } }
        backgroundExecutor.scheduleAtFixedRate(r3, 120, 120, TimeUnit.SECONDS)
    }

    private fun runSilently(target: () -> Unit) {
        try {
            target()
        } catch (e: Throwable) {
            // Do not throw anything
            log.error(e.toString())
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
            val urls = taskStatusTracker.takeLazyTasks(mode, backgroundTaskBatchSize)
            if (!urls.isEmpty()) {
                loadAll(urls.map {it.toString()}, backgroundTaskBatchSize, mode)
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

    private class SessionCacheLoader(val engine: QueryEngine): CacheLoader<DbSession, QuerySession>() {
        override fun load(dbSession: DbSession): QuerySession {
            log.warn("Create PulsarSession for h2 h2session {} via SessionCacheLoader (not expected ...)", dbSession)
            return createQuerySession(dbSession)
        }
    }

    private class SessionRemovalListener : RemovalListener<DbSession, QuerySession> {
        override fun onRemoval(notification: RemovalNotification<DbSession, QuerySession>) {
            val cause = notification.cause
            val dbSession = notification.key
            when (cause) {
                RemovalCause.EXPIRED, RemovalCause.SIZE -> {
                    // It's safe to close h2 h2session, @see {org.h2.api.ErrorCode#DATABASE_CALLED_AT_SHUTDOWN}
                    // h2session.close()
                    notification.value.close()
                    log.info("Session {} is closed for reason '{}', remaining {} sessions",
                            dbSession, cause, sessions.size())
                }
                else -> {
                }
            }
        }
    }
}
