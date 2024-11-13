package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.proxy.ProxyEntry
import java.nio.file.Path

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

    val userDataDir: Path
        get() = when {
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
        val NEXT_SEQUENTIAL get() = BrowserId(PrivacyAgent.createNextSequential())
        /**
         * Create a browser with random context dir.
         * */
        val RANDOM get() = BrowserId(PrivacyAgent.RANDOM)

        fun create(contextDir: Path) = BrowserId(PrivacyAgent.create(contextDir))
    }
}