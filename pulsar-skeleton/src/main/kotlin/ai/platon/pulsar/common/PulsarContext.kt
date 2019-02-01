package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.dom.data.BrowserControl
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.gora.GoraStorage
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

object PulsarContext {
    val log = LoggerFactory.getLogger(PulsarContext::class.java)
    val applicationContext: ClassPathXmlApplicationContext
    val unmodifiedConfig: ai.platon.pulsar.common.config.ImmutableConfig
    val storageService: AutoDetectedStorageService
    val startTime = Instant.now()
    // val log4jProperties: Properties
    val goraProperties: Properties
    // val shutdownHooks:

    init {
        // prerequisite system properties
        if (System.getProperty(PULSAR_CONFIG_PREFERRED_DIR) == null) {
            System.setProperty(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
        }
        if (System.getProperty(PULSAR_CONFIG_RESOURCES) == null) {
            System.setProperty(PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
        }
        if (System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION)
        }
        if (System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION)
        }

        // the spring application context
        applicationContext = ClassPathXmlApplicationContext(System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION))
        // shut down application context before progress exit
        applicationContext.registerShutdownHook()
        // the primary configuration, keep unchanged with the configuration files
        unmodifiedConfig = applicationContext.getBean(ai.platon.pulsar.common.config.MutableConfig::class.java)
        // gora properties
        goraProperties = GoraStorage.properties
        // storage service must be initialized in advance to ensure prerequisites
        storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)
        // set javascript for selenium engine to execute after every page is loaded
        if (SeleniumEngine.CLIENT_JS.isNullOrBlank()) {
            SeleniumEngine.CLIENT_JS = BrowserControl(unmodifiedConfig).getJs()
        }

        log.info("Pulsar context is initialized")
    }

    fun createSession(): PulsarSession {
        return PulsarSession(applicationContext, ai.platon.pulsar.common.config.VolatileConfig(unmodifiedConfig))
    }

    fun getBean(name: String): Any? {
        return applicationContext.getBean(name)
    }

    fun getBean(clazz: Class<Any>): Any? {
        return applicationContext.getBean(clazz)
    }

    fun getBean(clazz: KClass<Any>): Any? {
        return applicationContext.getBean(clazz.java)
    }
}
