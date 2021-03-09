package ai.platon.pulsar

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.H2_SESSION_FACTORY_CLASS

class PulsarEnvironment {
    init {
        initialize()
    }

    @Synchronized
    private fun initialize() {
        Systems.setPropertyIfAbsent(H2_SESSION_FACTORY_CLASS, "ai.platon.pulsar.ql.h2.H2SessionFactory")
        // TODO: tika.config is not working
        System.setProperty("tika.config", "tika-config.xml")
    }
}
