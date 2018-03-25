package fun.platonic.pulsar.common.config;

/**
 * Created by vincent on 17-4-17.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public interface Configurable {
    /**
     * Return the configuration used by this object.
     */
    ImmutableConfig getConf();

    /**
     * Set the configuration to be used by this object.
     */
    void setConf(ImmutableConfig conf);
}
