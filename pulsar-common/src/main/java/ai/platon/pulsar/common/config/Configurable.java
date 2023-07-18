package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-4-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface Configurable {
    /**
     * Return the configuration used by this object.
     *
     * @return a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     */
    ImmutableConfig getConf();

    /**
     * Set the configuration to be used by this object.
     *
     * @param jobConf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     */
    default void setConf(ImmutableConfig jobConf) {}
}
