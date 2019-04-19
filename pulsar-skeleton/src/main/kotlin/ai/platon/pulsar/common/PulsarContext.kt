package ai.platon.pulsar.common

import ai.platon.pulsar.common.PulsarEnv.applicationContext
import ai.platon.pulsar.common.PulsarEnv.unmodifiedConfig
import ai.platon.pulsar.common.config.VolatileConfig
import kotlin.reflect.KClass

object PulsarContext {

    fun createSession(): PulsarSession {
        return PulsarSession(applicationContext, VolatileConfig(unmodifiedConfig))
    }

    fun getBean(name: String): Any? {
        return applicationContext.getBean(name)
    }

    fun getBean(clazz: Class<Any>): Any? {
        return applicationContext.getBean(clazz)
    }

    fun getBean(clazz: KClass<Any>): Any? {
        return applicationContext.getBean(clazz.java)
    }
}
