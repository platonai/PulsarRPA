package ai.platon.pulsar.ql

import ai.platon.pulsar.skeleton.session.AbstractPulsarSession
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.common.types.ValueDom
import kotlin.reflect.KClass

abstract class AbstractSQLSession(
    context: AbstractPulsarContext,
    override val sessionDelegate: SessionDelegate,
    config: SessionConfig
) : AbstractPulsarSession(context, config, sessionDelegate.id.toLong()), SQLSession {

    override val udfClassSamples: MutableSet<KClass<out Any>> = mutableSetOf()

    override val registeredAllUdfClasses: MutableSet<Class<out Any>> = mutableSetOf()

    override fun parseValueDom(page: WebPage): ValueDom = ValueDom.get(parse(page))

    override fun execute(sql: String) = sqlContext.execute(sql)

    override fun executeQuery(sql: String) = sqlContext.executeQuery(sql)
}
