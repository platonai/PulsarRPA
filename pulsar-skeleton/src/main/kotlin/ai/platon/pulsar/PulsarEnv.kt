package ai.platon.pulsar

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.setPropertyIfAbsent
import ai.platon.pulsar.persist.gora.GoraStorage
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.ContextStartedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

// TODO: add event handlers instead of PulsarEnv like global context, spring context should be the only global context
class AppRefreshedEventHandler : ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        println("ContextRefreshedEvent Received")
    }
}

class AppStartEventHandler : ApplicationListener<ContextStartedEvent> {
    override fun onApplicationEvent(event: ContextStartedEvent) {
        println("ContextStartedEvent Received")
    }
}

class AppStopEventHandler : ApplicationListener<ContextStoppedEvent> {
    override fun onApplicationEvent(event: ContextStoppedEvent) {
        println("ContextStoppedEvent Received")
    }
}

class AppCloseEventHandler : ApplicationListener<ContextClosedEvent> {
    override fun onApplicationEvent(event: ContextClosedEvent) {
        println("ContextClosedEvent Received")
    }
}

/**
 * Holds all the runtime environment objects for a running Pulsar instance
 * All the threads shares the same PulsarEnv.
 *
 * TODO: make it compatible with spring
 */
class PulsarEnv {
    companion object {
        private val log = LoggerFactory.getLogger(PulsarEnv::class.java)

        // TODO: read form config file
        val clientJsVersion = "0.2.3"

        val contextConfigLocation: String
        val applicationContext: ClassPathXmlApplicationContext
        val startTime = Instant.now()
        /**
         * Gora properties
         * */
        val goraProperties: Properties
        /**
         * The unmodified config loaded from the config file at process startup, and never changes
         * */
        val unmodifiedConfig: ImmutableConfig

        private val env = AtomicReference<PulsarEnv>()

        private val active = AtomicBoolean()
        private val closed = AtomicBoolean()

        init {
            // prerequisite system properties
            setPropertyIfAbsent(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            setPropertyIfAbsent(SYSTEM_PROPERTY_SPECIFIED_RESOURCES, "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml")
            setPropertyIfAbsent(APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            setPropertyIfAbsent(PARAM_H2_SESSION_FACTORY, AppConstants.H2_SESSION_FACTORY)

            // the spring application context
            contextConfigLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION)
            applicationContext = ClassPathXmlApplicationContext(contextConfigLocation)
            // shut down application context before progress exit
            applicationContext.registerShutdownHook()
            // the primary configuration, keep unchanged with the configuration files
            unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)
            // gora properties
            goraProperties = GoraStorage.properties

            active.set(true)
        }

        fun get(): PulsarEnv {
            // TODO: is it necessary to keep an instance?
            synchronized(PulsarEnv::class.java) {
                if (env.get() == null) {
                    env.set(PulsarEnv())
                }

                return env.get()
            }
        }
    }

    @Throws(BeansException::class)
    fun <T> getBean(requiredType: Class<T>): T {
        return applicationContext.getBean(requiredType)
    }

    // other possible environment scope objects
    // rest ports, pythonWorkers, memoryManager, metricsSystem, securityManager, blockManager
    // serializers, etc

    fun shutdown() {
        if (closed.getAndSet(true)) {
            return
        }

        applicationContext.close()

        active.set(false)
    }
}
