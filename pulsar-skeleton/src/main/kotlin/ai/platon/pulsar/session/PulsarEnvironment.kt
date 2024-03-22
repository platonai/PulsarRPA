package ai.platon.pulsar.session

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.*
import java.util.concurrent.ConcurrentSkipListMap

class PulsarEnvironment {
    companion object {
        val properties = mutableMapOf(
            H2_SESSION_FACTORY_CLASS to "ai.platon.pulsar.ql.h2.H2SessionFactory",
            PRIVACY_AGENT_GENERATOR_CLASS to "ai.platon.pulsar.crawl.fetch.privacy.PrivacyAgentGenerator"
        )
    }
    
    init {
        initialize()
    }

    @Synchronized
    private fun initialize() {
        properties.forEach { (key, value) -> Systems.setPropertyIfAbsent(key, value) }
    }
}
