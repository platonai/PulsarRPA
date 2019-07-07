package ai.platon.pulsar

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CLIENT_JS_COMPUTED_STYLES
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CLIENT_JS_PROPERTY_NAMES
import ai.platon.pulsar.common.config.PulsarConstants.CLIENT_JS_PROPERTY_NAMES
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.features.defined.N
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.net.browser.WebDriverQueues
import ai.platon.pulsar.persist.AutoDetectedStorageService
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.GoraStorage
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds all the runtime environment objects for a running Pulsar instance,
 * including the Configuration, WebDb, InjectComponent, FetchComponent, LoadComponent, ParseComponent, etc.
 * Currently Pulsar code finds the PulsarEnv through a global variable, so all the threads shares the same PulsarEnv.
 */
object PulsarEnv {
    val clientJsVersion = "0.2.3"

    val applicationContext: ClassPathXmlApplicationContext
    val storageService: AutoDetectedStorageService
    val startTime = Instant.now()
    /**
     * The number of processors available to the Java virtual machine
     */
    val NCPU = Runtime.getRuntime().availableProcessors()
    // val log4jProperties: Properties
    val goraProperties: Properties
    // val shutdownHooks:

    /**
     * A immutable config loaded from the config file at process startup, and never changes
     * */
    val unmodifiedConfig: ImmutableConfig
//    /**
//     * A mutable config can be changed programmatically, usually be changed at the initialization phrase
//     * */
//    val defaultMutableConfig: MutableConfig
    /**
     * Url normalizers
     * */
    val urlNormalizers: UrlNormalizers
    /**
     * The web db
     * */
    val webDb: WebDb
    /**
     * The inject component
     * */
    val injectComponent: InjectComponent
    /**
     * The load component
     * */
    val loadComponent: LoadComponent
    /**
     * The parse component
     * */
    val parseComponent: ParseComponent
    /**
     * The fetch component
     * */
    val fetchComponent: BatchFetchComponent

    val globalExecutor: GlobalExecutor

    val browserControl: BrowserControl

    val webDrivers: WebDriverQueues

    val seleniumEngine: SeleniumEngine

    init {
        // prerequisite system properties
        if (System.getProperty(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR) == null) {
            System.setProperty(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf")
        }
        if (System.getProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES) == null) {
            System.setProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml")
        }
        if (System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)
        }
        if (System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.APP_CONTEXT_CONFIG_LOCATION)
        }

        // the spring application context
        applicationContext = ClassPathXmlApplicationContext(System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION))
        // shut down application context before progress exit
        applicationContext.registerShutdownHook()
        // gora properties
        goraProperties = GoraStorage.properties
        // storage service must be initialized in advance to ensure prerequisites
        // TODO: use spring boot
        storageService = applicationContext.getBean(AutoDetectedStorageService::class.java)

        // the primary configuration, keep unchanged with the configuration files
        unmodifiedConfig = applicationContext.getBean(MutableConfig::class.java)
        webDb = applicationContext.getBean(WebDb::class.java)
        injectComponent = applicationContext.getBean(InjectComponent::class.java)
        loadComponent = applicationContext.getBean(LoadComponent::class.java)
        fetchComponent = applicationContext.getBean(BatchFetchComponent::class.java)
        parseComponent = applicationContext.getBean(ParseComponent::class.java)
        urlNormalizers = applicationContext.getBean(UrlNormalizers::class.java)

        globalExecutor = GlobalExecutor(unmodifiedConfig)

        // The javascript to execute by Web browsers
        val propertyNames = unmodifiedConfig.getTrimmedStrings(FETCH_CLIENT_JS_COMPUTED_STYLES, CLIENT_JS_PROPERTY_NAMES)
        val parameters = mapOf(
                "version" to clientJsVersion,
                "propertyNames" to propertyNames
        )
        browserControl = BrowserControl(parameters)
        webDrivers = WebDriverQueues(browserControl, unmodifiedConfig)
        seleniumEngine = SeleniumEngine(browserControl, globalExecutor, webDrivers, unmodifiedConfig)
    }

    fun ensureEnv() {
        val nil = FeaturedDocument.NIL
        require(nil.html.startsWith("<html>"))
        require(nil.features.dimension == N)
    }

    fun stop() {
        globalExecutor.close()
        webDb.close()
        seleniumEngine.close()
        webDrivers.close()
        injectComponent.close()
        fetchComponent.close()
        // parseComponent.close()
    }
}
