package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

data class PrivacyAgentId(
    val contextDir: Path,
    val browserType: BrowserType
): Comparable<PrivacyAgentId> {

    val ident = contextDir.last().toString()

    val display = ident.substringAfter(PrivacyContext.CONTEXT_DIR_PREFIX)
    /**
     * If true, the privacy agent opens browser just like a real user does every day.
     * */
    val isSystemDefault get() = this.contextDir == AppPaths.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER
    /**
     * If true, the privacy agent opens browser with the default data dir, the default data dir will not be removed
     * after the browser closes.
     * */
    val isDefault get() = this.contextDir == PrivacyContext.DEFAULT_CONTEXT_DIR
    /**
     * If true, the privacy agent opens browser with the prototype data dir.
     * Every change to the browser will be kept in the prototype data dir, and every temporary privacy agent
     * uses a copy of the prototype data dir.
     * */
    val isPrototype get() = this.contextDir == PrivacyContext.PROTOTYPE_CONTEXT_DIR
    /**
     * If true, the privacy agent opens browser with a temporary data dir, the temporary data dir is created before the
     * browser starts and will be deleted after the browser closes.
     * */
    val isTemporary get() = this.contextDir.startsWith(AppPaths.CONTEXT_TMP_DIR)

    val isPermanent get() = isSystemDefault || isPrototype

    /**
     * The PrivacyAgent equality.
     * Note: do not use the default equality function
     * */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is PrivacyAgentId
                && other.contextDir == contextDir
                && other.browserType.name == browserType.name
    }

    override fun hashCode(): Int {
        return 31 * contextDir.hashCode() + browserType.name.hashCode()
    }

    override fun compareTo(other: PrivacyAgentId): Int {
        val b = contextDir.compareTo(other.contextDir)
        if (b != 0) {
            return b
        }

        return browserType.name.compareTo(other.browserType.name)
    }
}

/**
 * A privacy agent defines a unique agent to visit websites.
 *
 * Page visits through different privacy agents should not be detected
 * as the same person, even if the visits are from the same host.
 * */
data class PrivacyAgent(
    val contextDir: Path,
    var fingerprint: Fingerprint
): Comparable<PrivacyAgent> {

    val id = PrivacyAgentId(contextDir, fingerprint.browserType)
    val ident get() = id.ident
    val display get() = id.display
    val browserType get() = fingerprint.browserType
    val isSystemDefault get() = id.isSystemDefault
    val isDefault get() = id.isDefault
    val isPrototype get() = id.isPrototype
    val isTemporary get() = id.isTemporary
    val isPermanent get() = id.isPermanent

    constructor(contextDir: Path, browserType: BrowserType): this(contextDir, Fingerprint(browserType))

    /**
     * The PrivacyAgent equality.
     * Note: do not use the default equality function
     * */
    override fun equals(other: Any?) = other is PrivacyAgent && other.id == this.id

    override fun hashCode() = id.hashCode()

    override fun compareTo(other: PrivacyAgent) = id.compareTo(other.id)

//    override fun toString() = /** AUTO GENERATED **/

    companion object {
        /**
         * The system default privacy agent opens browser just like real users do every day.
         * */
        val SYSTEM_DEFAULT = PrivacyAgent(AppPaths.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER, BrowserType.PULSAR_CHROME)
        /**
         * The prototype privacy agent opens browser with the prototype data dir.
         * Every change to the browser will be kept in the prototype data dir, and every temporary privacy agent
         * uses a copy of the prototype data dir.
         * */
        val PROTOTYPE = PrivacyAgent(PrivacyContext.PROTOTYPE_CONTEXT_DIR, BrowserType.PULSAR_CHROME)
        /**
         * The default privacy agent opens browser with the default data dir, the default data dir will not be removed
         * after the browser closes.
         * */
        val DEFAULT = PrivacyAgent(PrivacyContext.DEFAULT_CONTEXT_DIR, BrowserType.PULSAR_CHROME)
        /**
         * The privacy agent opens browser with a sequential data dir.
         * */
        val NEXT_SEQUENTIAL get() = PrivacyAgent(PrivacyContext.NEXT_SEQUENTIAL_CONTEXT_DIR, BrowserType.PULSAR_CHROME)
        /**
         * The random privacy agent opens browser with a random data dir.
         * */
        val RANDOM get() = PrivacyAgent(PrivacyContext.RANDOM_CONTEXT_DIR, BrowserType.PULSAR_CHROME)
    }
}

/**
 * The unique browser id.
 *
 * Every browser instance have a unique fingerprint and a context directory.
 * */
