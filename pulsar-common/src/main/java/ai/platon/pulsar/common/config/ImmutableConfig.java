package ai.platon.pulsar.common.config;

import org.apache.hadoop.conf.Configuration;

public class ImmutableConfig extends AbstractConfiguration {

    public static final ImmutableConfig EMPTY = new ImmutableConfig(false);

    public static final ImmutableConfig DEFAULT = new ImmutableConfig(new Configuration());

    public ImmutableConfig() {
    }

    public ImmutableConfig(boolean loadDefaultResource) {
        super(loadDefaultResource);
    }

    public ImmutableConfig(String resourcePrefix) {
        super(resourcePrefix);
    }

    public ImmutableConfig(Configuration conf) {
        super(conf);
    }

    public MutableConfig toMutableConfig() {
        return new MutableConfig(this);
    }

    public VolatileConfig toVolatileConfig() {
        return new VolatileConfig(this);
    }
}
