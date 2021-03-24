package ai.platon.pulsar

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.context.support.AbstractPulsarContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BasicPulsarSession(
    /**
     * The pulsar context
     * */
    context: AbstractPulsarContext,
    /**
     * The session scope volatile config, every setting is supposed to be changed at any time and any place
     * */
    sessionConfig: VolatileConfig,
    /**
     * The session id. Session id is expected to be set by the container, e.g. the h2 database runtime
     * */
    id: Int = generateNextId()
) : AbstractPulsarSession(context, sessionConfig, id) {
}
