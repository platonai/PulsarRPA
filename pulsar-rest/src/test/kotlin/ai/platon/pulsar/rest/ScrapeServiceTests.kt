package ai.platon.pulsar.rest

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.skeleton.crawl.CrawlLoop
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.apache.commons.lang3.RandomStringUtils

import kotlin.test.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.logging.logback.LogbackLoggingSystem
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.ClassUtils
import kotlin.test.*


@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ScrapeServiceTests {
    final val username = RandomStringUtils.randomAlphanumeric(8)

    @Autowired
    private lateinit var service: ScrapeService

    @Autowired
    private lateinit var crawlLoop: CrawlLoop

    @Test
    fun testLogging() {
        val PRESENT = ClassUtils.isPresent(
            "ch.qos.logback.classic.LoggerContext",
            LogbackLoggingSystem.Factory::class.java.classLoader
        )
        println(PRESENT)
        
        getLogger(this).info("Logging system works correctly")
    }
    
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
