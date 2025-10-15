package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserContextMode
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_CONTEXT_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.logging.ThrottlingLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

/**
 * A privacy agent defines a unique agent to visit websites.
 *
 * Page visits through different privacy agents should not be detected
 * as the same person, even if the visits are from the same host.
 * */
data class BrowserProfile(
    val contextDir: Path,
    var fingerprint: Fingerprint
) : Comparable<BrowserProfile> {

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

    constructor(contextDir: Path) : this(contextDir, BrowserType.PULSAR_CHROME)

    constructor(contextDir: Path, browserType: BrowserType) : this(contextDir, Fingerprint(browserType))

    /**
     * The PrivacyAgent equality.
     * Note: do not use the default equality function
     * */
    override fun equals(other: Any?) = other is BrowserProfile && other.id == this.id

    override fun hashCode() = id.hashCode()

    override fun compareTo(other: BrowserProfile) = id.compareTo(other.id)

//    override fun toString() = /** AUTO GENERATED **/

    companion object {
        private val logger = getLogger(this)
        private val throttlingLogger = ThrottlingLogger(logger)

        /**
         * The random privacy agent opens browser with a random data dir.
         * */
        val RANDOM_TEMP get() = createRandomTemp()

        private fun create(contextDir: Path) = create(BrowserType.PULSAR_CHROME, contextDir)

        private fun create(browserType: BrowserType, contextDir: Path): BrowserProfile {
            val path = contextDir.resolve("fingerprint.json")
            val fingerprint: Fingerprint = if (Files.exists(path)) {
                pulsarObjectMapper().readValue<Fingerprint>(path.toFile()).also { it.source = path.toString() }
            } else {
                Fingerprint(browserType)
            }
            fingerprint.browserType = browserType
            return BrowserProfile(contextDir, fingerprint)
        }

        fun createSystemDefault(): BrowserProfile {
            return createSystemDefault(BrowserType.DEFAULT)
        }

        fun createSystemDefault(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a SYSTEM_DEFAULT browser context, force set max browser number to be 1")
            BrowserSettings.withBrowserContextMode(BrowserContextMode.SYSTEM_DEFAULT, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("SystemDefaultPrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER)
        }

        fun createDefault(): BrowserProfile {
            return createDefault(BrowserType.DEFAULT)
        }

        fun createDefault(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a DEFAULT browser context, force set max browser number to be 1")

            BrowserSettings.withBrowserContextMode(BrowserContextMode.DEFAULT, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("DefaultPrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.DEFAULT_CONTEXT_DIR)
        }

        fun createPrototype(): BrowserProfile {
            return createPrototype(BrowserType.DEFAULT)
        }

        fun createPrototype(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a PROTOTYPE browser context, force set max browser number to be 1")

            BrowserSettings.withBrowserContextMode(BrowserContextMode.PROTOTYPE, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("PrototypePrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.PROTOTYPE_CONTEXT_DIR)
        }

        fun createNextSequential() = createNextSequential(BrowserType.PULSAR_CHROME)

        fun createNextSequential(browserType: BrowserType): BrowserProfile {
            BrowserSettings.withBrowserContextMode(BrowserContextMode.SEQUENTIAL, browserType)
            return create(browserType, PrivacyContext.NEXT_SEQUENTIAL_CONTEXT_DIR)
        }

        fun createRandomTemp() = createRandomTemp(BrowserType.PULSAR_CHROME)

        fun createRandomTemp(browserType: BrowserType): BrowserProfile {
            BrowserSettings.withBrowserContextMode(BrowserContextMode.TEMPORARY, browserType)
            return create(browserType, PrivacyContext.createRandom(browserType))
        }
    }
}
