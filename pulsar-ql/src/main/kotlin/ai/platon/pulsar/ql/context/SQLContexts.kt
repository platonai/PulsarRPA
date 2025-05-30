package ai.platon.pulsar.ql.context

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.ql.AbstractSQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.SessionDelegate
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.H2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.context.support.ContextDefaults
import ai.platon.pulsar.skeleton.crawl.component.*
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.support.StaticApplicationContext
import java.sql.Connection

open class H2SQLContext(
    applicationContext: AbstractApplicationContext
) : AbstractSQLContext(applicationContext) {

    private val logger = LoggerFactory.getLogger(H2SQLContext::class.java)

    private val db = H2MemoryDb()

    override val randomConnection: Connection get() = db.getRandomConnection()

    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): H2SQLSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
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
    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}

open class StaticH2SQLContext(
    applicationContext: StaticApplicationContext = StaticApplicationContext()
) : H2SQLContext(applicationContext) {
    private val defaults = ContextDefaults()

    /**
     * The unmodified config
     * */
    override val unmodifiedConfig get() = getBeanOrNull() ?: defaults.unmodifiedConfig

    /**
     * Url normalizer
     * */
    override val urlNormalizer get() = getBeanOrNull() ?: defaults.urlNormalizer
    /**
     * The web db
     * */
    override val webDb get() = getBeanOrNull() ?: defaults.webDb
    /**
     * The global cache
     * */
    override val globalCacheFactory get() = getBeanOrNull() ?: defaults.globalCacheFactory
    /**
     * The fetch component
     * */
    override val fetchComponent get() = getBeanOrNull() ?: defaults.fetchComponent
    /**
     * The parse component
     * */
    override val parseComponent get() = getBeanOrNull() ?: defaults.parseComponent
    /**
     * The update component
     * */
    override val updateComponent get() = getBeanOrNull() ?: defaults.updateComponent
    /**
     * The load component
     * */
    override val loadComponent get() = getBeanOrNull() ?: defaults.loadComponent
    /**
     * The main loop
     * */
    override val crawlLoops get() = getBeanOrNull() ?: defaults.crawlLoops

    init {
        applicationContext.refresh()
    }
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
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
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
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
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
        AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
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
