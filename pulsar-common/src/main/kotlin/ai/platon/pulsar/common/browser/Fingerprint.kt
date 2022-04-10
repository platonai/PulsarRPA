package ai.platon.pulsar.common.browser

data class Fingerprint(
    val browserType: String,
    var proxyServer: String? = null,
    var username: String? = null,
    var password: String? = null,
    var userAgent: String? = null,
): Comparable<Fingerprint> {
    override fun compareTo(other: Fingerprint) = toString().compareTo(other.toString())
}
