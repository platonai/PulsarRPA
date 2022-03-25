package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

data class PrivacyContextId(val dataDir: Path): Comparable<PrivacyContextId> {
    val ident = dataDir.last().toString()
    val display = ident.substringAfter(PrivacyContext.IDENT_PREFIX)
    val isDefault get() = this == DEFAULT
    val isPrototype get() = this == PROTOTYPE

    // override fun hashCode() = /** AUTO GENERATED **/
    // override fun equals(other: Any?) = /** AUTO GENERATED **/

    override fun compareTo(other: PrivacyContextId) = dataDir.compareTo(other.dataDir)
    override fun toString() = "$dataDir"

    companion object {
        val DEFAULT = PrivacyContextId(PrivacyContext.DEFAULT_DIR)
        val PROTOTYPE = PrivacyContextId(PrivacyContext.PROTOTYPE_DIR)
    }
}

data class BrowserInstanceId(
        val dataDir: Path,
        var proxyServer: String? = null
): Comparable<BrowserInstanceId> {

    val contextDir = dataDir.parent
    val ident = contextDir.last().toString()
    val display = ident.substringAfter(PrivacyContext.IDENT_PREFIX)
    override fun hashCode() = dataDir.hashCode()
    override fun equals(other: Any?) = other is BrowserInstanceId && dataDir == other.dataDir
    override fun compareTo(other: BrowserInstanceId) = dataDir.compareTo(other.dataDir)
    override fun toString() = "$dataDir"

    companion object {
        val DIR_NAME = "browser"
        val DEFAULT = resolve(AppPaths.BROWSER_TMP_DIR)

        fun resolve(baseDir: Path) = BrowserInstanceId(baseDir.resolve(DIR_NAME))
    }
}

interface PrivacyContextIdGenerator {
    operator fun invoke(): PrivacyContextId
}

class DefaultPrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(): PrivacyContextId = PrivacyContextId(PrivacyContext.DEFAULT_DIR)
}

class PrototypePrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(): PrivacyContextId = PrivacyContextId(PrivacyContext.PROTOTYPE_DIR.parent)
}

class SequentialPrivacyContextIdGenerator: PrivacyContextIdGenerator {
    override fun invoke(): PrivacyContextId = PrivacyContextId(nextBaseDir())

    @Synchronized
    private fun nextBaseDir(): Path {
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

        logger.info("Using id generator {}", clazz)

        return clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PrivacyContextIdGenerator
    }
}
