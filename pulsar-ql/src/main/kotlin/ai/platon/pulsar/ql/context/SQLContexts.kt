package ai.platon.pulsar.ql.context

import ai.platon.pulsar.session.BasicPulsarSession
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.ql.AbstractSQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.SessionDelegate
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.H2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
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

    override fun createSession(sessionDelegate: SessionDelegate): H2SQLSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
        }
        logger.info("SQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as H2SQLSession
    }

    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}

class StaticH2SQLContext(
    override val applicationContext: StaticApplicationContext = StaticApplicationContext()
) : H2SQLContext(applicationContext) {

    /**
     * The unmodified config
     * */
    override val unmodifiedConfig = getBeanOrNull() ?: ImmutableConfig()
    /**
     * Url normalizers
     * */
    override val urlNormalizers = getBeanOrNull() ?: CrawlUrlNormalizers(unmodifiedConfig)
    /**
     * The web db
     * */
    override val webDb = getBeanOrNull() ?: WebDb(unmodifiedConfig)
    /**
     * The global cache
     * */
    override val globalCacheFactory = getBeanOrNull() ?: GlobalCacheFactory(unmodifiedConfig)
    /**
     * The main loop
     * */
    override val crawlLoops: CrawlLoops = getBeanOrNull() ?: CrawlLoops(StreamingCrawlLoop(globalCacheFactory, unmodifiedConfig))
    /**
     * The injection component
     * */
    override val injectComponent = getBeanOrNull() ?: InjectComponent(webDb, unmodifiedConfig)
    /**
     * The fetch component
     * */
    override val fetchComponent = getBeanOrNull() ?: BatchFetchComponent(webDb, unmodifiedConfig)
    /**
     * The parse component
     * */
    override val parseComponent: ParseComponent = getBeanOrNull() ?: ParseComponent(globalCacheFactory, unmodifiedConfig)
    /**
     * The update component
     * */
    override val updateComponent = getBeanOrNull() ?: UpdateComponent(webDb, unmodifiedConfig)
    /**
     * The load component
     * */
    override val loadComponent = getBeanOrNull() ?: LoadComponent(
        webDb, globalCacheFactory, fetchComponent, parseComponent, updateComponent, unmodifiedConfig)

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

    override fun createSession(sessionDelegate: SessionDelegate): AbstractSQLSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            H2SQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
        }
        logger.info("SQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as H2SQLSession
    }

    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }

    override fun close() {
        super.close()
        db.close()
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
    fun create(context: ApplicationContext): SQLContext =
        create(H2SQLContext(context as AbstractApplicationContext))

    @Synchronized
    fun create(contextLocation: String): SQLContext = create(ClassPathXmlSQLContext(contextLocation))

    @Synchronized
    fun createSession() = create().createSession()

    fun await() {
        PulsarContexts.await()
    }

    @Synchronized
    fun shutdown() {
        PulsarContexts.shutdown()
    }
}

fun withSQLContext(block: (context: SQLContext) -> Unit) {
    SQLContexts.create(DefaultClassPathXmlSQLContext()).use {
        block(it)
    }
}

fun withSQLContext(contextLocation: String, block: (context: SQLContext) -> Unit) {
    SQLContexts.create(ClassPathXmlSQLContext(contextLocation)).use {
        block(it)
    }
}
