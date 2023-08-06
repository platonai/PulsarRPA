package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.readableClassName
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class PrivacyAgentId(
    val contextDir: Path,
    val browserType: BrowserType
): Comparable<PrivacyAgentId> {

    val ident = contextDir.last().toString()

    val display = ident.substringAfter(PrivacyContext.CONTEXT_DIR_PREFIX)
    /**
     * If true, the privacy agent opens browser just like a real user does every day.
     * */
    val isUserDefault get() = this.contextDir == PrivacyContext.USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER
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

    val isPermanent get() = isUserDefault || isPrototype

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
    val isUserDefault get() = id.isUserDefault
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
         * The user default privacy agent opens browser just like real users do every day.
         * */
        val USER_DEFAULT = PrivacyAgent(PrivacyContext.USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER, BrowserType.PULSAR_CHROME)
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
data class BrowserId constructor(
    val contextDir: Path,
    val fingerprint: Fingerprint,
): Comparable<BrowserId> {

    val privacyAgent = PrivacyAgent(contextDir, fingerprint)
    val browserType: BrowserType get() = fingerprint.browserType

    val userDataDir: Path get() = when {
        privacyAgent.isUserDefault -> PrivacyContext.USER_DEFAULT_DATA_DIR_PLACEHOLDER
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
        val USER_DEFAULT = BrowserId(PrivacyAgent.USER_DEFAULT)
        /**
         * Represent the default browser.
         * */
        val DEFAULT = BrowserId(PrivacyAgent.DEFAULT)
        /**
         * Represent the prototype browser.
         * */
        val PROTOTYPE = BrowserId(PrivacyAgent.PROTOTYPE)
        /**
         * Represent a browser with random context dir.
         * */
        val RANDOM get() = BrowserId(PrivacyAgent.RANDOM)
    }
}

interface PrivacyAgentGenerator {
    operator fun invoke(fingerprint: Fingerprint): PrivacyAgent
}

class DefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    companion object {
        private val sequencer = AtomicInteger()
        private val nextContextDir
            get() = PrivacyContext.DEFAULT_CONTEXT_DIR.resolve(sequencer.incrementAndGet().toString())
    }

    override fun invoke(fingerprint: Fingerprint): PrivacyAgent = PrivacyAgent(nextContextDir, fingerprint)
}

class UserDefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.USER_DEFAULT
}

class PrototypePrivacyAgentGenerator: PrivacyAgentGenerator {
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.PROTOTYPE
}

class SequentialPrivacyAgentGenerator: PrivacyAgentGenerator {
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent =
        PrivacyAgent(PrivacyContext.computeNextSequentialContextDir(), fingerprint)
}

class PrivacyAgentGeneratorFactory(val conf: ImmutableConfig) {
    private val logger = LoggerFactory.getLogger(PrivacyAgentGeneratorFactory::class.java)

    val generators = ConcurrentHashMap<String, PrivacyAgentGenerator>()

    val generator: PrivacyAgentGenerator get() = create("")

    @Synchronized
    fun create(className: String): PrivacyAgentGenerator {
        var gen = generators[className]
        if (gen != null) {
            return gen
        }

        gen = when(className) {
            PrototypePrivacyAgentGenerator::class.java.name -> PrototypePrivacyAgentGenerator()
            UserDefaultPrivacyAgentGenerator::class.java.name -> UserDefaultPrivacyAgentGenerator()
            else -> createUsingGlobalConfig(conf)
        }

        generators[gen::class.java.name] = gen

        return gen
    }

    private fun createUsingGlobalConfig(conf: ImmutableConfig): PrivacyAgentGenerator {
        val defaultClazz = DefaultPrivacyAgentGenerator::class.java
        val clazz = try {
            conf.getClass(CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS, defaultClazz)
        } catch (e: Exception) {
            logger.warn("Configured privacy context id generator {}({}) is not found, use default ({})",
                CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS, conf[CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS],
                defaultClazz.simpleName)
            defaultClazz
        }

        logger.info("Using id generator {}", readableClassName(clazz))

        return clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyAgentGenerator
    }
}
