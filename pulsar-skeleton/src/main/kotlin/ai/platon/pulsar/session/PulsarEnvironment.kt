package ai.platon.pulsar.session

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes.H2_SESSION_FACTORY_CLASS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS

class PulsarEnvironment {
    companion object {
        val properties = mutableMapOf(
            H2_SESSION_FACTORY_CLASS to "ai.platon.pulsar.ql.h2.H2SessionFactory",
            /**
             * Use the random privacy agent generator by default.
             * If there is no prototype Chrome browser, it acts as indigo mode.
             * If there is a prototype Chrome browser, it copies and inherits the prototype Chrome browser's
             * user data directory.
             * */
            PRIVACY_AGENT_GENERATOR_CLASS to "ai.platon.pulsar.crawl.fetch.privacy.RandomPrivacyAgentGenerator"
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