data class BrowserId(
    val contextDir: Path,
    val fingerprint: Fingerprint,
): Comparable<BrowserId> {

    val privacyAgent = PrivacyAgent(contextDir, fingerprint)
    val browserType: BrowserType get() = fingerprint.browserType

    val userDataDir: Path get() = when {
        privacyAgent.isSystemDefault -> AppPaths.SYSTEM_DEFAULT_BROWSER_DATA_DIR_PLACEHOLDER
        privacyAgent.isPrototype -> PrivacyContext.PROTOTYPE_DATA_DIR
        else -> contextDir.resolve(browserType.name.lowercase())
    }

    /**
     * A human-readable short display of the context.
     * For example,
     * 1. prototype
     * 2. 07171ChsOE207
     * */
    val display get() = contextDir.last().toString().substringAfter(PrivacyContext.CONTEXT_DIR_PREFIX)

    constructor(privacyAgent: PrivacyAgent): this(privacyAgent.contextDir, privacyAgent.fingerprint)

    constructor(contextDir: Path, browserType: BrowserType): this(contextDir, Fingerprint(browserType))
    
    fun setProxy(schema: String, hostPort: String, username: String?, password: String?) {
        fingerprint.setProxy(schema, hostPort, username, password)
    }
    
    fun setProxy(proxy: ProxyEntry) = fingerprint.setProxy(proxy)
    
    override fun equals(other: Any?): Boolean {
        return other is BrowserId && other.privacyAgent == privacyAgent
    }

    override fun hashCode() = privacyAgent.hashCode()

    override fun compareTo(other: BrowserId) = privacyAgent.compareTo(other.privacyAgent)

    override fun toString(): String {
        return "{$fingerprint, $contextDir}"
    }

    companion object {
        /**
         * Represent the real user's default browser.
         * */
        val SYSTEM_DEFAULT = BrowserId(PrivacyAgent.SYSTEM_DEFAULT)
        @Deprecated("Use SYSTEM_DEFAULT instead", ReplaceWith("SYSTEM_DEFAULT"))
        val USER_DEFAULT = SYSTEM_DEFAULT
        /**
         * Represent the default browser.
         * */
        val DEFAULT = BrowserId(PrivacyAgent.DEFAULT)
        /**
         * Represent the prototype browser.
         * */
        val PROTOTYPE = BrowserId(PrivacyAgent.PROTOTYPE)
        /**
         * Represent a browser with a sequential context dir.
         * */
        val NEXT_SEQUENTIAL get() = BrowserId(PrivacyAgent.NEXT_SEQUENTIAL)
        /**
         * Create a browser with random context dir.
         * */
        val RANDOM get() = BrowserId(PrivacyAgent.RANDOM)
    }
}

interface PrivacyAgentGenerator {
    var conf: ImmutableConfig
    operator fun invoke(fingerprint: Fingerprint): PrivacyAgent
}

open class DefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig.DEFAULT
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent = PrivacyAgent.DEFAULT
}

open class SystemDefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.SYSTEM_DEFAULT
}

@Deprecated("Use SystemDefaultPrivacyAgentGenerator instead", ReplaceWith("SystemDefaultPrivacyAgentGenerator"))
open class UserDefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig.DEFAULT
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.SYSTEM_DEFAULT
}

open class PrototypePrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig.DEFAULT
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.PROTOTYPE
}

open class SequentialPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig.DEFAULT
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent {
        // The number of allowed active privacy contexts
        val privacyContextNumber = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)
        // The minimum number of sequential privacy agents, the active privacy contexts is chosen from them
        val minAgents = conf.getInt(MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER, 10)
        // The maximum number of sequential privacy agents, the active privacy contexts is chosen from them
        var maxAgents = conf.getInt(CapabilityTypes.MAX_SEQUENTIAL_PRIVACY_AGENT_NUMBER, minAgents)
        maxAgents = maxAgents.coerceAtLeast(privacyContextNumber).coerceAtLeast(minAgents)

        return PrivacyAgent(BrowserFiles.computeNextSequentialContextDir("default", fingerprint, maxAgents), fingerprint)
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
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent =
        PrivacyAgent(BrowserFiles.computeRandomContextDir(), fingerprint)
}

class PrivacyAgentGeneratorFactory(val conf: ImmutableConfig) {
    companion object {
        private val generators = ConcurrentHashMap<String, PrivacyAgentGenerator>()
    }
    
    private val logger = LoggerFactory.getLogger(PrivacyAgentGeneratorFactory::class.java)
    
    // TODO: there is always one generator.
    val generator: PrivacyAgentGenerator get() = getOrCreate(PRIVACY_AGENT_GENERATOR_CLASS)
    
    private fun getOrCreate(classKey: String): PrivacyAgentGenerator {
        synchronized(generators) {
            return getOrCreate0(PRIVACY_AGENT_GENERATOR_CLASS)
        }
    }
    
    private fun getOrCreate0(classKey: String): PrivacyAgentGenerator {
        var gen = generators[classKey]
        if (gen != null) {
            return gen
        }
        
        gen = createUsingGlobalConfig(conf, PRIVACY_AGENT_GENERATOR_CLASS)
        
        generators[gen::class.java.name] = gen
        generators[classKey] = gen

        logger.info("Created privacy agent generator | {}", gen::class.java.name)
        
        return gen
    }
    
    /**
     * Get the value of the `name` property as a `Class`.
     * If the property is not set, or the class is not found, use the default class.
     * The default class is `DefaultPageEvent`.
     *
     * Set the class:
     * `System.setProperty(CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.DefaultPrivacyAgentGenerator")`
     * */
    private fun createUsingGlobalConfig(conf: ImmutableConfig, classKey: String): PrivacyAgentGenerator {
        val defaultClazz = DefaultPrivacyAgentGenerator::class.java
        val clazz = try {
            conf.getClass(classKey, defaultClazz)
        } catch (e: Exception) {
            logger.warn("No configured privacy agent generator {}({}), use default ({})",
                classKey, conf[classKey], defaultClazz.simpleName)
            defaultClazz
        }

        val gen = clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyAgentGenerator
        gen.conf = conf
        return gen
    }
}
