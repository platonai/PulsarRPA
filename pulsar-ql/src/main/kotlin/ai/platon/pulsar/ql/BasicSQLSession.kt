package ai.platon.pulsar.ql

import ai.platon.pulsar.context.support.AbstractPulsarContext

class BasicSQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: SessionDelegate,
    config: SessionConfig
): AbstractSQLSession(context, sessionDelegate, config)
