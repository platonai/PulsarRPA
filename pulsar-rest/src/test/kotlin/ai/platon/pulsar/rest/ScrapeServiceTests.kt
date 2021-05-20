package ai.platon.pulsar.rest

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.apache.commons.lang3.RandomStringUtils
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ScrapeServiceTests {
    final val username = RandomStringUtils.randomAlphanumeric(8)

    @Autowired
    private lateinit var service: ScrapeService

    @Autowired
    private lateinit var session: PulsarSession

    @Autowired
    private lateinit var globalCache: GlobalCache

    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig

    private val crawlStarter by lazy { StreamingCrawlLoop(globalCache, unmodifiedConfig) }

    @Before
    fun setup() {
        crawlStarter.start()
    }

    @After
    fun tearDown() {
        crawlStarter.stop()
    }

    /**
     * Execute a normal sql
     * */
    @Test
    fun `Sum of 1+1 is 2`() {
        val sql = "select 1+1 as sum"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        assertEquals(2, records[0]["sum"].toString().toInt())
    }

    @Test
    fun `When scraping with load_and_select then the result returns synchronously`() {
        val url = "https://www.amazon.com/"
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url -i 0s', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["uri"].toString()
        assertTrue { actualUrl == url }
    }

    @Ignore("Disabled temporary, amazon_suggestions have to use a real browser with js support, not a mock browser")
    @Test
    fun `When call amazon_suggestions then the suggestions are retrieved`() {
        val url = "https://www.amazon.com/"
        val sql = "select amazon_suggestions('$url -i 0s', 'cups') as suggestions"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        println(pulsarObjectMapper().writeValueAsString(response))

        assertEquals(200, response.pageStatusCode, response.pageStatus)
        assertEquals(200, response.statusCode, response.status)
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
    }

    @Test
    fun `When scrape jd then the base uri returns asynchronously`() {
        val url = "https://www.jd.com/ -i 0s"
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url', ':root')"
        val request = ScrapeRequest(sql)

        val uuid = service.submitJob(request)

        assertTrue { uuid.isNotEmpty() }
        println(uuid)

        val scrapeStatusRequest = ScrapeStatusRequest(uuid)
        var status = service.getStatus(scrapeStatusRequest)
        var i = 60
        while (i-- > 0 && status.statusCode != ResourceStatus.SC_OK) {
            sleepSeconds(1)
            status = service.getStatus(scrapeStatusRequest)
        }
        println(pulsarObjectMapper().writeValueAsString(status))
        assertTrue { i > 0 }
        assertEquals(200, status.statusCode)
    }
}
