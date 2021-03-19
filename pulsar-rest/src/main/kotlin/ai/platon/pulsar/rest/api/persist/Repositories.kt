package ai.platon.pulsar.rest.api.persist

import ai.platon.pulsar.rest.api.entities.CrawlSeed
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

//interface BrowserInstanceRepository: CrudRepository<BrowserInstance, Long> {
//    override fun findById(id: Long): Optional<BrowserInstance>
//}
//
//interface ServerInstanceRepository: CrudRepository<ServerInstance, Long> {
//    override fun findById(id: Long): Optional<ServerInstance>
//}

interface CrawlSeedRepository: MongoRepository<CrawlSeed, String> {
    override fun findById(id: String): Optional<CrawlSeed>
    fun findByUrlAndStatus(url: String, status: Int): Optional<CrawlSeed>
    fun findAllByStatus(status: Int): List<CrawlSeed>
    fun deleteAllByStatus(status: Int)
    fun countByPartitionAndStatus(partition: Int, status: Int): Long
    fun countByPartitionAndStatus(partition: Int, status: Int, pageable: Pageable): Page<CrawlSeed>
    fun findAllByPartitionAndGroupAndPriorityAndStatus(
        partition: Int, group: Int, priority: Int, status: Int): List<CrawlSeed>
    fun findAllByPartitionAndGroupAndPriorityAndStatus(
        partition: Int, group: Int, priority: Int, status: Int, pageable: Pageable
    ): List<CrawlSeed>
    fun findAllByPartitionAndGroupAndPriorityAndStatusIn(
        partition: Int, group: Int, priority: Int, status: List<Int>, pageable: Pageable
    ): List<CrawlSeed>
    fun deleteAllByCreatedAtBefore(createdAt: Date)
}
