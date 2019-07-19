package ai.platon.pulsar.common.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class VolatileConfig extends MutableConfig {

    private MutableConfig fallbackConfig;
    private Map<String, Integer> ttls = Collections.synchronizedMap(new HashMap<>());

    public VolatileConfig() {
        super(false);
    }

    public VolatileConfig(ImmutableConfig fallbackConfig) {
        this(new MutableConfig(fallbackConfig));
    }

    public VolatileConfig(MutableConfig fallbackConfig) {
        super(false);
        this.fallbackConfig = Objects.requireNonNull(fallbackConfig);
    }

    @Nonnull
    @Override
    public String get(String name, String defaultValue) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(defaultValue);

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

        return fallbackConfig != null ? fallbackConfig.get(name, defaultValue) : defaultValue;
    }

    @Nullable
    @Override
    public String get(String name) {
        Objects.requireNonNull(name);

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
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        setTTL(name, ttl);
        super.set(name, value);
    }

    @Nullable
    public String getAndSet(String name, String value, int ttl) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        String old = get(name);
        if (old != null) {
            this.set(name, value, ttl);
        }
        return old;
    }

    public int getTTL(String name) {
        return ttls.getOrDefault(name, Integer.MAX_VALUE);
    }

    public void setTTL(String name, int ttl) {
        if (ttl > 0) {
            ttls.put(name, ttl);
        } else {
            ttls.remove(name);
            super.unset(name);
        }
    }

    public boolean isExpired(String key) {
        return false;
    }

    public MutableConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public void setFallbackConfig(MutableConfig fallbackConfig) {
        this.fallbackConfig = fallbackConfig;
    }
}
