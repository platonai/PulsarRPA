package ai.platon.pulsar.rest

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.crawl.CrawlLoop
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Ignore
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ImportResource
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
    private lateinit var crawlLoop: CrawlLoop

    /**
     * Execute a normal sql
     * */
    @Test
    fun `When perform 1+1 then the result is 2`() {
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
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url -i 10s', ':root')"
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
        var i = 120

        while (i-- > 0 && !status.isDone) {
            sleepSeconds(1)
            status = service.getStatus(scrapeStatusRequest)
        }
        println(pulsarObjectMapper().writeValueAsString(status))
        assertTrue { i > 0 }
        assertEquals(200, status.statusCode)
    }
}
