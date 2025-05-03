package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.skeleton.PulsarSettings
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

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
    val isGroup get() = id.isGroup
    val isTemporary get() = id.isTemporary
    val isPermanent get() = id.isPermanent
    
    constructor(contextDir: Path): this(contextDir, BrowserType.PULSAR_CHROME)

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
        private val logger = getLogger(this)

        /**
         * The system default privacy agent opens browser just like real users do every day.
         * */
        val SYSTEM_DEFAULT = createSystemDefault()

        /**
         * The default privacy agent opens browser with the default data dir, the default data dir will not be removed
         * after the browser closes.
         * */
        val DEFAULT = create(PrivacyContext.DEFAULT_CONTEXT_DIR)

        /**
         * The prototype privacy agent opens browser with the prototype data dir.
         * Every change to the browser will be kept in the prototype data dir, and every temporary privacy agent
         * uses a copy of the prototype data dir.
         * */
        val PROTOTYPE = create(PrivacyContext.PROTOTYPE_CONTEXT_DIR)

        /**
         * The privacy agent opens browser with a sequential data dir.
         * */
        val NEXT_SEQUENTIAL get() = createNextSequential()

        /**
         * The random privacy agent opens browser with a random data dir.
         * */
        val RANDOM get() = create(PrivacyContext.RANDOM_CONTEXT_DIR)

        fun create(contextDir: Path) = create(BrowserType.PULSAR_CHROME, contextDir)

        fun create(browserType: BrowserType, contextDir: Path): PrivacyAgent {
            val path = contextDir.resolve("fingerprint.json")
            val fingerprint: Fingerprint = if (Files.exists(path)) {
                pulsarObjectMapper().readValue<Fingerprint>(path.toFile()).also { it.source = path.toString() }
            } else {
                Fingerprint(browserType)
            }
            return PrivacyAgent(contextDir, fingerprint)
        }

        fun createSystemDefault(): PrivacyAgent {
            return createSystemDefault(BrowserType.DEFAULT)
        }

        fun createSystemDefault(browserType: BrowserType): PrivacyAgent {
            logger.info("You are creating a system default browser context, force set max browser number to be 1")
            PulsarSettings().maxBrowsers(1)
            return create(browserType, PrivacyContext.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER)
        }

        fun createDefault(): PrivacyAgent {
            return createDefault(BrowserType.DEFAULT)
        }

        fun createDefault(browserType: BrowserType): PrivacyAgent {
            logger.info("You are creating a default browser context, force set max browser number to be 1")
            PulsarSettings().maxBrowsers(1)
            return create(browserType, PrivacyContext.DEFAULT_CONTEXT_DIR)
        }

        fun createNextSequential() = create(PrivacyContext.NEXT_SEQUENTIAL_CONTEXT_DIR)

        fun createRandom() = create(PrivacyContext.RANDOM_CONTEXT_DIR)

        fun createRandom(browserType: BrowserType) = create(browserType, PrivacyContext.createRandom(browserType))
    }
}
