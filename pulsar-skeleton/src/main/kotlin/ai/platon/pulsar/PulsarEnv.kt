package ai.platon.pulsar

import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.setPropertyIfAbsent
import ai.platon.pulsar.crawl.component.SeleniumFetchComponent
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.gora.GoraStorage
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds all the runtime environment objects for a running Pulsar instance
 * All the threads shares the same PulsarEnv.
 *
 * TODO: make it compatible with spring boot
 */
class PulsarEnv {
    companion object {
        private val log = LoggerFactory.getLogger(PulsarEnv::class.java)

        // TODO: read form config file
        val clientJsVersion = "0.2.3"

        val contextConfigLocation: String
        val applicationContext: ClassPathXmlApplicationContext
        val storageService: AutoDetectedStorageService
        val startTime = Instant.now()
        /**
         * The number of processors available to the Java virtual machine
         */
        val NCPU = Runtime.getRuntime().availableProcessors()
        /**
         * Gora properties
         * */
        val goraProperties: Properties
        /**
         * The unmodified config loaded from the config file at process startup, and never changes
         * */
        val unmodifiedConfig: ImmutableConfig

        val globalExecutor: GlobalExecutor

        val proxyPool: ProxyPool

        val seleniumFetchComponent: SeleniumFetchComponent

        val monitor: PulsarMonitor

        private val env = AtomicReference<PulsarEnv>()

        private val active = AtomicBoolean()
        private val closed = AtomicBoolean()

        init {
            // prerequisite system properties
            setPropertyIfAbsent(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            setPropertyIfAbsent(PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
            setPropertyIfAbsent(APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)

            // the spring application context
            contextConfigLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION)
            applicationContext = ClassPathXmlApplicationContext(contextConfigLocation)
            // shut down application context before progress exit
            applicationContext.registerShutdownHook()
            // the primary configuration, keep unchanged with the configuration files
            unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)
            // gora properties
            goraProperties = GoraStorage.properties
            // storage service must be initialized in advance to ensure prerequisites
            // TODO: use spring boot
            storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)

            globalExecutor = applicationContext.getBean(GlobalExecutor::class.java)

            proxyPool = applicationContext.getBean(ProxyPool::class.java)

            seleniumFetchComponent = applicationContext.getBean(SeleniumFetchComponent::class.java)

            monitor = applicationContext.getBean(PulsarMonitor::class.java)

            monitor.start()

            active.set(true)
        }

        fun getOrCreate(): PulsarEnv {
            synchronized(PulsarEnv::class.java) {
                if (env.get() == null) {
                    env.set(PulsarEnv())
                }

                return env.get()
            }
        }
    }

    // other possible environment scope objects
    // rest ports, pythonWorkers, memoryManager, metricsSystem, securityManager, blockManager
    // serializers, etc

    fun shutdown() {
        if (closed.getAndSet(true)) {
            return
        }

        // Internal proxy server blocks can not be closed by spring, the reason should be investigated
        monitor.use { it.close() }
        applicationContext.use { it.close() }

        active.set(false)
    }

    /**
     * Proxy system can be enabled/disabled at runtime
     * */
    val useProxy: Boolean get() {
        if (RuntimeUtils.hasLocalFileCommand(PulsarConstants.CMD_ENABLE_PROXY)) {
            return true
        }

        // explicit set system environment property
        var useProxy = System.getProperty(PROXY_USE_PROXY)
        if (useProxy != null) {
            when (useProxy) {
                "yes" -> return true
                "no" -> return false
            }
        }

        useProxy = unmodifiedConfig.get(PROXY_USE_PROXY)
        if (useProxy != null) {
            when (useProxy) {
                "yes" -> return true
                "no" -> return false
            }
        }

        // if no one set the proxy availability explicitly, but we have providers, use it
        if (ProxyPool.hasEnabledProvider()) {
            return true
        }

        return false
    }
}
