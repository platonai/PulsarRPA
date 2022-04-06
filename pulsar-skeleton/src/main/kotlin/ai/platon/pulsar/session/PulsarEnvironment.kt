package ai.platon.pulsar.session

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.H2_SESSION_FACTORY_CLASS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS

class PulsarEnvironment {
    init {
        initialize()
    }

    @Synchronized
    private fun initialize() {
        Systems.setPropertyIfAbsent(H2_SESSION_FACTORY_CLASS, "ai.platon.pulsar.ql.h2.H2SessionFactory")
        Systems.setPropertyIfAbsent(PRIVACY_CONTEXT_ID_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.SequentialPrivacyContextIdGenerator")
    }
}
