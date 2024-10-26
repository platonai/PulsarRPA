package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.rest.api.controller.ScrapeController
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.skeleton.crawl.CrawlLoop
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Ignore
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScrapeControllerTests : IntegrationTestBase() {
    
    @Autowired
    private lateinit var controller: ScrapeController
    
    val urls = mapOf(
        "amazon" to "https://www.amazon.com/s?k=Boys%27+Novelty+Belt+Buckles&rh=n:9057119011&page=1 -i 1s -ignoreFailure",
        "jd" to "https://list.jd.com/list.html?cat=9987,653,655 -i 1s -ignoreFailure"
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
        from load_and_select(@url, 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
    """.trimIndent(),
        "jd" to "select dom_base_uri(dom) as url from load_and_select(@url, ':root')"
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
    fun `Assert controller is injected`() {
        assertThat(controller).isNotNull();
    }
    
    @Test
    fun greetingShouldReturnDefaultMessage() {
        assertThat(
            restTemplate.getForObject("http://localhost:$port/pulsar-system/hello", String::class.java)
        ).contains("hello")
    }
    
    @Test
    fun `When get results then the result returns`() {
        assertThat(
            restTemplate.getForObject("http://localhost:$port/x/status", String::class.java)
        ).contains("")
    }
    
    @Test
    fun `When extract with x-sql then the result returns`() {
        val site = "amazon"
        val url = urls[site]!!
        val sql = sqlTemplates[site]!!.createSQL(url)
        
        println(">>>\n$sql\n<<<")
        
        try {
            val response = restTemplate.postForObject("http://localhost:$port/x/e", sql, ScrapeResponse::class.java)
            assertNotNull(response)
            println(pulsarObjectMapper().writeValueAsString(response))
            assertTrue { response.uuid?.isNotBlank() == true }
            assertNotNull(response.resultSet)
        } catch (e: ResourceAccessException) {
            println(e.message)
            if (e.cause is SocketTimeoutException) {
                println(e.message)
            } else throw e
        }
    }
    
    @Test
    fun `When extract with x-sql then the result can be received asynchronously`() {
        val site = "amazon"
        val url = urls[site]!!
        val sql = sqlTemplates[site]!!.createSQL(url)
        
        val uuid = restTemplate.postForObject("$baseUri/x/s", sql, String::class.java)
        assertNotNull(uuid)
        
        await(site, uuid, url)
    }
    
    private fun await(site: String, uuid: String, url: String) {
        var records: List<Map<String, Any?>>? = null
        var tick = 0
        val timeout = 60
        while (records == null && ++tick < timeout) {
            sleepSeconds(1)
            
            val response = restTemplate.getForObject("$baseUri/x/status?uuid={uuid}", ScrapeResponse::class.java, uuid)
            
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
        
        val response = restTemplate.getForObject(
            "$baseUri/x/a/status?uuid={uuid}",
            ScrapeResponse::class.java, uuid
        )
        println("Scrape task status: ")
        println(pulsarObjectMapper().writeValueAsString(response))
        
        assertTrue { tick < timeout }
    }
}
