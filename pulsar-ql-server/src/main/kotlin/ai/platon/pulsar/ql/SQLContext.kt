package ai.platon.pulsar.ql

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.metadata.FetchMode
import org.h2.api.ErrorCode
import org.h2.engine.Session
import org.h2.engine.SessionInterface
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
class SQLContext(val pulsarContext: AbstractPulsarContext): AutoCloseable {

    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(SQLContext::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    val id = instanceSequencer.incrementAndGet()

    var status: Status = Status.NOT_READY

    val unmodifiedConfig: ImmutableConfig

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private val sessions = ConcurrentHashMap<DbSession, QuerySession>()

    private val loading = AtomicBoolean()

    private val closed = AtomicBoolean()

    val isActive = !closed.get() && pulsarContext.isActive

    init {
        Systems.setPropertyIfAbsent(SCENT_EXTRACT_TABULATE_CELL_TYPE, "DATABASE")

        status = Status.INITIALIZING

        unmodifiedConfig = pulsarContext.unmodifiedConfig

        status = Status.RUNNING

        log.info("SQLContext is created | #$id")
    }

    fun createSession(dbSession: DbSession): QuerySession {
        ensureRunning()
        val session = sessions.computeIfAbsent(dbSession) {
            QuerySession(pulsarContext, dbSession, SessionConfig(dbSession, unmodifiedConfig)) }
        log.info("Session is created | #{}/{}", session.id, id)
        return session
    }

    fun sessionCount(): Int {
        ensureRunning()
        return sessions.size
    }

    fun getSession(sessionInterface: SessionInterface): QuerySession {
        val h2session = sessionInterface as Session
        return getSession(h2session.serialId)
    }

    fun getSession(sessionId: Int): QuerySession {
        ensureRunning()
        val session = sessions[DbSession(sessionId, Any())]
        if (session == null) {
            val message = MessageFormat.format("Session is already closed | #{0}/{1}",
                    sessionId, id)
            log.warn(message)
            throw DbException.get(ErrorCode.OBJECT_CLOSED, message)
        }
        return session
    }

    fun closeSession(sessionId: Int) {
        ensureRunning()
        val key = DbSession(sessionId, Any())
        sessions.remove(key)?.close()
        log.debug("Session is closed | #{}/{}", sessionId, id)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            status = Status.CLOSING

            // database engine will close the sessions
            sessions.clear()

            status = Status.CLOSED

            log.info("SQLContext is closed | #$id")
        }
    }

    private fun ensureRunning() {
        if (!isActive) {
            throw IllegalStateException("SQLContext is closed | #$id")
        }
    }
}
