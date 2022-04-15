package ai.platon.pulsar.ql

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig

class SessionConfig(
    private val sessionDelegate: SessionDelegate,
    fallbackConfig: ImmutableConfig
): VolatileConfig(fallbackConfig) {

    override fun setTTL(name: String, ttl: Int) {
        super.setTTL(name, 1 + ttl + sessionDelegate.sqlSequence)
    }

    override fun isExpired(key: String): Boolean {
        val sequence = sessionDelegate.sqlSequence
        val ttl = super.getTTL(key)
        val expired = sequence > ttl
        // log.debug("Property {}, sequence: {}, ttl: {}", propertyName, sequence, ttl);
        if (logger.isDebugEnabled && expired) {
            logger.debug("Property {} is expired at the {}th command", key, sequence)
        }

        return expired
    }
}
