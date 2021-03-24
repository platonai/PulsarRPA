package ai.platon.pulsar.ql

import ai.platon.pulsar.AbstractPulsarSession
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.types.ValueDom

abstract class AbstractSQLSession(
    context: AbstractPulsarContext,
    val sessionDelegate: SessionDelegate,
    config: SessionConfig
) : AbstractPulsarSession(context, config, sessionDelegate.id), SQLSession {

    val registeredAllUdfClasses = mutableListOf<Class<out Any>>()
    val registeredAdminUdfClasses
        get() = registeredAllUdfClasses.filter {
            it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" }
        }
    val registeredUdfClasses
        get() = registeredAllUdfClasses.filterNot {
            it in registeredAdminUdfClasses
        }

    override fun parseValueDom(page: WebPage): ValueDom {
        return ValueDom.get(parse(page))
    }
}
