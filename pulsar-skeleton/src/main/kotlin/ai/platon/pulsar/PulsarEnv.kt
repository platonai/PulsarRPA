package ai.platon.pulsar

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CLIENT_JS_COMPUTED_STYLES
import ai.platon.pulsar.common.config.PulsarConstants.CLIENT_JS_PROPERTY_NAMES
import ai.platon.pulsar.common.getMemoryMax
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyUpdateThread
import ai.platon.pulsar.common.setPropertyIfAbsent
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.features.defined.N
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.net.browser.WebDriverQueues
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.GoraStorage
import org.springframework.beans.factory.annotation.Autowired
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
        // TODO: read form config file
        val clientJsVersion = "0.2.3"

        val applicationContext: ClassPathXmlApplicationContext
        val storageService: AutoDetectedStorageService
        val startTime = Instant.now()
        /**
         * The number of processors available to the Java virtual machine
         */
        val NCPU = Runtime.getRuntime().availableProcessors()
        // val log4jProperties: Properties
        val goraProperties: Properties
        /**
         * A immutable config loaded from the config file at process startup, and never changes
         * */
        val unmodifiedConfig: ImmutableConfig

        val globalExecutor: GlobalExecutor

        val proxyPool: ProxyPool

        val proxyUpdateThread: ProxyUpdateThread

        val browserControl: BrowserControl

        val webDrivers: WebDriverQueues

        val seleniumEngine: SeleniumEngine

        private val env = AtomicReference<PulsarEnv>()

        init {
            // prerequisite system properties
            setPropertyIfAbsent(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
            setPropertyIfAbsent(CapabilityTypes.PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
            setPropertyIfAbsent(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)

            // the spring application context
            applicationContext = ClassPathXmlApplicationContext(System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION))
            // shut down application context before progress exit
            applicationContext.registerShutdownHook()
            // gora properties
            goraProperties = GoraStorage.properties
            // storage service must be initialized in advance to ensure prerequisites
            // TODO: use spring boot
            storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)

            // the primary configuration, keep unchanged with the configuration files
            unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)

            proxyPool = applicationContext.getBean(ProxyPool::class.java)
            globalExecutor = applicationContext.getBean(GlobalExecutor::class.java)
            proxyUpdateThread = applicationContext.getBean(ProxyUpdateThread::class.java)
            browserControl = applicationContext.getBean(BrowserControl::class.java)
            webDrivers = applicationContext.getBean(WebDriverQueues::class.java)
            seleniumEngine = applicationContext.getBean(SeleniumEngine::class.java)

            // proxyUpdateThread.start()
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

    val isStopped = AtomicBoolean()

    // other possible environment scope objects
    // rest ports, pythonWorkers, memoryManager, metricsSystem, securityManager, blockManager
    // serializers, etc

    fun stop() {
        if (isStopped.getAndSet(true)) {
            return
        }
    }
}
