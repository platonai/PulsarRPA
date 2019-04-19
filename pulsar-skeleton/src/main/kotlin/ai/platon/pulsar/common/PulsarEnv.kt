package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.features.defined.N
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.gora.GoraStorage
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*

object PulsarEnv {
    val applicationContext: ClassPathXmlApplicationContext
    val unmodifiedConfig: ImmutableConfig
    val storageService: AutoDetectedStorageService
    val startTime = Instant.now()
    // val log4jProperties: Properties
    val goraProperties: Properties
    // val shutdownHooks:

    init {
        // prerequisite system properties
        if (System.getProperty(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR) == null) {
            System.setProperty(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
        }
        if (System.getProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES) == null) {
            System.setProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
        }
        if (System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)
        }
        if (System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)
        }

        // the spring application context
        applicationContext = ClassPathXmlApplicationContext(System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION))
        // shut down application context before progress exit
        applicationContext.registerShutdownHook()
        // the primary configuration, keep unchanged with the configuration files
        unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)
        // gora properties
        goraProperties = GoraStorage.properties
        // storage service must be initialized in advance to ensure prerequisites
        storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)
    }

    fun ensureEnv() {
        val nil = FeaturedDocument.NIL
        require(nil.html.startsWith("<html>"))
        require(nil.features.dimension == N)
    }
}
