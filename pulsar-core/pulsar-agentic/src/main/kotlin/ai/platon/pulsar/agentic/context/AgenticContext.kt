package ai.platon.pulsar.agentic.context

import ai.platon.pulsar.agentic.DefaultAgenticSession
import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.SessionDelegate
import ai.platon.pulsar.ql.context.AbstractH2SQLContext
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext

interface AgenticContext : SQLContext {
    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): AgenticSession
}

abstract class AbstractAgenticContext(
    applicationContext: AbstractApplicationContext
) : AbstractH2SQLContext(applicationContext), AgenticContext {
    private val logger = getLogger(this)

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

    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): AgenticSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            DefaultAgenticSession(this, sessionDelegate, SessionConfig(sessionDelegate, configuration))
        }
        logger.info("AgenticQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as DefaultAgenticSession
    }
}

open class QLAgenticContext(
    applicationContext: AbstractApplicationContext
) : AbstractAgenticContext(applicationContext) {
}

open class ClassPathXmlAgenticContext(configLocation: String) :
    AbstractAgenticContext(ClassPathXmlApplicationContext(configLocation)) {
}

open class DefaultClassPathXmlAgenticContext() : ClassPathXmlAgenticContext(
    System.getProperty(
        CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION,
        AppConstants.AGENTIC_CONTEXT_CONFIG_LOCATION
    )
)
