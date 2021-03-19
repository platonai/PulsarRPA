package ai.platon.pulsar.rest.api.entities

import ai.platon.pulsar.common.ResourceStatus
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*
import javax.persistence.*

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

/**
 * Mongodb document
 * */
@Document
@CompoundIndex(
    name = "partition_group_priority_status",
    def = "{'partition': 1, 'group': 1, 'priority': 1, 'status': 1}"
)
data class CrawlSeed(
    var url: String = "",
    var args: String? = null,
    var referer: String? = null,
    var anchorText: String? = null,
    var label: String? = null,
    var order: Int = 0,
    var group: Int = 0,
    var priority: Int = 0,
    @Indexed
    var partition: Int = 0,
    @Indexed
    var status: Int = ResourceStatus.SC_CREATED
) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: String? = null

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Indexed
    var createdAt: Date = Date()

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    var modifiedAt: Date = Date()
}
