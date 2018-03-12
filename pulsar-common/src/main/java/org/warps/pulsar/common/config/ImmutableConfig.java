package org.warps.pulsar.common.config;

import org.apache.hadoop.conf.Configuration;

public class ImmutableConfig extends AbstractConfiguration {

    public static final ImmutableConfig EMPTY = new ImmutableConfig(false);

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
}
