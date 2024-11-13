package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import java.io.File
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
    val fingerprint: Fingerprint
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
        /**
         * The system default privacy agent opens browser just like real users do every day.
         * */
        val SYSTEM_DEFAULT =
            PrivacyAgent(AppPaths.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER, BrowserType.PULSAR_CHROME)
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
        
        fun create(contextDir: Path): PrivacyAgent {
            val fingerprintFile = contextDir.resolve("fingerprint.json")
            val fingerprint: Fingerprint = if (Files.exists(fingerprintFile)) {
                pulsarObjectMapper().readValue(fingerprintFile.toFile())
            } else {
                Fingerprint(BrowserType.PULSAR_CHROME)
            }
            return PrivacyAgent(contextDir, fingerprint)
        }
        
        fun createNextSequential() = create(PrivacyContext.NEXT_SEQUENTIAL_CONTEXT_DIR)
    }
}
