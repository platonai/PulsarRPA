package ai.platon.pulsar.ql

import ai.platon.pulsar.AbstractPulsarSession
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.types.ValueDom

abstract class AbstractSQLSession(
    context: AbstractPulsarContext,
    override val sessionDelegate: SessionDelegate,
    config: SessionConfig
) : AbstractPulsarSession(context, config, sessionDelegate.id), SQLSession {

    override val registeredAllUdfClasses = mutableListOf<Class<out Any>>()

    override fun parseValueDom(page: WebPage): ValueDom {
        return ValueDom.get(parse(page))
    }
}
