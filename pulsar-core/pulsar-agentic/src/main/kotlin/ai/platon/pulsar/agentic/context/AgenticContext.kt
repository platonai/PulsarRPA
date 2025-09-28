package ai.platon.pulsar.agentic.context

import ai.platon.pulsar.agentic.QLAgenticSession
import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.SessionDelegate
import ai.platon.pulsar.ql.context.AbstractH2SQLContext
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.h2.H2SessionDelegate
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

    @Throws(Exception::class)
    override fun createSession(sessionDelegate: SessionDelegate): AgenticSession {
        require(sessionDelegate is H2SessionDelegate)
        val session = sqlSessions.computeIfAbsent(sessionDelegate.id) {
            QLAgenticSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
        }
        logger.info("AgenticQLSession is created | #{}/{}/{}", session.id, sessionDelegate.id, id)
        return session as QLAgenticSession
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
        AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
    )
)
