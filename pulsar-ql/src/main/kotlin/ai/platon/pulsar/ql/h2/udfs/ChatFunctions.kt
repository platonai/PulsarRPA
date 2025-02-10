package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import ai.platon.pulsar.ql.context.SQLContexts

/**
 * Created by vincent on 17-11-1.
 * Copyright @ 2013-2020 Platon AI. All rights reserved
 */
@Suppress("unused")
@UDFGroup(namespace = "DOM")
object ChatFunctions {

    private val sqlContext get() = SQLContexts.create()
    private val unmodifiedConfig get() = sqlContext.unmodifiedConfig

    @UDFunction(description = "Chat with the AI model")
    @JvmStatic
    fun chat(prompt: String): String {
        return sqlContext.chat(prompt).content
    }

    @UDFunction(description = "Chat with the AI model")
    @JvmStatic
    fun chat(userMessage: String, systemMessage: String, dom: ValueDom): String {
        return sqlContext.chat(userMessage, systemMessage).content
    }

    @UDFunction(description = "Chat with the AI model")
    @JvmStatic
    fun chat(dom: ValueDom): String {
        return sqlContext.chat(dom.element.text()).content
    }

    @UDFunction(description = "Chat with the AI model")
    @JvmStatic
    fun chat(dom: ValueDom, systemMessage: String): String {
        return sqlContext.chat(dom.element.text(), systemMessage).content
    }
}
