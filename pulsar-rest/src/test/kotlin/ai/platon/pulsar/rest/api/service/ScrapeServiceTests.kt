package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestHelper
import ai.platon.pulsar.rest.api.common.MockEcServerTestBase
import ai.platon.pulsar.rest.api.config.MockEcServerConfiguration
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@Import(MockEcServerConfiguration::class)
class ScrapeServiceTests : MockEcServerTestBase() {

    private val productListURL = "http://localhost:18182/ec/b?node=1292115012"

    private val productDetailURL = "http://localhost:18182/ec/dp/B0E000001"

    @Autowired
    private lateinit var config: ImmutableConfig

    @Autowired
    private lateinit var service: ScrapeService

    @BeforeEach
    fun `Ensure resources are prepared`() {
        super.setup() // Call parent setup to verify mock server is running
        TestHelper.ensurePage(productListURL)
        TestHelper.ensurePage(productDetailURL)
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
        logPrintln(records.toString())
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        assertEquals(2, records[0]["sum"].toString().toInt())
    }

    /**
     * Test [ai.platon.pulsar.ql.h2.udfs.DomFunctionTables.loadAndSelect]
     * Test [ScrapeService.executeQuery]
     * */
    @Test
    fun `When scraping with load_and_select then the result returns synchronously`() {
        val startTime = Instant.now()

        val sql = "select dom_base_uri(dom) as uri from load_and_select('$productListURL -i 10d', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["uri"].toString()
        assertTrue { actualUrl == productListURL }

        logPrintln("Done scraping with load_and_select, used " + DateTimes.elapsedTime(startTime))
    }

    @Test
    fun `When scrape amazon then the base uri returns asynchronously`() {
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$productListURL', ':root')"
        val request = ScrapeRequest(sql)

        val uuid = service.submitJob(request)

        assertTrue { uuid.isNotEmpty() }
        logPrintln(uuid.toString())

        val scrapeStatusRequest = ScrapeStatusRequest(uuid)
        var status = service.getStatus(scrapeStatusRequest)
        var i = 120

        while (i-- > 0 && !status.isDone) {
            sleepSeconds(1)
            status = service.getStatus(scrapeStatusRequest)
        }
        logPrintln(pulsarObjectMapper().writeValueAsString(status).toString())
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

        logPrintln(prettyPulsarObjectMapper().writeValueAsString(response).toString())

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["url"].toString()
        assertTrue("URL not expected \nExpected: $productDetailURL\nActual: $actualUrl") { actualUrl == productDetailURL }

        logPrintln("Done scraping with load_and_select, used " + DateTimes.elapsedTime(startTime))
    }
}

