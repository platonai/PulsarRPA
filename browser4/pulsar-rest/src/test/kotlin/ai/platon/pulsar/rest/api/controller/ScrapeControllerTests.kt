package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.h2.udfs.LLMFunctions
import ai.platon.pulsar.rest.api.TestUtils
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

open class ScrapeControllerTests : ScrapeControllerTestBase() {

    /**
     * Test [ScrapeController.submitJob]
     * */
    @Test
    fun `Test extracting product list page with X-SQL sync`() {
        val pageType = "productListPage"
        val url = requireNotNull(urls[pageType])
        val sql = requireNotNull(sqlTemplates[pageType]).createSQL(url)

        val response = restTemplate.postForObject("$baseUri/api/x/e", sql, ScrapeResponse::class.java)
        println(response)
    }

    /**
     * Test [ScrapeController.submitJob]
     * */
    @Test
    fun `Test extracting product list page with X-SQL`() {
        val pageType = "productListPage"
        val url = requireNotNull(urls[pageType])
        val sql = requireNotNull(sqlTemplates[pageType]).createSQL(url)

        val uuid = restTemplate.postForObject("$baseUri/api/x/s", sql, String::class.java)
        println("UUID: $uuid")
        assertNotNull(uuid)

        await(pageType, uuid, url)
    }

    /**
     * Test [ScrapeController.submitJob]
     * Test [LLMFunctions.extract]
     * */
    @Test
    fun `Test extracting product detail page with LLM + X-SQL`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])
        val sql = requireNotNull(sqlTemplates[pageType]).createSQL(url)

        val uuid = restTemplate.postForObject("$baseUri/api/x/s", sql, String::class.java)
        println("UUID: $uuid")
        assertNotNull(uuid)

        await(pageType, uuid, url)
    }

    protected fun await(pageType: String, uuid: String, url: String) {
        var records: List<Map<String, Any?>>? = null
        var tick = 0
        val timeout = 60
        while (records == null && ++tick < timeout) {
            sleepSeconds(1)

            val response = restTemplate.getForObject("$baseUri/api/x/status?uuid=$uuid", ScrapeResponse::class.java)

            if (tick % 10 == 0) {
                println(pulsarObjectMapper().writeValueAsString(response))
            }

            if (response.isDone) {
                println("response: ")
                println(prettyPulsarObjectMapper().writeValueAsString(response))

                // If the page content bytes is less than 20KB, it means the page is not loaded
                Assumptions.assumeThat(response.pageContentBytes).isGreaterThan(20_000) // 20KB
                Assumptions.assumeThat(response.pageStatusCode).isEqualTo(200)

                records = response.resultSet
                assertNotNull(records)

                println("records: $records")

                Assumptions.assumeThat(records).isNotEmpty
            }
        }

        // wait for callback
        sleepSeconds(3)

        val response = restTemplate.getForObject("$baseUri/api/x/a/status?uuid=$uuid", ScrapeResponse::class.java)
        println("Final scrape task status: ")
        println(pulsarObjectMapper().writeValueAsString(response))

        Assumptions.assumeThat(tick).isLessThanOrEqualTo(timeout)
    }
}
