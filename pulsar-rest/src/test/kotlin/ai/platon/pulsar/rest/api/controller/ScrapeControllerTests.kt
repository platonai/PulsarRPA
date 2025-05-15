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

class ScrapeControllerTests : IntegrationTestBase() {

    val urls = mapOf(
        "productListPage" to "https://www.amazon.com/b?node=1292115011",
        "productDetailPage" to "https://www.amazon.com/dp/B0C1H26C46"
    )

    val sqlTemplates = mapOf(
        "productListPage" to """
        select
            dom_base_uri(dom) as `url`,
            str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
            dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
            dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
            dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
            dom_first_text(dom, 'h2 a') as `title`,
            dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
        from load_and_select(@url, 'div[class*=search-result]');
    """.trimIndent(),
        "productDetailPage" to """
            select
              llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
              dom_base_uri(dom) as url,
              dom_first_text(dom, '#productTitle') as title,
              dom_first_slim_html(dom, 'img:expr(width > 400)') as img
            from load_and_select(@url, 'body');
        """.trimIndent()
    ).entries.associate { it.key to SQLTemplate(it.value) }

    @BeforeEach
    fun setUp() {
    }

    @BeforeEach
    fun `Ensure resources are prepared`() {
        TestUtils.ensurePage(requireNotNull(urls["productListPage"]))
        TestUtils.ensurePage(requireNotNull(urls["productDetailPage"]))
    }

    @Test
    fun greetingShouldReturnDefaultMessage() {
        assertThat(
            restTemplate.getForObject("$baseUri/pulsar-system/hello", String::class.java)
        ).contains("hello")
    }

    /**
     * Test [ScrapeController.submitJob]
     * */
    @Test
    fun `Test extracting product list page with X-SQL`() {
        val pageType = "productListPage"
        val url = requireNotNull(urls[pageType])
        val sql = requireNotNull(sqlTemplates[pageType]).createSQL(url)

        val uuid = restTemplate.postForObject("$baseUri/x/s", sql, String::class.java)
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

        val uuid = restTemplate.postForObject("$baseUri/x/s", sql, String::class.java)
        println("UUID: $uuid")
        assertNotNull(uuid)

        await(pageType, uuid, url)
    }

    private fun await(pageType: String, uuid: String, url: String) {
        var records: List<Map<String, Any?>>? = null
        var tick = 0
        val timeout = 60
        while (records == null && ++tick < timeout) {
            sleepSeconds(1)

            val response = restTemplate.getForObject("$baseUri/x/status?uuid=$uuid", ScrapeResponse::class.java)

            if (tick % 10 == 0) {
                println(pulsarObjectMapper().writeValueAsString(response))
            }

            if (response.isDone) {
                println("response: ")
                println(prettyPulsarObjectMapper().writeValueAsString(response))

                // If the page content bytes is less than 1KB, it means the page is not loaded
                Assumptions.assumeThat(response.pageContentBytes).isGreaterThan(2000) // 1KB
                Assumptions.assumeThat(response.pageStatusCode).isEqualTo(200)

                records = response.resultSet
                assertNotNull(records)

                println("records: $records")

                assertTrue { records.isNotEmpty() }
            }
        }

        // wait for callback
        sleepSeconds(3)

        val response = restTemplate.getForObject("$baseUri/x/a/status?uuid=$uuid", ScrapeResponse::class.java)
        println("Final scrape task status: ")
        println(pulsarObjectMapper().writeValueAsString(response))

        assertTrue { tick < timeout }
    }
}
