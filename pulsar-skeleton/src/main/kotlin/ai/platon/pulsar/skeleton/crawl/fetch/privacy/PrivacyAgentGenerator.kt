package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import java.io.IOException
import java.nio.file.Files

interface PrivacyAgentGenerator {
    var conf: ImmutableConfig
    @Throws(Exception::class)
    operator fun invoke(fingerprint: Fingerprint): PrivacyAgent
}

open class DefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent = PrivacyAgent.createDefault(fingerprint.browserType)
}

open class SystemDefaultPrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.createSystemDefault(fingerprint.browserType)
}

open class PrototypePrivacyAgentGenerator: PrivacyAgentGenerator {
    override var conf: ImmutableConfig = ImmutableConfig()
    @Throws(Exception::class)
    override fun invoke(fingerprint: Fingerprint) = PrivacyAgent.createDefault(fingerprint.browserType)
}

open class SequentialPrivacyAgentGenerator(
    var group: String = "default"
) : PrivacyAgentGenerator {
    // should be late initialized
    override var conf: ImmutableConfig = ImmutableConfig()

    private fun computeMaxAgentCount(): Int {
        // The number of allowed active privacy contexts

        // PRIVACY_CONTEXT_NUMBER is deprecated, use BROWSER_CONTEXT_NUMBER instead
//        val fallbackValue = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
//        val browserContextNumber = conf.getInt(BROWSER_CONTEXT_NUMBER, fallbackValue)
        val browserContextNumber = conf.getWithFallback(BROWSER_CONTEXT_NUMBER, PRIVACY_CONTEXT_NUMBER)?.toIntOrNull() ?: 2

        // The minimum number of sequential privacy agents, the active privacy contexts is chosen from them
        val minAgents = conf.getInt(MIN_SEQUENTIAL_PRIVACY_AGENT_NUMBER, 10)
        // The maximum number of sequential privacy agents, the active privacy contexts is chosen from them
        var maxAgents = conf.getInt(CapabilityTypes.MAX_SEQUENTIAL_PRIVACY_AGENT_NUMBER, minAgents)
        maxAgents = maxAgents.coerceAtLeast(browserContextNumber).coerceAtLeast(minAgents)

        return maxAgents
    }

    @Throws(IOException::class)
    override fun invoke(fingerprint: Fingerprint): PrivacyAgent {
        // The number of allowed active privacy contexts
        val maxAgents = computeMaxAgentCount()
        
        val contextDir = BrowserFiles.computeNextSequentialContextDir(group, fingerprint, maxAgents)
        // logger.info("Use sequential privacy agent | $contextDir")
        
        require(Files.exists(contextDir)) { "The context dir does not exist: $contextDir" }

        val agent = PrivacyAgent(contextDir, fingerprint)
        
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

