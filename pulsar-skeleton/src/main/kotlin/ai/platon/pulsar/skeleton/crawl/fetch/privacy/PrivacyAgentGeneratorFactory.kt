package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.browser.BrowserContextMode
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class PrivacyAgentGeneratorFactory(val conf: ImmutableConfig) {
    companion object {
        private val generators = ConcurrentHashMap<String, PrivacyAgentGenerator>()

        val BROWSER_CONTEXT_MODE_TO_AGENTS = mapOf(
            BrowserContextMode.PROTOTYPE  to PrototypePrivacyAgentGenerator::class,
            BrowserContextMode.SEQUENTIAL to SequentialPrivacyAgentGenerator::class,
            BrowserContextMode.TEMPORARY  to RandomPrivacyAgentGenerator::class,
            BrowserContextMode.SYSTEM_DEFAULT to SystemDefaultPrivacyAgentGenerator::class,
            BrowserContextMode.DEFAULT to DefaultPrivacyAgentGenerator::class
        )

        fun getPrivacyAgentGeneratorClass(mode: BrowserContextMode): KClass<out PrivacyAgentGenerator> {
            return when (mode) {
                BrowserContextMode.PROTOTYPE -> PrototypePrivacyAgentGenerator::class
                BrowserContextMode.SEQUENTIAL -> SequentialPrivacyAgentGenerator::class
                BrowserContextMode.TEMPORARY -> RandomPrivacyAgentGenerator::class
                BrowserContextMode.SYSTEM_DEFAULT -> SystemDefaultPrivacyAgentGenerator::class
                else -> DefaultPrivacyAgentGenerator::class
            }
        }
    }

    private val logger = LoggerFactory.getLogger(PrivacyAgentGeneratorFactory::class.java)

    val generator: PrivacyAgentGenerator
        get() {
            BrowserSettings.overrideBrowserContextMode(conf)

            // When the generator class is set, use it
            val className = conf[CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS] ?: DefaultPrivacyAgentGenerator::class.java.name
            return getOrCreate(className)
        }

    private fun getOrCreate(className: String): PrivacyAgentGenerator {
        synchronized(generators) {
            return getOrCreate0(className)
        }
    }

    private fun getOrCreate0(className: String): PrivacyAgentGenerator {
        var gen = generators[className]
        if (gen != null) {
            return gen
        }

        gen = forName(conf, className)

        generators[gen::class.java.name] = gen
        generators[className] = gen

        logger.info("Created privacy agent generator | {}", gen::class.java.name)

        return gen
    }

    /**
     * Get the value of the `name` property as a `Class`.
     * If the property is not set, or the class is not found, use the default class.
     * The default class is `DefaultPageEvent`.
     *
     * Set the class:
     * `System.setProperty(CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS, "ai.platon.pulsar.skeleton.crawl.fetch.privacy.DefaultPrivacyAgentGenerator")`
     * */
    private fun forName(conf: ImmutableConfig, className: String): PrivacyAgentGenerator {
        val defaultClazz = DefaultPrivacyAgentGenerator::class.java
        val clazz = try {
            SParser(className).getClass(defaultClazz)
        } catch (e: Exception) {
            logger.warn(
                "No configured privacy agent generator {}, use default ({})",
                className, defaultClazz.simpleName
            )
            defaultClazz
        }

        val gen = clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyAgentGenerator
        gen.conf = conf
        return gen
    }
}