package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.*

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ScrapeServiceTests {

    private val productListURL = "https://www.amazon.com/b?node=1292115011"

    private val productDetailURL = "https://www.amazon.com/dp/B0C1H26C46"

    @Autowired
    private lateinit var config: ImmutableConfig

    @Autowired
    private lateinit var service: ScrapeService

    @BeforeEach
    fun `Ensure resources are prepared`() {
        TestUtils.ensurePage(productListURL)
        TestUtils.ensurePage(productDetailURL)
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
        println(records)
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        assertEquals(2, records[0]["sum"].toString().toInt())
    }

    /**
     * Test [ai.platon.pulsar.ql.h2.udfs.DomFunctionTables.loadAndSelect]
     * Test [ScrapeService.executeQuery]
     * */
    @Test
    fun `test executeQuery with sql`() {
        val startTime = Instant.now()

        val sql = "select dom_base_uri(dom) as uri from load_and_select('$productListURL -i 10d', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["uri"].toString()
        assertTrue { actualUrl == productListURL }

        println("Done scraping with load_and_select, used " + DateTimes.elapsedTime(startTime))
    }

    @Test
    fun `test executeQuery with invalid sql`() {
        val sql = "select dom_base_uri(dom) as uri from load_and_select('{url}', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        assertEquals(ResourceStatus.SC_BAD_REQUEST, response.statusCode)
        assertFalse(response.isDone)
    }

    @Test
    fun `When scrape amazon then the base uri returns asynchronously`() {
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$productListURL -requireSize 1000', ':root')"
        val request = ScrapeRequest(sql)

        val uuid = service.submitJob(request).uuid

        assertTrue { uuid.isNotEmpty() }
        println(uuid)

        val scrapeStatusRequest = ScrapeStatusRequest(uuid)
        var status = service.getStatus(scrapeStatusRequest)
        var i = 60

        while (i-- > 0 && !status.isDone) {
            sleepSeconds(1)
            status = service.getStatus(scrapeStatusRequest)
        }
        Assumptions.assumeTrue(status.isDone, "Scraping is not done")
        Assumptions.assumeTrue(200 == status.statusCode, "Status code is not 200")

        println(pulsarObjectMapper().writeValueAsString(status))
        assertTrue { i > 0 }
        assertEquals(200, status.statusCode)
    }

    @Test
    fun `When scraping with LLM + X-SQL then the result returns synchronously`() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(config))

        val startTime = Instant.now()

        val sql = """
            select
              llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
              dom_base_uri(dom) as url,
              dom_first_text(dom, '#productTitle') as title,
              dom_first_slim_html(dom, 'img:expr(width > 400)') as img
            from load_and_select('$productDetailURL', 'body');
        """.trimIndent()
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        println(prettyPulsarObjectMapper().writeValueAsString(response))

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["url"].toString()
        assertTrue("URL not expected \nExpected: $productDetailURL\nActual: $actualUrl") { actualUrl == productDetailURL }

        println("Done scraping with load_and_select, used " + DateTimes.elapsedTime(startTime))
    }
}
