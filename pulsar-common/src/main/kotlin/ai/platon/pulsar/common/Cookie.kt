package ai.platon.pulsar.common

data class Cookie(
    var name: String? = null,
    var value: String? = null,
    var domain: String? = null,
    var path: String? = null,
    var expires: Double? = null,
    var size: Int? = null,
    var httpOnly: Boolean? = null,
    var secure: Boolean? = null,
    var session: Boolean? = null,
    var sameParty: Boolean? = null,
    var sourcePort: Int? = null,
    var sameSite: String? = null,
    var priority: String? = null,
    var sourceScheme: String? = null,
)
