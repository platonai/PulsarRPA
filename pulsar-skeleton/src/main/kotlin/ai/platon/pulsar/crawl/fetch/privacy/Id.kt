package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.readableClassName
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * The privacy context id
 * */
data class PrivacyContextId(
    val contextDir: Path,
    var fingerprint: Fingerprint
): Comparable<PrivacyContextId> {

    val ident = contextDir.last().toString()
    val display = ident.substringAfter(PrivacyContext.IDENT_PREFIX)
    val isDefault get() = this == DEFAULT
    val isPrototype get() = this == PROTOTYPE

    constructor(contextDir: Path, browserType: BrowserType): this(contextDir, Fingerprint(browserType))

    /**
     * The PrivacyContextId equality.
     * Note: do not use the default equality function
     * */
    override fun equals(other: Any?): Boolean {
        return other is PrivacyContextId
                && other.contextDir == contextDir
                && other.fingerprint.browserType.toString() == fingerprint.browserType.toString()
    }

    override fun hashCode(): Int {
        return 31 * contextDir.hashCode() + fingerprint.browserType.toString().hashCode()
    }

    override fun compareTo(other: PrivacyContextId): Int {
        val r = contextDir.compareTo(other.contextDir)
        if (r != 0) {
            return r
        }
        return fingerprint.browserType.toString().compareTo(other.fingerprint.browserType.toString())
    }

//    override fun toString() = /** AUTO GENERATED **/

    companion object {
        val DEFAULT = PrivacyContextId(PrivacyContext.DEFAULT_DIR, BrowserType.PULSAR_CHROME)
        val PROTOTYPE = PrivacyContextId(PrivacyContext.PROTOTYPE_DIR, BrowserType.PULSAR_CHROME)
    }
}

/**
 * The unique browser id.
 *
 * Every browser instance have a unique fingerprint and a context directory.
 * */
data class BrowserInstanceId constructor(
    val contextDir: Path,
    val fingerprint: Fingerprint,
): Comparable<BrowserInstanceId> {

    val browserType: BrowserType get() = fingerprint.browserType
    val proxyServer: String? get() = fingerprint.proxyServer

    val userDataDir get() = contextDir.resolve(browserType.name.lowercase())
    val ident get() = contextDir.last().toString() + browserType.ordinal
    val display get() = ident.substringAfter(PrivacyContext.IDENT_PREFIX)

    constructor(contextDir: Path, browserType: BrowserType): this(contextDir, Fingerprint(browserType))

    override fun equals(other: Any?): Boolean {
        return other is BrowserInstanceId
                && other.contextDir == contextDir
                && other.fingerprint.toString() == fingerprint.toString()
    }

    override fun hashCode(): Int {
        return 31 * contextDir.hashCode() + fingerprint.toString().hashCode()
    }

    override fun compareTo(other: BrowserInstanceId): Int {
        val r = contextDir.compareTo(other.contextDir)
        if (r != 0) {
            return r
        }
        return fingerprint.toString().compareTo(other.fingerprint.toString())
    }

    override fun toString(): String {
        return "{$fingerprint | $contextDir}"
    }

    companion object {
        val DEFAULT = BrowserInstanceId(AppPaths.BROWSER_TMP_DIR, Fingerprint(BrowserType.PULSAR_CHROME))
    }
}

typealias BrowserId = BrowserInstanceId

interface PrivacyContextIdGenerator {
    operator fun invoke(fingerprint: Fingerprint): PrivacyContextId
}

class DefaultPrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(fingerprint: Fingerprint): PrivacyContextId = PrivacyContextId(PrivacyContext.DEFAULT_DIR, fingerprint)
}

class PrototypePrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(fingerprint: Fingerprint): PrivacyContextId = PrivacyContextId(PrivacyContext.PROTOTYPE_DIR.parent, fingerprint)
}

class SequentialPrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(fingerprint: Fingerprint): PrivacyContextId = PrivacyContextId(nextContextDir(), fingerprint)

    @Synchronized
    private fun nextContextDir(): Path {
        sequencer.incrementAndGet()
        val impreciseNumInstances = 1 + Files.list(ROOT_DIR).filter { Files.isDirectory(it) }.count()
        val rand = RandomStringUtils.randomAlphanumeric(5)
        return ROOT_DIR.resolve("${PrivacyContext.IDENT_PREFIX}${sequencer}$rand$impreciseNumInstances")
    }

    companion object {
        /** The root directory of privacy contexts, every context have it's own directory in this fold */
        private val ROOT_DIR = AppPaths.CONTEXT_TMP_DIR
        private val sequencer = AtomicInteger()
    }
}

class PrivacyContextIdGeneratorFactory(val conf: ImmutableConfig) {
    private val logger = LoggerFactory.getLogger(PrivacyContextIdGeneratorFactory::class.java)
    val generator by lazy { createIfAbsent(conf) }

    private fun createIfAbsent(conf: ImmutableConfig): PrivacyContextIdGenerator {
        val defaultClazz = DefaultPrivacyContextIdGenerator::class.java
        val clazz = try {
            conf.getClass(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, defaultClazz)
        } catch (e: Exception) {
            logger.warn("Configured proxy loader {}({}) is not found, use default ({})",
                    CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, conf.get(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS), defaultClazz.simpleName)
            defaultClazz
        }

        logger.info("Using id generator {}", readableClassName(clazz))

        return clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyContextIdGenerator
    }
}
