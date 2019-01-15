package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.PulsarSession
import `fun`.platonic.pulsar.common.config.CapabilityTypes.*
import `fun`.platonic.pulsar.common.config.ImmutableConfig
import `fun`.platonic.pulsar.common.config.MutableConfig
import `fun`.platonic.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import `fun`.platonic.pulsar.common.config.VolatileConfig
import `fun`.platonic.pulsar.dom.data.BrowserControl
import `fun`.platonic.pulsar.net.SeleniumEngine
import org.springframework.context.support.ClassPathXmlApplicationContext
import kotlin.reflect.KClass

object PulsarContext {
    val applicationContext: ClassPathXmlApplicationContext
    val unmodifiedConfig: ImmutableConfig

    init {
        if (System.getProperty(PULSAR_CONFIG_PREFERRED_DIR) == null) {
            System.setProperty(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
        }
        if (System.getProperty(PULSAR_CONFIG_RESOURCES) == null) {
            System.setProperty(PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
        }
        if (System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION)
        }

        applicationContext = ClassPathXmlApplicationContext(System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION))
        applicationContext.registerShutdownHook()
        unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)

        if (SeleniumEngine.CLIENT_JS.isNullOrBlank()) {
            SeleniumEngine.CLIENT_JS = BrowserControl(unmodifiedConfig).getJs()
        }
    }

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
