package ai.platon.pulsar

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ExternalProxyManager
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.setPropertyIfAbsent
import ai.platon.pulsar.crawl.component.SeleniumFetchComponent
import ai.platon.pulsar.net.browser.WebDriverQueues
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.gora.GoraStorage
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds all the runtime environment objects for a running Pulsar instance
 * All the threads shares the same PulsarEnv.
 */
class PulsarEnv {
    companion object {
        private val log = LoggerFactory.getLogger(PulsarEnv::class.java)

        // TODO: read form config file
        val clientJsVersion = "0.2.3"

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

        val proxyManager: ExternalProxyManager

        val internalProxyServer: InternalProxyServer

        val browserControl: BrowserControl

        val webDrivers: WebDriverQueues

        val seleniumFetchComponent: SeleniumFetchComponent

        private val env = AtomicReference<PulsarEnv>()

        init {
            // prerequisite system properties
            setPropertyIfAbsent(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            setPropertyIfAbsent(PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
            setPropertyIfAbsent(APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)

            // the spring application context
            applicationContext = ClassPathXmlApplicationContext(System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION))
            // shut down application context before progress exit
            applicationContext.registerShutdownHook()
            // gora properties
            goraProperties = GoraStorage.properties
            // storage service must be initialized in advance to ensure prerequisites
            // TODO: use spring boot
            storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)

            // the primary configuration, keep unchanged with the configuration files
            unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)

            globalExecutor = applicationContext.getBean(GlobalExecutor::class.java)

            proxyPool = applicationContext.getBean(ProxyPool::class.java)
            proxyManager = applicationContext.getBean(ExternalProxyManager::class.java)
            internalProxyServer = applicationContext.getBean(InternalProxyServer::class.java)

            browserControl = applicationContext.getBean(BrowserControl::class.java)
            webDrivers = applicationContext.getBean(WebDriverQueues::class.java)
            seleniumFetchComponent = applicationContext.getBean(SeleniumFetchComponent::class.java)

            proxyManager.start()

            if (internalProxyServer.enabled) {
                internalProxyServer.start()
            }

            log.info("Pulsar env is initialized")
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

    val isQuit = AtomicBoolean()

    // other possible environment scope objects
    // rest ports, pythonWorkers, memoryManager, metricsSystem, securityManager, blockManager
    // serializers, etc

    fun exit() {
        if (isQuit.getAndSet(true)) {
            return
        }

        internalProxyServer.close()
    }
}
