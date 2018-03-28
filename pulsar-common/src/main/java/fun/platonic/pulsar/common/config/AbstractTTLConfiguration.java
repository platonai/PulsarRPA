package fun.platonic.pulsar.common.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public abstract class AbstractTTLConfiguration extends MutableConfig {

    private ImmutableConfig fallbackConfig;
    private Map<String, Integer> ttls = Collections.synchronizedMap(new HashMap<>());

    public AbstractTTLConfiguration() {
        super(false);
    }

    @Override
    public String get(String name, String defaultValue) {
        String value = super.get(name);
        if (value != null) {
            if (!isExpired(name)) {
                return super.get(name, defaultValue);
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Session config (with default) {} is expired", name);
                }
                ttls.remove(name);
                super.unset(name);
            }
        }

        return fallbackConfig != null ? fallbackConfig.get(name, defaultValue) : null;
    }

    @Override
    public String get(String name) {
        String value = super.get(name);
        if (value != null) {
            if (!isExpired(name)) {
                return value;
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Session config {} is expired", name);
                }
                ttls.remove(name);
                super.unset(name);
            }
        }

        return fallbackConfig != null ? fallbackConfig.get(name) : null;
    }

    public void set(String name, String value, int ttl) {
        setTTL(name, ttl);
        super.set(name, value);
    }

    public String getAndSet(String name, String value, int ttl) {
        String old = get(name);
        this.set(name, value, ttl);
        return old;
    }

    public int getTTL(String name) {
        return ttls.getOrDefault(name, -1);
    }

    public void setTTL(String name, int ttl) {
        if (ttl > 0) {
            ttls.put(name, ttl);
        } else {
            ttls.remove(name);
            super.unset(name);
        }
    }

    abstract public boolean isExpired(String key);

    public ImmutableConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public void setFallbackConfig(ImmutableConfig fallbackConfig) {
        this.fallbackConfig = fallbackConfig;
    }
}
