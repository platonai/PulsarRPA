package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScrapeControllerTests : IntegrationTestBase() {

    val urls = mapOf(
        "amazon" to "https://www.amazon.com/b?node=1292115011",
    )

    val sqlTemplates = mapOf(
        "amazon" to """
        select
            dom_base_uri(dom) as `url`,
            str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
            dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
            dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
            dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
            dom_first_text(dom, 'h2 a') as `title`,
            dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
        from load_and_select(@url, 'div[class*=search-result]');
    """.trimIndent()
    ).entries.associate { it.key to SQLTemplate(it.value) }

    private val resourceLoaded = AtomicBoolean()

    @BeforeEach
    fun setUp() {
    }

    @BeforeEach
    fun `Ensure resources are loaded`() {
        if (!resourceLoaded.getAndSet(true)) {
            return
        }

        val url = urls["amazon"]!!
        val page = session.load(url, "-i 60s")
        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.contentLength > 0 }
        if (page.isFetched) {
            assertTrue { page.persistedContentLength > 0 }
        }
        println("Ensure loaded | $url")
    }

    @Test
    fun greetingShouldReturnDefaultMessage() {
        assertThat(
            restTemplate.getForObject("$baseUri/pulsar-system/hello", String::class.java)
        ).contains("hello")
    }

    @Test
    fun `When extract with x-sql then the result can be received asynchronously`() {
        val site = "amazon"
        val url = urls[site]!! + " -refresh"
        val sql = sqlTemplates[site]!!.createSQL(url)

        val uuid = restTemplate.postForObject("$baseUri/x/s", sql, String::class.java)
        println("UUID: $uuid")
        assertNotNull(uuid)

        await(site, uuid, url)
    }

    private fun await(site: String, uuid: String, url: String) {
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
                println(pulsarObjectMapper().writeValueAsString(response))
                assertTrue { response.pageContentBytes > 0 }
                assertTrue { response.pageStatusCode == HttpStatus.SC_OK }

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
