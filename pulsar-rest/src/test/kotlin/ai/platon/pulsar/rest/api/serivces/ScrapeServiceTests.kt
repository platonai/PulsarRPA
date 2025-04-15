package ai.platon.pulsar.rest.api.serivces

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ScrapeServiceTests {

    private val url = "https://www.amazon.com/b?node=1292115011"

    @Autowired
    private lateinit var service: ScrapeService

    /**
     * Execute a normal sql
     * */
    @Test
    fun `When perform 1+1 then the result is 2`() {
        val sql = "select 1+1 as sum"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        println(records)
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        assertEquals(2, records[0]["sum"].toString().toInt())
    }

    @Test
    fun `When scraping with load_and_select then the result returns synchronously`() {
        val startTime = Instant.now()

        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url -i 10d', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["uri"].toString()
        assertTrue { actualUrl == url }
        
        println("Done scraping with load_and_select, used " + DateTimes.elapsedTime(startTime))
    }

    @Test
    fun `When scrape amazon then the base uri returns asynchronously`() {
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
