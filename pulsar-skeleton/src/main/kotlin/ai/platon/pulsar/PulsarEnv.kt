package ai.platon.pulsar

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*

object PulsarProperties {
    val properties = mapOf(
            PULSAR_CONFIG_PREFERRED_DIR to "pulsar-conf",
            SYSTEM_PROPERTY_SPECIFIED_RESOURCES to "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml",
            H2_SESSION_FACTORY_CLASS to AppConstants.H2_SESSION_FACTORY
    )

    fun setAllProperties(replaceIfExist: Boolean = false) {
        properties.forEach { (name, value) ->
            if (replaceIfExist) {
                Systems.setProperty(name, value)
            } else {
                Systems.setPropertyIfAbsent(name, value)
            }
        }
    }
}
