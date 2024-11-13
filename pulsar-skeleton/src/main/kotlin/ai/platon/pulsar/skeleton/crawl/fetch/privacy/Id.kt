package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.proxy.ProxyEntry
import java.nio.file.Path

data class PrivacyAgentId(
    val contextDir: Path,
    val browserType: BrowserType
): Comparable<PrivacyAgentId> {

    val ident = contextDir.last().toString()

    val display = when {
        isSystemDefault -> "system.default"
        isDefault -> "default"
        isPrototype -> "prototype"
        ident.length <= 5 -> ident
        else -> ident.substringAfter(PrivacyContext.CONTEXT_DIR_PREFIX)
    }
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
     * If true, the privacy agent opens browser with one of a set of pre-created data dirs, the pre-created data dirs will
     * not be removed after the browser closes.
     * */
    val isGroup get() = this.contextDir.startsWith(AppPaths.CONTEXT_GROUP_BASE_DIR)
    /**
     * Check if this browser is permanent.
     *
     * If a browser is temporary:
     * - it will be closed when the browser is idle
     * - the user data will be deleted after the browser is closed
     * */
    val isTemporary get() = this.contextDir.startsWith(AppPaths.CONTEXT_TMP_DIR)
    /**
     * Check if this browser is permanent.
     *
     * If a browser is permanent:
     * - it will not be closed when the browser is idle
     * - the user data will be kept after the browser is closed
     * */
    val isPermanent get() = isSystemDefault || isDefault || isPrototype

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

        fun create(contextDir: Path) = BrowserId(PrivacyAgent.create(contextDir))

        fun createNextSequential() = BrowserId(PrivacyAgent.createNextSequential())
    }
}
