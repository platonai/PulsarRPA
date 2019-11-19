package ai.platon.pulsar.jobs.fetch.service.jersey1

data class ServerInstance(
    val ip: String,
    val port: Int = 0,
    val type: String
) {
    var id: Long = -1

    enum class Type {
        FetchService, PulsarMaster
    }
}
