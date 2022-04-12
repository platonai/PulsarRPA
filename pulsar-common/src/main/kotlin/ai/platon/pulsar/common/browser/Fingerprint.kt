package ai.platon.pulsar.common.browser

/**
 * The browser fingerprint
 * */
data class Fingerprint(
    val browserType: BrowserType,
    var proxyServer: String? = null,
    var username: String? = null,
    var password: String? = null,
    var userAgent: String? = null,
): Comparable<Fingerprint> {
    override fun compareTo(other: Fingerprint) = toString().compareTo(other.toString())
    override fun toString(): String = listOfNotNull(
        browserType, proxyServer, username, password, userAgent
    ).joinToString()
}
