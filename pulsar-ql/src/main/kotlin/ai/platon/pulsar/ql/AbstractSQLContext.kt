package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarEnvironment
import ai.platon.pulsar.common.NotSupportedException
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.support.AbstractPulsarContext
import org.h2.api.ErrorCode
import org.h2.engine.Session
import org.h2.engine.SessionInterface
import org.h2.message.DbException
import org.slf4j.LoggerFactory
import org.springframework.context.support.AbstractApplicationContext
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
abstract class AbstractSQLContext constructor(
    override val applicationContext: AbstractApplicationContext,
    override val pulsarEnvironment: PulsarEnvironment = PulsarEnvironment()
): AbstractPulsarContext(applicationContext, pulsarEnvironment), SQLContext {

    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(AbstractSQLContext::class.java)

    enum class Status { NOT_READY, INITIALIZING, RUNNING, CLOSING, CLOSED }

    val id = instanceSequencer.incrementAndGet()

    var status: Status = Status.NOT_READY

    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    protected val sqlSessions = ConcurrentHashMap<SessionDelegate, AbstractSQLSession>()

    private val closed = AtomicBoolean()

    init {
        Systems.setPropertyIfAbsent(CapabilityTypes.SCENT_EXTRACT_TABULATE_CELL_TYPE, "DATABASE")

        status = Status.INITIALIZING

        status = Status.RUNNING

        log.info("SQLContext is created | #$id")
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
        val session = sqlSessions[SessionDelegate(sessionId, Any())]
        if (session == null) {
            val message = MessageFormat.format("Session is already closed | #{0}/{1}",
                sessionId, id)
            log.warn(message)
            throw DbException.get(ErrorCode.OBJECT_CLOSED, message)
        }
        return session
    }

    override fun closeSession(sessionId: Int) {
        ensureRunning()
        val key = SessionDelegate(sessionId, Any())
        sqlSessions.remove(key)?.close()
        log.debug("Session is closed | #{}/{}", sessionId, id)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            status = Status.CLOSING

            // database engine will close the sessions
            sqlSessions.clear()

            status = Status.CLOSED

            log.info("SQLContext is closed | #$id")
        }

        super.close()
    }

    private fun ensureRunning() {
        if (!isActive) {
            throw IllegalStateException("SQLContext is closed | #$id")
        }
    }
}
