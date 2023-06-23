package ai.platon.pulsar.session

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.support.AbstractPulsarContext

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
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
) : AbstractPulsarSession(context, sessionConfig, id)
