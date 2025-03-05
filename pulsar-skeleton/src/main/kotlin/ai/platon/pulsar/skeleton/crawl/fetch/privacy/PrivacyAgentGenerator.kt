package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

interface PrivacyAgentGenerator {
    var conf: ImmutableConfig
    @Throws(Exception::class)
    operator fun invoke(fingerprint: Fingerprint): PrivacyAgent
}

open class DefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent = PrivacyAgent.DEFAULT
}

open class SystemDefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.SYSTEM_DEFAULT
}

open class PrototypePrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.PROTOTYPE
}

open class SequentialPrivacyAgentGenerator(
    var group: String = "default"
) : PrivacyAgentGenerator {
    // should be late initialized
    override var conf: ImmutableConfig = ImmutableConfig()

    private fun computeMaxAgentCount(): Int {
        // The number of allowed active privacy contexts
        val privacyContextNumber = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)
        
        // The minimum number of sequential privacy agents, the active privacy contexts is chosen from them
        val minAgents = conf.getInt(MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER, 10)
        // The maximum number of sequential privacy agents, the active privacy contexts is chosen from them
        var maxAgents = conf.getInt(CapabilityTypes.MAX_SEQUENTIAL_PRIVACY_AGENT_NUMBER, minAgents)
        maxAgents = maxAgents.coerceAtLeast(privacyContextNumber).coerceAtLeast(minAgents)
        
        return maxAgents
    }

    @Throws(IOException::class)
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent {
        // The number of allowed active privacy contexts
        val maxAgents = computeMaxAgentCount()
        
        val contextDir = BrowserFiles.computeNextSequentialContextDir(group, fingerprint, maxAgents)
        // logger.info("Use sequential privacy agent | $contextDir")
        
        require(Files.exists(contextDir)) { "The context dir does not exist: $contextDir" }

        val agent = PrivacyAgent(contextDir)
        
        return agent
    }
}

/**
 * The random privacy agent generator.
 *
 * If the prototype Chrome browser does not exist, it acts as "New Incognito window", or in Chinese, "打开无痕浏览器".
 * If the prototype Chrome browser exists, it copies the prototype Chrome browser's user data directory, and inherits
 * the prototype Chrome browser's settings.
 * */
open class RandomPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig.DEFAULT

    @Throws(IOException::class)
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent =
        PrivacyAgent(BrowserFiles.computeRandomTmpContextDir(), fingerprint)
}

class PrivacyAgentGeneratorFactory(val conf: ImmutableConfig) {
    companion object {
        private val generators = ConcurrentHashMap<String, PrivacyAgentGenerator>()
    }
    
    private val logger = LoggerFactory.getLogger(PrivacyAgentGeneratorFactory::class.java)
    
    val generator: PrivacyAgentGenerator get() {
        val className = conf[PRIVACY_AGENT_GENERATOR_CLASS] ?: DefaultPrivacyAgentGenerator::class.java.name
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
            logger.warn("No configured privacy agent generator {}, use default ({})",
                className, defaultClazz.simpleName)
            defaultClazz
        }
        
        val gen = clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyAgentGenerator
        gen.conf = conf
        return gen
    }
}
