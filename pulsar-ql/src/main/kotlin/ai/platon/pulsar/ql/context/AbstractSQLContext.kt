package ai.platon.pulsar.ql.context

import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sql.SQLUtils
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.ql.AbstractSQLSession
import ai.platon.pulsar.ql.SessionDelegate
import org.h2.api.ErrorCode
import org.h2.engine.Session
import org.h2.engine.SessionInterface
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import org.springframework.context.support.AbstractApplicationContext
import java.sql.Connection
import java.sql.ResultSet
import java.text.MessageFormat
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The abstract SQL context, every X-SQL staff should be within the SQL context
 */
abstract class AbstractSQLContext constructor(
    override val applicationContext: AbstractApplicationContext,
    override val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment(),
) : AbstractPulsarContext(applicationContext, pulsarEnvironment), SQLContext {

    private val logger = LoggerFactory.getLogger(AbstractSQLContext::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    var status: Status = Status.NOT_READY

    abstract val randomConnection: Connection

    val randomConnectionOrNull: Connection? get() = kotlin.runCatching { randomConnection }
        .onFailure { logger.warn(it.stringify()) }
        .getOrNull()

    val connectionPool = ArrayBlockingQueue<Connection>(1000)
    private val resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE
    private val resultSetConcurrency = ResultSet.CONCUR_READ_ONLY

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    val sqlSessions = ConcurrentHashMap<Int, AbstractSQLSession>()

    private val closed = AtomicBoolean()

    init {
        Systems.setPropertyIfAbsent(CapabilityTypes.SCENT_EXTRACT_TABULATE_CELL_TYPE, "DATABASE")

        status = Status.INITIALIZING

        status = Status.RUNNING

        logger.info("SQLContext is created | {}/{} | {}", id, sessions.size, this::class.java.simpleName)
    }

    override fun normalize(url: String, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val normUrl = super.normalize(url, options, toItemOption)
        return NormUrl(SQLUtils.unsanitizeUrl(normUrl.spec), normUrl.options, hrefSpec = normUrl.hrefSpec)
    }

    override fun execute(sql: String) {
        val conn = connectionPool.poll() ?: randomConnection

        conn.createStatement(resultSetType, resultSetConcurrency).runCatching {
            execute(sql)
        }.getOrElse { t ->
            conn.takeUnless { it.isClosed }?.let { connectionPool.add(conn) }
            throw t
        }
    }

    @Throws(Exception::class)
    override fun executeQuery(sql: String): ResultSet {
        val conn = connectionPool.poll() ?: randomConnection

        return conn.createStatement(resultSetType, resultSetConcurrency).runCatching {
            executeQuery(sql)
        }.getOrElse { t ->
            conn.takeUnless { it.isClosed }?.let { connectionPool.add(conn) }
            throw t
        }
    }

    override fun run(block: (Connection) -> Unit) {
        val conn = connectionPool.poll() ?: randomConnection
        try {
            block(conn)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        } finally {
            conn.takeUnless { it.isClosed }?.let { connectionPool.add(conn) }
        }
    }

    @Throws(Exception::class)
    override fun runQuery(block: (Connection) -> ResultSet): ResultSet {
        val conn = connectionPool.poll() ?: randomConnection
        try {
            return block(conn)
        } finally {
            conn.takeUnless { it.isClosed }?.let { connectionPool.add(conn) }
        }
    }

    abstract override fun createSession(sessionDelegate: SessionDelegate): AbstractSQLSession

    override fun sessionCount(): Int {
        ensureRunning()
        return sqlSessions.size
    }

    override fun getSession(sessionInterface: SessionInterface): AbstractSQLSession {
        val h2session = sessionInterface as Session
        return getSession(h2session.serialId)
    }

    override fun getSession(sessionId: Int): AbstractSQLSession {
        ensureRunning()
        val session = sqlSessions[sessionId]
        if (session == null) {
            val message = MessageFormat.format(
                "Session is already closed | #{0}/{1}",
                sessionId, id
            )
            logger.warn(message)
            throw DbException.get(ErrorCode.OBJECT_CLOSED, message)
        }
        return session
    }

    override fun closeSession(sessionId: Int) {
        ensureRunning()
        sqlSessions.remove(sessionId)?.close()
        logger.info("SQLSession is closed | #{}/{}/{}", id, sessionId, sqlSessions.size)
    }

    override fun close() {
        logger.info("Closing SQLContext #{}, sql sessions: {}", id, sqlSessions.keys.joinToString { "$it" })

        if (closed.compareAndSet(false, true)) {
            status = Status.CLOSING

            // database engine will close the sessions
            sqlSessions.values.forEach { it.close() }
            sqlSessions.clear()
            connectionPool.forEach { it.close() }
            connectionPool.clear()

            status = Status.CLOSED

            // H2SessionFactory.shutdown()
        }

        super.close()
    }

    private fun ensureRunning() {
        if (!isActive) {
            throw IllegalStateException("SQLContext is closed | #$id")
        }
    }
}
