package ai.platon.pulsar.common

import ai.platon.pulsar.common.PulsarEnv.applicationContext
import ai.platon.pulsar.common.PulsarEnv.unmodifiedConfig
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.dom.data.BrowserControl
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.gora.GoraStorage
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
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
