package ai.platon.pulsar

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import org.springframework.beans.BeansException
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds all the runtime environment objects for a running Pulsar instance
 * All the threads shares the same PulsarEnv.
 */
class PulsarEnv {
    companion object {
        lateinit var applicationContext: ConfigurableApplicationContext
        /**
         * If the system is active
         * */
        val isActive get() = !closed.get() && applicationContext.isActive

        private var shutdownHookThread: Thread? = Thread { this.shutdown() }

        private val active = AtomicBoolean()
        private val closed = AtomicBoolean()

        fun initEnvironment() {
            // TODO: load from property files, use spring environment and profile
            // prerequisite system properties
            Systems.setPropertyIfAbsent(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            Systems.setPropertyIfAbsent(SYSTEM_PROPERTY_SPECIFIED_RESOURCES, "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml")
            Systems.setPropertyIfAbsent(H2_SESSION_FACTORY_CLASS, AppConstants.H2_SESSION_FACTORY)

            // shutdown application context before progress exit
            shutdownHookThread?.let { Runtime.getRuntime().addShutdownHook(it) }
        }

        fun initialize() {
            if (active.get()) {
                return
            }

            val configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            initialize(configLocation)
        }

        fun initialize(configLocation: String) {
            Systems.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, configLocation)
            val applicationContext = ClassPathXmlApplicationContext(AppConstants.APP_CONTEXT_CONFIG_LOCATION)
            initialize(applicationContext)
        }

        fun initialize(appContext: ConfigurableApplicationContext) {
            if (active.get()) {
                return
            }

            applicationContext = appContext

            initEnvironment()

            active.set(true)
        }

        @Throws(BeansException::class)
        fun <T> getBean(requiredType: Class<T>): T = applicationContext.getBean(requiredType)

        fun shutdown() {
            if (closed.compareAndSet(false, true)) {
                active.set(false)
                // TODO: still can be managed by spring
                // force close WebDriverPool, ProxyMonitor, etc
                PulsarContext.getOrCreate().use { it.close() }
                applicationContext.close()

                shutdownHookThread?.also {
                    Runtime.getRuntime().removeShutdownHook(shutdownHookThread)
                    shutdownHookThread = null
                }
            }
        }
    }

    // other possible environment scope objects
    // rest ports, memoryManager, securityManager, blockManager
    // serializers, etc
}
