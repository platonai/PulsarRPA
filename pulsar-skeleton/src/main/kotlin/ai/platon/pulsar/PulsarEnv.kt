package ai.platon.pulsar

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import com.codahale.metrics.CsvReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.Slf4jReporter
import com.codahale.metrics.jmx.JmxReporter
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.ContextStartedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
 */
class PulsarEnv {
    companion object {
        val contextConfigLocation: String
        val applicationContext: ClassPathXmlApplicationContext
        /**
         * If the system is active
         * */
        val isActive get() = !closed.get() && applicationContext.isActive
        val isInactive get() = !isActive

        private val shutdownHookThread: Thread = Thread { this.shutdown() }

        private val active = AtomicBoolean()
        private val closed = AtomicBoolean()

        init {
            // TODO: use spring config
            // prerequisite system properties
            Systems.setPropertyIfAbsent(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            Systems.setPropertyIfAbsent(SYSTEM_PROPERTY_SPECIFIED_RESOURCES, "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml")
            Systems.setPropertyIfAbsent(APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            Systems.setPropertyIfAbsent(H2_SESSION_FACTORY_CLASS, AppConstants.H2_SESSION_FACTORY)

            // the spring application context
            contextConfigLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION)
            applicationContext = ClassPathXmlApplicationContext(contextConfigLocation)
//            applicationContext.registerShutdownHook()

            // shutdown application context before progress exit
            Runtime.getRuntime().addShutdownHook(shutdownHookThread)

            active.set(true)
        }

        fun initialize() {
            // Nothing to do
        }

        @Throws(BeansException::class)
        fun <T> getBean(requiredType: Class<T>): T = applicationContext.getBean(requiredType)

        fun shutdown() {
            if (closed.compareAndSet(false, true)) {
                active.set(false)
                // TODO: still can be managed by spring
                PulsarContext.getOrCreate().use { it.close() }
//                applicationContext.getBean(MetricsManagement::class.java).use { it.close() }
                applicationContext.close()
            }
        }
    }

    // other possible environment scope objects
    // rest ports, memoryManager, securityManager, blockManager
    // serializers, etc
}
