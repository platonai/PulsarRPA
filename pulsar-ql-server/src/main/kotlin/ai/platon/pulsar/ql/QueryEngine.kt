package ai.platon.pulsar.ql

import ai.platon.pulsar.common.PulsarContext
import ai.platon.pulsar.common.PulsarContext.applicationContext
import ai.platon.pulsar.common.PulsarContext.unmodifiedConfig
import ai.platon.pulsar.common.PulsarSession
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT
import ai.platon.pulsar.common.config.CapabilityTypes.QE_HANDLE_PERIODICAL_FETCH_TASKS
import ai.platon.pulsar.dom.data.BrowserControl
import ai.platon.pulsar.persist.metadata.FetchMode
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

    var status: QueryEngine.Status = QueryEngine.Status.NOT_READY

    private var backgroundSession: PulsarSession = PulsarContext.createSession()

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions: LoadingCache<DbSession, QuerySession>

    private val taskStatusTracker: TaskStatusTracker

    private val proxyPool: ai.platon.pulsar.common.proxy.ProxyPool

    private val backgroundExecutor: ScheduledExecutorService

    private val backgroundTaskBatchSize: Int

    private var lazyTaskRound = 0

    private val loading = AtomicBoolean()

    private var handlePeriodicalFetchTasks: Boolean

    private val isClosed: AtomicBoolean = AtomicBoolean()

    init {
        QueryEngine.status = QueryEngine.Status.INITIALIZING

        Runtime.getRuntime().addShutdownHook(Thread(this::close))

        SeleniumEngine.CLIENT_JS = BrowserControl(unmodifiedConfig).getJs()
        QueryEngine.sessions = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(QueryEngine.SessionRemovalListener())
                .build(QueryEngine.SessionCacheLoader(this))
        QueryEngine.proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(unmodifiedConfig)
        QueryEngine.handlePeriodicalFetchTasks = unmodifiedConfig.getBoolean(QE_HANDLE_PERIODICAL_FETCH_TASKS, true)
        QueryEngine.taskStatusTracker = applicationContext.getBean(TaskStatusTracker::class.java)

        QueryEngine.backgroundSession.disableCache()
        QueryEngine.backgroundTaskBatchSize = unmodifiedConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20)
        QueryEngine.backgroundExecutor = Executors.newScheduledThreadPool(5)
        QueryEngine.registerBackgroundTasks()

        QueryEngine.status = QueryEngine.Status.RUNNING
    }

    fun createQuerySession(dbSession: DbSession): QuerySession {
        val querySession = QuerySession(dbSession, SessionConfig(dbSession, unmodifiedConfig))

        QueryEngine.sessions.put(dbSession, querySession)

        return querySession
    }

    /**
     * Get a query session from h2 session
     */
    fun getSession(dbSession: DbSession): QuerySession {
        try {
            return QueryEngine.sessions.get(dbSession)
        } catch (e: ExecutionException) {
            throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, e)
        }
    }

    fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        QueryEngine.status = QueryEngine.Status.CLOSING

        QueryEngine.log.info("[Destruction] Destructing QueryEngine ...")

        QueryEngine.backgroundExecutor.shutdownNow()

        QueryEngine.sessions.asMap().values.forEach { it.close() }
        QueryEngine.sessions.cleanUp()

        QueryEngine.taskStatusTracker.close()

        QueryEngine.proxyPool.close()

        QueryEngine.status = QueryEngine.Status.CLOSED
    }

    private fun registerBackgroundTasks() {
        val r = { QueryEngine.runSilently { QueryEngine.loadLazyTasks() } }
        QueryEngine.backgroundExecutor.scheduleAtFixedRate(r, 10, 30, TimeUnit.SECONDS)

        if (QueryEngine.handlePeriodicalFetchTasks) {
            val r2 = { QueryEngine.runSilently { QueryEngine.fetchSeeds() } }
            QueryEngine.backgroundExecutor.scheduleAtFixedRate(r2, 30, 120, TimeUnit.SECONDS)
        }

        val r3 = { QueryEngine.runSilently { QueryEngine.maintainProxyPool() } }
        QueryEngine.backgroundExecutor.scheduleAtFixedRate(r3, 120, 120, TimeUnit.SECONDS)
    }

    private fun runSilently(target: () -> Unit) {
        try {
            target()
        } catch (e: Throwable) {
            // Do not throw anything
            QueryEngine.log.error(e.toString())
        }
    }

    /**
     * Get background tasks and run them
     */
    private fun loadLazyTasks() {
        if (QueryEngine.loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = QueryEngine.taskStatusTracker.takeLazyTasks(mode, QueryEngine.backgroundTaskBatchSize)
            if (!urls.isEmpty()) {
                QueryEngine.loadAll(urls.map { it.toString() }, QueryEngine.backgroundTaskBatchSize, mode)
            }
        }
    }

    /**
     * Get periodical tasks and run them
     */
    private fun fetchSeeds() {
        if (QueryEngine.loading.get()) {
            return
        }

        for (mode in FetchMode.values()) {
            val urls = QueryEngine.taskStatusTracker.getSeeds(mode, 1000)
            if (!urls.isEmpty()) {
                QueryEngine.loadAll(urls, QueryEngine.backgroundTaskBatchSize, mode)
            }
        }
    }

    private fun maintainProxyPool() {
        QueryEngine.proxyPool.recover(100)
    }

    private fun loadAll(urls: Iterable<String>, batchSize: Int, mode: FetchMode) {
        if (!urls.iterator().hasNext() || batchSize <= 0) {
            QueryEngine.log.debug("Not loading lazy tasks")
            return
        }

        QueryEngine.loading.set(true)

        val loadOptions = ai.platon.pulsar.common.options.LoadOptions()
        loadOptions.fetchMode = mode
        loadOptions.isBackground = true

        val partitions: List<List<String>> = Lists.partition(IteratorUtils.toList(urls.iterator()), batchSize)
        partitions.forEach { QueryEngine.loadAll(it, loadOptions) }

        QueryEngine.loading.set(false)
    }

    private fun loadAll(urls: Collection<String>, loadOptions: ai.platon.pulsar.common.options.LoadOptions) {
        ++QueryEngine.lazyTaskRound
        QueryEngine.log.debug("Running {}th round for lazy tasks", QueryEngine.lazyTaskRound)
        QueryEngine.backgroundSession.parallelLoadAll(urls, loadOptions)
    }

    private class SessionCacheLoader(val engine: QueryEngine): CacheLoader<DbSession, QuerySession>() {
        override fun load(dbSession: DbSession): QuerySession {
            QueryEngine.log.warn("Create PulsarSession for h2 h2session {} via SessionCacheLoader (not expected ...)", dbSession)
            return QueryEngine.createQuerySession(dbSession)
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
                    QueryEngine.log.info("Session {} is closed for reason '{}', remaining {} sessions",
                            dbSession, cause, QueryEngine.sessions.size())
                }
                else -> {
                }
            }
        }
    }
}
