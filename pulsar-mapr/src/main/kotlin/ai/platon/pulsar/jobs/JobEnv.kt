package ai.platon.pulsar.jobs

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.setPropertyIfAbsent
import org.springframework.context.support.ClassPathXmlApplicationContext

class JobEnv {
    companion object {
        val contextConfigLocation: String
        val applicationContext: ClassPathXmlApplicationContext

        init {
            setPropertyIfAbsent(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, "mapr-conf")
            setPropertyIfAbsent(CapabilityTypes.SYSTEM_PROPERTY_SPECIFIED_RESOURCES, "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml")
            setPropertyIfAbsent(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.JOB_CONTEXT_CONFIG_LOCATION)

            PulsarEnv.initialize()

            contextConfigLocation = PulsarEnv.contextConfigLocation
            applicationContext = PulsarEnv.applicationContext
        }

        fun initialize() {
        }
    }
}
