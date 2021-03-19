package ai.platon.pulsar.rest

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.rest.api.entities.CrawlSeed
import ai.platon.pulsar.rest.api.persist.CrawlSeedRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
class TestCrawlSeedRepository {
    protected val queueSize = 20

    @Autowired
    lateinit var crawlSeedRepository: CrawlSeedRepository

    @Test
    fun testCount() {
        val seeds = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> CrawlSeed(url, partition = 1, order = 1 + i) }
        seeds.forEach {
            assertNull(it.id)
        }
        crawlSeedRepository.saveAll(seeds)

        val count = crawlSeedRepository.countByPartitionAndStatus(1, ResourceStatus.SC_CREATED).toInt()
        assertEquals(queueSize, count)
    }

    @Test
    fun `when save all then the results are the same objects`() {
        val seeds = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> CrawlSeed(url, partition = 1, order = 1 + i) }
        seeds.forEach {
            assertNull(it.id)
        }
        val savedSeeds = crawlSeedRepository.saveAll(seeds)
        savedSeeds.forEach {
            assertTrue("A saved seeds should be in seeds collection") { it in seeds }
            assertNotNull(it.id)
        }
    }

    @Test
    fun `when save records with id then no new record is created`() {
        crawlSeedRepository.deleteAll()

        val seeds = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> CrawlSeed(url, partition = 1, order = 1 + i) }
        seeds.forEach {
            assertNull(it.id)
        }

        repeat(10) {
            var savedSeeds = crawlSeedRepository.saveAll(seeds)
            savedSeeds.forEach {
                assertTrue("A saved seeds should be in seeds collection") { it in seeds }
                assertNotNull(it.id)
            }

            savedSeeds = crawlSeedRepository.saveAll(savedSeeds)
            savedSeeds.forEach {
                assertTrue("A saved seeds should be in seeds collection") { it in seeds }
                assertNotNull(it.id)
            }
        }

        val count = crawlSeedRepository.count()
        assertEquals(seeds.size, count.toInt())
    }

    @Test
    fun `when findAll with page number then return the correct items`() {
        val seeds = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> CrawlSeed(url, partition = 1, order = 1 + i) }
        crawlSeedRepository.saveAll(seeds)

        val loadedSeeds = crawlSeedRepository.findAllByPartitionAndGroupAndPriorityAndStatus(
                1, 0, 0, ResourceStatus.SC_CREATED)
        assertEquals(queueSize, loadedSeeds.size)

        var pageSize = 20
        var page = PageRequest.of(0, pageSize)
        var pagedSeeds = crawlSeedRepository.findAllByPartitionAndGroupAndPriorityAndStatus(
                1, 0, 0, ResourceStatus.SC_CREATED, page)
        assertEquals(20, pagedSeeds.size)

        pageSize = 3
        IntRange(0, 5).forEach { i ->
            page = PageRequest.of(i, pageSize)
            pagedSeeds = crawlSeedRepository.findAllByPartitionAndGroupAndPriorityAndStatus(
                    1, 0, 0, ResourceStatus.SC_CREATED, page)
            assertEquals(3, pagedSeeds.size)
        }
    }
}
