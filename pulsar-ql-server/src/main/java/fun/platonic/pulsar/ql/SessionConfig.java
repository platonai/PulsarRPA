package fun.platonic.pulsar.ql;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.VolatileConfig;

public class SessionConfig extends VolatileConfig {

    private DbSession dbSession;

    public SessionConfig(DbSession dbSession, ImmutableConfig fallbackConfig) {
        super(fallbackConfig);
        this.dbSession = dbSession;
    }

    @Override
    public void setTTL(String name, int ttl) {
        if (ttl > 0) {
            ttl = 1 + ttl + dbSession.getSqlSequence();
        }

        super.setTTL(name, ttl);
    }

    @Override
    public boolean isExpired(String propertyName) {
        int sequence = dbSession.getSqlSequence();
        int ttl = getTTL(propertyName);
        boolean expired = sequence > ttl;
        // LOG.debug("Property {}, sequence: {}, ttl: {}", propertyName, sequence, ttl);
        if (LOG.isDebugEnabled() && expired) {
            LOG.debug("Property {} is expired at the {}th command", propertyName, sequence);
        }

        return expired;
    }

}
