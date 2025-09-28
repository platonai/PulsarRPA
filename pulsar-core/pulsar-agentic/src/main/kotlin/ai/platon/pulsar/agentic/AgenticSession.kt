package ai.platon.pulsar.agentic

import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.h2.AbstractH2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext

interface AgenticSession: SQLSession

open class AbstractAgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractH2SQLSession(context, sessionDelegate, config), AgenticSession

open class AgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractAgenticQLSession(context, sessionDelegate, config)
