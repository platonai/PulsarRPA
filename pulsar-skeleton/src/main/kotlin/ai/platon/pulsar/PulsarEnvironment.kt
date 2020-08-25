package ai.platon.pulsar

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.H2_SESSION_FACTORY_CLASS
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_USE_PROXY

class PulsarEnvironment {
    fun initialize() {
        Systems.setPropertyIfAbsent(PROXY_USE_PROXY, "no")
        Systems.setPropertyIfAbsent(H2_SESSION_FACTORY_CLASS, "ai.platon.pulsar.ql.h2.H2SessionFactory")
        System.setProperty("tika.config", "tika-config.xml")
    }
}
