package ai.platon.pulsar.ql

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig

class SessionConfig(private val dbSession: DbSession, fallbackConfig: ImmutableConfig): VolatileConfig(fallbackConfig) {

    override fun setTTL(name: String, ttl: Int) {
        super.setTTL(name, 1 + ttl + dbSession.sqlSequence)
    }

    override fun isExpired(propertyName: String): Boolean {
        val sequence = dbSession.sqlSequence
        val ttl = super.getTTL(propertyName)
        val expired = sequence > ttl
        // log.debug("Property {}, sequence: {}, ttl: {}", propertyName, sequence, ttl);
        if (LOG.isDebugEnabled && expired) {
            LOG.debug("Property {} is expired at the {}th command", propertyName, sequence)
        }

        return expired
    }
}
