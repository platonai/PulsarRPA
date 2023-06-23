package ai.platon.pulsar.common.config

/**
 * Created by vincent on 17-4-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
interface KConfigurable {
    /**
     * Return the configuration used by this object.
     *
     * @return a [ImmutableConfig] object.
     */
    var conf: ImmutableConfig
}
