package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.apache.http.HttpStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScrapeControllerTests : IntegrationTestBase() {

    val urls = mapOf(
        "amazon" to "https://www.amazon.com/s?k=\"Boys%27+Novelty+Belt+Buckles\"&rh=n:9057119011&page=1 -i 1s  -retry",
        "jd" to "https://list.jd.com/list.html?cat=9987,653,655/"
    )
    val sqlTemplates = mapOf(
        "amazon" to """
        dom_base_uri(dom) as `url`,
            str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
            dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
            dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
            dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
            dom_first_text(dom, 'h2 a') as `title`,
            dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
        from load_and_select((@url, 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
    """.trimIndent(),
        "jd" to "select dom_base_uri(dom) as url from load_and_select(@url, ':root')"
    ).entries.associate { it.key to SQLTemplate(it.value) }

    @Test
    fun `When extract using x-sql then the result can be received asynchronously`() {
        val site = "jd"
        val url = urls[site]!!
        val sql = sqlTemplates[site]!!.createInstance(url)

        val request = ScrapeRequest(sql)
        val uuid = restTemplate.postForObject("$baseUri/x/e", request, String::class.java)
        assertNotNull(uuid)

        await(site, uuid, url)
    }

    private fun await(site: String, uuid: String, url: String) {
        var records: List<Map<String, Any?>>? = null
        var tick = 0
        val timeout = 120
        while (records == null && ++tick < timeout) {
            sleepSeconds(2)

            val response = restTemplate.getForObject("$baseUri/x/status?uuid={uuid}",
                ScrapeResponse::class.java, uuid)

            if (response.statusCode == HttpStatus.SC_OK) {
                println("response: ")
                println(pulsarObjectMapper().writeValueAsString(response))
                assertTrue { response.pageContentBytes > 0 }
                assertTrue { response.pageStatusCode == HttpStatus.SC_OK }

                records = response.resultSet
                assertNotNull(records)

                println("records: $records")

                assertTrue { records.isNotEmpty() }

                if (site == "jd") {
                    assertEquals(url, records[0]["url"])
                }
            }
        }

        // wait for callback
        sleepSeconds(3)

        val response = restTemplate.getForObject("$baseUri/x/a/status?uuid={uuid}",
            ScrapeResponse::class.java, uuid)
        println("Scrape task status: ")
        println(pulsarObjectMapper().writeValueAsString(response))

        assertTrue { tick < timeout }
    }
}
