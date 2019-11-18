package ai.platon.pulsar.rest.api

import java.util.*

data class BrowserInstance(
        var id: Long,
        var ip: String,
        var username: String,
        var password: String,
        var userAgent: String,
        var sesssion: String,
        var created: Date,
        var modified: Date
)

data class ServerInstance(
    val id: Long,
    val ip: String,
    val port: Int = 0,
    val type: String
) {
    enum class Type {
        FetchService, PulsarMaster
    }
}
