package ai.platon.pulsar.ql.context

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.ql.AbstractSQLSession
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.SessionDelegate
import ai.platon.pulsar.ql.h2.AbstractH2SQLSession
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.H2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.sql.Connection

abstract class AbstractH2SQLContext(
    applicationContext: AbstractApplicationContext
) : AbstractSQLContext(applicationContext) {

    private val logger = LoggerFactory.getLogger(AbstractH2SQLContext::class.java)

    private val db = H2MemoryDb()

    override val randomConnection: Connection get() = db.getRandomConnection()

    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): SQLSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, configuration))
        }
        logger.info("SQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as H2SQLSession
    }

    /**
     * Create a [BasicPulsarSession].
     *
     * > **NOTE:** The session is not a SQLSession, use [execute], [executeQuery] to access [ai.platon.pulsar.ql.SQLSession].
     * */
    @Throws(Exception::class)
    override fun createSession(): PulsarSession {
        val session = BasicPulsarSession(this, configuration.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}

open class H2SQLContext(
    applicationContext: AbstractApplicationContext
) : AbstractH2SQLContext(applicationContext) {
}

open class ClassPathXmlSQLContext(configLocation: String) :
    AbstractSQLContext(ClassPathXmlApplicationContext(configLocation)) {

    private val logger = LoggerFactory.getLogger(ClassPathXmlSQLContext::class.java)

    private val db = H2MemoryDb()

    override val randomConnection: Connection
        get() = db.getRandomConnection()

    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): AbstractSQLSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, configuration))
        }
        logger.info("SQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as H2SQLSession
    }

    /**
     * Create a [BasicPulsarSession].
     *
     * > **NOTE:** The session is not a SQLSession, use [execute], [executeQuery] to access [ai.platon.pulsar.ql.SQLSession].
     * */
    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, configuration.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }

    override fun close() {
        try {
            super.close()
            db.close()
        } catch (t: Throwable) {
            warnForClose(this, t)
        }
    }
}

open class DefaultClassPathXmlSQLContext() : ClassPathXmlSQLContext(
    System.getProperty(
        CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION,
        AppConstants.QL_CONTEXT_CONFIG_LOCATION
    )
)

object SQLContexts {
    @Synchronized
    fun create(): SQLContext = (PulsarContexts.activeContext as? SQLContext)
        ?: create(DefaultClassPathXmlSQLContext())

    @Synchronized
    fun create(context: SQLContext): SQLContext = context.also { PulsarContexts.create(it) }

    @Synchronized
    fun create(applicationContext: ApplicationContext): SQLContext {
        val context = PulsarContexts.activeContext
        if (context is H2SQLContext && context.applicationContext == applicationContext) {
            return PulsarContexts.activeContext as SQLContext
        }

        return create(H2SQLContext(applicationContext as AbstractApplicationContext))
    }

    @Synchronized
    fun create(contextLocation: String): SQLContext = create(ClassPathXmlSQLContext(contextLocation))

    @Synchronized
    fun createSession() = create().createSession()

    @Synchronized
    fun getOrCreateSession() = create().getOrCreateSession()

    @Throws(InterruptedException::class)
    fun await() {
        PulsarContexts.await()
    }

    @Synchronized
    fun shutdown() {
        PulsarContexts.shutdown()
    }
}
